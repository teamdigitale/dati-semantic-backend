package it.teamdigitale.ndc.integration;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;

public class CsvIndexingAndQueryingIntegrationTest extends BaseIntegrationTest {

    public static final String AGENCY_ID = "agid";
    public static final String KEY_CONCEPT = "licences";

    @Test
    void shouldRetrieveSampleLicenses() {
        when().get(format("http://localhost:%d/vocabularies/%s/%s", port, AGENCY_ID, KEY_CONCEPT))
                .then()
                .statusCode(200)
                .log().body()
                .body("totalResults", equalTo(50))
                .body("offset", equalTo(0))
                .body("limit", equalTo(10))
                .body("data.size()", equalTo(10))
                .body("data[0].codice_1_livello", equalTo("A"))
                .body("data[0].label_level_1", equalTo("Licenza Aperta"))
                .body("data[0].codice_2_livello", equalTo("A.1"))
                .body("data[0].label_level_2", equalTo("Dominio pubblico"))
                .body("data[0].codice_3_livello", equalTo("A.1.1"))
                .body("data[0].label_level_3", equalTo("Creative Commons CC0 1.0 Universal - Public Domain Dedication (CC0 1.0)"))

                .body("data[1].codice_3_livello", equalTo("A.1.2"))
                .body("data[2].codice_3_livello", equalTo("A.2.1"))
                .body("data[3].codice_3_livello", equalTo("A.2.2"));
    }

    @Test
    void shouldRetrieveLicenseByThirdLevelId() {
        when().get(format("http://localhost:%d/vocabularies/%s/%s/A.1.2", port, AGENCY_ID, KEY_CONCEPT))
                .then()
                .statusCode(200)
                .log().body()
                .body("codice_1_livello", equalTo("A"))
                .body("label_level_1", equalTo("Licenza Aperta"))
                .body("codice_2_livello", equalTo("A.1"))
                .body("label_level_2", equalTo("Dominio pubblico"))
                .body("codice_3_livello", equalTo("A.1.2"))
                .body("label_level_3", equalTo("ODC Public Domain Dedication and License (PDDL)"));
    }
}
