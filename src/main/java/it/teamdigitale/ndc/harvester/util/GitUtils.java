package it.teamdigitale.ndc.harvester.util;

import java.io.File;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

@Component
public class GitUtils {
    public void cloneRepo(String repoUrl, File destination) throws GitAPIException {
        Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(destination)
            .call();
    }
}
