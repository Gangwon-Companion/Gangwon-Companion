package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.dto.DestinationListResponseDto;
import com.gangwon.companion.domain.destination.dto.ThemeDestinationListResponseDto;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.theme.entity.Theme;
import com.gangwon.companion.domain.theme.repository.ThemeRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DestinationService {
    private final ThemeRepository themeRepository;
    private final DestinationRepository destinationRepository;

    public ThemeDestinationListResponseDto getDestinationListByThemeId(Long themeId, boolean pet, boolean accessibility) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.THEME_NOT_FOUND));

        List<Destination> destinations;
        if (pet && accessibility) {
            destinations = destinationRepository.findByThemeIdAndBothSourceTypes(
                    themeId,
                    SourceType.PET,
                    SourceType.ACCESSIBILITY
            );
        } else if (pet) {
            destinations = destinationRepository.findByThemeIdAndSourceType(themeId, SourceType.PET);
        } else if (accessibility) {
            destinations = destinationRepository.findByThemeIdAndSourceType(themeId, SourceType.ACCESSIBILITY);
        } else {
            destinations = destinationRepository.findByThemeId(themeId);
        }

        List<DestinationListResponseDto> destinationList = destinations.stream()
                .map(destination -> DestinationListResponseDto.from(destination))
                .toList();

        return ThemeDestinationListResponseDto.from(theme, destinationList);
    }

}
