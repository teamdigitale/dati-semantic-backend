package it.gov.innovazione.ndc.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Literal;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class OntologyDeltaClassifier implements AssetDeltaClassifier {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public boolean supports(SemanticAssetType type) {
        return type == SemanticAssetType.ONTOLOGY;
    }

    @Override
    public Optional<String> classify(String assetIri, Model added, Model removed,
                                      Model tmpModel, Model onlineModel) {
        if (added.isEmpty() && removed.isEmpty()) {
            return Optional.empty();
        }

        Buckets addedBuckets = bucketsOf(added);
        Buckets removedBuckets = bucketsOf(removed);
        Buckets tmpBuckets = bucketsOf(tmpModel);
        Buckets onlineBuckets = bucketsOf(onlineModel);

        Set<String> commonClasses = intersect(tmpBuckets.classes, onlineBuckets.classes);
        Set<String> commonProperties = intersect(tmpBuckets.properties, onlineBuckets.properties);

        Set<String> classesAdded = difference(addedBuckets.classes, removedBuckets.classes);
        Set<String> classesRemoved = difference(removedBuckets.classes, addedBuckets.classes);

        Set<String> propsAdded = difference(addedBuckets.properties, removedBuckets.properties);
        Set<String> propsRemoved = difference(removedBuckets.properties, addedBuckets.properties);

        List<Map<String, Object>> classesModified = buildModified(commonClasses, added, removed);
        List<Map<String, Object>> propertiesModified = buildModified(commonProperties, added, removed);

        Map<String, Object> classes = new LinkedHashMap<>();
        classes.put("added", classesAdded);
        classes.put("removed", classesRemoved);
        classes.put("modified", classesModified);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("added", propsAdded);
        properties.put("removed", propsRemoved);
        properties.put("modified", propertiesModified);

        Map<String, Object> tripleStats = new LinkedHashMap<>();
        tripleStats.put("added", added.size());
        tripleStats.put("removed", removed.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("classes", classes);
        summary.put("properties", properties);
        summary.put("deprecated", new LinkedHashSet<>(addedBuckets.deprecated));
        summary.put("tripleStats", tripleStats);

        return Optional.of(toJson(summary));
    }

    private static List<Map<String, Object>> buildModified(Set<String> commonIris, Model added, Model removed) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String iri : commonIris) {
            Resource subj = added.getResource(iri);
            List<Map<String, Object>> triplesAdded = collectTriples(added, iri);
            List<Map<String, Object>> triplesRemoved = collectTriples(removed, iri);
            if (triplesAdded.isEmpty() && triplesRemoved.isEmpty()) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("iri", iri);
            entry.put("triplesAdded", triplesAdded);
            entry.put("triplesRemoved", triplesRemoved);
            out.add(entry);
        }
        return out;
    }

    private static List<Map<String, Object>> collectTriples(Model model, String subjectIri) {
        Resource subj = model.getResource(subjectIri);
        List<Map<String, Object>> out = new ArrayList<>();
        StmtIterator it = model.listStatements(subj, null, (RDFNode) null);
        while (it.hasNext()) {
            Statement st = it.nextStatement();
            out.add(serializeTriple(st));
        }
        return out;
    }

    private static Map<String, Object> serializeTriple(Statement st) {
        Map<String, Object> triple = new LinkedHashMap<>();
        triple.put("p", st.getPredicate().getURI());
        RDFNode o = st.getObject();
        Map<String, Object> object = new LinkedHashMap<>();
        if (o.isURIResource()) {
            object.put("type", "uri");
            object.put("value", o.asResource().getURI());
        } else if (o.isLiteral()) {
            Literal lit = o.asLiteral();
            object.put("type", "literal");
            object.put("value", lit.getLexicalForm());
            String lang = lit.getLanguage();
            if (lang != null && !lang.isEmpty()) {
                object.put("lang", lang);
            } else if (lit.getDatatypeURI() != null) {
                object.put("datatype", lit.getDatatypeURI());
            }
        } else {
            object.put("type", "bnode");
            object.put("value", o.toString());
        }
        triple.put("o", object);
        return triple;
    }

    private static Buckets bucketsOf(Model model) {
        Buckets b = new Buckets();
        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement st = it.nextStatement();
            Resource s = st.getSubject();
            Property p = st.getPredicate();
            RDFNode o = st.getObject();
            if (!s.isURIResource()) {
                continue;
            }
            String subjectIri = s.getURI();

            if (RDF.type.equals(p) && o.isURIResource()) {
                String typeIri = o.asResource().getURI();
                if (OWL.Class.getURI().equals(typeIri) || RDFS.Class.getURI().equals(typeIri)) {
                    b.classes.add(subjectIri);
                } else if (OWL.DatatypeProperty.getURI().equals(typeIri)
                        || OWL.ObjectProperty.getURI().equals(typeIri)
                        || RDF.Property.getURI().equals(typeIri)
                        || OWL.AnnotationProperty.getURI().equals(typeIri)) {
                    b.properties.add(subjectIri);
                }
            }
            if (OWL.deprecated.equals(p) && o.isLiteral() && asBoolean(o)) {
                b.deprecated.add(subjectIri);
            }
        }
        return b;
    }

    private static boolean asBoolean(RDFNode o) {
        try {
            return o.asLiteral().getBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static Set<String> intersect(Set<String> a, Set<String> b) {
        Set<String> out = new LinkedHashSet<>(a);
        out.retainAll(b);
        return out;
    }

    private static Set<String> difference(Set<String> a, Set<String> b) {
        Set<String> out = new LinkedHashSet<>(a);
        out.removeAll(b);
        return out;
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize delta summary", e);
            return "{}";
        }
    }

    private static final class Buckets {
        final Set<String> classes = new LinkedHashSet<>();
        final Set<String> properties = new LinkedHashSet<>();
        final Set<String> deprecated = new LinkedHashSet<>();
    }
}
