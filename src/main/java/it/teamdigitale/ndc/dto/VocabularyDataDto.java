package it.teamdigitale.ndc.dto;


import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class VocabularyDataDto {
    Long totalResults;
    Integer pageNumber;
    List<Map> data;

    public VocabularyDataDto(Long totalResults, Integer pageIndex, List<Map> data) {
        this.totalResults = totalResults;
        this.pageNumber = pageIndex + 1;
        this.data = data;
    }
}

