package ch.admin.bag.covidcertificate.client.signing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class VerifySignatureRequestDto {
    private final String dataToSign;
    private final String signature;
    private final String certificateAlias;
}