package com.gangwon.companion.domain.touristcongestion.repository;

import com.gangwon.companion.domain.touristcongestion.dto.request.HotplaceSearchCriteria;
import com.gangwon.companion.domain.touristcongestion.dto.request.HotplacePeriod;
import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class TouristCongestionRateSpecifications {

    private TouristCongestionRateSpecifications() {
    }

    public static Specification<TouristCongestionRate> from(HotplaceSearchCriteria criteria) {
        return keywordContains(criteria.keyword())
                .and(periodBetween(criteria.period()))
                .and(regionEquals(criteria.region()));
    }

    private static Specification<TouristCongestionRate> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("attractionName")), pattern),
                cb.like(cb.lower(root.get("areaName")), pattern),
                cb.like(cb.lower(root.get("signguName")), pattern)
        );
    }

    private static Specification<TouristCongestionRate> regionEquals(String region) {
        if (!StringUtils.hasText(region)) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("signguName"), region);
    }

    private static Specification<TouristCongestionRate> periodBetween(String period) {
        HotplacePeriod resolved = HotplacePeriod.from(period);
        String start = resolved.startDateValue();
        String end = resolved.endDateValue();
        return (root, query, cb) -> cb.between(root.get("baseDate"), start, end);
    }
}
