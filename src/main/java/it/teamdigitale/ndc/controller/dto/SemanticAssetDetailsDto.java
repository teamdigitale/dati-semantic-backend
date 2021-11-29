package it.teamdigitale.ndc.controller.dto;


import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SemanticAssetDetailsDto {

    private String iri;
    private String title;
    private String description;
    private SemanticAssetType type;
    private LocalDate modifiedOn;
    private List<String> themes;
    private NodeSummary rightsHolder;
    private String accrualPeriodicity;
    private List<String> distributionUrls;
    private List<String> subjects;
    private NodeSummary contactPoint;
    private List<NodeSummary> publishers;
    private List<NodeSummary> creators;
    private String versionInfo;
    private LocalDate issuedOn;
    private List<String> languages;
    private String temporal;
    private List<NodeSummary> conformsTo;
    private String keyConcept;
    private String endpointUrl;

    public static SemanticAssetDetailsDto from(SemanticAssetMetadata metadata) {
        return SemanticAssetDetailsDto.builder()
            .iri(metadata.getIri())
            .title(metadata.getTitle())
            .description(metadata.getDescription())
            .type(metadata.getType())
            .modifiedOn(metadata.getModifiedOn())
            .themes(metadata.getThemes())
            .rightsHolder(metadata.getRightsHolder())
            .accrualPeriodicity(metadata.getAccrualPeriodicity())
            .distributionUrls(metadata.getDistributionUrls())
            .subjects(metadata.getSubjects())
            .contactPoint(metadata.getContactPoint())
            .publishers(metadata.getPublishers())
            .creators(metadata.getCreators())
            .versionInfo(metadata.getVersionInfo())
            .issuedOn(metadata.getIssuedOn())
            .languages(metadata.getLanguages())
            .temporal(metadata.getTemporal())
            .conformsTo(metadata.getConformsTo())
            .keyConcept(metadata.getKeyConcept())
            .endpointUrl(metadata.getEndpointUrl())
            .build();
    }
}
