package com.gangwon.companion.domain.restaurant.repository;

import com.gangwon.companion.domain.restaurant.dto.RestaurantSearchCriteria;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.Arrays;

public class RestaurantSpecifications {

    private RestaurantSpecifications() {
    }

    public static Specification<Restaurant> from(RestaurantSearchCriteria criteria) {
        return keywordContains(criteria.keyword())
                .and(menuTypeEquals(criteria.menuType()))
                .and(regionEquals(criteria.region()));
    }

    private static Specification<Restaurant> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return (root, query, cb) -> cb.conjunction();
        String[] terms = keyword.trim().toLowerCase().split("\\s+");
        return (root, query, cb) -> cb.and(Arrays.stream(terms)
                .map(term -> {
                    String pattern = "%" + term + "%";
                    return cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("menuType")), pattern),
                            cb.like(cb.lower(root.get("region")), pattern),
                            cb.like(cb.lower(root.get("address")), pattern)
                    );
                })
                .toArray(Predicate[]::new));
    }

    private static Specification<Restaurant> menuTypeEquals(String menuType) {
        if (!StringUtils.hasText(menuType)) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("menuType"), menuType);
    }

    private static Specification<Restaurant> regionEquals(String region) {
        if (!StringUtils.hasText(region)) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("region"), region);
    }
}
