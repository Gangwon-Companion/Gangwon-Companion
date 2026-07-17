package com.gangwon.companion.domain.activity.service;

import com.gangwon.companion.domain.activity.client.TourApiActivityClient;
import com.gangwon.companion.domain.activity.client.TourApiActivityDetail;
import com.gangwon.companion.domain.activity.dto.ActivityDetailResponse;
import com.gangwon.companion.domain.activity.dto.ActivityListResponse;
import com.gangwon.companion.domain.activity.dto.ActivitySummaryResponse;
import com.gangwon.companion.domain.activity.entity.Activity;
import com.gangwon.companion.domain.activity.entity.ActivityCategory;
import com.gangwon.companion.domain.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activityRepository;
    private final TourApiActivityClient tourApiActivityClient;

    @Value("${activity.detail-refresh-days:7}")
    private int detailRefreshDays;

    @Transactional(readOnly = true)
    public ActivityListResponse getActivities(
            ActivityCategory category,
            String region,
            String keyword,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }

        Specification<Activity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (category != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (hasText(region)) {
            String pattern = containsPattern(region);
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("region")), pattern),
                    cb.like(cb.lower(root.get("address")), pattern)
            ));
        }
        if (hasText(keyword)) {
            String pattern = containsPattern(keyword);
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("address")), pattern)
            ));
        }

        Page<Activity> result = activityRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "title"))
        );

        return new ActivityListResponse(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public ActivityDetailResponse getActivity(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "액티비티를 찾을 수 없습니다."));

        if (detailNeedsRefresh(activity)) {
            refreshDetail(activity);
        }
        return toDetail(activity);
    }

    private void refreshDetail(Activity activity) {
        try {
            TourApiActivityDetail detail = tourApiActivityClient.getActivityDetail(
                    activity.getTourContentId(),
                    activity.getContentTypeId()
            );
            activity.updateDetail(
                    detail.homepageUrl(),
                    detail.overview(),
                    detail.telephone(),
                    detail.operatingHours(),
                    detail.restDate(),
                    detail.usageFee(),
                    detail.parkingInfo(),
                    detail.reservationUrl()
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh TourAPI activity detail. contentId={}", activity.getTourContentId(), exception);
        }
    }

    private boolean detailNeedsRefresh(Activity activity) {
        return activity.getDetailSyncedAt() == null
                || activity.getDetailSyncedAt().isBefore(LocalDateTime.now().minusDays(detailRefreshDays));
    }

    private ActivitySummaryResponse toSummary(Activity activity) {
        return new ActivitySummaryResponse(
                activity.getId(),
                activity.getTourContentId(),
                activity.getTitle(),
                activity.getCategory(),
                activity.getRegion(),
                activity.getAddress(),
                activity.getImageUrl(),
                activity.getThumbnailUrl(),
                activity.getLatitude(),
                activity.getLongitude()
        );
    }

    private ActivityDetailResponse toDetail(Activity activity) {
        return new ActivityDetailResponse(
                activity.getId(),
                activity.getTourContentId(),
                activity.getTitle(),
                activity.getCategory(),
                activity.getRegion(),
                activity.getAddress(),
                activity.getImageUrl(),
                activity.getTelephone(),
                activity.getHomepageUrl(),
                activity.getOverview(),
                activity.getOperatingHours(),
                activity.getRestDate(),
                activity.getUsageFee(),
                activity.getParkingInfo(),
                activity.getReservationUrl(),
                activity.getLatitude(),
                activity.getLongitude()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String containsPattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
