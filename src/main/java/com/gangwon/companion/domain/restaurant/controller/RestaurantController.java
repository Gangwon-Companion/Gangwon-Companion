package com.gangwon.companion.domain.restaurant.controller;

import com.gangwon.companion.domain.restaurant.dto.RestaurantDetailResponse;
import com.gangwon.companion.domain.restaurant.dto.RestaurantListResponse;
import com.gangwon.companion.domain.restaurant.query.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.service.RestaurantService;
import com.gangwon.companion.global.route.RouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<RestaurantListResponse> searchRestaurants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String menuType,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RestaurantSearchCriteria criteria = new RestaurantSearchCriteria(keyword, menuType, region, page, size);
        return ResponseEntity.ok(restaurantService.searchRestaurants(criteria));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantDetailResponse> getRestaurantDetail(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getRestaurantDetail(restaurantId));
    }

    @GetMapping("/{restaurantId}/route")
    public ResponseEntity<RouteResponse> getRoute(
            @PathVariable Long restaurantId,
            @RequestParam Double userLat,
            @RequestParam Double userLng) {
        return ResponseEntity.ok(restaurantService.getRoute(restaurantId, userLat, userLng));
    }
}
