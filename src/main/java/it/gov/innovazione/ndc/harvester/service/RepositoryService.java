package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.controller.RepositoryController;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.startsWithIgnoreCase;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepositoryService {

    private static final String QUERY_GET_ALL =
            "SELECT "
            + "ID, "
            + "URL, "
            + "NAME, "
            + "DESCRIPTION, "
            + "OWNER, "
            + "ACTIVE, "
            + "CREATED, "
            + "CREATED_BY, "
            + "UPDATED, "
            + "UPDATED_BY, "
            + "MAX_FILE_SIZE_BYTES, "
            + "RIGHTS_HOLDER "
            + "FROM REPOSITORY";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("#{'${harvester.repositories}'}")
    private final String repositories;

    public List<Repository> getActiveRepos() {
        return getAllReposIncludingInactive().stream()
                .filter(Repository::getActive)
                .collect(toList());
    }

    private List<Repository> getAllReposIncludingInactive() {
        List<Repository> allRepos = jdbcTemplate.query(
                QUERY_GET_ALL,
                (rs, rowNum) ->
                        Repository.builder()
                                .id(rs.getString("ID"))
                                .url(rs.getString("URL"))
                                .name(rs.getString("NAME"))
                                .description(rs.getString("DESCRIPTION"))
                                .owner(rs.getString("OWNER"))
                                .active(rs.getBoolean("ACTIVE"))
                                .createdAt(rs.getTimestamp("CREATED").toInstant())
                                .createdBy(rs.getString("CREATED_BY"))
                                .updatedAt(rs.getTimestamp("UPDATED").toInstant())
                                .updatedBy(rs.getString("UPDATED_BY"))
                                .maxFileSizeBytes(rs.getLong("MAX_FILE_SIZE_BYTES"))
                                .rightsHolders(readSafely(rs.getString("RIGHTS_HOLDER")))
                                .build());

        if (!allRepos.isEmpty()) {
            log.info("Found {} repositories in the database", allRepos.size());
            log.debug("Repositories: "
                      + allRepos.stream().map(Repository::forLogging).collect(Collectors.joining(", ")));
            return allRepos;
        }

        log.warn("No repositories found in the database. Using the default repositories from configuration");

        List<Repository> defaultRepositories = Optional.ofNullable(repositories)
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(emptyList())
                .stream()
                .map(RepositoryUtils::asRepo)
                .collect(toList());

        saveDefaultRepositories(defaultRepositories);

        return defaultRepositories;
    }

    private Map<String, Map<String, String>> readSafely(String rightsHolders) {
        try {
            return objectMapper.readValue(rightsHolders, Map.class);
        } catch (Exception e) {
            log.error("Error reading rights holders", e);
            return Collections.emptyMap();
        }
    }

    private void saveDefaultRepositories(List<Repository> defaultRepositories) {
        defaultRepositories.forEach(this::save);
    }

    private void save(Repository repo) {
        log.info("Saving repository {}", repo);
        String query = "INSERT INTO REPOSITORY ("
                       + "ID, "
                       + "URL, "
                       + "NAME, "
                       + "DESCRIPTION, "
                       + "OWNER, "
                       + "ACTIVE, "
                       + "CREATED, "
                       + "CREATED_BY, "
                       + "UPDATED, "
                       + "UPDATED_BY, "
                       + "MAX_FILE_SIZE_BYTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query,
                repo.getId(),
                repo.getUrl(),
                repo.getName(),
                repo.getDescription(),
                repo.getOwner(),
                repo.getActive(),
                repo.getCreatedAt(),
                repo.getCreatedBy(),
                repo.getUpdatedAt(),
                repo.getUpdatedBy(),
                repo.getMaxFileSizeBytes());
    }

    public Optional<Repository> findActiveRepoById(String id) {
        return getActiveRepos().stream()
                .filter(repo -> repo.getId().equals(id))
                .findFirst();
    }

    @SneakyThrows
    public void createRepo(String url, String name, String description, Long maxFileSizeBytes, Principal principal) {
        if (repoAlreadyExists(url)) {
            log.info("Repository {} already exists, reactivating", url);
            reactivate(url, name, description, maxFileSizeBytes, principal);
            return;
        }

        // does not exist but repo to create is a substring of an existing repo,
        // or existing repo is a substring of the repo to create
        boolean isDuplicate = getAllReposIncludingInactive().stream()
                .anyMatch(repo ->
                        startsWithIgnoreCase(
                                repo.getUrl(),
                                url)
                        || startsWithIgnoreCase(
                                url,
                                repo.getUrl()));

        if (isDuplicate) {
            throw new IllegalArgumentException("Duplicate repository " + url);
        }

        log.info("Creating repository {}", url);

        String query = "INSERT INTO REPOSITORY ("
                       + "ID, "
                       + "URL, "
                       + "NAME, "
                       + "DESCRIPTION, "
                       + "OWNER, "
                       + "ACTIVE, "
                       + "CREATED, "
                       + "CREATED_BY, "
                       + "UPDATED, "
                       + "UPDATED_BY,"
                       + "MAX_FILE_SIZE_BYTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(query,
                RepositoryUtils.generateId(),
                url,
                name,
                description,
                principal.getName(),
                true,
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
                maxFileSizeBytes);
    }

    private boolean repoAlreadyExists(String url) {
        return getAllReposIncludingInactive().stream()
                .anyMatch(repo -> repo.getUrl().equals(url));
    }

    public int updateRepo(String id, RepositoryController.CreateRepository loadedRepo, Principal principal) {
        log.info("Updating repository {} using name={}, description={}, maxFileSizeBytes={}",
                id, loadedRepo.getName(), loadedRepo.getDescription(), loadedRepo.getMaxFileSizeBytes());
        String query = "UPDATE REPOSITORY SET "
                       + "NAME = ?, "
                       + "DESCRIPTION = ?, "
                       + "UPDATED = ?, "
                       + "UPDATED_BY = ?, "
                       + "MAX_FILE_SIZE_BYTES = ? "
                       + "WHERE ID = ?";
        return jdbcTemplate.update(query,
                loadedRepo.getName(),
                loadedRepo.getDescription(),
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
                loadedRepo.getMaxFileSizeBytes(),
                id);
    }

    public int delete(String id, Principal principal) {
        log.info("Deleting repository {}", id);
        String query = "UPDATE REPOSITORY SET "
                       + "ACTIVE = ?, "
                       + "UPDATED = ?, "
                       + "UPDATED_BY = ? "
                       + "WHERE ID = ?";
        return jdbcTemplate.update(query,
                false,
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
                id);
    }

    public int reactivate(String url, String name, String description, Long maxFileSizeBytes, Principal principal) {
        log.info("Reactivating repository {}", url);
        String query = "UPDATE REPOSITORY SET "
                       + "ACTIVE = ?, "
                       + "NAME = ?, "
                       + "DESCRIPTION = ?, "
                       + "MAX_FILE_SIZE_BYTES = ?, "
                       + "UPDATED = ?, "
                       + "UPDATED_BY = ? "
                       + "WHERE URL = ?";
        return jdbcTemplate.update(query,
                true,
                name,
                description,
                maxFileSizeBytes,
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
                url);
    }

    @SneakyThrows
    public void storeRightsHolders(Repository repository, Map<String, Map<String, String>> rightsHolders) {
        log.info("Storing {} rights holders for repository {}", rightsHolders.keySet().size(), repository);
        String query = "UPDATE REPOSITORY SET "
                       + "RIGHTS_HOLDER = ? "
                       + "WHERE ID = ?";
        jdbcTemplate.update(query,
                objectMapper.writeValueAsString(rightsHolders),
                repository.getId());
    }

    public List<RightsHolder> getRightsHolders() {
        return getActiveRepos().stream()
                .map(Repository::getRightsHolders)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(groupingBy(
                        entry -> entry.getKey().toLowerCase(),
                        mapping(
                                Map.Entry::getValue,
                                toList())))
                .entrySet().stream()
                .map(entry -> withDefaultLangIfNecessary(RightsHolder.builder()
                        .identifier(entry.getKey())
                        .name(entry.getValue().get(0))
                        .build()))
                .collect(Collectors.toList());
    }

    private RightsHolder withDefaultLangIfNecessary(RightsHolder rightsHolder) {
        if (containsSuitableLang(rightsHolder)) {
            return rightsHolder;
        }

        Map<String, String> names = rightsHolder.getName();

        String name = names
                .entrySet()
                .stream()
                .findFirst()
                .map(Map.Entry::getValue)
                .filter(StringUtils::isNoneBlank)
                .orElse(rightsHolder.getIdentifier());

        return RightsHolder.builder()
                .identifier(rightsHolder.getIdentifier())
                .name(Collections.singletonMap("DEFAULT", name))
                .build();
    }

    private boolean containsSuitableLang(RightsHolder rightsHolder) {
        return Optional.ofNullable(rightsHolder.getName())
                .map(Map::keySet)
                .orElse(emptySet())
                .stream()
                .map(String::toLowerCase)
                .anyMatch(lang -> lang.equals("en") || lang.equals("it"));
    }

}
