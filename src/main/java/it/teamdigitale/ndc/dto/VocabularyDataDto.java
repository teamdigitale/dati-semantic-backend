package it.teamdigitale.ndc.dto;


import lombok.AllArgsConstructor;
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
    Long totalRows;
    Integer pageNumber;
    List<JSONObject> data;

    public VocabularyDataDto(Long totalRows, Integer pageNumber, List<JSONObject> data) {
        this.totalRows = totalRows;
        this.pageNumber = pageNumber + 1;
        this.data = data;
    }
}

