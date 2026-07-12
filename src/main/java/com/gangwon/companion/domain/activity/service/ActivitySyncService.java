package com.gangwon.companion.domain.activity.service;

import com.gangwon.companion.domain.activity.client.TourApiActivityClient;
import com.gangwon.companion.domain.activity.client.TourApiActivityItem;
import com.gangwon.companion.domain.activity.entity.Activity;
import com.gangwon.companion.domain.activity.entity.ActivityCategory;
import com.gangwon.companion.domain.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivitySyncService {

    private static final Logger log = LoggerFactory.getLogger(ActivitySyncService.class);
    private static final List<Integer> CONTENT_TYPE_IDS = List.of(12, 14, 28);
    private static final String EXPERIENCE_CATEGORY_CODE = "A0203";

    private final TourApiActivityClient tourApiActivityClient;
    private final ActivityRepository activityRepository;

    @Value("${activity.sync.rows-per-page:200}")
    private int rowsPerPage;

    @Value("${activity.sync.max-pages:3}")
    private int maxPages;

    public int syncGangwonActivities() {
        List<TourApiActivityItem> items = new ArrayList<>();
        for (int contentTypeId : CONTENT_TYPE_IDS) {
            items.addAll(tourApiActivityClient.getGangwonActivities(contentTypeId, rowsPerPage, maxPages));
        }
        int savedCount = save(items);
        log.info("TourAPI activity sync completed. received={}, saved={}", items.size(), savedCount);
        return savedCount;
    }

    @Transactional
    protected int save(List<TourApiActivityItem> items) {
        List<TourApiActivityItem> validItems = items.stream()
                .filter(item -> item.contentId() != null)
                .filter(item -> item.title() != null && !item.title().isBlank())
                .toList();

        Map<Long, Activity> existingActivities = activityRepository
                .findAllByTourContentIdIn(validItems.stream().map(TourApiActivityItem::contentId).toList())
                .stream()
                .collect(Collectors.toMap(Activity::getTourContentId, Function.identity()));

        List<Activity> activities = validItems.stream()
                .map(item -> toActivity(item, existingActivities.get(item.contentId())))
                .filter(Objects::nonNull)
                .toList();
        activityRepository.saveAll(activities);
        return activities.size();
    }

    private Activity toActivity(TourApiActivityItem item, Activity activity) {
        Activity target = activity == null
                ? new Activity(item.contentId(), item.contentTypeId())
                : activity;
        target.updateListData(
                category(item),
                item.title(),
                extractRegion(item.address()),
                item.address(),
                item.imageUrl(),
                item.thumbnailUrl(),
                item.telephone(),
                item.latitude(),
                item.longitude(),
                item.modifiedAt()
        );
        return target;
    }

    private ActivityCategory category(TourApiActivityItem item) {
        if (item.contentTypeId() == 28) {
            return ActivityCategory.LEISURE;
        }
        if (EXPERIENCE_CATEGORY_CODE.equals(item.categoryCode2())) {
            return ActivityCategory.EXPERIENCE;
        }
        return ActivityCategory.TOURISM;
    }

    private String extractRegion(String address) {
        if (address == null || address.isBlank()) {
            return "강원특별자치도";
        }
        String[] parts = address.trim().split("\\s+");
        return parts.length > 1 ? parts[1] : parts[0];
    }
}
