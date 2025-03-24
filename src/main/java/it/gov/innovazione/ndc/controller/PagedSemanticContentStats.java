package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PagedSemanticContentStats {
    private final List<SemanticContentStats> content;
    private final Integer page;
    private final Integer pageSize;
    private final Integer total;

    public static PagedSemanticContentStats of(List<SemanticContentStats> content, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        return new PagedSemanticContentStats(
                content.stream()
                        .skip(pageRequest.getOffset())
                        .limit(pageRequest.getPageSize())
                        .toList(),
                page,
                pageSize,
                content.size());
    }
}
