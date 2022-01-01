package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.gen.api.VocabulariesApi;
import it.gov.innovazione.ndc.gen.dto.AssetType;
import it.gov.innovazione.ndc.gen.dto.VocabulariesResult;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;
import it.gov.innovazione.ndc.service.SemanticAssetSearchService;
import it.gov.innovazione.ndc.service.VocabularyDataService;
import it.gov.innovazione.ndc.service.VocabularyIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class VocabularyDataController implements VocabulariesApi {
    private final VocabularyDataService vocabularyDataService;
    private final SemanticAssetSearchService searchService;

    @Override
    public ResponseEntity<VocabularyData> fetchVocabularyData(String agencyId, String keyConcept, Integer limit, Integer offset) {
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        return AppJsonResponse.ok(vocabularyDataService.getData(new VocabularyIdentifier(agencyId, keyConcept), pageable));
    }

    @Override
    public ResponseEntity<Map<String, String>> fetchVocabularyItem(String agencyId, String keyConcept, String id) {
        return AppJsonResponse.ok(vocabularyDataService.getItem(new VocabularyIdentifier(agencyId, keyConcept), id));
    }

    @Override
    public ResponseEntity<VocabulariesResult> fetchVocabularies(Integer limit, Integer offset) {
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        return AppJsonResponse.ok(searchService.getVocabularies(pageable));
    }
}
