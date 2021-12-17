package it.teamdigitale.ndc.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "virtuoso")
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripleStoreProperties {
    /**
     * Details for the SparQL endpoint.
     * See https://www.w3.org/TR/sparql11-protocol/ for description.
     * Usually virtuoso uses the base URL with a {@code /sparql} suffix.
     */
    private String sparql;

    /**
     * Details for the SparQL Graph Store HTTP Protocol endpoint.
     * See http://vos.openlinksw.com/owiki/wiki/VOS/VirtGraphUpdateProtocol.
     * Usually Virtuoso uses the base URL with a {@code /sparql-graph-crud/} suffix.
     */
    private String sparqlGraphStore;

    private String username;
    private String password;
}
