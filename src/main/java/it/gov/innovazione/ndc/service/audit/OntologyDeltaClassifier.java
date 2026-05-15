package it.gov.innovazione.ndc.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.extern.slf4j.Slf4j;
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
    public Optional<String> classifyModified(String assetIri, Model added, Model removed,
                                              Model tmpModel, Model onlineModel) {
        if (added.isEmpty() && removed.isEmpty()) {
            return Optional.empty();
        }

        Buckets a = bucketsOf(added);
        Buckets r = bucketsOf(removed);
        // Full sets of classes/properties known to either side (used to classify modified-only subjects)
        Buckets full = bucketsOf(tmpModel);
        Buckets onlineFull = bucketsOf(onlineModel);
        Set<String> allClasses = new LinkedHashSet<>(full.classes);
        allClasses.addAll(onlineFull.classes);
        Set<String> allProperties = new LinkedHashSet<>(full.properties);
        allProperties.addAll(onlineFull.properties);

        Set<String> classesAdded = new LinkedHashSet<>(a.classes);
        classesAdded.removeAll(r.classes);
        Set<String> classesRemoved = new LinkedHashSet<>(r.classes);
        classesRemoved.removeAll(a.classes);

        Set<String> propsAdded = new LinkedHashSet<>(a.properties);
        propsAdded.removeAll(r.properties);
        Set<String> propsRemoved = new LinkedHashSet<>(r.properties);
        propsRemoved.removeAll(a.properties);

        Set<String> labelChanged = new LinkedHashSet<>(a.labelSubjects);
        labelChanged.retainAll(r.labelSubjects);
        Set<String> commentChanged = new LinkedHashSet<>(a.commentSubjects);
        commentChanged.retainAll(r.commentSubjects);

        Set<String> deprecated = new LinkedHashSet<>(a.deprecated);

        Map<String, Object> classes = new LinkedHashMap<>();
        classes.put("added", classesAdded);
        classes.put("removed", classesRemoved);
        classes.put("modified", buildModified(labelChanged, commentChanged, classesAdded, classesRemoved, allClasses));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("added", propsAdded);
        properties.put("removed", propsRemoved);
        properties.put("modified", buildModified(labelChanged, commentChanged, propsAdded, propsRemoved, allProperties));

        Map<String, Object> tripleStats = new LinkedHashMap<>();
        tripleStats.put("added", added.size());
        tripleStats.put("removed", removed.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("classes", classes);
        summary.put("properties", properties);
        summary.put("deprecated", deprecated);
        summary.put("tripleStats", tripleStats);

        return Optional.of(toJson(summary));
    }

    @Override
    public String summarizeAdded(String assetIri, Model assetModel) {
        Buckets b = bucketsOf(assetModel);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("classesCount", b.classes.size());
        summary.put("propertiesCount", b.properties.size());
        Map<String, Object> tripleStats = new LinkedHashMap<>();
        tripleStats.put("added", assetModel.size());
        tripleStats.put("removed", 0L);
        summary.put("tripleStats", tripleStats);
        return toJson(summary);
    }

    private static java.util.List<Map<String, Object>> buildModified(
            Set<String> labelChanged, Set<String> commentChanged,
            Set<String> addedTypeSet, Set<String> removedTypeSet,
            Set<String> bucketUniverse) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.addAll(labelChanged);
        candidates.addAll(commentChanged);
        candidates.retainAll(bucketUniverse);
        candidates.removeAll(addedTypeSet);
        candidates.removeAll(removedTypeSet);

        return candidates.stream()
                .map(iri -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("iri", iri);
                    entry.put("labelChanged", labelChanged.contains(iri));
                    entry.put("commentChanged", commentChanged.contains(iri));
                    return entry;
                })
                .toList();
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
            if (RDFS.label.equals(p)) {
                b.labelSubjects.add(subjectIri);
            }
            if (RDFS.comment.equals(p)) {
                b.commentSubjects.add(subjectIri);
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
        final Set<String> labelSubjects = new LinkedHashSet<>();
        final Set<String> commentSubjects = new LinkedHashSet<>();
        final Set<String> deprecated = new LinkedHashSet<>();
    }
}
