package ch.admin.bag.covidcertificate.service;

import ch.admin.bag.covidcertificate.api.exception.CreateCertificateException;
import ch.admin.bag.covidcertificate.api.request.pdfgeneration.RecoveryCertificatePdfGenerateRequestDto;
import ch.admin.bag.covidcertificate.api.request.pdfgeneration.TestCertificatePdfGenerateRequestDto;
import ch.admin.bag.covidcertificate.api.request.pdfgeneration.VaccinationCertificatePdfGenerateRequestDto;
import ch.admin.bag.covidcertificate.client.inapp_delivery.InAppDeliveryClient;
import ch.admin.bag.covidcertificate.client.printing.PrintQueueClient;
import ch.admin.bag.covidcertificate.service.document.CovidPdfCertificateGenerationService;
import ch.admin.bag.covidcertificate.service.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.flextrade.jfixture.JFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import se.digg.dgc.encoding.Barcode;
import se.digg.dgc.encoding.BarcodeException;
import se.digg.dgc.encoding.impl.DefaultBarcodeCreator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static ch.admin.bag.covidcertificate.TestModelProvider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class CovidCertificateGenerationServiceTest {
    @InjectMocks
    private CovidCertificateGenerationService service;

    @Mock
    private BarcodeService barcodeService;
    @Mock
    private CovidPdfCertificateGenerationService covidPdfCertificateGenerationService;
    @Mock
    private CovidCertificateDtoMapperService covidCertificateDtoMapperService;
    @Mock
    private CovidCertificatePdfGenerateRequestDtoMapperService covidCertificatePdfGenerateRequestDtoMapperService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PrintQueueClient printQueueClient;
    @Mock
    private InAppDeliveryClient inAppDeliveryClient;

    private final JFixture fixture = new JFixture();

    @BeforeEach
    public void setUp() throws IOException {
        lenient().when(barcodeService.createBarcode(any())).thenReturn(fixture.create(Barcode.class));
        lenient().when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(fixture.create(byte[].class));

        lenient().when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(any())).thenReturn(fixture.create(VaccinationCertificateQrCode.class));
        lenient().when(covidCertificateDtoMapperService.toVaccinationCertificatePdf(any(), any())).thenReturn(fixture.create(VaccinationCertificatePdf.class));
        lenient().when(covidCertificateDtoMapperService.toTestCertificateQrCode(any())).thenReturn(fixture.create(TestCertificateQrCode.class));
        lenient().when(covidCertificateDtoMapperService.toTestCertificatePdf(any(), any())).thenReturn(fixture.create(TestCertificatePdf.class));
        lenient().when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(any())).thenReturn(fixture.create(RecoveryCertificateQrCode.class));
        lenient().when(covidCertificateDtoMapperService.toRecoveryCertificatePdf(any(), any())).thenReturn(fixture.create(RecoveryCertificatePdf.class));

        ObjectWriter objectWriter = mock(ObjectWriter.class);
        lenient().when(objectMapper.writer()).thenReturn(objectWriter);
        lenient().when(objectWriter.writeValueAsString(any())).thenReturn(fixture.create(String.class));
    }

    @Nested
    class GenerateVaccinationFromExistingCovidCertificate {
        @Test
        void shouldMapDtoToVaccinationCertificatePdf() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);
            verify(covidCertificatePdfGenerateRequestDtoMapperService).toVaccinationCertificatePdf(pdfGenerateRequestDto);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToVaccinationCertificatePdfThrowsCreateCertificateException() {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificatePdfGenerateRequestDtoMapperService.toVaccinationCertificatePdf(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class,
                    () -> service.generateFromExistingCovidCertificate(pdfGenerateRequestDto)
            );

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreatePdf_withCorrectPdfData() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var vaccinationPdf = fixture.create(VaccinationCertificatePdf.class);
            when(covidCertificatePdfGenerateRequestDtoMapperService.toVaccinationCertificatePdf(any())).thenReturn(vaccinationPdf);

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(eq(vaccinationPdf), any(), any());
        }

        @Test
        void shouldCreatePdf_withCorrectBarcode() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var barcode = new DefaultBarcodeCreator().create(pdfGenerateRequestDto.getHcert(), StandardCharsets.US_ASCII);

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(any(), eq(barcode.getPayload()), any());
        }

        @Test
        void shouldCreatePdf_withCorrectIssuedAt() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var issuedAt = getLocalDateTimeFromEpochMillis(pdfGenerateRequestDto.getIssuedAt());

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(any(), any(), eq(issuedAt));
        }

        @Test
        void shouldReturnBarcode() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var barcode = new DefaultBarcodeCreator().create(pdfGenerateRequestDto.getHcert(), StandardCharsets.US_ASCII);

            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertArrayEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(pdf);

            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(VaccinationCertificatePdfGenerateRequestDto.class);
            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertNotNull(actual.getUvci());
        }
    }

    @Nested
    class GenerateTestFromExistingCovidCertificate {
        @Test
        void shouldMapDtoToTestCertificatePdf() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);
            verify(covidCertificatePdfGenerateRequestDtoMapperService).toTestCertificatePdf(pdfGenerateRequestDto);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToTestCertificatePdfThrowsCreateCertificateException() {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificatePdfGenerateRequestDtoMapperService.toTestCertificatePdf(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class,
                    () -> service.generateFromExistingCovidCertificate(pdfGenerateRequestDto)
            );

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreatePdf_withCorrectPdfData() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var testPdf = fixture.create(TestCertificatePdf.class);
            when(covidCertificatePdfGenerateRequestDtoMapperService.toTestCertificatePdf(any())).thenReturn(testPdf);

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(eq(testPdf), any(), any());
        }

        @Test
        void shouldCreatePdf_withCorrectBarcode() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var barcode = new DefaultBarcodeCreator().create(pdfGenerateRequestDto.getHcert(), StandardCharsets.US_ASCII);

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(any(), eq(barcode.getPayload()), any());
        }

        @Test
        void shouldCreatePdf_withCorrectIssuedAt() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var issuedAt = getLocalDateTimeFromEpochMillis(pdfGenerateRequestDto.getIssuedAt());

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(any(), any(), eq(issuedAt));
        }

        @Test
        void shouldReturnBarcode() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var barcode = new DefaultBarcodeCreator().create(pdfGenerateRequestDto.getHcert(), StandardCharsets.US_ASCII);

            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertArrayEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(pdf);

            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(TestCertificatePdfGenerateRequestDto.class);
            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertNotNull(actual.getUvci());
        }
    }

    @Nested
    class GenerateRecoveryFromExistingCovidCertificate {
        @Test
        void shouldMapDtoToRecoveryCertificatePdf() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);
            verify(covidCertificatePdfGenerateRequestDtoMapperService).toRecoveryCertificatePdf(pdfGenerateRequestDto);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToRecoveryCertificatePdfThrowsCreateCertificateException() {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificatePdfGenerateRequestDtoMapperService.toRecoveryCertificatePdf(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class,
                    () -> service.generateFromExistingCovidCertificate(pdfGenerateRequestDto)
            );

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreatePdf_withCorrectPdfData() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var recoveryPdf = fixture.create(RecoveryCertificatePdf.class);
            when(covidCertificatePdfGenerateRequestDtoMapperService.toRecoveryCertificatePdf(any())).thenReturn(recoveryPdf);

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(eq(recoveryPdf), any(), any());
        }

        @Test
        void shouldCreatePdf_withCorrectBarcode() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var barcode = new DefaultBarcodeCreator().create(pdfGenerateRequestDto.getHcert(), StandardCharsets.US_ASCII);

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(any(), eq(barcode.getPayload()), any());
        }

        @Test
        void shouldCreatePdf_withCorrectIssuedAt() throws BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var issuedAt = getLocalDateTimeFromEpochMillis(pdfGenerateRequestDto.getIssuedAt());

            service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(any(), any(), eq(issuedAt));
        }

        @Test
        void shouldReturnBarcode() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var barcode = new DefaultBarcodeCreator().create(pdfGenerateRequestDto.getHcert(), StandardCharsets.US_ASCII);

            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertArrayEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(pdf);

            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException, BarcodeException {
            var pdfGenerateRequestDto = fixture.create(RecoveryCertificatePdfGenerateRequestDto.class);
            var actual = service.generateFromExistingCovidCertificate(pdfGenerateRequestDto);

            assertNotNull(actual.getUvci());
        }
    }
    
    @Nested
    class GenerateVaccinationCovidCertificate {
        @Test
        void shouldMapDtoToVaccinationCertificateQrCode() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toVaccinationCertificateQrCode(createDto);
        }

        @Test
        void shouldMapDtoToVaccinationCertificatePdf() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var qrCodeData = fixture.create(VaccinationCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(createDto)).thenReturn(qrCodeData);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toVaccinationCertificatePdf(createDto, qrCodeData);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToVaccinationCertificateQrCodeThrowsCreateCertificateException() {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void throwsInvalidCountryOfVaccination_ifMapDtoToVaccinationCertificatePdfThrowsCreateCertificateException() {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificatePdf(any(), any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreateBarcode() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var qrCodeData = fixture.create(VaccinationCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(createDto)).thenReturn(qrCodeData);
            var contents = fixture.create(String.class);
            var objectWriter = mock(ObjectWriter.class);
            when(objectMapper.writer()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(qrCodeData)).thenReturn(contents);

            service.generateCovidCertificate(createDto);

            verify(barcodeService).createBarcode(contents);
        }

        @Test
        void shouldCreatePdf() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var vaccinationPdf = fixture.create(VaccinationCertificatePdf.class);
            var barcode = fixture.create(Barcode.class);
            var now = LocalDateTime.now();
            when(covidCertificateDtoMapperService.toVaccinationCertificatePdf(any(), any())).thenReturn(vaccinationPdf);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
                localDateTimeMock.when(LocalDateTime::now).thenReturn(now);
                service.generateCovidCertificate(createDto);

                verify(covidPdfCertificateGenerationService).generateCovidCertificate(vaccinationPdf, barcode.getPayload(), now);
            }
        }

        @Test
        void shouldReturnBarcode() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var barcode = fixture.create(Barcode.class);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(pdf);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");

            var actual = service.generateCovidCertificate(createDto);

            assertNotNull(actual.getUvci());
        }

        @Test
        void shouldSendInAppDelivery__whenCodeIsPassed() {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de", "BITBITBIT");

            assertDoesNotThrow(() -> service.generateCovidCertificate(createDto));
            verify(inAppDeliveryClient, times(1)).deliverToApp(any());
        }

        @Test
        void shouldCallPrintingService__whenAddressPassed() {
            var createDto = getVaccinationCertificateCreateDto("EU/1/20/1507", "de");

            assertDoesNotThrow(() -> service.generateCovidCertificate(createDto));
            verify(printQueueClient, times(1)).sendPrintJob(any());
        }
    }

    @Nested
    class GenerateTestCovidCertificate {
        @Test
        void shouldMapDtoToTestCertificateQrCode() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toTestCertificateQrCode(createDto);
        }

        @Test
        void shouldMapDtoToTestCertificatePdf() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var qrCodeData = fixture.create(TestCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toTestCertificateQrCode(createDto)).thenReturn(qrCodeData);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toTestCertificatePdf(createDto, qrCodeData);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToTestCertificateQrCodeThrowsCreateCertificateException() {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toTestCertificateQrCode(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void throwsInvalidCountryOfTest_ifMapDtoToTestCertificatePdfThrowsCreateCertificateException() {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toTestCertificatePdf(any(), any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreateBarcode() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var qrCodeData = fixture.create(TestCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toTestCertificateQrCode(createDto)).thenReturn(qrCodeData);
            var contents = fixture.create(String.class);
            var objectWriter = mock(ObjectWriter.class);
            when(objectMapper.writer()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(qrCodeData)).thenReturn(contents);

            service.generateCovidCertificate(createDto);

            verify(barcodeService).createBarcode(contents);
        }

        @Test
        void shouldCreatePdf() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var TestPdf = fixture.create(TestCertificatePdf.class);
            var barcode = fixture.create(Barcode.class);
            var now = LocalDateTime.now();
            when(covidCertificateDtoMapperService.toTestCertificatePdf(any(), any())).thenReturn(TestPdf);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
                localDateTimeMock.when(LocalDateTime::now).thenReturn(now);

                service.generateCovidCertificate(createDto);

                verify(covidPdfCertificateGenerationService).generateCovidCertificate(TestPdf, barcode.getPayload(), LocalDateTime.now());
            }
        }

        @Test
        void shouldReturnBarcode() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var barcode = fixture.create(Barcode.class);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(pdf);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");

            var actual = service.generateCovidCertificate(createDto);

            assertNotNull(actual.getUvci());
        }

        @Test
        void shouldSendInAppDelivery__whenCodeIsPassed() {
            var createDto = getTestCertificateCreateDto(null, "1833", "de", "BITBITBIT");

            assertDoesNotThrow(() -> service.generateCovidCertificate(createDto));
            verify(inAppDeliveryClient, times(1)).deliverToApp(any());
        }

        @Test
        void shouldCallPrintingService__whenAddressPassed() {
            var createDto = getTestCertificateCreateDto(null, "1833", "de");

            assertDoesNotThrow(() -> service.generateCovidCertificate(createDto));
            verify(printQueueClient, times(1)).sendPrintJob(any());
        }
    }

    @Nested
    class GenerateRecoveryCovidCertificate {
        @Test
        void shouldMapDtoToRecoveryCertificateQrCode() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toRecoveryCertificateQrCode(createDto);
        }

        @Test
        void shouldMapDtoToRecoveryCertificatePdf() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");
            var qrCodeData = fixture.create(RecoveryCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(createDto)).thenReturn(qrCodeData);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toRecoveryCertificatePdf(createDto, qrCodeData);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToRecoveryCertificateQrCodeThrowsCreateCertificateException() {
            var createDto = getRecoveryCertificateCreateDto("de");
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void throwsInvalidCountryOfRecovery_ifMapDtoToRecoveryCertificatePdfThrowsCreateCertificateException() {
            var createDto = getRecoveryCertificateCreateDto("de");
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificatePdf(any(), any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreateBarcode() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");
            var qrCodeData = fixture.create(RecoveryCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(createDto)).thenReturn(qrCodeData);
            var contents = fixture.create(String.class);
            var objectWriter = mock(ObjectWriter.class);
            when(objectMapper.writer()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(qrCodeData)).thenReturn(contents);

            service.generateCovidCertificate(createDto);

            verify(barcodeService).createBarcode(contents);
        }

        @Test
        void shouldCreatePdf() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");
            var RecoveryPdf = fixture.create(RecoveryCertificatePdf.class);
            var barcode = fixture.create(Barcode.class);
            var now = LocalDateTime.now();
            when(covidCertificateDtoMapperService.toRecoveryCertificatePdf(any(), any())).thenReturn(RecoveryPdf);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
                localDateTimeMock.when(LocalDateTime::now).thenReturn(now);

                service.generateCovidCertificate(createDto);

                verify(covidPdfCertificateGenerationService).generateCovidCertificate(RecoveryPdf, barcode.getPayload(), now);
            }
        }

        @Test
        void shouldReturnBarcode() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");
            var barcode = fixture.create(Barcode.class);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any(), any())).thenReturn(pdf);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException {
            var createDto = getRecoveryCertificateCreateDto("de");

            var actual = service.generateCovidCertificate(createDto);

            assertNotNull(actual.getUvci());
        }

        @Test
        void shouldSendInAppDelivery__whenCodeIsPassed() {
            var createDto = getRecoveryCertificateCreateDto("de", "BITBITBIT");

            assertDoesNotThrow(() -> service.generateCovidCertificate(createDto));
            verify(inAppDeliveryClient, times(1)).deliverToApp(any());
        }

        @Test
        void shouldCallPrintingService__whenAddressPassed() {
            var createDto = getRecoveryCertificateCreateDto("de");

            assertDoesNotThrow(() -> service.generateCovidCertificate(createDto));
            verify(printQueueClient, times(1)).sendPrintJob(any());
        }
    }

    LocalDateTime getLocalDateTimeFromEpochMillis(long millis) {
        var instant = Instant.ofEpochMilli(millis);
        return ZonedDateTime.from(instant.atZone(ZoneId.systemDefault())).toLocalDateTime();
    }
}
