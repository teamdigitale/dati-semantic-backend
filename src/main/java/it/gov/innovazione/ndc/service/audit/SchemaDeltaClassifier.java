package it.gov.innovazione.ndc.service.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SchemaDeltaClassifier extends BaseDeltaClassifier {

    private static final String CAT_DATASETS = "datasets";
    private static final String CAT_DISTRIBUTIONS = "distributions";
    private static final String DCATAPIT_DATASET = "http://dati.gov.it/onto/dcatapit#Dataset";
    private static final String DCATAPIT_DISTRIBUTION = "http://dati.gov.it/onto/dcatapit#Distribution";

    @Override
    public boolean supports(SemanticAssetType type) {
        return type == SemanticAssetType.SCHEMA;
    }

    @Override
    protected List<String> categoryNames() {
        return List.of(CAT_DATASETS, CAT_DISTRIBUTIONS);
    }

    @Override
    protected Map<String, Set<String>> categorize(Model model) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        result.put(CAT_DATASETS, new LinkedHashSet<>());
        result.put(CAT_DISTRIBUTIONS, new LinkedHashSet<>());

        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement st = it.nextStatement();
            Resource s = st.getSubject();
            Property p = st.getPredicate();
            RDFNode o = st.getObject();
            if (!s.isURIResource() || !RDF.type.equals(p) || !o.isURIResource()) {
                continue;
            }
            String typeIri = o.asResource().getURI();
            if (DCAT.Dataset.getURI().equals(typeIri) || DCATAPIT_DATASET.equals(typeIri)) {
                result.get(CAT_DATASETS).add(s.getURI());
            } else if (DCAT.Distribution.getURI().equals(typeIri) || DCATAPIT_DISTRIBUTION.equals(typeIri)) {
                result.get(CAT_DISTRIBUTIONS).add(s.getURI());
            }
        }
        return result;
    }
}
