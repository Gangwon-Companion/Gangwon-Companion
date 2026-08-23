package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DestinationSearchDataBackfillServiceTest {
    @Test
    void repeatedBackfillIsIdempotent() {
        PetInfo petInfo = PetInfo.builder()
                .contentId(1L)
                .accompanyType("소형견만 동반 가능")
                .build();
        AccessibilityInfo accessibilityInfo = AccessibilityInfo.builder()
                .contentId(2L)
                .entrance("경사로 있음")
                .build();
        PetInfoRepository petRepository = mock(PetInfoRepository.class);
        AccessibilityInfoRepository accessibilityRepository = mock(AccessibilityInfoRepository.class);
        when(petRepository.findAll()).thenReturn(List.of(petInfo));
        when(accessibilityRepository.findAll()).thenReturn(List.of(accessibilityInfo));
        DestinationSearchDataBackfillService service = new DestinationSearchDataBackfillService(
                petRepository, accessibilityRepository, new DestinationSearchDataNormalizer());

        var first = service.backfill();
        var second = service.backfill();

        assertThat(first.petUpdated()).isEqualTo(1);
        assertThat(first.accessibilityUpdated()).isEqualTo(1);
        assertThat(second.petUpdated()).isZero();
        assertThat(second.accessibilityUpdated()).isZero();
    }
}
