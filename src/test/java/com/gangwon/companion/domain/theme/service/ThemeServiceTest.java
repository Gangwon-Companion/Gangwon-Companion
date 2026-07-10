package com.gangwon.companion.domain.theme.service;

import com.gangwon.companion.domain.theme.dto.ThemeResponseDto;
import com.gangwon.companion.domain.theme.entity.Theme;
import com.gangwon.companion.domain.theme.repository.ThemeRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@SpringBootTest
class ThemeServiceTest {

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ThemeService themeService;

    Theme exampleTheme = new Theme("NA_Test", "자연관광", 1);
    Theme exampleTheme2 = new Theme("EV_Test", "축제 공연", 2);

    @Test
    void getThemes() {
        themeRepository.save(exampleTheme);
        themeRepository.save(exampleTheme2);

        List<ThemeResponseDto> themes = themeService.getThemes();

        Assertions.assertThat(themes)
                .extracting(theme -> theme.getCode())
                .contains("NA_Test", "EV_Test");

    }
}