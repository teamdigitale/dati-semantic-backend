package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class OntologyModel extends BaseSemanticAssetModel {
    private static final String ADMSAPIT_DISTRIBUTION = "https://w3id.org/italia/onto/ADMS/hasSemanticAssetDistribution";
    public static final Property ADMSAPIT_DISTRIBUTION_PROPERTY = createProperty(ADMSAPIT_DISTRIBUTION);

    public OntologyModel(Model coreModel, String source, String repoUrl) {
        super(coreModel, source, repoUrl);
    }

    @Override
    protected String getMainResourceTypeIri() {
        return ONTOLOGY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
                .distributionUrls(getDistributionUrls())
                .build();
    }

    private List<String> getDistributionUrls() {
        return extractNodes(getMainResource(), ADMSAPIT_DISTRIBUTION_PROPERTY)
                .stream()
                .map(Resource::getURI)
                .collect(Collectors.toList());
    }
}
