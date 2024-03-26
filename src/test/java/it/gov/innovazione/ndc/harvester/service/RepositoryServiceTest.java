package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.model.harvester.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepositoryServiceTest {

    private JdbcTemplate jdbcTemplate;
    private RepositoryService repositoryService;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        repositoryService = new RepositoryService(jdbcTemplate, new ObjectMapper(), "repos");
    }

    @Test
    void testDefaultNameForRightsHolder() {
        when(jdbcTemplate.query(anyString(), (RowMapper<Repository>) any()))
                .thenReturn(
                        List.of(
                                Repository.builder()
                                        .rightsHolders(
                                                Map.of(
                                                        "identifier",
                                                        Map.of(
                                                                "lang", "identifier in lang")))
                                        .name("repo")
                                        .active(true)
                                        .build()));

        List<RightsHolder> rightsHolders =
                repositoryService.getRightsHolders();

        assertTrue(rightsHolders.get(0).getName().containsKey("DEFAULT"));
        assertEquals("identifier in lang", rightsHolders.get(0).getName().get("DEFAULT"));
    }

}
