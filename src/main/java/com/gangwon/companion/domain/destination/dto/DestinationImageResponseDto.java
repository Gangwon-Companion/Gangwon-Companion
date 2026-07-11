package com.gangwon.companion.domain.destination.dto;

import com.gangwon.companion.domain.destination.entity.DestinationImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DestinationImageResponseDto {
    private Long id;
    private Long destinationId;
    private String originImgUrl;
    private String smallImgUrl;
    private String serialNum;

    public static DestinationImageResponseDto from(DestinationImage destinationImage) {
        return new DestinationImageResponseDto(
                destinationImage.getId(),
                destinationImage.getDestination().getId(),
                destinationImage.getOriginImgUrl(),
                destinationImage.getSmallImgUrl(),
                destinationImage.getSerialNum()
        );
    }
}
