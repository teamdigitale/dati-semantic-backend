package it.gov.innovazione.ndc.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties("search.mlt")
public class MltProperties {
    private List<String> defaultFields = List.of("searchableText");
    private int minTermFreq = 1;
    private int minDocFreq = 2;
    private int maxQueryTerms = 25;
    private String minimumShouldMatch = "30%";
    private int sizeMax = 10;
    private int timeoutMs = 1200;
}
