package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
        @RequestParam(name = "page_number", required = false, defaultValue = "1")
        @Min(1) Integer pageNumber,
        @RequestParam(name = "page_size", required = false, defaultValue = "10")
        @Min(1) @Max(200) Integer pageSize) {

        int pageIndexForElasticsearch = pageNumber - 1;
        return vocabularyDataService.getData(rightsHolder, keyConcept, pageIndexForElasticsearch,
            pageSize);
    }
}
