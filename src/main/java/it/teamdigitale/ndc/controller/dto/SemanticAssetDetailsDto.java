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
    private LocalDate modified;
    private List<String> theme;
    private NodeSummary rightsHolder;
    private String accrualPeriodicity;
    private List<String> distribution;
    private List<String> subject;
    private NodeSummary contactPoint;
    private List<NodeSummary> publisher;
    private List<NodeSummary> creator;
    private String versionInfo;
    private LocalDate issued;
    private List<String> language;
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
            .modified(metadata.getModified())
            .theme(metadata.getTheme())
            .rightsHolder(metadata.getRightsHolder())
            .accrualPeriodicity(metadata.getAccrualPeriodicity())
            .distribution(metadata.getDistribution())
            .subject(metadata.getSubject())
            .contactPoint(metadata.getContactPoint())
            .publisher(metadata.getPublisher())
            .creator(metadata.getCreator())
            .versionInfo(metadata.getVersionInfo())
            .issued(metadata.getIssued())
            .language(metadata.getLanguage())
            .temporal(metadata.getTemporal())
            .conformsTo(metadata.getConformsTo())
            .keyConcept(metadata.getKeyConcept())
            .endpointUrl(metadata.getEndpointUrl())
            .build();
    }
}
