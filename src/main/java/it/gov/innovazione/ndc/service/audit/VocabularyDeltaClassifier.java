package it.gov.innovazione.ndc.service.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class VocabularyDeltaClassifier extends BaseDeltaClassifier {

    private static final String CAT_CONCEPTS = "concepts";
    private static final String CAT_SCHEMES = "schemes";
    private static final String CAT_COLLECTIONS = "collections";

    @Override
    public boolean supports(SemanticAssetType type) {
        return type == SemanticAssetType.CONTROLLED_VOCABULARY;
    }

    @Override
    protected List<String> categoryNames() {
        return List.of(CAT_CONCEPTS, CAT_SCHEMES, CAT_COLLECTIONS);
    }

    @Override
    protected Map<String, Set<String>> categorize(Model model) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        result.put(CAT_CONCEPTS, new LinkedHashSet<>());
        result.put(CAT_SCHEMES, new LinkedHashSet<>());
        result.put(CAT_COLLECTIONS, new LinkedHashSet<>());

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
            if (SKOS.Concept.getURI().equals(typeIri)) {
                result.get(CAT_CONCEPTS).add(s.getURI());
            } else if (SKOS.ConceptScheme.getURI().equals(typeIri)) {
                result.get(CAT_SCHEMES).add(s.getURI());
            } else if (SKOS.Collection.getURI().equals(typeIri)
                    || SKOS.OrderedCollection.getURI().equals(typeIri)) {
                result.get(CAT_COLLECTIONS).add(s.getURI());
            }
        }
        return result;
    }
}
