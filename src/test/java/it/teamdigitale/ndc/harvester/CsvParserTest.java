package it.teamdigitale.ndc.harvester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CsvParserTest {
    @Test
    void shouldConvertCsvToJson() {
        String testCsvFile = "src/test/resources/testdata/cv.csv";
        CsvParser csvParser = new CsvParser();

        List<Map<String, String>> json = csvParser.convertCsvToJson(testCsvFile);

        assertThat(json.size()).isEqualTo(2);
        Map<String, String> firstRecord = json.get(0);
        assertThat(firstRecord.get("code_level_1")).isEqualTo("3.0");
        assertThat(firstRecord.get("label_level_1")).isEqualTo("3 stelle");

        Map<String, String> secondRecord = json.get(1);
        assertThat(secondRecord.get("code_level_1")).isEqualTo("3S");
        assertThat(secondRecord.get("label_level_1")).isEqualTo("3 stelle superior");
    }

    @Test
    void shouldReThrowRuntimeException() {
        String testCsvFile = "invalid-path.csv";
        CsvParser csvParser = new CsvParser();

        assertThrows(RuntimeException.class, () -> csvParser.convertCsvToJson(testCsvFile));
    }
}