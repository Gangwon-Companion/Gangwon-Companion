package com.gangwon.companion.domain.destination.dto;

import com.gangwon.companion.domain.theme.entity.Theme;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ThemeDestinationListResponseDto {
    private final Long themeId;
    private final String themeCode;
    private final String themeName;
    private final List<DestinationListResponseDto> destinationList;

    public static ThemeDestinationListResponseDto from(Theme theme, List<DestinationListResponseDto> destinationList) {
        return new ThemeDestinationListResponseDto(
                theme.getId(),
                theme.getCode(),
                theme.getName(),
                destinationList
        );
    }
}
