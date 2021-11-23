package it.teamdigitale.ndc.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "virtuoso")
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties("$$beanFactory")
public class TripleStoreRepositoryProperties {
    private String url;
    private String username;
    private String password;
}
