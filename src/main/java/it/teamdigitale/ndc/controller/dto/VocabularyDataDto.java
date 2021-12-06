package it.teamdigitale.ndc.controller.dto;


import java.awt.print.Pageable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class VocabularyDataDto {
    Long totalResults;
    Integer limit;
    Long offset;
    List<Map> data;
}

