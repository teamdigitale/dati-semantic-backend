package it.gov.innovazione.ndc.harvester.model.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

@Document(indexName = "semantic-asset-metadata")
@Setting(settingPath = "elasticsearch-settings.json")
@Data
@Builder(toBuilder = true)
@FieldNameConstants
public class SemanticAssetMetadata {

    @Id
    private String iri;
    @Field(type = Keyword)
    private SemanticAssetType type;
    @Field(type = Keyword)
    private String repoUrl;

    @Field(type = Text, copyTo = "searchableText")
    private String title;
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
    @Field(index = false, type = Keyword)
    private String agencyId;
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

    //for the searchable content in multiple fields
    @JsonIgnore
    private String searchableText;
}
