package it.gov.innovazione.ndc.service;

import lombok.Builder;
import lombok.Getter;
import org.kohsuke.github.GitHub;

@Builder
@Getter
public class NdcGitHubClient {

    private final GitHub gitHub;
    private final Boolean enabled;
}
