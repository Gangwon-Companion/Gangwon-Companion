package com.gangwon.companion.domain.restaurant.repository;

import com.gangwon.companion.domain.restaurant.dto.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class RestaurantSpecifications {

    private RestaurantSpecifications() {
    }

    public static Specification<Restaurant> from(RestaurantSearchCriteria criteria) {
        return Specification
                .where(keywordContains(criteria.keyword()))
                .and(menuTypeEquals(criteria.menuType()))
                .and(regionEquals(criteria.region()));
    }

    private static Specification<Restaurant> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    private static Specification<Restaurant> menuTypeEquals(String menuType) {
        if (!StringUtils.hasText(menuType)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("menuType"), menuType);
    }

    private static Specification<Restaurant> regionEquals(String region) {
        if (!StringUtils.hasText(region)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("region"), region);
    }
}
