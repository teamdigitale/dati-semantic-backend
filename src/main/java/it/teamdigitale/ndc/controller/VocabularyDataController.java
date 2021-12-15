package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.gen.api.VocabulariesApi;
import it.teamdigitale.ndc.gen.dto.VocabularyData;
import it.teamdigitale.ndc.service.VocabularyDataService;
import it.teamdigitale.ndc.service.VocabularyIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VocabularyDataController implements VocabulariesApi {
    final VocabularyDataService vocabularyDataService;

    @Override
    public ResponseEntity<VocabularyData> fetchVocabularyData(String agencyId, String keyConcept, Integer offset, Integer limit) {
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        return ResponseEntity.ok(vocabularyDataService.getData(new VocabularyIdentifier(agencyId, keyConcept), pageable));
    }
}
