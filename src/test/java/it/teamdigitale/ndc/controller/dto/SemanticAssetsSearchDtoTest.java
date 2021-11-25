package it.teamdigitale.ndc.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import java.time.LocalDate;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;

class SemanticAssetsSearchDtoTest {

    @Test
    void shouldCreateFromSemanticAssetMetadata() {
        SemanticAssetMetadata build = SemanticAssetMetadata.builder()
            .iri("iri")
            .title("title")
            .description("description")
            .type(SemanticAssetType.ONTOLOGY)
            .rightsHolder("rightsHolder")
            .modified(LocalDate.parse("2020-01-01"))
            .theme(List.of("education", "health"))
            .build();

        SemanticAssetsSearchDto entry =
            SemanticAssetsSearchDto.from(build);

        assertThat(entry.getIri()).isEqualTo("iri");
        assertThat(entry.getType()).isEqualTo(SemanticAssetType.ONTOLOGY);
        assertThat(entry.getRightsHolder()).isEqualTo("rightsHolder");
        assertThat(entry.getModified()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(entry.getTheme()).isEqualTo(List.of("education", "health"));
        assertThat(entry.getTitle()).isEqualTo("title");
        assertThat(entry.getDescription()).isEqualTo("description");
    }
}