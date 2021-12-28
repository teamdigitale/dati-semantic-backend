package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDFS;

import java.util.List;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.model.profiles.Admsapit.hasSemanticAssetDistribution;
import static it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class OntologyModel extends BaseSemanticAssetModel {

    public OntologyModel(Model coreModel, String source, String repoUrl) {
        super(coreModel, source, repoUrl);
    }

    @Override
    protected String getMainResourceTypeIri() {
        return SemanticAssetType.ONTOLOGY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
            .type(SemanticAssetType.ONTOLOGY)
            .distributions(getDistributions())
            .keyClasses(getKeyClasses())
            .prefix(LiteralExtractor.extractOptional(getMainResource(), Admsapit.prefix))
            .projects(NodeSummaryExtractor.maybeNodeSummaries(getMainResource(), Admsapit.semanticAssetInUse,
                createProperty("https://w3id.org/italia/onto/l0/name")))
            .build();
    }

    private List<NodeSummary> getKeyClasses() {
        return NodeSummaryExtractor.maybeNodeSummaries(getMainResource(), Admsapit.hasKeyClass, RDFS.label);
    }

    protected List<Distribution> getDistributions() {
        return extractDistributionsFilteredByFormat(hasSemanticAssetDistribution, FILE_TYPE_RDF_TURTLE);
    }
}
