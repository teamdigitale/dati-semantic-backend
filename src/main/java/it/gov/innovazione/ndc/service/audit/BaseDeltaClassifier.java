package it.gov.innovazione.ndc.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Common scaffolding for all asset delta classifiers. Sub-classes provide category names,
 * a {@code categorize(model)} bucketer and optional extra top-level summary fields.
 * The output shape is identical across ADDED / REMOVED / MODIFIED: each category has
 * {@code added}/{@code removed}/{@code modified} (populated only for MODIFIED) plus a
 * {@code counts} sub-object that is always present.
 */
@Slf4j
public abstract class BaseDeltaClassifier implements AssetDeltaClassifier {

    protected static final ObjectMapper JSON = new ObjectMapper();

    protected abstract List<String> categoryNames();

    /**
     * Buckets URI subjects of the given model into the supported categories.
     * Implementations should not include blank-node subjects.
     */
    protected abstract Map<String, Set<String>> categorize(Model model);

    /**
     * Optional hook for additional top-level fields in the summary (between buckets and tripleStats).
     * Default: nothing added.
     */
    protected Map<String, Object> extraSummaryFields(Model added, Model removed, Model tmpModel, Model onlineModel) {
        return new LinkedHashMap<>();
    }

    @Override
    public Optional<String> classify(String assetIri, Model added, Model removed,
                                      Model tmpModel, Model onlineModel) {
        if (added.isEmpty() && removed.isEmpty()) {
            return Optional.empty();
        }

        boolean isModified = !tmpModel.isEmpty() && !onlineModel.isEmpty();

        Map<String, Set<String>> addedCat = categorize(added);
        Map<String, Set<String>> removedCat = categorize(removed);
        Map<String, Set<String>> tmpCat = categorize(tmpModel);
        Map<String, Set<String>> onlineCat = categorize(onlineModel);

        Map<String, Object> summary = new LinkedHashMap<>();
        for (String cat : categoryNames()) {
            Set<String> addedSet = difference(get(addedCat, cat), get(removedCat, cat));
            Set<String> removedSet = difference(get(removedCat, cat), get(addedCat, cat));
            Set<String> commonSet = intersect(get(tmpCat, cat), get(onlineCat, cat));
            List<Map<String, Object>> modifiedList = isModified
                    ? buildModified(commonSet, added, removed)
                    : List.of();

            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("added", isModified ? addedSet : List.of());
            bucket.put("removed", isModified ? removedSet : List.of());
            bucket.put("modified", modifiedList);

            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("added", addedSet.size());
            counts.put("removed", removedSet.size());
            counts.put("modified", modifiedList.size());
            bucket.put("counts", counts);

            summary.put(cat, bucket);
        }

        summary.putAll(extraSummaryFields(added, removed, tmpModel, onlineModel));

        Map<String, Object> tripleStats = new LinkedHashMap<>();
        tripleStats.put("added", added.size());
        tripleStats.put("removed", removed.size());
        summary.put("tripleStats", tripleStats);

        return Optional.of(toJson(summary));
    }

    private static List<Map<String, Object>> buildModified(Set<String> commonIris, Model added, Model removed) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String iri : commonIris) {
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

    protected static Map<String, Object> serializeTriple(Statement st) {
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

    protected static Set<String> get(Map<String, Set<String>> map, String key) {
        return map.getOrDefault(key, new LinkedHashSet<>());
    }

    protected static Set<String> intersect(Set<String> a, Set<String> b) {
        Set<String> out = new LinkedHashSet<>(a);
        out.retainAll(b);
        return out;
    }

    protected static Set<String> difference(Set<String> a, Set<String> b) {
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
}
