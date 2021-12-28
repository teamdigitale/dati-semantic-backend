package it.gov.innovazione.ndc.harvester.model.index;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;

import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

@Data
@Builder
public class Distribution {
    @Field(index = false, type = Keyword)
    private String accessUrl;
    @Field(index = false, type = Keyword)
    private String downloadUrl;
}
