package ch.admin.bag.covidcertificate.api.request;

import ch.admin.bag.covidcertificate.api.exception.CreateCertificateException;
import lombok.*;
import org.springframework.util.StringUtils;

import static ch.admin.bag.covidcertificate.api.Constants.INVALID_ADDRESS;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CovidCertificateAddressDto {
    private String line1;
    private String line2;
    private int zipCode;
    private String city;

    public void validate() {
        if (StringUtils.hasText(line1) || StringUtils.hasText(city)) {
            throw new CreateCertificateException(INVALID_ADDRESS);
        }
        if (zipCode < 1000 || zipCode > 9999) {
            throw new CreateCertificateException(INVALID_ADDRESS);
        }
    }
}
