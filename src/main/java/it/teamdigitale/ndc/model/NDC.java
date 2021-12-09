package it.teamdigitale.ndc.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class NDC {
    private NDC() {
    }

    private static final Model model = ModelFactory.createDefaultModel();

    public static final String NS = "https://w3id.org/italia/onto/ndc-profile/";

    /**
     * A description of the services available via the endpoints, including their operations, parameters etc.
     */
    public static final Property endpointDescription = model.createProperty(NS + "endpointDescription");
    /**
     * The root location or primary endpoint of the service (a Web-resolvable IRI).
     */
    public static final Property endpointURL = model.createProperty(NS + "endpointURL");
    /**
     * Inverse property of 'serves dataset' used to specify the data services used for the datasets.
     */
    public static final Property hasDataService = model.createProperty(NS + "hasDataService");
    /**
     * A collection of data that the data service can distribute.
     */
    public static final Property servesDataset = model.createProperty(NS + "servesDataset");

    /**
     * This property allows a key concept of a dataset to be specified. Within the application profile for the national
     * data catalogue, the key concept is associated with controlled vocabularies, which are datasets, to indicate the
     * key concept they represent as reference vocabularies.
     * Example: the dataset https://w3id.org/italia/controlled-vocabulary/classifications-for-public-services/authentication-type
     * has as key concept 'authentication type'.
     */
    public static final Property keyConcept = model.createProperty(NS + "keyConcept");

    /**
     * A collection of operations that provides access to one or more datasets or data processing functions.
     */
    public static final Resource DataService = model.createResource(NS + "DataService");
}
