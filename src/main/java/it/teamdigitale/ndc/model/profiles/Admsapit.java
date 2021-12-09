package it.teamdigitale.ndc.model.profiles;

import org.apache.jena.rdf.model.Property;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class Admsapit {
    public static final String NS = "https://w3id.org/italia/onto/ADMS/";
    public static final Property hasSemanticAssetDistribution = createProperty(NS + "hasSemanticAssetDistribution");
    public static final Property hasKeyClass = createProperty(NS + "hasKeyClass");
    public static final Property semanticAssetInUse = createProperty(NS + "semanticAssetInUse");
    public static final Property prefix = createProperty(NS + "prefix");
    public static final Property project = createProperty(NS + "Project");
}
