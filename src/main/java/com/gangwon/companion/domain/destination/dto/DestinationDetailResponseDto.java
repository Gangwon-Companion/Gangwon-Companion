package com.gangwon.companion.domain.destination.dto;

import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DestinationDetailResponseDto {
    private Long id;
    private Long destinationId;
    private String title;
    private String addr1;
    private String addr2;
    private String tel;
    private String overview;
    private String homepage;
    private String usageTime;
    private String restDate;
    private String parking;
    private String inquiry;
    private List<DestinationImageResponseDto> destinationImageList;
    private PetInfoResponseDto petInfo;
    private AccessibilityInfoResponseDto accessibilityInfo;

    public static DestinationDetailResponseDto from(
            DestinationDetail destinationDetail,
            List<DestinationImageResponseDto> destinationImageList,
            PetInfoResponseDto petInfo,
            AccessibilityInfoResponseDto accessibilityInfo
    ) {
        return new DestinationDetailResponseDto(
                destinationDetail.getId(),
                destinationDetail.getDestination().getId(),
                destinationDetail.getDestination().getTitle(),
                destinationDetail.getDestination().getAddr1(),
                destinationDetail.getDestination().getAddr2(),
                destinationDetail.getDestination().getTel(),
                destinationDetail.getOverview(),
                destinationDetail.getHomepage(),
                destinationDetail.getUsageTime(),
                destinationDetail.getRestDate(),
                destinationDetail.getParking(),
                destinationDetail.getInquiry(),
                destinationImageList,
                petInfo,
                accessibilityInfo
        );
    }



}
