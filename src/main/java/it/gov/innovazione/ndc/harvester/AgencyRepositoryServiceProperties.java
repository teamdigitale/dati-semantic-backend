package it.gov.innovazione.ndc.harvester;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ConfigurationProperties("harvester.folder")
public class AgencyRepositoryServiceProperties {
    private List<String> skipWords;

    public static AgencyRepositoryServiceProperties forWords(String... words) {
        return new AgencyRepositoryServiceProperties(List.of(words));
    }
}
