package com.gangwon.companion.domain.search.controller;

import com.gangwon.companion.domain.search.elasticsearch.ElasticsearchIndexService;
import com.gangwon.companion.domain.search.elasticsearch.ElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/search/index")
public class InternalSearchIndexController {
    private final ElasticsearchIndexService indexService;
    private final ElasticsearchProperties properties;

    @PostMapping("/rebuild")
    public ResponseEntity<ElasticsearchIndexService.ReindexReport> rebuild(
            @RequestHeader("X-Search-Reindex-Key") String suppliedKey) {
        String expected = properties.getReindexKey();
        if (expected == null || expected.isBlank() || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid reindex key");
        }
        return ResponseEntity.ok(indexService.reindex());
    }
}
