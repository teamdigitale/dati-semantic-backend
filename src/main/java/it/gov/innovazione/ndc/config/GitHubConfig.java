package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.DefaultAlertableEvent;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.service.NdcGitHubClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GitHubConfig {

    private final NdcEventPublisher eventPublisher;


    @Bean
    @SneakyThrows
    NdcGitHubClient gitHub(@Value("${github.personal-access-token}") String token) {
        if (token == null || token.isEmpty() || StringUtils.equalsAnyIgnoreCase("no_token", token)) {
            log.warn("GitHub personal access token not provided. The GitHub issuer capability will be disabled");
            eventPublisher.publishAlertableEvent(
                    "GitHubConfig",
                    DefaultAlertableEvent.builder()
                            .name("GitHubConfig not provided")
                            .description("GitHubConfig personal access token not provided. The GitHub issuer capability will be disabled")
                            .occurredAt(Instant.now())
                            .category(EventCategory.APPLICATION)
                            .severity(Severity.WARNING)
                            .build());

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
