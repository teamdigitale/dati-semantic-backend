package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.controller.RunningInstance;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.gov.innovazione.ndc.config.SimpleHarvestRepositoryProcessor.getAllRunningHarvestThreadNames;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

@Service
@Slf4j
@RequiredArgsConstructor
public class HarvesterRunService {

    private final JdbcTemplate jdbcTemplate;

    private static final Long HARVESTING_RECENT_DAYS = 30L;

    public int saveHarvesterRun(HarvesterRun harvesterRun) {
        String query = "INSERT INTO HARVESTER_RUN ("
                + "ID, "
                + "CORRELATION_ID, "
                + "REPOSITORY_ID, "
                + "REPOSITORY_URL, "
                + "INSTANCE, "
                + "REVISION, "
                + "STARTED, "
                + "STARTED_BY, "
                + "FINISHED, "
                + "STATUS, "
                + "REASON) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(query,
                harvesterRun.getId(),
                harvesterRun.getCorrelationId(),
                harvesterRun.getRepositoryId(),
                harvesterRun.getRepositoryUrl(),
                harvesterRun.getInstance(),
                harvesterRun.getRevision(),
                harvesterRun.getStartedAt(),
                harvesterRun.getStartedBy(),
                harvesterRun.getEndedAt(),
                harvesterRun.getStatus().toString(),
                harvesterRun.getReason());
    }

    public Optional<HarvesterRun> isHarvestingInProgress(String runId, Repository repository) {
        return getRecentRuns(HARVESTING_RECENT_DAYS)
                .filter(harvesterRun -> !equalsIgnoreCase(harvesterRun.getId(), runId))
                .filter(harvesterRun -> isAlreadyRunning(harvesterRun, repository))
                .findFirst();
    }

    private boolean isMoreRecentThan(HarvesterRun harvesterRun, Long days) {
        return Optional.of(harvesterRun)
                .map(HarvesterRun::getStartedAt)
                .filter(startedAt -> startedAt.isAfter(Instant.now().minus(days, ChronoUnit.DAYS)))
                .isPresent();
    }

    private boolean isAlreadyRunning(HarvesterRun harvesterRun, Repository repository) {
        return (
                harvesterRun.getRepositoryId().equals(repository.getId())
                        || startsWithIgnoreCase(
                        harvesterRun.getRepositoryUrl(),
                        repository.getUrl())
                        || startsWithIgnoreCase(
                        repository.getUrl(),
                        harvesterRun.getRepositoryUrl()))
                && harvesterRun.getStatus() == HarvesterRun.Status.RUNNING;
    }

    public Optional<HarvesterRun> isHarvestingAlreadyExecuted(String repositoryId, String revision) {
        return getRecentRuns(HARVESTING_RECENT_DAYS)
                .filter(harvesterRun -> harvesterRun.getRepositoryId().equals(repositoryId))
                .filter(harvesterRun -> harvesterRun.getStatus() == HarvesterRun.Status.SUCCESS)
                .max(Comparator.comparing(HarvesterRun::getStartedAt))
                .filter(harvesterRun -> equalsIgnoreCase(harvesterRun.getRevision(), revision));
    }

    public int updateHarvesterRun(HarvesterRun harvesterRun) {
        String query = "UPDATE HARVESTER_RUN SET "
                + "FINISHED = ?, "
                + "STATUS = ?, "
                + "REASON = ? "
                + "WHERE ID = ?";
        return jdbcTemplate.update(query,
                harvesterRun.getEndedAt(),
                harvesterRun.getStatus().toString(),
                harvesterRun.getReason(),
                harvesterRun.getId());
    }

    public Stream<HarvesterRun> getRecentRuns(Long days) {
        return getAllRuns().stream()
                .filter(harvesterRun -> isMoreRecentThan(harvesterRun, days));
    }

    public List<HarvesterRun> getAllRuns() {
        String sqlQuery = "SELECT "
                + "ID, "
                + "CORRELATION_ID, "
                + "REPOSITORY_ID, "
                + "REPOSITORY_URL, "
                + "INSTANCE, "
                + "REVISION, "
                + "STARTED, "
                + "STARTED_BY, "
                + "FINISHED, "
                + "STATUS, "
                + "REASON "
                + "FROM HARVESTER_RUN "
                + "ORDER BY STARTED DESC";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) ->
                HarvesterRun.builder()
                        .id(rs.getString("ID"))
                        .correlationId(rs.getString("CORRELATION_ID"))
                        .repositoryId(rs.getString("REPOSITORY_ID"))
                        .repositoryUrl(rs.getString("REPOSITORY_URL"))
                        .instance(rs.getString("INSTANCE"))
                        .revision(rs.getString("REVISION"))
                        .startedAt(getInstant(rs, "STARTED"))
                        .startedBy(rs.getString("STARTED_BY"))
                        .endedAt(getInstant(rs, "FINISHED"))
                        .status(getStatusSafely(rs))
                        .reason(rs.getString("REASON"))
                        .build());
    }

    private HarvesterRun.Status getStatusSafely(ResultSet rs) {
        try {
            return HarvesterRun.Status.valueOf(rs.getString("STATUS"));
        } catch (Exception e) {
            return null;
        }
    }

    private Instant getInstant(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    public void deletePendingRuns() {
        getAllRuns()
                .stream()
                .filter(harvesterRun -> harvesterRun.getStatus() == HarvesterRun.Status.RUNNING)
                .forEach(this::deleteIfNecessary);
    }

    private void deleteIfNecessary(HarvesterRun harvesterRun) {
        if (getAllRunningHarvestThreadNames()
                .stream()
                .noneMatch(threadName -> matchRunning(harvesterRun, threadName))) {
            delete(harvesterRun);
        }
    }

    private boolean matchRunning(HarvesterRun harvesterRun, String threadName) {
        return contains(threadName, harvesterRun.getRepositoryId())
                && contains(threadName, harvesterRun.getRevision())
                && contains(threadName, harvesterRun.getId())
                && endsWithIgnoreCase(threadName, "RUNNING");
    }

    private void delete(HarvesterRun harvesterRun) {
        String query = "DELETE FROM HARVESTER_RUN WHERE ID = ?";
        jdbcTemplate.update(query, harvesterRun.getId());
    }

    public List<RunningInstance> getAllRunningInstances() {
        List<HarvesterRun> allRuns = getAllRuns();
        return getAllRunningHarvestThreadNames()
                .stream()
                .map(threadName -> asRunningInstance(allRuns, threadName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RunningInstance asRunningInstance(List<HarvesterRun> allRuns, String threadName) {
        HarvesterRun harvesterRun = findHarvesterRun(allRuns, threadName);
        if (harvesterRun != null) {
            return RunningInstance.builder()
                    .threadName(threadName)
                    .harvesterRun(findHarvesterRun(allRuns, threadName))
                    .build();
        }
        return null;
    }

    private HarvesterRun findHarvesterRun(List<HarvesterRun> allRuns, String threadName) {
        try {
            String[] split = threadName.split("\\|");
            String runId = split[1];
            return allRuns.stream()
                    .filter(harvesterRun -> harvesterRun.getId().equals(runId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
