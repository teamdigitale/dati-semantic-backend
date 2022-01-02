package it.gov.innovazione.ndc.model;

import it.gov.innovazione.ndc.gen.dto.AssetType;
import it.gov.innovazione.ndc.gen.dto.Distribution;
import it.gov.innovazione.ndc.gen.dto.SearchResultItem;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.gen.dto.Theme;
import it.gov.innovazione.ndc.gen.dto.VocabulariesResult;
import it.gov.innovazione.ndc.gen.dto.VocabularySummary;
import it.gov.innovazione.ndc.gen.dto.VocabularySummaryLinks;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticAssetsMetadataMapperTest {
    private SemanticAssetsMetadataMapper mapper = new SemanticAssetsMetadataMapperImpl();

    @Test
    void shouldConstructFromSemanticAssetMetadataForControlledVocabulary() {
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder()
                .iri("https://example.com/asset")
                .title("Asset")
                .description("Asset description")
                .type(SemanticAssetType.CONTROLLED_VOCABULARY)
                .modifiedOn(LocalDate.parse("2020-01-01"))
                .themes(List.of("http://publications.europa.eu/resource/authority/data-theme/ECON",
                        "http://publications.europa.eu/resource/authority/data-theme/ENVI"))
                .rightsHolder(
                        buildNodeSummary("https://example.com/rightsHolder", "example rights holder"))
                .accrualPeriodicity("yearly")
                .distributions(List.of(
                        it.gov.innovazione.ndc.harvester.model.index.Distribution.builder()
                                .accessUrl("https://example.com/schemas/myschema")
                                .downloadUrl("https://example.com/schemas/myschema.csv")
                                .build(),
                        it.gov.innovazione.ndc.harvester.model.index.Distribution.builder()
                                .accessUrl(null)
                                .downloadUrl("https://example.com/schemas/myschema2.csv").build()
                        ))
                .subjects(List.of("subject1", "subject2"))
                .contactPoint(buildNodeSummary("https://example.com/contact", "mailto:test@test.com"))
                .publishers(List.of(buildNodeSummary("http://publisher1", "publisher 1 name"),
                        buildNodeSummary("http://publisher2", "publisher 2 name")))
                .creators(List.of(buildNodeSummary("http://creator1", "creator 1 name"),
                        buildNodeSummary("http://creator2", "creator 2 name")))
                .versionInfo("1.0")
                .issuedOn(LocalDate.parse("2020-01-02"))
                .languages(List.of("en", "it"))
                .temporal("monthly")
                .conformsTo(List.of(buildNodeSummary("http://skos1", "skos1 name"),
                        buildNodeSummary("http://skos2", "skos2 name")))
                .agencyId("agid")
                .keyConcept("keyConcept")
                .endpointUrl("https://example.com/endpoint")
                .keyClasses(List.of(buildNodeSummary("http://Class1", "Class1"),
                        buildNodeSummary("http://Class2", "Class2")))
                .prefix("prefix")
                .projects(List.of(buildNodeSummary("http://project1", "project1"),
                        buildNodeSummary("http://project2", "project2")))
                .build();

        SemanticAssetDetails dto = mapper.detailsToDto(metadata);

        assertThat(dto.getAssetIri()).isEqualTo("https://example.com/asset");
        assertThat(dto.getTitle()).isEqualTo("Asset");
        assertThat(dto.getDescription()).isEqualTo("Asset description");
        assertThat(dto.getType()).isEqualTo(AssetType.CONTROLLED_VOCABULARY);
        assertThat(dto.getModifiedOn()).isEqualTo("2020-01-01");
        Assertions.assertThat(dto.getThemes()).containsExactlyInAnyOrder(Theme.ECON, Theme.ENVI);
        assertThat(dto.getRightsHolder().getIri()).isEqualTo("https://example.com/rightsHolder");
        assertThat(dto.getRightsHolder().getSummary()).isEqualTo("example rights holder");
        assertThat(dto.getAccrualPeriodicity()).isEqualTo("yearly");
        List<Distribution> distributions = dto.getDistributions();
        assertThat(distributions).hasSize(2);
        assertThat(distributions.get(0).getAccessUrl().toString()).isEqualTo("https://example.com/schemas/myschema");
        assertThat(distributions.get(0).getDownloadUrl().toString()).isEqualTo("https://example.com/schemas/myschema.csv");
        assertThat(distributions.get(1).getAccessUrl()).isNull();
        assertThat(distributions.get(1).getDownloadUrl().toString()).isEqualTo("https://example.com/schemas/myschema2.csv");
        assertThat(dto.getSubjects()).containsExactlyInAnyOrder("subject1", "subject2");
        assertThat(dto.getContactPoint().getIri()).isEqualTo("https://example.com/contact");
        assertThat(dto.getContactPoint().getSummary()).isEqualTo("mailto:test@test.com");

        Assertions.assertThat(dto.getPublishers()).hasSize(2);
        assertThat(dto.getPublishers().get(0).getIri()).isEqualTo("http://publisher1");
        assertThat(dto.getPublishers().get(0).getSummary()).isEqualTo("publisher 1 name");
        assertThat(dto.getPublishers().get(1).getIri()).isEqualTo("http://publisher2");
        assertThat(dto.getPublishers().get(1).getSummary()).isEqualTo("publisher 2 name");

        Assertions.assertThat(dto.getCreators()).hasSize(2);
        assertThat(dto.getCreators().get(0).getIri()).isEqualTo("http://creator1");
        assertThat(dto.getCreators().get(0).getSummary()).isEqualTo("creator 1 name");
        assertThat(dto.getCreators().get(1).getIri()).isEqualTo("http://creator2");
        assertThat(dto.getCreators().get(1).getSummary()).isEqualTo("creator 2 name");

        assertThat(dto.getVersionInfo()).isEqualTo("1.0");
        assertThat(dto.getIssuedOn()).isEqualTo("2020-01-02");
        assertThat(dto.getLanguages()).containsExactlyInAnyOrder("en", "it");
        assertThat(dto.getTemporal()).isEqualTo("monthly");

        Assertions.assertThat(dto.getConformsTo()).hasSize(2);
        assertThat(dto.getConformsTo().get(0).getIri()).isEqualTo("http://skos1");
        assertThat(dto.getConformsTo().get(0).getSummary()).isEqualTo("skos1 name");
        assertThat(dto.getConformsTo().get(1).getIri()).isEqualTo("http://skos2");
        assertThat(dto.getConformsTo().get(1).getSummary()).isEqualTo("skos2 name");

        assertThat(dto.getAgencyId()).isEqualTo("agid");
        assertThat(dto.getKeyConcept()).isEqualTo("keyConcept");
        assertThat(dto.getEndpointUrl()).isEqualTo("https://example.com/endpoint");

        Assertions.assertThat(dto.getKeyClasses()).hasSize(2);
        assertThat(dto.getKeyClasses().get(0).getIri()).isEqualTo("http://Class1");
        assertThat(dto.getKeyClasses().get(0).getSummary()).isEqualTo("Class1");
        assertThat(dto.getKeyClasses().get(1).getIri()).isEqualTo("http://Class2");
        assertThat(dto.getKeyClasses().get(1).getSummary()).isEqualTo("Class2");

        assertThat(dto.getPrefix()).isEqualTo("prefix");

        Assertions.assertThat(dto.getProjects()).hasSize(2);
        assertThat(dto.getProjects().get(0).getIri()).isEqualTo("http://project1");
        assertThat(dto.getProjects().get(0).getSummary()).isEqualTo("project1");
        assertThat(dto.getProjects().get(1).getIri()).isEqualTo("http://project2");
        assertThat(dto.getProjects().get(1).getSummary()).isEqualTo("project2");
    }

    private NodeSummary buildNodeSummary(String iri, String summary) {
        return NodeSummary.builder()
                .iri(iri)
                .summary(summary)
                .build();
    }

    @Test
    void shouldBuildSearchResultFromSemanticAssetMetadata() {
        SemanticAssetMetadata model = SemanticAssetMetadata.builder()
                .iri("iri")
                .title("title")
                .description("description")
                .type(SemanticAssetType.ONTOLOGY)
                .rightsHolder(NodeSummary.builder()
                        .iri("http://rightsHolder")
                        .summary("rights holder name")
                        .build())
                .modifiedOn(LocalDate.parse("2020-01-01"))
                .themes(List.of("http://publications.europa.eu/resource/authority/data-theme/EDUC",
                        "http://publications.europa.eu/resource/authority/data-theme/HEAL"))
                .versionInfo("versionInfo")
                .build();

        SearchResultItem dto = mapper.resultItemToDto(model);

        assertThat(dto.getAssetIri()).isEqualTo("iri");
        assertThat(dto.getType()).isEqualTo(AssetType.ONTOLOGY);
        assertThat(dto.getRightsHolder().getIri()).isEqualTo("http://rightsHolder");
        assertThat(dto.getRightsHolder().getSummary()).isEqualTo("rights holder name");
        assertThat(dto.getModifiedOn()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(dto.getThemes()).containsAll(List.of(Theme.EDUC, Theme.HEAL));
        assertThat(dto.getTitle()).isEqualTo("title");
        assertThat(dto.getDescription()).isEqualTo("description");
        assertThat(dto.getVersionInfo()).isEqualTo("versionInfo");
    }

    @Test
    void shouldBuildVocabulariesResultFromSemanticAssetMetadata() {
        SemanticAssetMetadata model = SemanticAssetMetadata.builder()
                .iri("iri")
                .title("title")
                .description("description")
                .type(SemanticAssetType.CONTROLLED_VOCABULARY)
                .rightsHolder(NodeSummary.builder()
                        .iri("http://rightsHolder")
                        .summary("rights holder name")
                        .build())
                .modifiedOn(LocalDate.parse("2020-01-01"))
                .themes(List.of("http://publications.europa.eu/resource/authority/data-theme/EDUC",
                        "http://publications.europa.eu/resource/authority/data-theme/HEAL"))
                .versionInfo("versionInfo")
                .agencyId("agid")
                .keyConcept("interesting-stuff")
                .endpointUrl("https://www.disney.com")
                .build();

        VocabularySummary dto = mapper.vocabularySummaryToDto(model);

        assertThat(dto.getTitle()).isEqualTo("title");
        assertThat(dto.getDescription()).isEqualTo("description");
        assertThat(dto.getAgencyId()).isEqualTo("agid");
        assertThat(dto.getKeyConcept()).isEqualTo("interesting-stuff");
        List<VocabularySummaryLinks> links = dto.getLinks();
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getHref()).isEqualTo("https://www.disney.com");
    }

    @Test
    void shouldIgnoreUnexpectedThemes() {
        String nonExistentTheme = "http://publications.europa.eu/resource/authority/data-theme/TRANS";
        String validTheme = "http://publications.europa.eu/resource/authority/data-theme/EDUC";
        SemanticAssetMetadata model = SemanticAssetMetadata.builder()
                .iri("iri")
                .themes(List.of(validTheme, nonExistentTheme))
                .build();

        SearchResultItem dto = mapper.resultItemToDto(model);
        assertThat(dto.getThemes()).hasSize(1);
        assertThat(dto.getThemes()).contains(Theme.EDUC);
    }

    @Test
    void shouldPropagateMissingThemes() {
        SemanticAssetMetadata model = SemanticAssetMetadata.builder()
                .iri("iri")
                .themes(null)
                .build();

        SearchResultItem dto = mapper.resultItemToDto(model);
        assertThat(dto.getThemes()).isNull();
    }
}