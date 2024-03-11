package it.gov.innovazione.ndc.controller;

import lombok.EqualsAndHashCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@EqualsAndHashCode
public class OffsetBasedPageRequest extends PageRequest {

    private final long offset;

    private OffsetBasedPageRequest(int offset, int limit, Sort sort) {
        super(offset / limit, limit, sort);
        this.offset = offset;
    }

    public static OffsetBasedPageRequest of(int offset, int limit) {
        return new OffsetBasedPageRequest(offset, limit, Sort.unsorted());
    }

    public static OffsetBasedPageRequest of(int offset, int limit, Sort sort) {
        return new OffsetBasedPageRequest(offset, limit, sort);
    }

    @Override
    public long getOffset() {
        return offset;
    }
}
