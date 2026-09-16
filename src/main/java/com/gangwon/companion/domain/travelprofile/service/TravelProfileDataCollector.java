package com.gangwon.companion.domain.travelprofile.service;

import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import com.gangwon.companion.domain.destination.repository.*;
import com.gangwon.companion.domain.lodging.repository.*;
import com.gangwon.companion.domain.restaurant.repository.*;
import com.gangwon.companion.domain.search.repository.SearchHistoryRepository;
import com.gangwon.companion.domain.travel.entity.PlaceType;
import com.gangwon.companion.domain.travelprofile.dto.AiTravelProfileRequest;
import com.gangwon.companion.domain.visit.repository.VisitRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Component @RequiredArgsConstructor
public class TravelProfileDataCollector {
    private static final int LIMIT = 50;
    private final SearchHistoryRepository searchHistoryRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final LodgingReviewRepository lodgingReviewRepository;
    private final DestinationRepository destinationRepository;
    private final RestaurantRepository restaurantRepository;
    private final LodgingRepository lodgingRepository;

    @Transactional(readOnly = true)
    public AiTravelProfileRequest collect(String username) {
        var page = PageRequest.of(0, LIMIT);
        var searches = searchHistoryRepository.findByUserUsernameOrderBySearchedAtDesc(username, page).stream()
            .map(v -> new AiTravelProfileRequest.Search(v.getKeyword(), v.getRegion(), instant(v.getSearchedAt()))).toList();
        var visits = visitRecordRepository.findByUserUsernameOrderByVisitedAtDesc(username, page).stream()
            .map(v -> place(v.getPlaceType(), v.getPlaceId(), instant(v.getVisitedAt()))).toList();
        var courses = savedCourseRepository.findByUserUsernameOrderByCreatedAtDesc(username, page).stream()
            .map(c -> new AiTravelProfileRequest.SavedCourse(c.getName(), instant(c.getCreatedAt()), c.getPlaces().stream()
                .map(p -> new AiTravelProfileRequest.Place(p.getPlaceType().name(), p.getPlaceId(), p.getName(), region(p.getAddress()))).toList())).toList();
        List<AiTravelProfileRequest.Review> reviews = new ArrayList<>();
        destinationReviewRepository.findByUserUsernameOrderByCreatedAtDesc(username, page).forEach(r -> reviews.add(new AiTravelProfileRequest.Review(
            PlaceType.ATTRACTION.name(), r.getDestination().getId(), r.getDestination().getTitle(), r.getRating(), instant(r.getCreatedAt()))));
        restaurantReviewRepository.findByUserUsernameOrderByCreatedAtDesc(username, page).forEach(r -> reviews.add(new AiTravelProfileRequest.Review(
            PlaceType.RESTAURANT.name(), r.getRestaurant().getId(), r.getRestaurant().getName(), r.getRating(), instant(r.getCreatedAt()))));
        lodgingReviewRepository.findByUserUsernameOrderByCreatedAtDesc(username, page).forEach(r -> reviews.add(new AiTravelProfileRequest.Review(
            PlaceType.LODGING.name(), r.getLodging().getId(), r.getLodging().getName(), r.getRating(), instant(r.getCreatedAt()))));
        reviews.sort(Comparator.comparing(AiTravelProfileRequest.Review::reviewedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return new AiTravelProfileRequest("1.0", Instant.now(), searches, visits, courses, reviews.stream().limit(LIMIT).toList());
    }

    private AiTravelProfileRequest.Visit place(PlaceType type, Long id, Instant at) {
        if (type == PlaceType.ATTRACTION) return destinationRepository.findById(id).map(v -> new AiTravelProfileRequest.Visit(type.name(), id, v.getTitle(), v.getLclsSystem1(), region(v.getAddr1()), at)).orElseGet(() -> unknown(type,id,at));
        if (type == PlaceType.RESTAURANT) return restaurantRepository.findById(id).map(v -> new AiTravelProfileRequest.Visit(type.name(), id, v.getName(), v.getMenuType(), v.getRegion(), at)).orElseGet(() -> unknown(type,id,at));
        return lodgingRepository.findById(id).map(v -> new AiTravelProfileRequest.Visit(type.name(), id, v.getName(), "LODGING", v.getRegion(), at)).orElseGet(() -> unknown(type,id,at));
    }
    private static AiTravelProfileRequest.Visit unknown(PlaceType type, Long id, Instant at) { return new AiTravelProfileRequest.Visit(type.name(), id, null, null, null, at); }
    private static Instant instant(LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant(); }
    private static String region(String address) { return address == null || address.isBlank() ? null : address.trim().split("\\s+")[0]; }
}
