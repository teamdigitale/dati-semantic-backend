package it.gov.innovazione.ndc.service;


import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

    public static final String SCHEMA_GOV = "[SCHEMAGOV]";
    private final NdcGitHubClient gitHubClient;
    private final ConfigService configService;
    private final RepositoryService repositoryService;

    public static Optional<OwnerRepo> extractOwnerRepo(String url) {
        try {
            URI uri = new URI(url);

            if (!StringUtils.contains(uri.getHost(), "github.com")) {
                log.warn("Only GitHub URLs are supported, got {} instead", url);
                return Optional.empty();
            }

            String path = uri.getPath();

            // Remove leading slash if present
            path = path.startsWith("/") ? path.substring(1) : path;

            String[] segments = path.split("/");

            if (segments.length == 2) {
                return Optional.of(OwnerRepo.of(segments[0], segments[1]));
            }
            log.warn("Invalid URL: {}, it doesn't contain owner/repo", url);
        } catch (URISyntaxException e) {
            log.error("Error parsing URL", e);
        }
        return Optional.empty();
    }

    public Optional<GHIssue> getNdcIssueIfPresent(String repoUrl) {
        Optional<GHIssue> ndcIssue = getOpenedIssues(repoUrl).stream()
                .filter(this::isNdc)
                .findAny();

        if (ndcIssue.isPresent()) {
            log.info("Repo {} contains an ndc issue #{}", repoUrl, ndcIssue.get().getNumber());
            return ndcIssue;
        }
        log.info("ndc issues not detected on repo {}", repoUrl);
        return Optional.empty();
    }

    @SneakyThrows
    private boolean isNdc(GHIssue issue) {
        if (!gitHubClient.getEnabled()) {
            log.warn("GitHub service is not enabled, skipping issue check");
            return false;
        }
        GitHub gitHub = gitHubClient.getGitHub();
        String login = gitHub.getMyself().getLogin();
        String user = issue.getUser().getLogin();
        String title = issue.getTitle().toLowerCase();

        boolean isNdc = StringUtils.containsIgnoreCase(title, SCHEMA_GOV)
                        && user.equalsIgnoreCase(login);

        if (isNdc) {
            log.info("issue #{} on repo {} is NDC -- title {}, user {}",
                    issue.getNumber(),
                    issue.getRepository().getUrl(),
                    title,
                    user);
        }
        return isNdc;
    }

    private List<GHIssue> getOpenedIssues(String repoUrl) {
        return getRepository(repoUrl)
                .map(ghRepository -> {
                    try {
                        return ghRepository.getIssues(GHIssueState.OPEN);
                    } catch (IOException e) {
                        log.error("Error getting issues", e);
                        return List.<GHIssue>of();
                    }
                })
                .orElse(List.of());
    }

    private Optional<GHRepository> getRepository(String repoUrl) {
        if (!gitHubClient.getEnabled()) {
            log.warn("GitHub service is not enabled, skipping issue check");
            return Optional.empty();
        }
        GitHub gitHub = gitHubClient.getGitHub();
        try {
            if (isIssuerDisabledForRepo(repoUrl)) {
                log.warn("GitHub service is not enabled, or repository is not found");
                return Optional.empty();
            }

            Optional<String> ownerRepo = extractOwnerRepo(repoUrl)
                    .map(OwnerRepo::toString);

            if (ownerRepo.isEmpty()) {
                log.warn("This URL [{}] looks invalid, cannot use GitHubService on it", repoUrl);
                return Optional.empty();
            }

            return Optional.of(gitHub.getRepository(ownerRepo.get()));

        } catch (Exception e) {
            log.error(String.format("Error getting repository for URL %s", repoUrl), e);
            return Optional.empty();
        }
    }

    private boolean isIssuerDisabledForRepo(String repoUrl) {
        Optional<Repository> repository = repositoryService.findActiveRepoByUrl(repoUrl);

        if (repository.isEmpty()) {
            log.warn("Repository {} not found", repoUrl);
            return true;
        }

        return !configService.getFromRepoOrGlobalOrDefault(
                ActualConfigService.ConfigKey.GITHUB_ISSUER_ENABLED,
                repository.get().getId(),
                false);
    }

    public void openIssueIfNecessary() {
        if (!gitHubClient.getEnabled()) {
            log.warn("GitHub service is not enabled, skipping issue creation");
            return;
        }
        try {
            if (isIssuerDisabledForRepo(
                HarvestExecutionContextUtils.getContext().getRepository().getUrl())) {
                log.warn("GitHub service is not enabled for repo {}, skipping issue creation",
                    HarvestExecutionContextUtils.getContext().getRepository().getUrl());
                return;
            }
            HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
            List<HarvestExecutionContext.HarvesterExecutionError> errors = context.getErrors();
            if (errors.isEmpty()) {
                log.info("No errors detected on repository {}, skipping issue creation",
                    context.getRepository().getUrl());
                return;
            }
            String repoUrl = context.getRepository().getUrl();

            Optional<GHRepository> ghRepository = getRepository(repoUrl);

            if (ghRepository.isEmpty()) {
                log.warn("Repository {} not found, skipping issue creation", repoUrl);
                return;
            }

            GHRepository repository = ghRepository.get();

            String issueBody = String.join("\n",
                IntStream.range(0, errors.size())
                    .map(i -> i + 1)
                    .mapToObj(i -> asIssueBody(i, errors.get(i - 1)))
                    .collect(Collectors.joining("\n\n")),
                "---",
                "**Origin**: Automatically opened by the harvester",
                "**runId**: " + context.getRunId(),
                "");

            String issueTitle =
                format("%s Error processing Semantic Assets (%s)", SCHEMA_GOV, context.getRunId());

            if (context.getMaintainers().isEmpty()) {
                log.warn("No maintainers found for repository {}", repoUrl);
                try {
                    String maintainer = "@" + repository.getOwner().getLogin();
                    issueBody += "\n\n" + maintainer;
                } catch (IOException e) {
                    log.error("Error getting owner", e);
                }
            } else {
                String maintainers = context.getMaintainers().stream()
                    .map(Repository.Maintainer::getGit)
                    .map(s -> "@" + s)
                    .collect(Collectors.joining(", "));
                issueBody += "\n\n" + maintainers;
            }

            repository.createIssue(issueTitle)
                .body(issueBody)
                .label("ndc")
                .create();
        } catch (Exception e) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                if (StringUtils.containsIgnoreCase(httpException.getMessage(),
                    "Issues are disabled")) {
                    log.warn("Cannot create issue because issues feature is disabled for repo {}",
                        HarvestExecutionContextUtils.getContext().getRepository().getUrl());
                    return;
                }
            }
            log.error("An error occurred while creating issue for repo {}",
                HarvestExecutionContextUtils.getContext().getRepository().getUrl(), e);
        }
    }

    private String asIssueBody(int i, HarvestExecutionContext.HarvesterExecutionError error) {
        return String.join("\n",
                format("### %d. Exception in File: [%s](%s) ", i,
                        getFile(error),
                        getFileUrl(error)),
                format("   - **Description**: %s", error.getException().getMessage()),
                format("   - **Date and Time**: %s", LocalDateTime.now()), "", "");
    }

    private String getFile(HarvestExecutionContext.HarvesterExecutionError error) {
        return error.getFiles().size() == 1 ? error.getFiles().get(0) :
                error.getFiles()
                        .stream()
                        .filter(f -> StringUtils.endsWithIgnoreCase(f, ".csv"))
                        .findFirst()
                        .orElse(error.getFiles().get(0));
    }

    private String getFileUrl(HarvestExecutionContext.HarvesterExecutionError error) {
        String repoUrl = HarvestExecutionContextUtils.getContext().getRepository().getUrl();
        return format("%s/blob/%s/%s", repoUrl,
                HarvestExecutionContextUtils.getContext().getRevision(),
                getFile(error));
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class OwnerRepo {
        private final String owner;
        private final String repo;

        @Override
        public String toString() {
            return owner + "/" + repo;
        }
    }

}
