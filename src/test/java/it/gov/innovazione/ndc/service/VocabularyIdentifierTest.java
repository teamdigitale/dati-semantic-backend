package it.gov.innovazione.ndc.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyIdentifierTest {
    @Test
    void canCreateWithValidValues() {
        VocabularyIdentifier vi = new VocabularyIdentifier("agid", "testfacts");
        assertThat(vi.getIndexName()).isEqualTo("agid.testfacts");
    }

    @Test
    void canCreateWithNullValues() {
        VocabularyIdentifier vi = new VocabularyIdentifier(null, null);
        assertThat(vi.getIndexName()).isEqualTo("null.null");
    }
}