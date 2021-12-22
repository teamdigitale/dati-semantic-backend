package it.gov.innovazione.ndc.harvester.csv;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CsvParserTest {
    private List<HeadersToIdNameExtractor> nameExtractors = List.of(headerNames -> headerNames.get(0));

    @Test
    void shouldLoadCsvDataFromFile() {
        String testCsvFile = "src/test/resources/testdata/cv.csv";
        CsvParser csvParser = new CsvParser(nameExtractors);

        CsvParser.CsvData csvData = csvParser.loadCsvDataFromFile(testCsvFile);

        assertThat(csvData.getIdName()).isEqualTo("code_level_1");

        List<Map<String, String>> maps = csvData.getRecords();
        assertThat(maps.size()).isEqualTo(2);
        Map<String, String> firstRecord = maps.get(0);
        assertThat(firstRecord.get("code_level_1")).isEqualTo("3.0");
        assertThat(firstRecord.get("label_level_1")).isEqualTo("3 stelle");

        Map<String, String> secondRecord = maps.get(1);
        assertThat(secondRecord.get("code_level_1")).isEqualTo("3S");
        assertThat(secondRecord.get("label_level_1")).isEqualTo("3 stelle superior");
    }

    @Test
    void shouldLoadEmptyCsvFromFile() {
        String testCsvFile = "src/test/resources/csvs/just-header.csv";
        CsvParser csvParser = new CsvParser(nameExtractors);

        CsvParser.CsvData csvData = csvParser.loadCsvDataFromFile(testCsvFile);

        assertThat(csvData.getIdName()).isEqualTo("code_level_1");
        List<Map<String, String>> maps = csvData.getRecords();
        assertThat(maps).isEmpty();
    }

    @Test
    void shouldComplainForEmptyCsv() {
        String testCsvFile = "src/test/resources/csvs/empty.csv";
        CsvParser csvParser = new CsvParser(nameExtractors);

        assertThatThrownBy(() -> csvParser.loadCsvDataFromFile(testCsvFile))
                .isInstanceOf(InvalidCsvException.class);
    }

    @Test
    void shouldComplainForMissingFile() {
        String testCsvFile = "invalid-path.csv";
        CsvParser csvParser = new CsvParser(nameExtractors);

        assertThatThrownBy(() -> csvParser.loadCsvDataFromFile(testCsvFile))
                .isInstanceOf(InvalidCsvException.class);
    }

    @Test
    void shouldSupportSingleIdExtractor() {
        CsvParser csvParser = new CsvParser(List.of(headerNames -> "id"));
        String testCsvFile = "src/test/resources/csvs/just-header.csv";

        CsvParser.CsvData csvData = csvParser.loadCsvDataFromFile(testCsvFile);

        assertThat(csvData.getIdName()).isEqualTo("id");
    }

    @Test
    void shouldSupportMultipleIdExtractorsAndUseFirstToReturnValidHeaders() {
        CsvParser csvParser = new CsvParser(List.of(headerNames -> "id1", headerNames -> "id2", headerNames -> "id3"));
        String testCsvFile = "src/test/resources/csvs/just-header.csv";

        CsvParser.CsvData csvData = csvParser.loadCsvDataFromFile(testCsvFile);

        assertThat(csvData.getIdName()).isEqualTo("id1");
    }

    @Test
    void shouldSupportMultipleIdExtractorsAndUseFirstToReturnValidHeaders2() {
        CsvParser csvParser = new CsvParser(List.of(headerNames -> null, headerNames -> null, headerNames -> "id3"));
        String testCsvFile = "src/test/resources/csvs/just-header.csv";

        CsvParser.CsvData csvData = csvParser.loadCsvDataFromFile(testCsvFile);

        assertThat(csvData.getIdName()).isEqualTo("id3");
    }

    @Test
    void shouldThrowIfNoIdExtractorCanFindAnId() {
        CsvParser csvParser = new CsvParser(List.of(headerNames -> null, headerNames -> null, headerNames -> null));
        String testCsvFile = "src/test/resources/csvs/just-header.csv";

        assertThatThrownBy(() -> csvParser.loadCsvDataFromFile(testCsvFile))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("id");
    }
}