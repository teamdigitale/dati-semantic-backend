package it.gov.innovazione.ndc.harvester.csv;

import it.gov.innovazione.ndc.repository.MarkerElasticSearchRepository;
import it.gov.innovazione.ndc.repository.VirtuosoClient;
import it.gov.innovazione.ndc.service.GithubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"int-test", "no-level"})
class NoDeepestLevelExtractorIntTest {
    @Autowired
    private List<HeadersToIdNameExtractor> extractors;

    @MockBean
    private MarkerElasticSearchRepository elasticSearchRepository;

    @MockBean
    private VirtuosoClient virtuosoClient;

    @MockBean
    private GithubService githubService;

    @Test
    void shouldNotBeActivated() {
        assertThat(extractors.stream().noneMatch(e -> e.getClass().isAssignableFrom(DeepestLevelExtractor.class))).isTrue();
    }
}
