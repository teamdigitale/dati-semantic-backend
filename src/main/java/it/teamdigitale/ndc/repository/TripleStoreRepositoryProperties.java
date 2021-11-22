package it.teamdigitale.ndc.repository;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "virtuoso")
@Component
public class TripleStoreRepositoryProperties {
    private final String url;
    private final String username;
    private final String password;
}
