package com.gangwon.companion.domain.theme.service;

import com.gangwon.companion.domain.theme.dto.ThemeResponseDto;
import com.gangwon.companion.domain.theme.entity.Theme;
import com.gangwon.companion.domain.theme.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeService {
    private final ThemeRepository themeRepository;

    public List<ThemeResponseDto> getThemes() {
        List<Theme> themeList = themeRepository.findAllByOrderByDisplayOrderAsc();
        List<ThemeResponseDto> themeResponseList = themeList.stream()
                .map(theme -> ThemeResponseDto.from(theme))
                .toList();

        return themeResponseList;
    }
}
