package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.dto.ThemeDestinationListResponseDto;
import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.DestinationSource;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.repository.DestinationRepository;
import com.gangwon.companion.domain.destination.repository.DestinationSourceRepository;
import com.gangwon.companion.domain.theme.entity.Theme;
import com.gangwon.companion.domain.theme.repository.ThemeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DestinationServiceTest {

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private DestinationSourceRepository destinationSourceRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    void getDestinationsByThemeId() {
        Theme testThemeNa = themeRepository.save(new Theme("TEST_NA", "Test Nature", 999));
        Theme testThemeHs = themeRepository.save(new Theme("TEST_HS", "Test History", 998));

        destinationRepository.save(Destination.builder().theme(testThemeNa)
                .primaryContentId(1001L)
                .primarySourceType(SourceType.KOREAN)
                .contentTypeId(12)
                .title("Test Nature Destination")
                .addr1("Gangwon Sokcho")
                .firstImage("https://example.com/nature.jpg")
                .tel("033-000-0000")
                .build());

        destinationRepository.save(Destination.builder().theme(testThemeNa)
                .primaryContentId(1002L)
                .primarySourceType(SourceType.KOREAN)
                .contentTypeId(12)
                .title("Test Nature Destination 2")
                .addr1("Gangwon Gangneung")
                .firstImage("https://example.com/nature2.jpg")
                .tel("033-000-0000")
                .build());

        destinationRepository.save(Destination.builder().theme(testThemeHs)
                .primaryContentId(1003L)
                .primarySourceType(SourceType.KOREAN)
                .contentTypeId(13)
                .title("Test History Destination")
                .addr1("Gangwon Sokcho")
                .firstImage("https://example.com/history.jpg")
                .tel("033-000-0000")
                .build());

        ThemeDestinationListResponseDto naResult =
                destinationService.getDestinationListByThemeId(testThemeNa.getId(), false, false);
        assertThat(naResult.getDestinationList())
                .extracting(destination -> destination.getTitle())
                .contains("Test Nature Destination", "Test Nature Destination 2");

        assertThat(naResult.getDestinationList()).hasSize(2);
        assertThat(naResult.getThemeId()).isEqualTo(testThemeNa.getId());
        assertThat(naResult.getThemeCode()).isEqualTo("TEST_NA");
        assertThat(naResult.getThemeName()).isEqualTo(testThemeNa.getName());

        ThemeDestinationListResponseDto hsResult =
                destinationService.getDestinationListByThemeId(testThemeHs.getId(), false, false);
        assertThat(hsResult.getDestinationList())
                .extracting(destination -> destination.getTitle())
                .contains("Test History Destination");

        assertThat(hsResult.getDestinationList()).hasSize(1);
        assertThat(hsResult.getThemeId()).isEqualTo(testThemeHs.getId());
        assertThat(hsResult.getThemeCode()).isEqualTo("TEST_HS");
        assertThat(hsResult.getThemeName()).isEqualTo(testThemeHs.getName());
    }

    @Test
    void getDestinationsByThemeIdWithSourceFilters() {
        Theme theme = themeRepository.save(new Theme("TEST_FILTER", "Test Filter", 997));

        Destination petOnly = saveDestination(theme, "Pet Only", 2001L);
        Destination accessibilityOnly = saveDestination(theme, "Accessibility Only", 2002L);
        Destination both = saveDestination(theme, "Both", 2003L);
        saveDestination(theme, "No Filter Source", 2004L);

        saveSource(petOnly, SourceType.PET, 3001L);
        saveSource(accessibilityOnly, SourceType.ACCESSIBILITY, 3002L);
        saveSource(both, SourceType.PET, 3003L);
        saveSource(both, SourceType.ACCESSIBILITY, 3004L);

        ThemeDestinationListResponseDto allResult =
                destinationService.getDestinationListByThemeId(theme.getId(), false, false);
        assertThat(allResult.getDestinationList())
                .extracting(destination -> destination.getTitle())
                .containsExactlyInAnyOrder("Pet Only", "Accessibility Only", "Both", "No Filter Source");

        ThemeDestinationListResponseDto petResult =
                destinationService.getDestinationListByThemeId(theme.getId(), true, false);
        assertThat(petResult.getDestinationList())
                .extracting(destination -> destination.getTitle())
                .containsExactlyInAnyOrder("Pet Only", "Both");

        ThemeDestinationListResponseDto accessibilityResult =
                destinationService.getDestinationListByThemeId(theme.getId(), false, true);
        assertThat(accessibilityResult.getDestinationList())
                .extracting(destination -> destination.getTitle())
                .containsExactlyInAnyOrder("Accessibility Only", "Both");

        ThemeDestinationListResponseDto bothResult =
                destinationService.getDestinationListByThemeId(theme.getId(), true, true);
        assertThat(bothResult.getDestinationList())
                .extracting(destination -> destination.getTitle())
                .containsExactly("Both");
    }

    private Destination saveDestination(Theme theme, String title, Long contentId) {
        return destinationRepository.save(Destination.builder()
                .theme(theme)
                .primaryContentId(contentId)
                .primarySourceType(SourceType.KOREAN)
                .contentTypeId(12)
                .title(title)
                .addr1("Gangwon Test Address")
                .firstImage("https://example.com/test.jpg")
                .tel("033-000-0000")
                .build());
    }

    private void saveSource(Destination destination, SourceType sourceType, Long contentId) {
        destinationSourceRepository.save(DestinationSource.builder()
                .destination(destination)
                .sourceType(sourceType)
                .contentId(contentId)
                .contentTypeId(12)
                .build());
    }
}
