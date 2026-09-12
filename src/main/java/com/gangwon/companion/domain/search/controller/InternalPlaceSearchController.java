package com.gangwon.companion.domain.search.controller;

import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import com.gangwon.companion.domain.search.service.GroundingContractValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/internal/search")
public class InternalPlaceSearchController {
    private final PlaceSearchEngine searchEngine;
    private final GroundingContractValidator groundingContractValidator;

    @PostMapping({"/places", "/place"})
    public ResponseEntity<PlaceSearchResponse> search(@RequestBody PlaceSearchRequest request) {
        log.info("Search Tool request: domain={}, slot={}, regions={}, queryText='{}', hardFilters={}, softPreferences={}, limit={}",
                request.domain(), request.slot(), request.regionCodes(), request.queryText(),
                request.hardFilters(), request.softPreferences(), request.limit());
        PlaceSearchResponse response = searchEngine.search(request);
        groundingContractValidator.validate(response);
        log.info("Search Tool response: domain={}, slot={}, resultCount={}, diagnostics={}",
                request.domain(), request.slot(), response.results().size(),
                response.diagnostics() == null ? "none" : response.diagnostics().failureReasons());
        return ResponseEntity.ok()
                .header("X-Grounding-Contract", GroundingContractValidator.CONTRACT_VERSION)
                .header("X-Grounding-Status", response.results().stream()
                        .anyMatch(candidate -> candidate.status() == PlaceSearchResponse.Status.INSUFFICIENT_EVIDENCE)
                        ? "INSUFFICIENT_EVIDENCE" : "OK")
                .body(response);
    }
}
