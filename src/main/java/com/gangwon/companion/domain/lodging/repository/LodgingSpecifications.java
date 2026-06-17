package com.gangwon.companion.domain.lodging.repository;

import com.gangwon.companion.domain.lodging.dto.LodgingSearchCriteria;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class LodgingSpecifications {

    private LodgingSpecifications() {
    }

    public static Specification<Lodging> from(LodgingSearchCriteria criteria) {
        return Specification
                .where(keywordContains(criteria.keyword()))
                .and(regionEquals(criteria.region()))
                .and(priceGreaterThanOrEqual(criteria.minPrice()))
                .and(priceLessThanOrEqual(criteria.maxPrice()))
                .and(ratingGreaterThanOrEqual(criteria.rating()));
    }

    private static Specification<Lodging> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    private static Specification<Lodging> regionEquals(String region) {
        if (!StringUtils.hasText(region)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("region"), region);
    }

    private static Specification<Lodging> priceGreaterThanOrEqual(Long minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private static Specification<Lodging> priceLessThanOrEqual(Long maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    private static Specification<Lodging> ratingGreaterThanOrEqual(Double rating) {
        if (rating == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), rating);
    }
}
