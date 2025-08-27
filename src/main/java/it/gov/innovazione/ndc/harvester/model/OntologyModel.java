package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata.Fields;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
import static it.gov.innovazione.ndc.model.profiles.Admsapit.hasSemanticAssetDistribution;
import static it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class OntologyModel extends BaseSemanticAssetModel {

    public OntologyModel(Model coreModel, String source, String repoUrl, Instance instance) {
        super(coreModel, source, repoUrl, instance);
    }

    private OntologyModel(Model coreModel, String source, String repoUrl, SemanticAssetModelValidationContext validationContext, Instance instance) {
        super(coreModel, source, repoUrl, validationContext, instance);
    }

    public static OntologyModel forValidation(Model rdfModel, String source, String repoUrl, Instance instance) {
        return new OntologyModel(rdfModel, source, repoUrl, SemanticAssetModelValidationContext.getForValidation(), instance);
    }

    @Override
    protected String getMainResourceTypeIri() {
        return SemanticAssetType.ONTOLOGY.getTypeIri();
    }

    private static List<NodeSummary> getKeyClasses(Resource mainResource, SemanticAssetModelValidationContext validationContext) {
        return NodeSummaryExtractor.maybeNodeSummaries(mainResource, Admsapit.hasKeyClass, RDFS.label, validationContext);
    }

    private List<NodeSummary> getKeyClasses() {
        return getKeyClasses(getMainResource(), NO_VALIDATION);
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
                .type(SemanticAssetType.ONTOLOGY)
                .distributions(getDistributions())
                .keyClasses(getKeyClasses())
                .prefix(extractOptional(getMainResource(), Admsapit.prefix, validationContext))
                .projects(NodeSummaryExtractor.maybeNodeSummaries(getMainResource(), Admsapit.semanticAssetInUse,
                        createProperty("https://w3id.org/italia/onto/l0/name"), validationContext))
                .build();
    }

    @Override
    public SemanticAssetModelValidationContext validateMetadata() {
        SemanticAssetModelValidationContext superContext = super.validateMetadata();

        SemanticAssetModelValidationContext context = Stream.<Consumer<SemanticAssetModelValidationContext>>of(
                        v -> getDistributions(v.error().field(Fields.distributions)),
                        v -> getKeyClasses(getMainResource(), v.warning().field(Fields.keyClasses)),
                        v -> extractOptional(getMainResource(), Admsapit.prefix, v.warning().field(Fields.prefix)),
                        v -> maybeNodeSummaries(
                                getMainResource(),
                                Admsapit.semanticAssetInUse,
                                createProperty("https://w3id.org/italia/onto/l0/name"),
                                v.warning().field(Fields.projects)
                        )
                )
                .map(consumer -> returningValidationContext(this.validationContext, consumer))
                .reduce(SemanticAssetModelValidationContext::merge)
                .orElse(superContext);

        return SemanticAssetModelValidationContext.merge(superContext, context);
    }

    @Override
    protected List<Distribution> getDistributions(SemanticAssetModelValidationContext validationContext) {
        return extractDistributionsFilteredByFormat(getMainResource(), hasSemanticAssetDistribution, FILE_TYPE_RDF_TURTLE, validationContext);
    }
}
