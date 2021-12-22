package it.gov.innovazione.ndc.harvester.model;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

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
            .distributionUrls(getDistributionUrls())
            .keyClasses(getKeyClasses())
            .prefix(LiteralExtractor.extractOptional(getMainResource(), Admsapit.prefix))
            .projects(NodeSummaryExtractor.maybeNodeSummaries(getMainResource(), Admsapit.semanticAssetInUse,
                createProperty("https://w3id.org/italia/onto/l0/name")))
            .build();
    }

    private List<NodeSummary> getKeyClasses() {
        return NodeSummaryExtractor.maybeNodeSummaries(getMainResource(), Admsapit.hasKeyClass, RDFS.label);
    }

    private List<String> getDistributionUrls() {
        return NodeExtractor.extractNodes(getMainResource(), Admsapit.hasSemanticAssetDistribution).stream()
            .filter(node -> Objects.nonNull(node.getProperty(DCTerms.format))
                    && node.getProperty(DCTerms.format).getResource().getURI().equals(
                EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE.getURI()))
            .map(node -> node.getProperty(DCAT.accessURL).getResource().getURI())
            .collect(Collectors.toList());
    }
}
