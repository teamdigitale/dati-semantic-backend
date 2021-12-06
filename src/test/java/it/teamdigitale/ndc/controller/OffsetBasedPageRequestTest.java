package it.teamdigitale.ndc.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class OffsetBasedPageRequestTest {

    @Test
    void shouldReturnProvidedOffset() {
        OffsetBasedPageRequest pageRequest = OffsetBasedPageRequest.of(5, 10);

        assertThat(pageRequest.getOffset()).isEqualTo(5);
        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(10);
        assertThat(pageRequest.getSort()).isEqualTo(Sort.unsorted());
    }
}