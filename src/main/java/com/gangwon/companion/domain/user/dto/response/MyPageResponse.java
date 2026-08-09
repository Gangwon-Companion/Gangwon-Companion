package com.gangwon.companion.domain.user.dto.response;

import com.gangwon.companion.domain.user.entity.User;

import java.time.LocalDateTime;

public record MyPageResponse(
        String username,
        String email,
        String nickname,
        LocalDateTime joinedAt,
        TravelStats travelStats
) {

    public static MyPageResponse of(User user, long savedCourseCount,
                                    long visitedPlaceCount, long reviewCount) {
        return new MyPageResponse(
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getCreatedAt(),
                new TravelStats(savedCourseCount, visitedPlaceCount, reviewCount)
        );
    }

    public record TravelStats(
            long savedCourseCount,
            long visitedPlaceCount,
            long reviewCount
    ) {
    }
}
