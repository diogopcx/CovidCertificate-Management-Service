package ch.admin.bag.covidcertificate.api.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CheckRevocationListResponseDto {
    private Map<String, String> uvciToErrorMessage;
    private Map<String, String> uvciToWarningMessage;
    private List<String> revocableUvcis;
}
