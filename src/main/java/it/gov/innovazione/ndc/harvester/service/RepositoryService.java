package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.controller.RepositoryController;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
        return getAllRepos().stream()
                .filter(Repository::getActive)
                .collect(toList());
    }

    private List<Repository> getAllRepos() {
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
            allRepos.forEach(repo -> log.info("Repository: " + repo.toString()));
            return allRepos.stream()
                    .filter(Repository::getActive)
                    .collect(toList());
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

    public Optional<Repository> findRepoById(String id) {
        return getAllRepos().stream()
                .filter(repo -> repo.getId().equals(id))
                .filter(Repository::getActive)
                .findFirst();
    }

    @SneakyThrows
    public void createRepo(String url, String name, String description, Long maxFileSizeBytes, Principal principal) {
        boolean isDuplicate = getAllRepos().stream()
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

    public int updateRepo(String id, RepositoryController.CreateRepository loadedRepo, Principal principal) {
        String query = "UPDATE REPOSITORY SET "
                       + "URL = ?, "
                       + "NAME = ?, "
                       + "DESCRIPTION = ?, "
                       + "UPDATED = ?, "
                       + "UPDATED_BY = ?, "
                       + "MAX_FILE_SIZE_BYTES = ? "
                       + "WHERE ID = ?";
        return jdbcTemplate.update(query,
                loadedRepo.getUrl(),
                loadedRepo.getName(),
                loadedRepo.getDescription(),
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
                loadedRepo.getMaxFileSizeBytes(),
                id);
    }

    public int delete(String id, Principal principal) {
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

    @SneakyThrows
    public void storeRightsHolders(Repository repository, Map<String, Map<String, String>> rightsHolders) {
        log.info("Storing rights holders for repository {}", repository);
        String query = "UPDATE REPOSITORY SET "
                       + "RIGHTS_HOLDER = ? "
                       + "WHERE ID = ?";
        jdbcTemplate.update(query,
                objectMapper.writeValueAsString(rightsHolders),
                repository.getId());
    }

    public List<RightsHolder> getRightsHolders() {
        return getAllRepos().stream()
                .map(Repository::getRightsHolders)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(groupingBy(
                        Map.Entry::getKey,
                        mapping(
                                Map.Entry::getValue,
                                toList())))
                .entrySet().stream()
                .map(entry -> RightsHolder.builder()
                        .identifier(entry.getKey())
                        .name(entry.getValue().get(0))
                        .build())
                .collect(Collectors.toList());
    }
}
