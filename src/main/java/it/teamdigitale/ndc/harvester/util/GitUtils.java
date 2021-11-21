package it.teamdigitale.ndc.harvester.util;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

@Component
public class GitUtils {
    public void cloneRepo(String repoUrl, File destination) {
        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destination)
                    .call();
        } catch (GitAPIException e) {
            throw new GitRepoCloneException(String.format("Cannot clone repo '%s'", repoUrl), e);
        }
    }
}
