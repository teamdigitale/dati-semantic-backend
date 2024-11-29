package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import it.gov.innovazione.ndc.harvester.service.SemanticContentStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
public class SemanticAssetsStatsController {

  private final SemanticContentStatsService semanticContentStatsService;

  @Operation(
      tags = {"semantic-assets"},
      summary = "Retrieves the statistics",
      description = "Retrieves the statistics of the semantic assets.",
      operationId = "getStats",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = SemanticAssetStats.class))
            })
      })
  @GetMapping(
      value = "semantic-assets/stats",
      produces = {"application/json"})
  public SemanticAssetStats getStats(@RequestParam(name = "year", required = false) Integer year) {
    // get current year from system date
    if (year == null) {
      year = LocalDate.now().getYear();
    }
    return semanticContentStatsService.getStats(year);
  }
}
