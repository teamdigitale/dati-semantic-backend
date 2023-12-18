package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.controller.RepositoryController;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

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
            + "UPDATED_BY "
            + "FROM REPOSITORY";

    private final JdbcTemplate jdbcTemplate;

    @Value("#{'${harvester.repositories}'}")
    private final String repositories;

    public List<Repository> getAllRepos() {
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
                                .build());

        if (!allRepos.isEmpty()) {
            allRepos.forEach(repo -> log.info("Repository: " + repo.toString()));
            return allRepos.stream()
                    .filter(Repository::getActive)
                    .collect(Collectors.toList());
        }

        log.info("No repositories found in the database. Using the default repositories from configuration");

        List<Repository> defaultRepositories = Optional.ofNullable(repositories)
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(emptyList())
                .stream()
                .map(RepositoryUtils::asRepo)
                .collect(Collectors.toList());

        saveDefaultRepositories(defaultRepositories);

        return defaultRepositories;
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
                       + "UPDATED_BY) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                repo.getUpdatedBy());
    }

    public Optional<Repository> findRepoById(String id) {
        return getAllRepos().stream()
                .filter(repo -> repo.getId().equals(id))
                .filter(Repository::getActive)
                .findFirst();
    }

    @SneakyThrows
    public void createRepo(String url, String name, String description, Principal principal) {
        boolean isDuplicate = getAllRepos().stream()
                .anyMatch(repo -> repo.getUrl().startsWith(url) || url.startsWith(repo.getUrl()));

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
                       + "UPDATED_BY) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                principal.getName());
    }

    public Optional<Repository> getRepoById(String id) {
        return getAllRepos().stream()
                .filter(repo -> repo.getId().equals(id))
                .findFirst();
    }

    public int updateRepo(String id, RepositoryController.CreateRepository loadedRepo, Principal principal) {
        String query = "UPDATE REPOSITORY SET "
                       + "URL = ?, "
                       + "NAME = ?, "
                       + "DESCRIPTION = ?, "
                       + "UPDATED = ?, "
                       + "UPDATED_BY = ? "
                       + "WHERE ID = ?";
        return jdbcTemplate.update(query,
                loadedRepo.getUrl(),
                loadedRepo.getName(),
                loadedRepo.getDescription(),
                java.sql.Timestamp.from(java.time.Instant.now()),
                principal.getName(),
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

}
