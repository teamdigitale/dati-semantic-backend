package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Validated
public class VocabularyDataController {
    @Autowired VocabularyDataService vocabularyDataService;

    @GetMapping("vocabularies/{agency_id}/{vocabulary_id}")
    public VocabularyDataDto fetchVocabularyData(
            @PathVariable("agency_id") String agencyId,
            @PathVariable("vocabulary_id") String vocabularyId,
            @RequestParam(name = "page_number", required = false, defaultValue = "1")
            @Min(1) Integer pageNumber,
            @RequestParam(name = "page_size", required = false, defaultValue = "10")
            @Min(1) @Max(200) Integer pageSize) {

        int pageIndexForElasticsearch = pageNumber - 1;
        return vocabularyDataService.getData(agencyId, vocabularyId, pageIndexForElasticsearch, pageSize);
    }
}
