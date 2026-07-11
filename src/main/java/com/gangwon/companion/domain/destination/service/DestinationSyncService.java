package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationSource;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.external.client.AccessibilityTourApiClient;
import com.gangwon.companion.domain.destination.external.client.PetTourApiClient;
import com.gangwon.companion.domain.destination.external.client.TourApiClient;
import com.gangwon.companion.domain.destination.external.dto.destinationApi.DestinationApiItem;
import com.gangwon.companion.domain.destination.external.dto.destinationApi.DestinationApiResponse;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.DestinationSourceRepository;
import com.gangwon.companion.domain.theme.entity.Theme;
import com.gangwon.companion.domain.theme.repository.ThemeRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class DestinationSyncService {

    private static final Set<String> INCLUDED_THEME_CODES = Set.of(
            "EV",
            "EX",
            "HS",
            "LS",
            "NA",
            "SH",
            "VE"
    );

    private final TourApiClient tourApiClient;
    private final PetTourApiClient petTourApiClient;
    private final AccessibilityTourApiClient accessibilityTourApiClient;
    private final DestinationSourceRepository destinationSourceRepository;
    private final DestinationRepository destinationRepository;
    private final ThemeRepository themeRepository;

    @Transactional
    public int syncKoreanDestinations() {
        return syncDestinationPages(
                SourceType.KOREAN,
                (pageNo, numOfRows) -> tourApiClient.fetchDestinations(pageNo, numOfRows)
        );
    }

    @Transactional
    public int syncPetDestinations() {
        return syncDestinationPages(
                SourceType.PET,
                (pageNo, numOfRows) -> petTourApiClient.fetchDestinations(pageNo, numOfRows)
        );
    }

    @Transactional
    public int syncAccessibilityDestinations() {
        return syncDestinationPages(
                SourceType.ACCESSIBILITY,
                (pageNo, numOfRows) -> accessibilityTourApiClient.fetchDestinations(pageNo, numOfRows)
        );
    }

    private <T extends DestinationApiItem, R extends DestinationApiResponse<T>> int syncDestinationPages(
            SourceType sourceType,
            BiFunction<Integer, Integer, R> fetcher
    ) {
        int pageNo = 1;
        int numOfRows = 100;
        int savedCount = 0;
        int totalPages = 1;

        while (pageNo <= totalPages) {
            R response = fetcher.apply(pageNo, numOfRows);
            if (response == null) {
                break;
            }

            if (pageNo == 1) {
                totalPages = calculateTotalPages(response.getTotalCount(), numOfRows);
            }

            List<T> items = response.getItems();
            for (T item : items) {
                if (syncDestinationItem(item, sourceType)) {
                    savedCount++;
                }
            }

            pageNo++;
        }

        return savedCount;
    }

    private int calculateTotalPages(int totalCount, int numOfRows) {
        if (totalCount <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalCount / numOfRows);
    }

    private boolean isSyncTarget(DestinationApiItem item) {
        return item.getContentId() != null
                && item.getTitle() != null
                && !item.getTitle().isBlank()
                && INCLUDED_THEME_CODES.contains(item.getLclsSystem1());
    }

    private boolean syncDestinationItem(DestinationApiItem item, SourceType sourceType) {
        if (!isSyncTarget(item)) {
            return false;
        }

        if (destinationSourceRepository.existsBySourceTypeAndContentId(sourceType, item.getContentId())) {
            return false;
        }

        Theme theme = themeRepository.findByCode(item.getLclsSystem1())
                .orElseThrow(() -> new BusinessException(ErrorCode.THEME_NOT_FOUND));

        Optional<Destination> existingDestination =
                destinationRepository.findByTitleAndAddr1(item.getTitle(), item.getAddr1());

        Destination destination;
        if (existingDestination.isPresent()) {
            destination = existingDestination.get();
        } else {
            destination = destinationRepository.save(toDestination(item, theme, sourceType));
        }

        destinationSourceRepository.save(toDestinationSource(destination, item, sourceType));
        return true;
    }

    private Destination toDestination(DestinationApiItem item, Theme theme, SourceType sourceType) {
        return Destination.builder()
                .primaryContentId(item.getContentId())
                .primarySourceType(sourceType)
                .contentTypeId(item.getContentTypeId())
                .title(item.getTitle())
                .addr1(item.getAddr1())
                .addr2(item.getAddr2())
                .mapX(item.getMapXAsBigDecimal())
                .mapY(item.getMapYAsBigDecimal())
                .firstImage(item.getFirstImage())
                .firstImage2(item.getFirstImage2())
                .tel(item.getTel())
                .sigunguCode(item.getSigunguCode())
                .lclsSystem1(item.getLclsSystem1())
                .lclsSystem2(item.getLclsSystem2())
                .lclsSystem3(item.getLclsSystem3())
                .theme(theme)
                .build();
    }

    private DestinationSource toDestinationSource(
            Destination destination,
            DestinationApiItem item,
            SourceType sourceType
    ) {
        return DestinationSource.builder()
                .destination(destination)
                .sourceType(sourceType)
                .contentId(item.getContentId())
                .contentTypeId(item.getContentTypeId())
                .build();
    }
}
