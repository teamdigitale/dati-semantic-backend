package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.service.VocabularyDataService;
import java.util.List;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class VocabularyDataController {
    @Autowired
    VocabularyDataService vocabularyDataService;

    @GetMapping("vocabularies/{agency_id}/{vocabulary_id}")
    public List<JSONObject> fetchControlledVocabularyData(@PathVariable("agency_id") String agencyId,
                                                          @PathVariable("vocabulary_id") String vocabularyId,
                                                          @RequestParam(name = "page-number", required = false)
                                                                      Integer pageNumber,
                                                          @RequestParam(name = "page-size", required = false)
                                                                      Integer pageSize) {
        return vocabularyDataService.getData(vocabularyId, pageNumber, pageSize);
    }
}

