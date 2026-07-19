package com.gangwon.companion.domain.destination.dto;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccessibilityInfoResponseDto {
    private Long id;
    private Long destinationId;
    private Long contentId;
    private String parking;
    private String route;
    private String entrance;
    private String elevator;
    private String restroom;
    private String wheelchair;
    private String braileBlock;
    private String helpDog;
    private String guideHuman;

    public static AccessibilityInfoResponseDto from(AccessibilityInfo accessibilityInfo) {
        return new AccessibilityInfoResponseDto(
                accessibilityInfo.getId(),
                accessibilityInfo.getDestination().getId(),
                accessibilityInfo.getContentId(),
                accessibilityInfo.getParking(),
                accessibilityInfo.getRoute(),
                accessibilityInfo.getEntrance(),
                accessibilityInfo.getElevator(),
                accessibilityInfo.getRestroom(),
                accessibilityInfo.getWheelchair(),
                accessibilityInfo.getBraileBlock(),
                accessibilityInfo.getHelpDog(),
                accessibilityInfo.getGuideHuman()
        );
    }
}
