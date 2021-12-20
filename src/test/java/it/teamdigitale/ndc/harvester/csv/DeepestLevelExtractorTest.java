package it.teamdigitale.ndc.harvester.csv;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeepestLevelExtractorTest {
    private final HeadersToIdNameExtractor extractor = new DeepestLevelExtractor();

    @Test
    void shouldExtractFirstLevel() {
        List<String> headerNames = List.of("codice_1_livello,label_ITA_1_livello,label_ENG_1_livello,definizione_ITA,definizione_ENG".split(","));
        assertThat(extractor.extract(headerNames)).isEqualTo("codice_1_livello");
    }

    @Test
    void shouldExtractThirdLevel() {
        List<String> headerNames = List.of("codice_1_livello,label_ITA_1_livello,codice_3_livello,definizione_ITA,codice_2_livello".split(","));
        assertThat(extractor.extract(headerNames)).isEqualTo("codice_3_livello");
    }

    @Test
    void shouldIgnoreCaseWhenComparing() {
        List<String> headerNames = List.of("codice_1_livello,label_ITA_1_livello,Codice_3_Livello,definizione_ITA,CODICE_2_LIVELLO".split(","));
        assertThat(extractor.extract(headerNames)).isEqualTo("Codice_3_Livello");
    }
}