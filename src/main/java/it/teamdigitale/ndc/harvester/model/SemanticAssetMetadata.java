package it.teamdigitale.ndc.harvester.model;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

@Document(indexName = "semantic-asset-metadata")
@Data
@Builder(toBuilder = true)
public class SemanticAssetMetadata {

    @Id
    private String identifier;
    @Field(index = false, type = Keyword)
    private String iri;
    @Field(type = Keyword)
    private SemanticAssetType type;

    @Field(type = Text, copyTo = "searchableText")
    private String title;
    @Field(type = Text, copyTo = "searchableText")
    private String description;
    @Field(type = Keyword, copyTo = "searchableText")
    private List<String> keywords;

    @Field(type = Date)
    private LocalDate modified;
    @Field(type = Keyword)
    private List<String> theme;
    @Field(index = false, type = Keyword)
    private String rightsHolder;
    @Field(index = false, type = Keyword)
    private String accrualPeriodicity;
    @Field(index = false, type = Keyword)
    private List<String> distribution;
    @Field(index = false, type = Keyword)
    private List<String> subject;
    @Field(index = false, type = Keyword)
    private String contactPoint;
    @Field(index = false, type = Keyword)
    private List<String> publisher;
    @Field(index = false, type = Keyword)
    private List<String> creator;
    @Field(index = false, type = Keyword)
    private String versionInfo;
    @Field(index = false, type = Date)
    private LocalDate issued;
    @Field(index = false, type = Keyword)
    private List<String> language;
    @Field(index = false, type = Keyword)
    private String temporal;
    @Field(index = false, type = Keyword)
    private List<String> conformsTo;

    // Controlled Vocabulary Specific
    @Field(index = false, type = Keyword)
    private String keyConcept;
    @Field(index = false, type = Keyword)
    private String endpointUrl;

    //for the searchable content in multiple fields
    @JsonIgnore
    private String searchableText;
}
