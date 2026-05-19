package it.gov.innovazione.ndc.service.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OntologyDeltaClassifier extends BaseDeltaClassifier {

    private static final String CAT_CLASSES = "classes";
    private static final String CAT_PROPERTIES = "properties";

    @Override
    public boolean supports(SemanticAssetType type) {
        return type == SemanticAssetType.ONTOLOGY;
    }

    @Override
    protected List<String> categoryNames() {
        return List.of(CAT_CLASSES, CAT_PROPERTIES);
    }

    @Override
    protected Map<String, Set<String>> categorize(Model model) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        result.put(CAT_CLASSES, new LinkedHashSet<>());
        result.put(CAT_PROPERTIES, new LinkedHashSet<>());

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
            if (OWL.Class.getURI().equals(typeIri) || RDFS.Class.getURI().equals(typeIri)) {
                result.get(CAT_CLASSES).add(s.getURI());
            } else if (OWL.DatatypeProperty.getURI().equals(typeIri)
                    || OWL.ObjectProperty.getURI().equals(typeIri)
                    || RDF.Property.getURI().equals(typeIri)
                    || OWL.AnnotationProperty.getURI().equals(typeIri)) {
                result.get(CAT_PROPERTIES).add(s.getURI());
            }
        }
        return result;
    }

    @Override
    protected Map<String, Object> extraSummaryFields(Model added, Model removed, Model tmpModel, Model onlineModel) {
        boolean isModified = !tmpModel.isEmpty() && !onlineModel.isEmpty();
        Set<String> deprecatedNow = deprecatedSubjects(added);
        Map<String, Object> deprecated = new LinkedHashMap<>();
        deprecated.put("items", isModified ? deprecatedNow : new LinkedHashSet<String>());
        deprecated.put("count", deprecatedNow.size());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("deprecated", deprecated);
        return out;
    }

    private static Set<String> deprecatedSubjects(Model model) {
        Set<String> deprecated = new LinkedHashSet<>();
        StmtIterator it = model.listStatements(null, OWL.deprecated, (RDFNode) null);
        while (it.hasNext()) {
            Statement st = it.nextStatement();
            if (!st.getSubject().isURIResource() || !st.getObject().isLiteral()) {
                continue;
            }
            try {
                if (st.getObject().asLiteral().getBoolean()) {
                    deprecated.add(st.getSubject().getURI());
                }
            } catch (Exception e) {
                // ignore non-boolean literals
            }
        }
        return deprecated;
    }
}
