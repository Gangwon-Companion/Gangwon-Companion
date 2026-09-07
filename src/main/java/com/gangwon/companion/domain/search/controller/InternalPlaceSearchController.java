package com.gangwon.companion.domain.search.controller;

import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.dto.PlaceSearchResponse;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/search")
public class InternalPlaceSearchController {
    private final PlaceSearchEngine searchEngine;

    @PostMapping("/places")
    public ResponseEntity<?> search(
            @RequestBody PlaceSearchRequest request,
            @RequestHeader(value = "X-Grounding-Trace", defaultValue = "false") boolean includeTrace
    ) {
        PlaceSearchResponse response = searchEngine.search(request);
        return ResponseEntity.ok(includeTrace
                ? response
                : Map.of("results", response.results()));
    }
}
