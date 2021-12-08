package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.controller.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import it.teamdigitale.ndc.service.VocabularyIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@RestController
@Validated
@RequiredArgsConstructor
public class VocabularyDataController {
    final VocabularyDataService vocabularyDataService;

    @GetMapping("vocabularies/{rights_holder}/{key_concept}")
    public VocabularyDataDto fetchVocabularyData(
        @PathVariable("rights_holder") String rightsHolder,
        @PathVariable("key_concept") String keyConcept,
        @RequestParam(value = "offset", defaultValue = "0")
        @Min(0) @Max(30000) Integer offset,
        @RequestParam(value = "limit", defaultValue = "10")
        @Min(1) @Max(200) Integer limit) {

        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        return vocabularyDataService.getData(new VocabularyIdentifier(rightsHolder, keyConcept), pageable);
    }
}
