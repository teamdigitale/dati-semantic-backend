package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDFS;

import java.util.List;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.SCHEMA;
import static it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary.FILE_TYPE_JSON;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.keyword;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.conformsTo;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.issued;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;

public class SchemaModel extends BaseSemanticAssetModel {

    public SchemaModel(Model coreModel, String source, String repoUrl) {
        super(coreModel, source, repoUrl);
    }

    @Override
    protected String getMainResourceTypeIri() {
        return SCHEMA.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        Resource mainResource = getMainResource();
        return SemanticAssetMetadata.builder()
            .iri(mainResource.getURI())
            .repoUrl(repoUrl)
            .title(LiteralExtractor.extract(mainResource, title))
            .description(LiteralExtractor.extract(mainResource, description))
            .distributions(getDistributions())
            .rightsHolder(NodeSummaryExtractor.extractRequiredNodeSummary(mainResource, rightsHolder, FOAF.name))
            .type(SCHEMA)
            .modifiedOn(parseDate(LiteralExtractor.extractOptional(mainResource, modified)))
            .themes(asIriList(NodeExtractor.requireNodes(mainResource, theme)))
            .issuedOn(parseDate(LiteralExtractor.extractOptional(mainResource, issued)))
            .versionInfo(LiteralExtractor.extract(mainResource, versionInfo))
            .keywords(LiteralExtractor.extractAll(mainResource, keyword))
            .conformsTo(NodeSummaryExtractor.maybeNodeSummaries(mainResource, conformsTo, FOAF.name))
            .keyClasses(getKeyClasses())
            .status(LiteralExtractor.extractAll(mainResource, Admsapit.status))
            .build();
    }

    private List<NodeSummary> getKeyClasses() {
        return NodeSummaryExtractor.maybeNodeSummaries(getMainResource(), Admsapit.hasKeyClass, RDFS.label);
    }

    protected List<Distribution> getDistributions() {
        return extractDistributionsFilteredByFormat(distribution, FILE_TYPE_JSON);
    }
}
