package it.teamdigitale.ndc.harvester;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CsvParserTest {
    @Test
    void shouldLoadCsvDataFromFile() {
        String testCsvFile = "src/test/resources/testdata/cv.csv";
        CsvParser csvParser = new CsvParser();

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
        CsvParser csvParser = new CsvParser();

        CsvParser.CsvData csvData = csvParser.loadCsvDataFromFile(testCsvFile);

        assertThat(csvData.getIdName()).isEqualTo("code_level_1");
        List<Map<String, String>> maps = csvData.getRecords();
        assertThat(maps).isEmpty();
    }

    @Test
    void shouldComplainForEmptyCsv() {
        String testCsvFile = "src/test/resources/csvs/empty.csv";
        CsvParser csvParser = new CsvParser();

        assertThatThrownBy(() -> csvParser.loadCsvDataFromFile(testCsvFile))
                .isInstanceOf(InvalidCsvException.class);
    }

    @Test
    void shouldComplainForMissingFile() {
        String testCsvFile = "invalid-path.csv";
        CsvParser csvParser = new CsvParser();

        assertThatThrownBy(() -> csvParser.loadCsvDataFromFile(testCsvFile))
                .isInstanceOf(InvalidCsvException.class);
    }
}