package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.gen.api.VocabulariesApi;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;
import it.gov.innovazione.ndc.service.VocabularyDataService;
import it.gov.innovazione.ndc.service.VocabularyIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VocabularyDataController implements VocabulariesApi {
    final VocabularyDataService vocabularyDataService;

    @Override
    public ResponseEntity<VocabularyData> fetchVocabularyData(String agencyId, String keyConcept, Integer limit, Integer offset) {
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        return AppJsonResponse.ok(vocabularyDataService.getData(new VocabularyIdentifier(agencyId, keyConcept), pageable));
    }

    @Override
    public ResponseEntity<Map<String, String>> fetchVocabularyItem(String agencyId, String keyConcept, String id) {
        return AppJsonResponse.ok(vocabularyDataService.getItem(new VocabularyIdentifier(agencyId, keyConcept), id));
    }

}
