package it.gov.innovazione.ndc.harvester.model.index;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;

import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

@Data
@Builder
public class NodeSummary {

    @Field(index = false, type = Keyword)
    private String iri;
    @Field(index = false, type = Text)
    private String summary;
}
