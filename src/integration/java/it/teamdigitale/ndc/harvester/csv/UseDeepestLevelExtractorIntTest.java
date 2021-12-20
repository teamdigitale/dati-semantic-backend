package it.teamdigitale.ndc.harvester.csv;

import it.teamdigitale.ndc.repository.MarkerElasticSearchRepository;
import it.teamdigitale.ndc.repository.VirtuosoClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"int-test", "use-level"})
class UseDeepestLevelExtractorIntTest {
    @Autowired
    private List<HeadersToIdNameExtractor> extractors;

    @MockBean
    private MarkerElasticSearchRepository elasticSearchRepository;

    @MockBean
    private VirtuosoClient virtuosoClient;

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