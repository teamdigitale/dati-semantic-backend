package it.gov.innovazione.ndc.harvester.util;

import it.gov.innovazione.ndc.eventhandler.HarvesterEventPublisher;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class GitUtils {

    private final HarvesterEventPublisher harvesterEventPublisher;

    public void cloneRepoAndGetRevision(String repoUrl, File destination) {
        cloneRepoAndGetRevision(repoUrl, destination, null);
    }

    public void cloneRepoAndGetRevision(String repoUrl, File destination, String revision) {
        try {
            Git call = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destination)
                    .call();

            if (StringUtils.isNotBlank(revision)) {
                call.checkout().setName(revision).call();
            }
        } catch (GitAPIException e) {
            throw new GitRepoCloneException(String.format("Cannot clone repo '%s'", repoUrl), e);
        }
    }

    private boolean isHead(Ref ref) {
        return StringUtils.endsWith(ref.getName(), "HEAD");
    }

    public String getCurrentRemoteRevision(String url) {
        try {
            return Git.lsRemoteRepository()
                    .setRemote(url)
                    .call()
                    .stream()
                    .filter(this::isHead)
                    .map(Ref::getObjectId)
                    .map(AnyObjectId::getName)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to get HEAD revision"));
        } catch (Exception e) {
            throw new GitRepoCloneException(String.format("Cannot get latest revision from repo '%s'", url), e);
        }
    }
}
