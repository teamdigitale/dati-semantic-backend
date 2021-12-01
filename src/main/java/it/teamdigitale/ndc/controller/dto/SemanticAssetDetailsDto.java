package it.teamdigitale.ndc.controller.dto;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonInclude(NON_NULL)
public class SemanticAssetDetailsDto {

    private String assetIri;
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
    private List<NodeSummary> keyClasses;
    private String prefix;
    private List<NodeSummary> projects;

    public static SemanticAssetDetailsDto from(SemanticAssetMetadata metadata) {
        return SemanticAssetDetailsDto.builder()
            .assetIri(metadata.getIri())
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
            .keyClasses(metadata.getKeyClasses())
            .prefix(metadata.getPrefix())
            .projects(metadata.getProjects())
            .build();
    }
}
