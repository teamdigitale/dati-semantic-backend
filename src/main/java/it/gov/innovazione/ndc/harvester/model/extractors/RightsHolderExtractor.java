package it.gov.innovazione.ndc.harvester.model.extractors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;

import java.util.Map;

import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extractAllLanguages;
import static java.lang.String.format;

public class RightsHolderExtractor {

    public static RightsHolder getAgencyId(Resource mainResource, SemanticAssetModelValidationContext validationContext) {
        Statement rightsHolder;
        try {
            rightsHolder = mainResource.getRequiredProperty(DCTerms.rightsHolder);
        } catch (Exception e) {
            InvalidModelException invalidModelException = new InvalidModelException(format("Cannot find required rightsHolder property (%s)", DCTerms.rightsHolder));
            validationContext.addValidationException(invalidModelException);
            throw invalidModelException;
        }
        Statement idProperty;
        try {
            idProperty = rightsHolder.getProperty(DCTerms.identifier);
        } catch (Exception e) {
            String rightsHolderIri = rightsHolder.getObject().toString();
            InvalidModelException invalidModelException = new InvalidModelException(format("Cannot find required id (%s) for rightsHolder '%s'", DCTerms.identifier, rightsHolderIri));
            validationContext.addValidationException(invalidModelException);
            throw invalidModelException;
        }
        Map<String, String> names = extractAllLanguages(rightsHolder.getResource(), FOAF.name);

        return RightsHolder.builder()
                .identifier(idProperty.getString())
                .name(names)
                .build();
    }

}
