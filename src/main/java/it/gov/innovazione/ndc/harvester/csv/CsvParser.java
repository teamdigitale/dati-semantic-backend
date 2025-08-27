package it.gov.innovazione.ndc.harvester.csv;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

@Component
@RequiredArgsConstructor
public class CsvParser {
    private final List<HeadersToIdNameExtractor> nameExtractors;

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class CsvData {
        private final List<Map<String, String>> records;
        private final String idName;
    }


    public CsvData loadCsvDataFromFile(String csvFile) {
        try (FileReader csvReader = new FileReader(csvFile, UTF_8)) {
            return tryParseCsv(csvReader, csvFile);

        } catch (IOException e) {
            logSemanticError(
                    LoggingContext.builder()
                            .message(format("Cannot parse CSV file '%s'", csvFile))
                            .stage(HarvesterStage.PROCESS_RESOURCE)
                            .harvesterStatus(HarvesterRun.Status.RUNNING)
                            .build());
            throw new InvalidCsvException(format("Cannot parse CSV file '%s'", csvFile), e);
        }
    }

    private CsvData tryParseCsv(FileReader csvReader, String csvFile) throws IOException {
        try (CSVParser parser = parseReader(csvReader)) {
            CsvData csvData = buildCsvDataFromParser(parser, csvFile);
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .message("Parsed CSV file")
                    .additionalInfo("csvFile", csvFile)
                    .additionalInfo("idName", csvData.getIdName())
                    .additionalInfo("records", csvData.getRecords().size())
                    .build());
            return csvData;
        }
    }

    private CsvData buildCsvDataFromParser(CSVParser parser, String csvFile) {
        String idName = getIdName(parser, csvFile);
        List<Map<String, String>> records = readRecords(parser);

        return new CsvData(records, idName);
    }

    private CSVParser parseReader(FileReader csvReader) throws IOException {
        return Objects.requireNonNull(CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(csvReader));
    }

    private List<Map<String, String>> readRecords(CSVParser parser) {
        return parser.stream()
                .map(CSVRecord::toMap)
                .map(this::withSanitizedKeys)
                .collect(Collectors.toList());
    }

    private Map<String, String> withSanitizedKeys(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        return map.entrySet().stream()
                .collect(toMap(
                        e -> {
                            String k = e.getKey();
                            return k == null ? null : k.replace('.', '_').trim();
                        },
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }

    private String getIdName(CSVParser parser, String csvFile) {
        List<String> headerNames = parser.getHeaderNames();
        if (headerNames.isEmpty()) {
            throw new InvalidCsvException(format("Cannot find any headers in '%s'", csvFile));
        }

        return nameExtractors.stream()
                .map(e -> e.extract(headerNames))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new InvalidCsvException(format("Cannot find id column in '%s'", csvFile)));
    }
}