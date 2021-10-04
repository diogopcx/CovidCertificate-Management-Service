package ch.admin.bag.covidcertificate.service;

import ch.admin.bag.covidcertificate.api.exception.CreateCertificateException;
import ch.admin.bag.covidcertificate.api.request.RecoveryCertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.VaccinationCertificateCreateDto;
import ch.admin.bag.covidcertificate.domain.SigningInformation;
import ch.admin.bag.covidcertificate.domain.SigningInformationRepository;
import ch.admin.bag.covidcertificate.service.domain.SigningCertificateCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static ch.admin.bag.covidcertificate.api.Constants.AMBIGUOUS_SIGNING_CERTIFICATE;
import static ch.admin.bag.covidcertificate.api.Constants.SIGNING_CERTIFICATE_MISSING;

@Service
@RequiredArgsConstructor
@Slf4j
public class SigningInformationService {
    private final SigningInformationRepository signingInformationRepository;

    public SigningInformation getVaccinationSigningInformation(VaccinationCertificateCreateDto createDto){
        var medicinalProductCode = createDto.getVaccinationInfo().get(0).getMedicinalProductCode();
        var signingInformation =  signingInformationRepository.findSigningInformation(SigningCertificateCategory.VACCINATION.value, medicinalProductCode);

        if(signingInformation == null){
            log.error("No signing certificate was found to sign the certificate for the {} vaccine.", medicinalProductCode);
            throw new CreateCertificateException(SIGNING_CERTIFICATE_MISSING);
        }
        return signingInformation;
    }

    public SigningInformation getTestSigningInformation(){
        var signingInformationList = signingInformationRepository.findSigningInformation(SigningCertificateCategory.TEST.value);
        if(signingInformationList == null || signingInformationList.isEmpty()){
            log.error("No signing certificate was found to sign the test certificate.");
            throw new CreateCertificateException(SIGNING_CERTIFICATE_MISSING);
        }else if(signingInformationList.size() > 1){
            log.error("Ambiguous signing certificate. Multiple signing certificates were found to sign the test certificate");
            throw new CreateCertificateException(AMBIGUOUS_SIGNING_CERTIFICATE);
        }
        return signingInformationList.get(0);
    }

    public SigningInformation getRecoverySigningInformation(RecoveryCertificateCreateDto createDto){
        var countryOfTest = createDto.getRecoveryInfo().get(0).getCountryOfTest();
        var signingCertificateCategory = SigningCertificateCategory.RECOVERY_NON_CH.value;
        if(Objects.equals(countryOfTest, "CH")) {
            signingCertificateCategory = SigningCertificateCategory.RECOVERY_CH.value;
        }
        var signingInformationList = signingInformationRepository.findSigningInformation(signingCertificateCategory);

        if(signingInformationList == null || signingInformationList.isEmpty()){
            log.error("No signing certificate was found to sign the recovery certificate for positive test in {}.", countryOfTest);
            throw new CreateCertificateException(SIGNING_CERTIFICATE_MISSING);
        }else if(signingInformationList.size() > 1){
            log.error("Ambiguous signing certificate. Multiple signing certificates were found for positive test in {}.", countryOfTest);
            throw new CreateCertificateException(AMBIGUOUS_SIGNING_CERTIFICATE);
        }
        return signingInformationList.get(0);
    }
}
