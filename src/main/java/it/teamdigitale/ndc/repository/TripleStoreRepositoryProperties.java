package it.teamdigitale.ndc.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "virtuoso")
@Configuration
@NoArgsConstructor
@AllArgsConstructor
public class TripleStoreRepositoryProperties {
    private String url;
    private String username;
    private String password;
}
