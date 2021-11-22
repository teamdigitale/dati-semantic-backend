package it.teamdigitale.ndc.harvester;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class CsvParser {

    public List<Map<String, String>> convertCsvToJson(String csvFile) {
        try (FileReader csvReader = new FileReader(csvFile, UTF_8)) {
            List<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .parse(csvReader)
                .getRecords();
            return records.stream()
                .map(CSVRecord::toMap)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}