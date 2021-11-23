package it.teamdigitale.ndc.harvester;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.teamdigitale.ndc.harvester.exception.UnknownTypeIriException;
import org.junit.jupiter.api.Test;

class SemanticAssetTypeTest {

    @Test
    void shouldGetAssetTypeByIri() {
        assertEquals(SemanticAssetType.CONTROLLED_VOCABULARY,
            SemanticAssetType.getByIri("http://dati.gov.it/onto/dcatapit#Dataset"));
        assertEquals(SemanticAssetType.ONTOLOGY,
            SemanticAssetType.getByIri("http://www.w3.org/2002/07/owl#Ontology"));
        assertEquals(SemanticAssetType.SCHEMA,
            SemanticAssetType.getByIri("to be added"));

        assertThatThrownBy(() -> SemanticAssetType.getByIri("unknown iri"))
            .isInstanceOf(UnknownTypeIriException.class);
    }
}