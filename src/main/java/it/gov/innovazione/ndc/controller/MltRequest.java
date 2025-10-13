package it.gov.innovazione.ndc.controller;

import lombok.Builder;

public record MltRequest(
        String assetIri,
        int minDocFreq,
        int minTermFreq,
        int maxQueryTerms,
        String minimumShouldMatch,
        int offset,
        int limit) {
}
