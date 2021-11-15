package it.teamdigitale.ndc.dto;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import org.json.simple.JSONObject;

@Getter
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class VocabularyDataDto {
    Long totalResults;
    Integer pageNumber;
    List<JSONObject> data;

    public VocabularyDataDto(Long totalResults, Integer pageNumber, List<JSONObject> data) {
        this.totalResults = totalResults;
        this.pageNumber = pageNumber + 1;
        this.data = data;
    }
}

