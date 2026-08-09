package com.gangwon.companion.domain.theme.dto;

import com.gangwon.companion.domain.theme.entity.Theme;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ThemeResponseDto {
    private final Long id;
    private final String code;
    private final String name;

    public static ThemeResponseDto from(Theme theme) {
        return new ThemeResponseDto(theme.getId(), theme.getCode(), theme.getName());
    }
}
