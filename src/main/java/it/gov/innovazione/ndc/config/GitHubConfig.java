package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.service.NdcGitHubClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GitHubConfig {

    @Bean
    @SneakyThrows
    NdcGitHubClient gitHub(@Value("${github.personal-access-token}") String token) {
        if (token == null || token.isEmpty() || StringUtils.equalsAnyIgnoreCase("no_token", token)) {
            log.warn("GitHub personal access token not provided. The GitHub issuer capability will be disabled");
            return NdcGitHubClient.builder()
                    .enabled(false)
                    .build();
        }
        log.info("GitHub personal access token provided. The GitHub issuer capability will be enabled");
        return NdcGitHubClient.builder()
                .gitHub(new GitHubBuilder().withOAuthToken(token).build())
                .enabled(true)
                .build();
    }
}
