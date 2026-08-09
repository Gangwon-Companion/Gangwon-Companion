package com.gangwon.companion.domain.home.controller;

import com.gangwon.companion.domain.home.dto.HotPlaceResponse;
import com.gangwon.companion.domain.home.dto.PromotionBannerListResponse;
import com.gangwon.companion.domain.home.dto.PromotionBannerResponse;
import com.gangwon.companion.domain.home.dto.SpecialOfferResponse;
import com.gangwon.companion.domain.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/v1/banners")
    public ResponseEntity<PromotionBannerListResponse> getPromotionBanners(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(homeService.getPromotionBanners(category, limit));
    }

    @GetMapping("/api/v1/home/promotions/hotplace")

    public ResponseEntity<List<HotPlaceResponse>> getHotPlaces(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(homeService.getHotPlaces(limit));
    }

    @GetMapping("/api/v1/promotions/details")
    public ResponseEntity<List<SpecialOfferResponse>> getSpecialOffers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(homeService.getSpecialOffers(username, keyword, region, limit));
    }
}
