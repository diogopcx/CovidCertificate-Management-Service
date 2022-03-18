package ch.admin.bag.covidcertificate.authorization;

import ch.admin.bag.covidcertificate.authorization.config.RoleData;
import ch.admin.bag.covidcertificate.authorization.config.ServiceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api/v1/authorization")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {
    private final AuthorizationService authorizationService;

    @GetMapping("/current/{service}")
    public Set<String> getCurrent(@PathVariable String service, @RequestBody UserDto user) {
        log.info("current authorization service={} user={}", service, user);
        Set<String> result = authorizationService.getCurrent(service, user.getRoles());
        log.info("found: "+result);
        return result;
    }

    @GetMapping("/definition/{service}")
    public ServiceData getDefinition(@PathVariable String service) {
        log.info("authorization service={}", service);
        ServiceData result = authorizationService.getDefinition(service);
        log.info("found: "+result);
        return result;
    }

    @GetMapping("/role-mapping")
    public List<RoleData> getRoleMapping() {
        log.info("authorization role-mapping");
        List<RoleData> result = authorizationService.getRoleMapping();
        log.info("found: "+result);
        return result;
    }
}
