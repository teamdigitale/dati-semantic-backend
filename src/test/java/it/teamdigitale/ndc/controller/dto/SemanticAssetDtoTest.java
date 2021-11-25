package it.teamdigitale.ndc.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import java.time.LocalDate;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;

class SemanticAssetDtoTest {

    @Test
    void shouldConstructFromSemanticAssetMetadataForControlledVocabulary() {
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder()
            .iri("https://example.com/asset")
            .title("Asset")
            .description("Asset description")
            .type(SemanticAssetType.CONTROLLED_VOCABULARY)
            .modified(LocalDate.parse("2020-01-01"))
            .theme(List.of("study", "science"))
            .rightsHolder("https://example.com/rights")
            .accrualPeriodicity("yearly")
            .distribution(
                List.of("https://example.com/distribution", "https://example.com/distribution2"))
            .subject(List.of("subject1", "subject2"))
            .contactPoint("https://example.com/contact")
            .publisher(List.of("publisher1", "publisher2"))
            .creator(List.of("creator1", "creator2"))
            .versionInfo("1.0")
            .issued(LocalDate.parse("2020-01-02"))
            .language(List.of("en", "it"))
            .temporal("monthly")
            .conformsTo(
                List.of("https://example.com/conformsTo", "https://example.com/conformsTo2"))
            .keyConcept("keyConcept")
            .endpointUrl("https://example.com/endpoint")
            .build();

        SemanticAssetDetailsDto dto = SemanticAssetDetailsDto.from(metadata);

        assertThat(dto.getIri()).isEqualTo("https://example.com/asset");
        assertThat(dto.getTitle()).isEqualTo("Asset");
        assertThat(dto.getDescription()).isEqualTo("Asset description");
        assertThat(dto.getType()).isEqualTo(SemanticAssetType.CONTROLLED_VOCABULARY);
        assertThat(dto.getModified()).isEqualTo("2020-01-01");
        assertThat(dto.getTheme()).containsExactlyInAnyOrder("study", "science");
        assertThat(dto.getRightsHolder()).isEqualTo("https://example.com/rights");
        assertThat(dto.getAccrualPeriodicity()).isEqualTo("yearly");
        assertThat(dto.getDistribution()).containsExactlyInAnyOrder(
            "https://example.com/distribution", "https://example.com/distribution2");
        assertThat(dto.getSubject()).containsExactlyInAnyOrder("subject1", "subject2");
        assertThat(dto.getContactPoint()).isEqualTo("https://example.com/contact");
        assertThat(dto.getPublisher()).containsExactlyInAnyOrder("publisher1", "publisher2");
        assertThat(dto.getCreator()).containsExactlyInAnyOrder("creator1", "creator2");
        assertThat(dto.getVersionInfo()).isEqualTo("1.0");
        assertThat(dto.getIssued()).isEqualTo("2020-01-02");
        assertThat(dto.getLanguage()).containsExactlyInAnyOrder("en", "it");
        assertThat(dto.getTemporal()).isEqualTo("monthly");
        assertThat(dto.getConformsTo()).containsExactlyInAnyOrder(
            "https://example.com/conformsTo", "https://example.com/conformsTo2");
        assertThat(dto.getKeyConcept()).isEqualTo("keyConcept");
        assertThat(dto.getEndpointUrl()).isEqualTo("https://example.com/endpoint");

    }
}