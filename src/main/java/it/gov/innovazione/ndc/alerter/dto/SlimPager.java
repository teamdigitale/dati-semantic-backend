package it.gov.innovazione.ndc.alerter.dto;

import it.gov.innovazione.ndc.alerter.entities.Nameable;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@RequiredArgsConstructor(staticName = "of")
public class SlimPager<D extends Nameable> {
    private final List<D> content;
    private final PageInfo pageInfo;

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class PageInfo {
        private final List<SlimOrder> sort;
        private final int pageNumber;
        private final int pageSize;
        private final int totalPages;
        private final long totalElements;
        private final boolean first;
        private final boolean last;
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class SlimOrder {
        private final String property;
        private final Sort.Direction direction;
    }
}
