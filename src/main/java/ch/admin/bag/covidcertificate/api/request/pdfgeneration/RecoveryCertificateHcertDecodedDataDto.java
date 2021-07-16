package ch.admin.bag.covidcertificate.api.request.pdfgeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RecoveryCertificateHcertDecodedDataDto {
    @JsonProperty("tg")
    private String diseaseOrAgentTargeted;
    @JsonProperty("fr")
    private LocalDate dateOfFirstPositiveTestResult;
    @JsonProperty("co")
    private String countryOfTest;
    @JsonProperty("df")
    private LocalDate validFrom;
    @JsonProperty("du")
    private LocalDate validUntil;
    @JsonProperty("is")
    private String issuer;
    @JsonProperty("ci")
    private String identifier;
}
