package it.teamdigitale.ndc.harvester.model;

import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    @NotBlank
    private String identifier;
    @Field(index = false)
    @NotBlank
    private String iri;
    @Field(index = false)
    @NotNull
    private SemanticAssetType type;

    @Field(type = Text)
    @NotBlank
    private String title;
    @Field(type = Text)
    @NotBlank
    private String description;
    @Field(type = Keyword)
    private List<String> keywords;

    @Field(type = FieldType.Date, index = false)
    @NotNull
    private LocalDate modified;
    @Field(index = false)
    @NotBlank
    private String theme;
    @Field(index = false)
    @NotBlank
    private String rightsHolder;
    @Field(index = false)
    @NotBlank
    private String accrualPeriodicity;
    @Field(index = false)
    @NotEmpty
    private List<@NotBlank String> distribution;
    @Field(index = false)
    private List<String> subject;
    @Field(index = false)
    private String contactPoint;
    @Field(index = false)
    private String publisher;
    @Field(index = false)
    private String creator;
    @Field(index = false)
    private String versionInfo;
    @Field(index = false, type = FieldType.Date)
    private LocalDate issued;
    @Field(index = false)
    private String language;
    @Field(index = false)
    private String temporal;
    @Field(index = false)
    private String conformsTo;

    // Controlled Vocabulary Specific
    @Field(index = false)
    private String keyConcept;
}
