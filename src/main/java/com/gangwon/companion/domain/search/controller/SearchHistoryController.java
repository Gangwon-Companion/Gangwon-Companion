package com.gangwon.companion.domain.search.controller;

import com.gangwon.companion.domain.search.dto.SearchHistoryRequest;
import com.gangwon.companion.domain.search.dto.SearchHistoryResponse;
import com.gangwon.companion.domain.search.service.SearchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping("/api/v1/search-history")
    public ResponseEntity<SearchHistoryResponse> saveSearchHistory(
            @Valid @RequestBody SearchHistoryRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(searchHistoryService.saveSearchHistory(username, request));
    }
}
