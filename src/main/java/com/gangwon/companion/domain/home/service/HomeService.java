package com.gangwon.companion.domain.home.service;

import com.gangwon.companion.domain.home.client.TourApiClient;
import com.gangwon.companion.domain.home.dto.HotPlaceResponse;
import com.gangwon.companion.domain.home.dto.PromotionBannerListResponse;
import com.gangwon.companion.domain.home.dto.PromotionBannerResponse;
import com.gangwon.companion.domain.home.dto.SpecialOfferResponse;
import com.gangwon.companion.domain.home.entity.SpecialOffer;
import com.gangwon.companion.domain.home.repository.SpecialOfferRepository;
import com.gangwon.companion.domain.search.entity.SearchHistory;
import com.gangwon.companion.domain.search.repository.SearchHistoryRepository;
import com.gangwon.companion.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);
    private static final int DEFAULT_LIMIT = 5;

    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final SpecialOfferRepository specialOfferRepository;
    private final TourApiClient tourApiClient;

    public PromotionBannerListResponse getPromotionBanners(String category, int limit) {
        String normalizedCategory = normalize(category);
        List<PromotionBannerResponse> tourApiBanners = getTourApiFestivalBanners(limit);

        List<PromotionBannerResponse> filteredBanners = tourApiBanners.stream()
                .filter(banner -> normalizedCategory == null
                        || banner.category().toLowerCase(Locale.ROOT).equals(normalizedCategory))
                .limit(normalizeLimit(limit))
                .toList();
        boolean festivalAvailable = filteredBanners.stream()
                .anyMatch(banner -> "festival".equals(banner.category()));
        String message = festivalAvailable ? null : "현재 예정 중인 축제가 없습니다.";

        return new PromotionBannerListResponse(festivalAvailable, message, filteredBanners);
    }

    private List<PromotionBannerResponse> getTourApiFestivalBanners(int limit) {
        try {
            int normalizedLimit = normalizeLimit(limit);
            List<PromotionBannerResponse> banners = new ArrayList<>(
                    tourApiClient.getGangwonFestivalBanners(normalizedLimit)
            );

            if (banners.size() < normalizedLimit) {
                int remainingLimit = normalizedLimit - banners.size();
                banners.addAll(tourApiClient.getGangwonSpotBanners(remainingLimit));
            }

            if (!banners.isEmpty()) {
                return banners;
            }
        } catch (RuntimeException e) {
            log.warn("Failed to load TourAPI festival banners. Falling back to sample banners.", e);
        }

        return promotionBanners();
    }

    public List<HotPlaceResponse> getHotPlaces(int limit) {
        return hotPlaces().stream()
                .sorted(Comparator.comparingDouble(HotPlaceResponse::recommendationScore).reversed())
                .limit(normalizeLimit(limit))
                .toList();
    }

    public List<SpecialOfferResponse> getSpecialOffers(String username, String keyword, String region, int limit) {
        String normalizedKeyword = normalize(keyword);
        String normalizedRegion = normalize(region);
        List<SearchHistory> recentSearchHistories = getRecentSearchHistories(username);

        return specialOfferRepository.findAll().stream()
                .map(this::toSpecialOfferResponse)
                .sorted(Comparator.comparingInt((SpecialOfferResponse offer) ->
                        personalizationScore(offer, normalizedKeyword, normalizedRegion, recentSearchHistories)).reversed())
                .limit(normalizeLimit(limit))
                .toList();
    }

    private SpecialOfferResponse toSpecialOfferResponse(SpecialOffer specialOffer) {
        return new SpecialOfferResponse(
                specialOffer.getId(),
                specialOffer.getTitle(),
                specialOffer.getRegion(),
                specialOffer.getCategory(),
                specialOffer.getOriginalPrice(),
                specialOffer.getSalePrice(),
                specialOffer.getDiscountRate(),
                specialOffer.getReason(),
                specialOffer.getImageUrl(),
                specialOffer.getLinkUrl()
        );
    }

    private List<SearchHistory> getRecentSearchHistories(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        return userRepository.findByUsername(username)
                .map(searchHistoryRepository::findTop5ByUserOrderBySearchedAtDesc)
                .orElseGet(List::of);
    }

    private int personalizationScore(
            SpecialOfferResponse offer,
            String keyword,
            String region,
            List<SearchHistory> recentSearchHistories
    ) {
        int score = offer.discountRate();

        if (region != null && offer.region().toLowerCase(Locale.ROOT).contains(region)) {
            score += 30;
        }
        if (keyword != null && (
                offer.title().toLowerCase(Locale.ROOT).contains(keyword)
                        || offer.category().toLowerCase(Locale.ROOT).contains(keyword))) {
            score += 20;
        }

        for (SearchHistory history : recentSearchHistories) {
            String historyKeyword = normalize(history.getKeyword());
            String historyRegion = normalize(history.getRegion());

            if (historyRegion != null && offer.region().toLowerCase(Locale.ROOT).contains(historyRegion)) {
                score += 30;
            }
            if (historyKeyword != null && (
                    offer.title().toLowerCase(Locale.ROOT).contains(historyKeyword)
                            || offer.category().toLowerCase(Locale.ROOT).contains(historyKeyword))) {
                score += 20;
            }
        }

        return score;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 20);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private List<PromotionBannerResponse> promotionBanners() {
        return List.of(
                new PromotionBannerResponse(1L, "festival", "Gangneung Danoje", "Traditional performances and local experiences.", "gangneung", "/images/home/banner-gangneung-dano.jpg", LocalDate.of(2026, 6, 18), LocalDate.of(2026, 6, 25), "/promotions/1"),
                new PromotionBannerResponse(2L, "event", "Chuncheon Lake Busking", "Weekend music around the lakeside trail.", "chuncheon", "/images/home/banner-chuncheon-busking.jpg", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 8, 30), "/promotions/2"),
                new PromotionBannerResponse(3L, "spot", "Jeongseon Arirang Market", "Local food and shops in Jeongseon.", "jeongseon", "/images/home/banner-jeongseon-market.jpg", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "/promotions/3"),
                new PromotionBannerResponse(4L, "spot", "Sokcho Yeonggeumjeong", "Sea walk and sunrise spot.", "sokcho", "/images/home/banner-sokcho-yeonggeumjeong.jpg", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "/promotions/4")
        );
    }

    private List<HotPlaceResponse> hotPlaces() {
        return List.of(
                new HotPlaceResponse(1L, "Sokcho Beach", "sokcho", "East coast beach with rising summer visits.", 42, "normal", 91, 95.4, "/images/home/hot-sokcho-beach.jpg"),
                new HotPlaceResponse(2L, "Gangneung Anmok Beach", "gangneung", "Cafe street and sea view course with high interest.", 35, "crowded", 88, 90.8, "/images/home/hot-anmok.jpg"),
                new HotPlaceResponse(3L, "Chuncheon Samaksan Cable Car", "chuncheon", "View point with growing family travel demand.", 27, "low", 84, 86.1, "/images/home/hot-samyaksan.jpg"),
                new HotPlaceResponse(4L, "Pyeongchang Daegwallyeong Ranch", "pyeongchang", "Highland experience spot with steady interest.", 18, "normal", 82, 81.7, "/images/home/hot-daegwallyeong.jpg")
        );
    }
}
