package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.ThemeDestinationListResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/themes/{themeId}/destinations")
public class DestinationController {
    private final DestinationService destinationService;

    @GetMapping
    public ResponseEntity<ThemeDestinationListResponseDto> getDestinationListByThemeId(@PathVariable Long themeId,
                                                                                       @RequestParam(defaultValue = "false") boolean pet,
                                                                                       @RequestParam(defaultValue = "false") boolean accessibility) {
        ThemeDestinationListResponseDto destinationListByThemeId = destinationService.getDestinationListByThemeId(themeId, pet, accessibility);

        return ResponseEntity.ok(destinationListByThemeId);
    }
}
