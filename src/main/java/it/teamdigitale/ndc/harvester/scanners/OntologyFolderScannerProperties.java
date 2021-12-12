package it.teamdigitale.ndc.harvester.scanners;

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
@ConfigurationProperties("ontology.scanner")
public class OntologyFolderScannerProperties {
    private List<String> skipWords;

    public static OntologyFolderScannerProperties forWords(String... words) {
        return new OntologyFolderScannerProperties(List.of(words));
    }
}
