package it.teamdigitale.ndc.controller.exception;

public class SemanticAssetNotFoundException extends RuntimeException {
    public SemanticAssetNotFoundException(String iri) {
        super("Semantic Asset not found for Iri : " + iri);
    }
}
