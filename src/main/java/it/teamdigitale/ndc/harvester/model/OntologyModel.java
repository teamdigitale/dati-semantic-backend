package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import static it.teamdigitale.ndc.harvester.model.vocabulary.Admsapit.hasKeyClass;
import static it.teamdigitale.ndc.harvester.model.vocabulary.Admsapit.hasSemanticAssetDistribution;
import static it.teamdigitale.ndc.harvester.model.vocabulary.Admsapit.prefix;
import static it.teamdigitale.ndc.harvester.model.vocabulary.Admsapit.semanticAssetInUse;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import org.apache.jena.vocabulary.RDFS;

public class OntologyModel extends BaseSemanticAssetModel {

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
            .type(ONTOLOGY)
            .distributionUrls(getDistributionUrls())
            .keyClasses(getKeyClass())
            .prefix(extractOptional(getMainResource(), prefix))
            .projects(maybeNodeSummaries(getMainResource(), semanticAssetInUse, createProperty("https://w3id.org/italia/onto/l0/name")))
            .build();
    }

    private List<NodeSummary> getKeyClass() {
        return maybeNodeSummaries(getMainResource(), hasKeyClass, RDFS.label);
    }

    private List<String> getDistributionUrls() {
        return extractNodes(getMainResource(), hasSemanticAssetDistribution)
            .stream()
            .map(Resource::getURI)
            .collect(Collectors.toList());
    }
}
