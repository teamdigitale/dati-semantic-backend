package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
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
                                .source(Repository.Source.DATABASE)
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
                .findFirst();
    }

    @SneakyThrows
    private HarvesterRun buildHarvesterRun(ResultSet rs) {
        return HarvesterRun.builder()
                .id(rs.getString("ID"))
                .correlationId(rs.getString("CORRELATION_ID"))
                .repositoryId(rs.getString("REPOSITORY_ID"))
                .repositoryUrl(rs.getString("REPOSITORY_URL"))
                .revision(rs.getString("REVISION"))
                .startedAt(rs.getTimestamp("STARTED").toInstant())
                .endedAt(rs.getTimestamp("FINISHED").toInstant())
                .status(HarvesterRun.Status.valueOf(rs.getString("STATUS")))
                .build();
    }

    public void saveHarvesterRun(HarvesterRun harvesterRun) {
        String query = "INSERT INTO HARVESTER_RUN ("
                       + "ID, "
                       + "CORRELATION_ID, "
                       + "REPOSITORY_ID, "
                       + "REPOSITORY_URL, "
                       + "REVISION, "
                       + "STARTED, "
                       + "FINISHED, "
                       + "STATUS, "
                       + "REASON) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query,
                harvesterRun.getId(),
                harvesterRun.getCorrelationId(),
                harvesterRun.getRepositoryId(),
                harvesterRun.getRepositoryUrl(),
                harvesterRun.getRevision(),
                harvesterRun.getStartedAt(),
                harvesterRun.getEndedAt(),
                harvesterRun.getStatus().toString(),
                harvesterRun.getReason());
    }

    public boolean isHarvestingInProgress(Repository repository) {
        return Thread.getAllStackTraces().keySet().stream()
                       .map(Thread::getName)
                       .filter(name -> StringUtils.equals(name, "harvester-" + repository.getId()))
                       .count() > 1;
    }

    public void updateHarvesterRun(HarvesterRun harvesterRun) {
        String query = "UPDATE HARVESTER_RUN SET "
                       + "FINISHED = ?, "
                       + "STATUS = ?, "
                       + "REASON = ? "
                       + "WHERE ID = ?";
        jdbcTemplate.update(query,
                harvesterRun.getEndedAt(),
                harvesterRun.getStatus().toString(),
                harvesterRun.getReason(),
                harvesterRun.getId());
    }
}
