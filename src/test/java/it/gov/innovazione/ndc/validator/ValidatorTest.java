package it.gov.innovazione.ndc.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.gov.innovazione.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.gov.innovazione.ndc.harvester.SemanticAssetType.SCHEMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValidatorTest {

    @Test
    void assertValidatorGetModel() {
        Stream.of(
                Pair.of(ONTOLOGY, new OntologyValidator()),
                Pair.of(CONTROLLED_VOCABULARY, new ControlledVocabularyValidator()),
                Pair.of(SCHEMA, new SchemaValidator()))
                .forEach(pair -> {
                    assertEquals(pair.getKey(), pair.getValue().getType());
                    try {
                        pair.getRight().validate(null);
                    } catch (Exception e) {
                        assertNotNull(e);
                    }
                });
    }
}
