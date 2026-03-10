package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.exception.InvalidFileException;
import it.gov.innovazione.ndc.controller.exception.SemanticAssetGenericErrorException;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    MultipartFile multipartFile;

    @Mock
    SemanticAssetValidator semanticAssetValidator;

    @Mock
    RdfSyntaxValidator rdfSyntaxValidator;

    private ValidationService validationService;

    @BeforeEach
    public void setUp() {
        validationService = new ValidationService(List.of(semanticAssetValidator), rdfSyntaxValidator);
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

    @Test
    void validateSyntaxShouldDelegateToRdfSyntaxValidator() throws Exception {
        when(multipartFile.getContentType()).thenReturn("text/turtle");

        RdfSyntaxValidationResult expectedResult = RdfSyntaxValidationResult.builder()
                .error(RdfSyntaxValidationResult.Issue.builder()
                        .line(3).col(5).message("Bad syntax").build())
                .build();
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(expectedResult);

        RdfSyntaxValidationResult result = validationService.validateSyntax(multipartFile);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).isEqualTo("Bad syntax");
        verify(rdfSyntaxValidator).validateTurtle(anyString());
    }

    @Test
    void validateSyntaxShouldRejectNonTurtleContentType() {
        when(multipartFile.getContentType()).thenReturn("text/plain");

        assertThrows(InvalidFileException.class, () -> validationService.validateSyntax(multipartFile));
    }
}
