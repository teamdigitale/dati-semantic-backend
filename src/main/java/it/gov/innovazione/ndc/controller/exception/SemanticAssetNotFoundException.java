package it.gov.innovazione.ndc.controller.exception;

public class SemanticAssetNotFoundException extends BaseNotFoundException {
    public SemanticAssetNotFoundException(String iri) {
        super("Semantic Asset not found for Iri : " + iri);
    }
}
