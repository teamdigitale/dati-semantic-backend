package it.gov.innovazione.ndc.harvester.service;

import co.elastic.clients.util.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.Parser.TO_BOOLEAN;
import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.Parser.TO_LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActualConfigServiceTest {

    static Stream<Arguments> provideValidValues() {
        return Stream.of(
                Arguments.of(TO_BOOLEAN, "true", true),
                Arguments.of(TO_BOOLEAN, " false", false),
                Arguments.of(TO_BOOLEAN, "True", true),
                Arguments.of(TO_BOOLEAN, "False", false),
                Arguments.of(TO_BOOLEAN, "tRuE", true),
                Arguments.of(TO_BOOLEAN, "fAlSe", false),
                Arguments.of(TO_LONG, "1", 1L),
                Arguments.of(TO_LONG, "0", 0L),
                Arguments.of(TO_LONG, "100", 100L));
    }

    static Stream<Arguments> provideInvalidValues() {
        return Stream.of(
                        Pair.of(TO_BOOLEAN, "invalid"),
                        Pair.of(TO_BOOLEAN, "1"),
                        Pair.of(TO_BOOLEAN, "0"),
                        Pair.of(TO_BOOLEAN, "yes"),
                        Pair.of(TO_BOOLEAN, "no"),
                        Pair.of(TO_BOOLEAN, null),
                        Pair.of(TO_LONG, "invalid"),
                        Pair.of(TO_LONG, "true"),
                        Pair.of(TO_LONG, "false"),
                        Pair.of(TO_LONG, "yes"),
                        Pair.of(TO_LONG, "no"))
                .map(parserPair -> Arguments.of(parserPair.key(), parserPair.value()));
    }

    @ParameterizedTest
    @MethodSource("provideValidValues")
    void testParseTrue(ActualConfigService.Parser parser, String validString, Object expectedValue) {
        assertEquals(expectedValue, parser.getParsingFunction().apply(validString));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValues")
    void testParseNull(ActualConfigService.Parser parser, String invalidValue) {
        assertThrows(IllegalArgumentException.class, () -> parser.getParsingFunction().apply(invalidValue));
    }

}
