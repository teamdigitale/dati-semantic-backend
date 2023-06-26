package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.exception.InvalidFileException;
import it.gov.innovazione.ndc.controller.exception.SemanticAssetGenericErrorException;
import it.gov.innovazione.ndc.validator.SemanticAssetValidator;
import it.gov.innovazione.ndc.validator.ValidationResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    MultipartFile multipartFile;

    @Mock
    SemanticAssetValidator semanticAssetValidator;

    private ValidationService validationService;

    @BeforeEach
    public void setUp() {
        validationService = new ValidationService(List.of(semanticAssetValidator));
    }

    @Test
    void assertInvokesValidator() throws Exception {
        when(semanticAssetValidator.getType()).thenReturn(CONTROLLED_VOCABULARY);

        when(multipartFile.getContentType()).thenReturn("text/turtle");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        validationService.validate(multipartFile, "controlled vocabulary");
        verify(semanticAssetValidator, times(1)).validate(any());
    }

    @Test
    void assertThrowsExceptionWhenContentTypeNotValid() {

        when(multipartFile.getContentType()).thenReturn("text/plain");

        assertThrows(InvalidFileException.class,
                () -> validationService.validate(multipartFile, "controlled vocabulary"));
    }

    @Test
    void assertExceptionThrownWhenIOException() throws Exception {
        when(semanticAssetValidator.getType()).thenReturn(CONTROLLED_VOCABULARY);

        when(multipartFile.getContentType()).thenReturn("text/turtle");
        when(multipartFile.getInputStream()).thenThrow(new IOException());

        assertThrows(SemanticAssetGenericErrorException.class,
                () -> validationService.validate(multipartFile, "controlled vocabulary"));
    }

    @Test
    void assertWhenRiotExceptionIsThrown() throws Exception {
        when(semanticAssetValidator.getType()).thenReturn(CONTROLLED_VOCABULARY);

        when(multipartFile.getContentType()).thenReturn("text/turtle");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("wrecked".getBytes(StandardCharsets.UTF_8)));

        ValidationResultDto validationResult = validationService.validate(multipartFile, "controlled vocabulary");

        assertEquals(1, validationResult.getErrors().size());
    }
}
