package it.gov.innovazione.ndc.harvester.csv;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.repository.MarkerElasticSearchRepository;
import it.gov.innovazione.ndc.repository.VirtuosoClient;
import it.gov.innovazione.ndc.service.GithubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"int-test", "use-level"})
class UseDeepestLevelExtractorIntTest {
    @Autowired
    private List<HeadersToIdNameExtractor> extractors;

    @MockitoBean
    private MarkerElasticSearchRepository elasticSearchRepository;

    @MockitoBean
    private VirtuosoClient virtuosoClient;

    @MockitoBean
    private GithubService githubService;

    @MockitoBean
    private NdcEventPublisher eventPublisher;

    @Test
    void shouldBeActivated() {
        assertThat(extractors.stream().anyMatch(e -> e.getClass().isAssignableFrom(DeepestLevelExtractor.class))).isTrue();
    }

    @Test
    void shouldBeUsedBeforeFirstColumn() {
        Class<DeepestLevelExtractor> extractorClass = DeepestLevelExtractor.class;
        int indexOfDeepestLevelExtractor = findExtractorIndexByClass(extractorClass);
        assertThat(indexOfDeepestLevelExtractor).isGreaterThanOrEqualTo(0);

        int indexOfFirstColumnExtractor = findExtractorIndexByClass(FirstColumnExtractor.class);
        assertThat(indexOfFirstColumnExtractor).isGreaterThanOrEqualTo(0);

        assertThat(indexOfDeepestLevelExtractor).isLessThan(indexOfFirstColumnExtractor);
    }

    private int findExtractorIndexByClass(Class<? extends HeadersToIdNameExtractor> extractorClass) {
        int bound = extractors.size();
        for (int i = 0; i < bound; i++) {
            if (extractors.get(i).getClass().isAssignableFrom(extractorClass)) {
                return i;
            }
        }
        return -1;
    }
}
