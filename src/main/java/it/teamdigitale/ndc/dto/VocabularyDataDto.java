package it.teamdigitale.ndc.dto;


import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class VocabularyDataDto {
    Long totalResults;
    Integer pageNumber;
    List<Map> data;

    public VocabularyDataDto(Long totalResults, Integer pageIndex, List<Map> data) {
        this.totalResults = totalResults;
        this.pageNumber = pageIndex;
        this.data = data;
    }
}

