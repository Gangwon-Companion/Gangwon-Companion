package com.gangwon.companion.domain.destination.dto;

import com.gangwon.companion.domain.destination.entity.Destination;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DestinationListResponseDto {
    private final Long id;
    private final String title;
    private final String firstImage;
    private final Double rating;
    private final Long reviewCount;

    public static DestinationListResponseDto from(Destination destination) {
        return new DestinationListResponseDto(
                destination.getId(),
                destination.getTitle(),
                destination.getFirstImage(),
                destination.getRating(),
                destination.getReviewCount());
    }

}
