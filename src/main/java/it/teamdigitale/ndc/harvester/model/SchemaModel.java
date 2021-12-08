package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.SCHEMA;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extract;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractAll;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import it.teamdigitale.ndc.harvester.model.extractors.NodeSummaryExtractor;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import static it.teamdigitale.ndc.harvester.model.vocabulary.Admsapit.hasKeyClass;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.keyword;
import static org.apache.jena.vocabulary.DCAT.theme;
import org.apache.jena.vocabulary.DCTerms;
import static org.apache.jena.vocabulary.DCTerms.conformsTo;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.issued;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;
import org.apache.jena.vocabulary.RDFS;

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
            .title(extract(mainResource, title))
            .description(extract(mainResource, description))
            .distributionUrls(getDistributionUrls())
            .rightsHolder(
                NodeSummaryExtractor.mustExtractNodeSummary(mainResource, rightsHolder, FOAF.name))
            .type(SCHEMA)
            .modifiedOn(parseDate(extractOptional(mainResource, modified)))
            .themes(asIriList(extractNodes(mainResource, theme)))
            .issuedOn(parseDate(extractOptional(mainResource, issued)))
            .versionInfo(extract(mainResource, versionInfo))
            .keywords(extractAll(mainResource, keyword))
            .conformsTo(maybeNodeSummaries(mainResource, conformsTo, FOAF.name))
            .keyClasses(getKeyClass())
            .build();
    }

    private List<NodeSummary> getKeyClass() {
        return maybeNodeSummaries(getMainResource(), hasKeyClass, RDFS.label);
    }

    private List<String> getDistributionUrls() {
        return extractNodes(getMainResource(), distribution).stream()
            .filter(node -> node.getProperty(DCTerms.format).getResource().getURI().equals("http://publications.europa.eu/resource/authority/file-type/JSON"))
            .map(node -> node.getProperty(accessURL).getResource().getURI())
            .collect(Collectors.toList());
    }
}
