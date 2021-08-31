package ch.admin.bag.covidcertificate;

import ch.admin.bag.covidcertificate.api.exception.CreateCertificateError;
import ch.admin.bag.covidcertificate.api.exception.CreateCertificateException;
import ch.admin.bag.covidcertificate.api.request.CertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.CovidCertificateAddressDto;
import ch.admin.bag.covidcertificate.api.request.RecoveryCertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.RevocationDto;
import ch.admin.bag.covidcertificate.api.request.TestCertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.VaccinationCertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.VaccinationCertificateDataDto;
import ch.admin.bag.covidcertificate.api.response.CovidCertificateCreateResponseDto;
import ch.admin.bag.covidcertificate.api.valueset.CountryCode;
import ch.admin.bag.covidcertificate.api.valueset.IssuableTestDto;
import ch.admin.bag.covidcertificate.api.valueset.IssuableVaccineDto;
import ch.admin.bag.covidcertificate.api.valueset.TestType;
import ch.admin.bag.covidcertificate.domain.RapidTest;
import ch.admin.bag.covidcertificate.domain.Vaccine;
import com.flextrade.jfixture.JFixture;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static ch.admin.bag.covidcertificate.api.valueset.AcceptedLanguages.DE;

public class FixtureCustomization {
    public static void customizeIssuableVaccineDto(JFixture fixture) {
        fixture.customise().lazyInstance(IssuableVaccineDto.class, () -> new IssuableVaccineDto(
                fixture.create(String.class), fixture.create(String.class),
                fixture.create(String.class), fixture.create(String.class),
                fixture.create(String.class), fixture.create(String.class)
        ));
    }

    public static void customizeTestValueSet(JFixture fixture) {
        fixture.customise().lazyInstance(IssuableTestDto.class, () -> new IssuableTestDto(fixture.create(String.class), fixture.create(String.class), fixture.create(TestType.class)));
    }

    public static void customizeCountryCode(JFixture fixture) {
        fixture.customise().lazyInstance(CountryCode.class, () -> {
            var countryCode = new CountryCode();
            ReflectionTestUtils.setField(countryCode, "shortName", fixture.create(String.class));
            ReflectionTestUtils.setField(countryCode, "display", fixture.create(String.class));
            ReflectionTestUtils.setField(countryCode, "lang", fixture.create(String.class));
            ReflectionTestUtils.setField(countryCode, "active", fixture.create(Boolean.class));
            ReflectionTestUtils.setField(countryCode, "version", fixture.create(String.class));
            ReflectionTestUtils.setField(countryCode, "system", fixture.create(String.class));
            return countryCode;
        });
    }

    public static void customizeVaccine(JFixture fixture) {
        fixture.customise().lazyInstance(Vaccine.class, () -> {
            var vaccine = new Vaccine();
            ReflectionTestUtils.setField(vaccine, "code", fixture.create(String.class));
            ReflectionTestUtils.setField(vaccine, "display", fixture.create(String.class));
            ReflectionTestUtils.setField(vaccine, "active", fixture.create(Boolean.class));
            ReflectionTestUtils.setField(vaccine, "chIssuable", fixture.create(Boolean.class));
            ReflectionTestUtils.setField(vaccine, "modifiedAt", fixture.create(LocalDateTime.class));
            return vaccine;
        });
    }

    public static void customizeRapidTest(JFixture fixture) {
        fixture.customise().lazyInstance(RapidTest.class, () -> {
            return new RapidTest(
                    fixture.create(String.class),
                    fixture.create(String.class),
                    fixture.create(Boolean.class),
                    fixture.create(LocalDateTime.class)
            );
        });
    }

    public static void customizeVaccinationCertificateCreateDto(JFixture fixture) {
        fixture.customise().lazyInstance(VaccinationCertificateCreateDto.class, () -> {
            var helperFixture = new JFixture();
            customizeVaccinationCertificateDataDto(helperFixture);
            var vaccinationCertificateCreateDto = helperFixture.create(VaccinationCertificateCreateDto.class);
            ReflectionTestUtils.setField(vaccinationCertificateCreateDto, "language", DE);
            return vaccinationCertificateCreateDto;
        });
    }

    public static void customizeCovidCertificateAddressDto(JFixture fixture, CertificateCreateDto createDto, String field, Object value) {
        fixture.customise().lazyInstance(CovidCertificateAddressDto.class, () -> {
            var covidCertificateAddressDto = new JFixture().create(CovidCertificateAddressDto.class);
            ReflectionTestUtils.setField(covidCertificateAddressDto, field, value);
            return covidCertificateAddressDto;
        });
        ReflectionTestUtils.setField(createDto, "address", fixture.create(CovidCertificateAddressDto.class));
    }

    private static void customizeVaccinationCertificateDataDto(JFixture fixture) {
        fixture.customise().lazyInstance(VaccinationCertificateDataDto.class, () -> {
            var numberOfDoses = fixture.create(Integer.class) % 9 + 1;
            var totalNumberOfDoses = numberOfDoses + (int) Math.ceil(Math.random() * (9 - numberOfDoses));
            var vaccinationCertificateCreateDto = new JFixture().create(VaccinationCertificateDataDto.class);
            ReflectionTestUtils.setField(vaccinationCertificateCreateDto, "numberOfDoses", numberOfDoses);
            ReflectionTestUtils.setField(vaccinationCertificateCreateDto, "totalNumberOfDoses", totalNumberOfDoses);
            return vaccinationCertificateCreateDto;
        });
    }

    public static void customizeTestCertificateCreateDto(JFixture fixture) {
        fixture.customise().lazyInstance(TestCertificateCreateDto.class, () -> {
            var helperFixture = new JFixture();
            var testCertificateCreateDto = helperFixture.create(TestCertificateCreateDto.class);
            ReflectionTestUtils.setField(testCertificateCreateDto, "language", DE);
            return testCertificateCreateDto;
        });
    }

    public static void customizeRecoveryCertificateCreateDto(JFixture fixture) {
        fixture.customise().lazyInstance(RecoveryCertificateCreateDto.class, () -> {
            var helperFixture = new JFixture();
            var recoveryCertificateCreateDto = helperFixture.create(RecoveryCertificateCreateDto.class);
            ReflectionTestUtils.setField(recoveryCertificateCreateDto, "language", DE);
            return recoveryCertificateCreateDto;
        });
    }

    public static void customizeCreateCertificateException(JFixture fixture) {
        fixture.customise().lazyInstance(CreateCertificateException.class, () -> {
            var createCertificateError = fixture.create(CreateCertificateError.class);
            return new CreateCertificateException(createCertificateError);
        });
    }

    public static void customizeRevocationDto(JFixture fixture) {
        fixture.customise().lazyInstance(RevocationDto.class, () -> new RevocationDto(createUVCI()));
    }

    private static String createUVCI() {
        return "urn:uvci:01:CH:" + RandomStringUtils.randomAlphanumeric(24).toUpperCase();
    }

    public static void customizeCovidCertificateCreateResponseDto(JFixture fixture) {
        fixture.customise().lazyInstance(CovidCertificateCreateResponseDto.class, () -> {
            var helperFixture = new JFixture();
            var covidCertificateCreateResponseDto = helperFixture.create(CovidCertificateCreateResponseDto.class);
            ReflectionTestUtils.setField(covidCertificateCreateResponseDto, "appDeliveryError", null);
            return covidCertificateCreateResponseDto;
        });
    }
}
