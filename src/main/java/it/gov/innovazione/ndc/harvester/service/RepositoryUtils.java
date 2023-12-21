package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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
                .build();
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
