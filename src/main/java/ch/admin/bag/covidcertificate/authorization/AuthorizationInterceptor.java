package ch.admin.bag.covidcertificate.authorization;

import ch.admin.bag.covidcertificate.api.Constants;
import ch.admin.bag.covidcertificate.api.exception.AuthorizationException;
import ch.admin.bag.covidcertificate.authorization.config.ServiceData;
import ch.admin.bag.covidcertificate.config.security.authentication.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Objects;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static net.logstash.logback.argument.StructuredArguments.kv;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final List<String> WHITELISTED_URIS = List.of(
            "/error",
            "/actuator/.*",
            "/swagger-ui.html",
            "/swagger-ui/.*",
            "/v3/api-docs/.*",
            "/api/v1/revocation-list",
            "/api/v1/ping"
    );

    private final AuthorizationService authorizationService;

    @Value("${cc-management-service.auth.allow-unauthenticated}")
    private String allowUnauthenticated;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        log.info("Call of preHandle with URI: {}", uri);
        boolean isWhitelisted = WHITELISTED_URIS
                .stream()
                .anyMatch(whitelistedUri -> whitelistedUri.matches(uri));

        if (isWhitelisted) {
            log.info("URI {} is whitelisted.", uri);
            return true;
        }

        JeapAuthenticationToken authentication = ((JeapAuthenticationToken) SecurityContextHolder
                .getContext()
                .getAuthentication());

        String clientId = authentication.getClientId();
        if (Objects.areEqual(allowUnauthenticated, clientId)) {
            log.info("Allow unauthenticated because clientId is {}", clientId);
            return true;
        }

        ServiceData.Function function = authorizationService.getDefinition("management")
                .getFunctions()
                .values()
                .stream()
                .filter(f -> StringUtils.hasText(f.getUri()))
                .filter(f -> urisAreEqual(f.getUri(), uri))
                .filter(f -> f.isBetween(LocalDateTime.now()))
                .findAny()
                .orElseThrow(() -> new AuthorizationException(Constants.NO_FUNCTION_CONFIGURED, uri));

        List<String> roles = new ArrayList<>(authentication.getUserRoles());
        Set<String> permittedFunctions = authorizationService.getCurrent("management", roles);

        log.info("Verify function authorization: {}, {}, {}",
                kv("clientId", clientId),
                kv("roles", roles),
                kv("function", function.getIdentifier()));

        if (!permittedFunctions.contains(function.getIdentifier())) {
            throw new AuthorizationException(Constants.FORBIDDEN, uri);
        }

        return true;
    }

    private boolean urisAreEqual(String uri1, String uri2) {
        String[] paths = uri1.split("/");
        String[] pathsToCompare = uri2.split("/");

        if (paths.length != pathsToCompare.length) {
            return false;
        }

        return IntStream.range(0, paths.length)
                .map(i -> paths.length - 1 - i) // reverse since uris start with /api/v1/
                .filter(i -> !paths[i].startsWith("{") && !paths[i].startsWith("}"))
                .allMatch(i -> Objects.areEqual(paths[i], pathsToCompare[i]));
    }
}