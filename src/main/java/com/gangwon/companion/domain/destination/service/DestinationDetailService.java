package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.dto.DestinationDetailResponseDto;
import com.gangwon.companion.domain.destination.dto.DestinationImageResponseDto;
import com.gangwon.companion.domain.destination.dto.PetInfoResponseDto;
import com.gangwon.companion.domain.destination.dto.AccessibilityInfoResponseDto;
import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationDetailRepository;
import com.gangwon.companion.domain.destination.repository.DestinationImageRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DestinationDetailService {
    private final DestinationDetailRepository destinationDetailRepository;
    private final DestinationImageRepository destinationImageRepository;
    private final PetInfoRepository petInfoRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;

    public DestinationDetailResponseDto getDestinationDetailByDestinationId(Long destinationId, boolean pet, boolean accessibility) {

        SourceType sourceType = resolveSourceType(pet, accessibility);
        DestinationDetail destinationDetail = destinationDetailRepository.findByDestinationIdAndSourceType(destinationId, sourceType)
                .orElseThrow(() -> new BusinessException(ErrorCode.DESTINATION_DETAIL_NOT_FOUND));

        List<DestinationImageResponseDto> destinationImageList = destinationImageRepository.findByDestinationIdAndSourceType(destinationId, sourceType).stream()
                .map(destinationImage -> DestinationImageResponseDto.from(destinationImage))
                .toList();

        PetInfoResponseDto petInfo = pet
                ? petInfoRepository.findByDestinationId(destinationId)
                .map(PetInfoResponseDto::from)
                .orElse(null)
                : null;

        AccessibilityInfoResponseDto accessibilityInfo = accessibility
                ? accessibilityInfoRepository.findByDestinationId(destinationId)
                .map(AccessibilityInfoResponseDto::from)
                .orElse(null)
                : null;

        return DestinationDetailResponseDto.from(destinationDetail, destinationImageList, petInfo, accessibilityInfo);
    }

    private SourceType resolveSourceType(boolean pet, boolean accessibility) {
        if (pet && accessibility) {
            return SourceType.PET;
        }

        if (pet) {
            return SourceType.PET;
        }

        if (accessibility) {
            return SourceType.ACCESSIBILITY;
        }

        return SourceType.KOREAN;
    }
}
