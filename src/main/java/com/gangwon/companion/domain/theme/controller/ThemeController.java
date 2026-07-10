package com.gangwon.companion.domain.theme.controller;

import com.gangwon.companion.domain.theme.dto.ThemeResponseDto;
import com.gangwon.companion.domain.theme.service.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
public class ThemeController {
    private final ThemeService themeService;

    @GetMapping
    public ResponseEntity<List<ThemeResponseDto>> getThemeList() {
        List<ThemeResponseDto> themes = themeService.getThemes();

        return ResponseEntity.ok(themes);
    }
}
