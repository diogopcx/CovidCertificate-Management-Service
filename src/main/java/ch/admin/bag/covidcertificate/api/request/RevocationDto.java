package ch.admin.bag.covidcertificate.api.request;

import ch.admin.bag.covidcertificate.api.exception.RevocationException;
import ch.admin.bag.covidcertificate.util.UVCI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static ch.admin.bag.covidcertificate.api.Constants.INVALID_UVCI;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Slf4j
public class RevocationDto {
    private String uvci;
    private SystemSource systemSource;
    private String userExtId;

    public void validate() {
        if (uvci == null || !UVCI.isValid(uvci)) {
            log.info("Validate revocation for {} failed.", uvci);
            throw new RevocationException(INVALID_UVCI);
        }
    }
}
