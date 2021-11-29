package it.teamdigitale.ndc.harvester.model.index;

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
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "semantic-asset-metadata")
@Data
@Builder(toBuilder = true)
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

    @Field(type = Date)
    private LocalDate modified;
    @Field(type = Keyword)
    private List<String> theme;
    @Field(index = false, type = FieldType.Object)
    private NodeSummary rightsHolder;
    @Field(index = false, type = Keyword)
    private String accrualPeriodicity;
    @Field(index = false, type = Keyword)
    private List<String> distribution;
    @Field(index = false, type = Keyword)
    private List<String> subject;
    @Field(index = false, type = FieldType.Object)
    private NodeSummary contactPoint;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> publisher;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> creator;
    @Field(index = false, type = Keyword)
    private String versionInfo;
    @Field(index = false, type = Date)
    private LocalDate issued;
    @Field(index = false, type = Keyword)
    private List<String> language;
    @Field(index = false, type = Keyword)
    private String temporal;
    @Field(index = false, type = FieldType.Object)
    private List<NodeSummary> conformsTo;

    // Controlled Vocabulary Specific
    @Field(index = false, type = Keyword)
    private String keyConcept;
    @Field(index = false, type = Keyword)
    private String endpointUrl;

    //for the searchable content in multiple fields
    @JsonIgnore
    private String searchableText;
}
