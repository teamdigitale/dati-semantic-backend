package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.validator.model.ValidationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ValidationResultDtoTest {

    private ValidationResultDto validationresultdto;

    @Test
    void assertValidationOutcomeDtoFromValidationOutcome() {
        SemanticAssetModelValidationContext context = SemanticAssetModelValidationContext.builder()
                .isValidation(true)
                .errors(List.of(
                        new ValidationOutcome("field1", "message1", new RuntimeException("message1")),
                        new ValidationOutcome("field3", "message1", new RuntimeException("message1")),
                        new ValidationOutcome("field2", "message2", new RuntimeException("message2"))))
                .warnings(List.of(
                        new ValidationOutcome("field4", "message3", new RuntimeException("message3")),
                        new ValidationOutcome("field5", "message3", new RuntimeException("message3")),
                        new ValidationOutcome("field6", "message4", new RuntimeException("message4"))))
                .build();

        validationresultdto = new ValidationResultDto(context);

        assertEquals(validationresultdto.getErrors().size(), 2);
        assertEquals(validationresultdto.getWarnings().size(), 2);


    }
}
