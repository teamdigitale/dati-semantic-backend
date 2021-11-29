package it.teamdigitale.ndc.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
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
            .rightsHolder(
                buildNodeSummary("https://example.com/rightsHolder", "example rights holder"))
            .accrualPeriodicity("yearly")
            .distribution(
                List.of("https://example.com/distribution", "https://example.com/distribution2"))
            .subject(List.of("subject1", "subject2"))
            .contactPoint(buildNodeSummary("https://example.com/contact", "mailto:test@test.com"))
            .publisher(List.of(buildNodeSummary("http://publisher1", "publisher 1 name"),
                buildNodeSummary("http://publisher2", "publisher 2 name")))
            .creator(List.of(buildNodeSummary("http://creator1", "creator 1 name"),
                buildNodeSummary("http://creator2", "creator 2 name")))
            .versionInfo("1.0")
            .issued(LocalDate.parse("2020-01-02"))
            .language(List.of("en", "it"))
            .temporal("monthly")
            .conformsTo(List.of(buildNodeSummary("http://skos1", "skos1 name"),
                buildNodeSummary("http://skos2", "skos2 name")))
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
        assertThat(dto.getRightsHolder().getIri()).isEqualTo("https://example.com/rightsHolder");
        assertThat(dto.getRightsHolder().getSummary()).isEqualTo("example rights holder");
        assertThat(dto.getAccrualPeriodicity()).isEqualTo("yearly");
        assertThat(dto.getDistribution()).containsExactlyInAnyOrder(
            "https://example.com/distribution", "https://example.com/distribution2");
        assertThat(dto.getSubject()).containsExactlyInAnyOrder("subject1", "subject2");
        assertThat(dto.getContactPoint().getIri()).isEqualTo("https://example.com/contact");
        assertThat(dto.getContactPoint().getSummary()).isEqualTo("mailto:test@test.com");

        assertThat(dto.getPublisher()).hasSize(2);
        assertThat(dto.getPublisher().get(0).getIri()).isEqualTo("http://publisher1");
        assertThat(dto.getPublisher().get(0).getSummary()).isEqualTo("publisher 1 name");
        assertThat(dto.getPublisher().get(1).getIri()).isEqualTo("http://publisher2");
        assertThat(dto.getPublisher().get(1).getSummary()).isEqualTo("publisher 2 name");

        assertThat(dto.getCreator()).hasSize(2);
        assertThat(dto.getCreator().get(0).getIri()).isEqualTo("http://creator1");
        assertThat(dto.getCreator().get(0).getSummary()).isEqualTo("creator 1 name");
        assertThat(dto.getCreator().get(1).getIri()).isEqualTo("http://creator2");
        assertThat(dto.getCreator().get(1).getSummary()).isEqualTo("creator 2 name");

        assertThat(dto.getVersionInfo()).isEqualTo("1.0");
        assertThat(dto.getIssued()).isEqualTo("2020-01-02");
        assertThat(dto.getLanguage()).containsExactlyInAnyOrder("en", "it");
        assertThat(dto.getTemporal()).isEqualTo("monthly");

        assertThat(dto.getConformsTo()).hasSize(2);
        assertThat(dto.getConformsTo().get(0).getIri()).isEqualTo("http://skos1");
        assertThat(dto.getConformsTo().get(0).getSummary()).isEqualTo("skos1 name");
        assertThat(dto.getConformsTo().get(1).getIri()).isEqualTo("http://skos2");
        assertThat(dto.getConformsTo().get(1).getSummary()).isEqualTo("skos2 name");

        assertThat(dto.getKeyConcept()).isEqualTo("keyConcept");
        assertThat(dto.getEndpointUrl()).isEqualTo("https://example.com/endpoint");
    }

    private NodeSummary buildNodeSummary(String iri, String summary) {
        return NodeSummary.builder()
            .iri(iri)
            .summary(summary)
            .build();
    }
}