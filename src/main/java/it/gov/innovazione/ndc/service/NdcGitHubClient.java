package it.gov.innovazione.ndc.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Service;

@Builder
@Getter
public class NdcGitHubClient {

    private final GitHub gitHub;
    private final Boolean enabled;
}
