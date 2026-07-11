package com.gangwon.companion.domain.destination.controller;

import com.gangwon.companion.domain.destination.dto.DestinationDetailResponseDto;
import com.gangwon.companion.domain.destination.service.DestinationDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/destinations")
public class DestinationDetailController {
    private final DestinationDetailService destinationDetailService;

    @GetMapping("/{destinationId}/detail")
    public ResponseEntity<DestinationDetailResponseDto> getDestinationDetail(
            @PathVariable Long destinationId,
            @RequestParam(defaultValue = "false") boolean pet,
            @RequestParam(defaultValue = "false") boolean accessibility
    ) {
        DestinationDetailResponseDto destinationDetail = destinationDetailService.getDestinationDetailByDestinationId(destinationId, pet, accessibility);

        return ResponseEntity.ok(destinationDetail);
    }

}
