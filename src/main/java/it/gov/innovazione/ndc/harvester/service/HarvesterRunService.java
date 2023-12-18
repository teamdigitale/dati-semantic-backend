package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HarvesterRunService {

    private final JdbcTemplate jdbcTemplate;

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


    public List<HarvesterRun> getAllRuns() {
        String sqlQuery = "SELECT "
                          + "ID, "
                          + "CORRELATION_ID, "
                          + "REPOSITORY_ID, "
                          + "REPOSITORY_URL, "
                          + "REVISION, "
                          + "STARTED, "
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
                        .revision(rs.getString("REVISION"))
                        .startedAt(rs.getTimestamp("STARTED").toInstant())
                        .endedAt(rs.getTimestamp("FINISHED").toInstant())
                        .status(HarvesterRun.Status.valueOf(rs.getString("STATUS")))
                        .reason(rs.getString("REASON"))
                        .build());
    }

}
