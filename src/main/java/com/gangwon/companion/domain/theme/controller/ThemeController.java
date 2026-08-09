package com.gangwon.companion.domain.theme.controller;

import com.gangwon.companion.domain.theme.dto.ThemeResponseDto;
import com.gangwon.companion.domain.theme.service.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "테마", description = "여행 테마 목록 조회 API")
@RestController
@RequestMapping("/api/v1/themes")
@RequiredArgsConstructor
public class ThemeController {
    private final ThemeService themeService;

    @Operation(summary = "테마 목록 조회")
    @GetMapping
    public ResponseEntity<List<ThemeResponseDto>> getThemeList() {
        List<ThemeResponseDto> themes = themeService.getThemes();

        return ResponseEntity.ok(themes);
    }
}
