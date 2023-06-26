package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.service.ValidationService;
import it.gov.innovazione.ndc.validator.ValidationResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationControllerTest {

    @Mock
    private ValidationService validationService;

    @Test
    void testConstructor() {
        when(validationService.validate(any(), any())).thenReturn(new ValidationResultDto(SemanticAssetModelValidationContext.NO_VALIDATION));
        ValidationController validationController = new ValidationController(validationService);
        validationController.validateFile("assetType", null);
    }
}
