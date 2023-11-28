package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class RepositoryUtils {

    private static final String CONFIG = "CONFIG";

    public static Repository asRepo(String repo) {
        return Repository.builder()
                .id(UUID.randomUUID().toString())
                .url(repo)
                .name(repo)
                .owner(CONFIG)
                .active(true)
                .createdBy(CONFIG)
                .updatedBy(CONFIG)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .description("Repository from configuration")
                .source(Repository.Source.CONFIG)
                .build();
    }

    public static List<Repository> asRepos(String repoUrls) {
        return isNull(repoUrls) ? new ArrayList<>() : Arrays.stream(repoUrls.split(","))
                .map(RepositoryUtils::asRepo)
                .collect(Collectors.toList());
    }
}
