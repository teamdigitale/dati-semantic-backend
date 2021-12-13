package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
import static it.teamdigitale.ndc.model.profiles.Admsapit.hasKeyClass;
import static it.teamdigitale.ndc.model.profiles.Admsapit.hasSemanticAssetDistribution;
import static it.teamdigitale.ndc.model.profiles.Admsapit.prefix;
import static it.teamdigitale.ndc.model.profiles.Admsapit.semanticAssetInUse;
import static it.teamdigitale.ndc.model.profiles.EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
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
        return ONTOLOGY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
            .type(ONTOLOGY)
            .distributionUrls(getDistributionUrls())
            .keyClasses(getKeyClasses())
            .prefix(extractOptional(getMainResource(), prefix))
            .projects(maybeNodeSummaries(getMainResource(), semanticAssetInUse,
                createProperty("https://w3id.org/italia/onto/l0/name")))
            .build();
    }

    private List<NodeSummary> getKeyClasses() {
        return maybeNodeSummaries(getMainResource(), hasKeyClass, RDFS.label);
    }

    private List<String> getDistributionUrls() {
        return extractNodes(getMainResource(), hasSemanticAssetDistribution).stream()
            .filter(node -> Objects.nonNull(node.getProperty(DCTerms.format))
                    && node.getProperty(DCTerms.format).getResource().getURI().equals(
                FILE_TYPE_RDF_TURTLE.getURI()))
            .map(node -> node.getProperty(DCAT.accessURL).getResource().getURI())
            .collect(Collectors.toList());
    }
}
