package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.exception.InvalidFileException;
import it.gov.innovazione.ndc.controller.exception.InvalidSemanticAssetException;
import it.gov.innovazione.ndc.controller.exception.SemanticAssetGenericErrorException;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.validator.SemanticAssetValidator;
import it.gov.innovazione.ndc.validator.ValidationResultDto;
import it.gov.innovazione.ndc.validator.model.ValidationOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private static final String FILE_CONTENT_TYPE = "text/turtle";

    private final List<SemanticAssetValidator> semanticAssetValidators;

    public ValidationResultDto validate(MultipartFile file, String assetType) {

        assertContentType(file);

        SemanticAssetValidator semanticAssetValidator = semanticAssetValidators.stream()
                .filter(s -> s.getType().getDescription().equalsIgnoreCase(assetType))
                .findFirst()
                .orElseThrow(() -> new InvalidSemanticAssetException(String.format("Invalid semantic asset type: %s", assetType)));

        try (InputStream i = file.getInputStream()) {
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, i, Lang.TURTLE);

            return semanticAssetValidator.validate(model);

        } catch (IOException e) {
            log.error("Error during validation on file", e);
            throw new SemanticAssetGenericErrorException();
        } catch (RiotException e) {
            return new ValidationResultDto(
                    SemanticAssetModelValidationContext.builder()
                            .errors(List.of(new ValidationOutcome(null, e.getMessage(), e)))
                            .build());
        }
    }

    private void assertContentType(MultipartFile file) {
        Optional.of(file)
                .map(MultipartFile::getContentType)
                .filter(StringUtils::hasText)
                .filter(FILE_CONTENT_TYPE::equalsIgnoreCase)
                .orElseThrow(() -> new InvalidFileException("Only Turtle file are supported"));
    }
}
