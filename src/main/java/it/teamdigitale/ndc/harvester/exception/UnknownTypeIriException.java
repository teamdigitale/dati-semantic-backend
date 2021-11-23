package it.teamdigitale.ndc.harvester.exception;

public class UnknownTypeIriException extends RuntimeException {
    public UnknownTypeIriException(String typeIri) {
        super("Unknown type IRI: " + typeIri);
    }
}
