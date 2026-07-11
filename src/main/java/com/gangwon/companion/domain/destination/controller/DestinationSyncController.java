package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.DestinationDetailSyncResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationSyncService;
import com.gangwon.companion.domain.destination.service.DestinationDetailSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/destinations")
public class DestinationSyncController {

    private final DestinationSyncService destinationSyncService;
    private final DestinationDetailSyncService destinationDetailSyncService;

    @PostMapping("/sync/korean")
    public ResponseEntity<Map<String, Integer>> syncKoreanDestinations() {
        int savedCount = destinationSyncService.syncKoreanDestinations();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    @PostMapping("/sync/pet")
    public ResponseEntity<Map<String, Integer>> syncPetDestinations() {
        int savedCount = destinationSyncService.syncPetDestinations();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    @PostMapping("/sync/accessibility")
    public ResponseEntity<Map<String, Integer>> syncAccessibilityDestinations() {
        int savedCount = destinationSyncService.syncAccessibilityDestinations();
        return ResponseEntity.ok(Map.of("savedCount", savedCount));
    }

    @PostMapping("/details/sync/korean")
    public ResponseEntity<DestinationDetailSyncResponseDto> syncKoreanDestinationDetails(
            @RequestParam(defaultValue = "50") int limit
    ) {
        DestinationDetailSyncResponseDto response = destinationDetailSyncService.syncKoreanDestinationDetails(limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/details/sync/pet")
    public ResponseEntity<DestinationDetailSyncResponseDto> syncPetDestinationDetails(
            @RequestParam(defaultValue = "50") int limit
    ) {
        DestinationDetailSyncResponseDto response = destinationDetailSyncService.syncPetDestinationDetails(limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/details/sync/accessibility")
    public ResponseEntity<DestinationDetailSyncResponseDto> syncAccessibilityDestinationDetails(
            @RequestParam(defaultValue = "50") int limit
    ) {
        DestinationDetailSyncResponseDto response = destinationDetailSyncService.syncAccessibilityDestinationDetails(limit);
        return ResponseEntity.ok(response);
    }
}
