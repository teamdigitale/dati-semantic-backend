package it.teamdigitale.ndc.harvester.model;

import static java.lang.String.format;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

public abstract class BaseSemanticAssetModel implements SemanticAssetModel {
    protected final Model coreModel;
    protected final String source;
    private Resource mainResource;

    public BaseSemanticAssetModel(Model coreModel, String source) {
        this.coreModel = coreModel;
        this.source = source;
    }

    @Override
    public Resource getMainResource() {
        if (mainResource == null) {
            mainResource = getUniqueResourceByType(getMainResourceTypeIri());
        }

        return mainResource;
    }

    protected abstract String getMainResourceTypeIri();

    private Resource getUniqueResourceByType(String resourceTypeIri) {
        List<Resource> resources = coreModel
            .listResourcesWithProperty(RDF.type, createResource(resourceTypeIri))
            .toList();

        checkFileDeclaresSingleResource(resources, resourceTypeIri);
        return resources.get(0);
    }

    private void checkFileDeclaresSingleResource(List<Resource> resources, String typeIri) {
        if (resources.size() == 1) {
            return;
        }

        if (resources.isEmpty()) {
            throw new InvalidAssetException(
                format("No statement for a node whose type is '%s' in '%s'", typeIri, source));
        }
        throw new InvalidAssetException(
            format(
                "Found %d statements for nodes whose type is '%s' in '%s', expecting only 1",
                resources.size(), typeIri, source));
    }

    public SemanticAssetMetadata extractMetadata() {
        Resource mainResource = getMainResource();
        return SemanticAssetMetadata.builder()
            .iri(mainResource.getURI())
            .rightsHolder(
                mainResource.getRequiredProperty(DCTerms.rightsHolder).getResource().getURI())
            .identifier(mainResource.getRequiredProperty(DCTerms.identifier).getString())
            .type(getType())
            .build();
    }

    private SemanticAssetType getType() {
        return SemanticAssetType.getByIri(getMainResourceTypeIri());
    }

}
