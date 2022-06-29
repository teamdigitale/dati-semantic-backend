package it.gov.innovazione.ndc.harvester.scanners;

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
@ConfigurationProperties("harvester.controlled-vocabulary.scanner")
public class ControlledVocabularyFolderScannerProperties {
    private List<String> skipWords;

    public static ControlledVocabularyFolderScannerProperties forWords(String... words) {
        return new ControlledVocabularyFolderScannerProperties(List.of(words));
    }
}
