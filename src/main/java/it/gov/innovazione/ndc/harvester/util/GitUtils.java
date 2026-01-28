package it.gov.innovazione.ndc.harvester.util;

import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static it.gov.innovazione.ndc.harvester.AgencyRepositoryService.TEMP_DIR_PREFIX;

@Component
@RequiredArgsConstructor
public class GitUtils {

    private final FileUtils fileUtils;

    public void cloneRepo(String repoUrl, File destination, String branch, String revision) {
        cloneRepoAndGetLastCommitDate(repoUrl, destination, branch, revision);
    }

    public Instant cloneRepoAndGetLastCommitDate(String repoUrl, File destination, String branch, String revision) {
        try {
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destination);

            if (StringUtils.isNotBlank(branch)) {
                cloneCommand.setBranch(branch);
            }

            Git call = cloneCommand.call();

            if (StringUtils.isNotBlank(revision)) {
                call.checkout().setName(revision).call();
            }
            return safelyGetLastCommitDate(call);
        } catch (GitAPIException e) {
            throw new GitRepoCloneException(String.format("Cannot clone repo '%s'", repoUrl), e);
        }
    }

    private boolean isHead(Ref ref) {
        return StringUtils.endsWith(ref.getName(), "HEAD");
    }

    public String getHeadRemoteRevision(String url) {
        return getHeadRemoteRevision(url, null);
    }

    public String getHeadRemoteRevision(String url, String branch) {
        try {
            return Git.lsRemoteRepository()
                    .setRemote(url)
                    .call()
                    .stream()
                    .filter(ref -> isTargetRef(ref, branch))
                    .map(Ref::getObjectId)
                    .map(AnyObjectId::getName)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to get HEAD revision for branch: " + branch));
        } catch (Exception e) {
            throw new GitRepoCloneException(String.format("Cannot get latest revision from repo '%s' branch '%s'", url, branch), e);
        }
    }

    private boolean isTargetRef(Ref ref, String branch) {
        if (StringUtils.isBlank(branch)) {
            return isHead(ref);
        }
        return StringUtils.equals(ref.getName(), "refs/heads/" + branch);
    }

    public Optional<Instant> getCommitDate(String repositoryUrl, String branch, String revision) {
        Optional<Path> tempDirectory = safelyGetTempDirectory(repositoryUrl, revision);
        if (tempDirectory.isEmpty()) {
            return Optional.empty();
        }

        Optional<Git> gitOpt = cloneSafely(repositoryUrl, tempDirectory.get(), branch, revision);

        if (gitOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Instant> instant = gitOpt.map(this::safelyGetLastCommitDate);
        tryRemoveDirectory(tempDirectory.get());
        return instant;
    }

    private void tryRemoveDirectory(Path path) {
        try {
            fileUtils.removeDirectory(path);
        } catch (IOException e) {
            NDCHarvesterLogger.logApplicationError(LoggingContext.builder()
                    .message("Error removing temp directory")
                    .details(e.getMessage())
                    .build());
        }
    }

    private Optional<Path> safelyGetTempDirectory(String repositoryUrl, String revision) {
        try {
            return Optional.of(fileUtils.createTempDirectory(TEMP_DIR_PREFIX));
        } catch (IOException e) {
            NDCHarvesterLogger.logApplicationError(LoggingContext.builder()
                    .message("Error creating temp directory while retrieving commit date")
                    .details(e.getMessage())
                    .additionalInfo("repositoryUrl", repositoryUrl)
                    .additionalInfo("revision", revision)
                    .build());
            return Optional.empty();
        }
    }

    private Instant safelyGetLastCommitDate(Git git) {
        try {
            return StreamSupport.stream(git.log().call().spliterator(), false)
                    .findFirst()
                    .map(RevCommit::getAuthorIdent)
                    .map(PersonIdent::getWhenAsInstant)
                    .orElse(null);
        } catch (GitAPIException e) {
            NDCHarvesterLogger.logApplicationError(LoggingContext.builder()
                    .message("Error getting commit date")
                    .details(e.getMessage())
                    .build());
            return null;
        }
    }

    private Optional<Git> cloneSafely(String repositoryUrl, Path tempDirectory, String branch, String revision) {
        try {
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(tempDirectory.toFile());

            if (StringUtils.isNotBlank(branch)) {
                cloneCommand.setBranch(branch);
            }

            Git git = cloneCommand.call();

            if (StringUtils.isNotBlank(revision)) {
                git.checkout().setName(revision).call();
            }

            return Optional.of(git);
        } catch (GitAPIException e) {
            return Optional.empty();
        }
    }
}
