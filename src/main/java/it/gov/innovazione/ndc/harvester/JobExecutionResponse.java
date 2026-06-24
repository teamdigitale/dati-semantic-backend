package it.gov.innovazione.ndc.harvester;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobExecutionResponse {
    private final String runId;
    private final String correlationId;
    private final String repositoryId;
    private final String repositoryUrl;
    private final String startedAt;
    private final boolean forced;
    /**
     * Esito dell'avvio del job per questo repo: "STARTED" se l'harvest e' stato
     * lanciato (lo stato del run finale arriva poi via HarvesterRun.status),
     * "FAILED" se il job non e' nemmeno partito (es. errore in clone/HEAD).
     */
    private final String status;
    /** Messaggio dell'eccezione quando {@code status} = FAILED. */
    private final String error;
}
