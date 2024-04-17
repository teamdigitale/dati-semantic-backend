package it.gov.innovazione.ndc.config;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GitHubConfig {

    @Bean
    GitHub gitHub(@Value("${github.personal-access-token}") String token) {
        try {
            return new GitHubBuilder().withOAuthToken(token).build();
        } catch (Exception e) {
            log.error("Error creating GitHub client instance. The GitHub issuer capability will be disabled", e);
            return null;
        }
    }
}
