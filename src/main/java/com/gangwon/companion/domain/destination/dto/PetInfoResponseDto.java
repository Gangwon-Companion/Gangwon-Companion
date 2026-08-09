package com.gangwon.companion.domain.destination.dto;

import com.gangwon.companion.domain.destination.entity.PetInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetInfoResponseDto {
    private Long id;
    private Long destinationId;
    private Long contentId;
    private String accompanyType;
    private String needItems;
    private String petFacilities;
    private String caution;
    private String accidentRisk;

    public static PetInfoResponseDto from(PetInfo petInfo) {
        return new PetInfoResponseDto(
                petInfo.getId(),
                petInfo.getDestination().getId(),
                petInfo.getContentId(),
                petInfo.getAccompanyType(),
                petInfo.getNeedItems(),
                petInfo.getPetFacilities(),
                petInfo.getCaution(),
                petInfo.getAccidentRisk()
        );
    }
}
