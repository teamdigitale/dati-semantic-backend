package it.gov.innovazione.ndc.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubServiceTest {

    @Test
    void testExtractOwnerRepo() {
        // just a sample github url
        String gitHubUrl = "https://github.com/owner/repo";
        Optional<GithubService.OwnerRepo> ownerRepo =
                GithubService.extractOwnerRepo(gitHubUrl);

        // assert that the ownerRepo is present
        assertTrue(ownerRepo.isPresent());

        // assert that the owner is "owner"
        assertEquals("owner", ownerRepo.get().getOwner());

        // assert that the repo is "repo"
        assertEquals("repo", ownerRepo.get().getRepo());
    }
}
