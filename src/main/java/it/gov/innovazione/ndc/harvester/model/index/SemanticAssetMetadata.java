package it.gov.innovazione.ndc.harvester.model.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

import static it.gov.innovazione.ndc.config.SynonymsElasticsearchIndexInitializer.INDEX_NAME;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

@Document(indexName = INDEX_NAME, createIndex = false)
@Data
@Builder(toBuilder = true)
@FieldNameConstants
public class SemanticAssetMetadata {

    @Field(type = Keyword)
    private String iri;
    @Field(type = Keyword)
    private SemanticAssetType type;
    @Field(type = Keyword)
    private String repoUrl;
    @Field(type = Keyword)
    private String instance;

    @Field(index = false, type = Keyword, normalizer = "lowercase_normalizer")
    private String title;
    @Field(type = Text, copyTo = "searchableText")
    private String rawTitle;

    @Field(type = Text, copyTo = "searchableText")
    private String description;
    @Field(type = Keyword, copyTo = "searchableText")
    private List<String> keywords;

    @Field(type = Keyword)
    private List<String> status;

    @Field(type = Date)
    private LocalDate modifiedOn;
    @Field(type = Keyword)
    private List<String> themes;
    @Field(index = false, type = FieldType.Object)
    private NodeSummary rightsHolder;
    @Field(index = false, type = Keyword)
    private String accrualPeriodicity;
    @Field(index = false, type = FieldType.Object)
    private List<Distribution> distributions;
    @Field(index = false, type = Keyword)
    private List<String> subjects;
    @Field(index = false, type = FieldType.Object)
    private NodeSummary contactPoint;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> publishers;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> creators;
    @Field(index = false, type = Keyword)
    private String versionInfo;
    @Field(index = false, type = Date)
    private LocalDate issuedOn;
    @Field(index = false, type = Keyword)
    private List<String> languages;
    @Field(index = false, type = Keyword)
    private String temporal;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> conformsTo;

    // Controlled Vocabulary Specific
    @Field(index = false, type = Keyword)
    private String keyConcept;
    @Field(type = Keyword)
    private String agencyId;

    @JsonIgnore
    @Field(type = Text, copyTo = "searchableText")
    private List<String> agencyLabel;

    @Field(index = false, type = Keyword)
    private String endpointUrl;

    //Ontology specific
    @Field(index = false, type = Keyword)
    private String prefix;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> projects;

    //Schema and Ontology specific
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> keyClasses;

    @Field(type = Text, copyTo = "searchableText")
    private List<String> keyClassesLabels;

    @Field(type = Text, copyTo = "searchableText")
    private List<String> labels;

    @Field(type = Text, copyTo = "searchableText")
    private List<String> comments;

    //for the searchable content in multiple fields
    @JsonIgnore
    private String searchableText;

    @Id
    @AccessType(AccessType.Type.PROPERTY)
    public String getElasticsearchId() {
        return iri + '-' + instance;
    }
}
