package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.List;

import static java.lang.String.format;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

public abstract class SemanticAssetModel {
    protected final Model coreModel;
    protected final String source;
    private Resource mainResource;

    public SemanticAssetModel(Model coreModel, String source) {
        this.coreModel = coreModel;
        this.source = source;
    }

    public Resource getMainResource() {
        if (mainResource == null) {
            mainResource = getUniqueResourceByType(getMainResourceIri());
        }

        return mainResource;
    }

    protected abstract String getMainResourceIri();

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
}
