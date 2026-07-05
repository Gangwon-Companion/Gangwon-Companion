package com.gangwon.companion.domain.touristcongestion.service;

import com.gangwon.companion.domain.touristcongestion.dto.request.HotplaceSearchCriteria;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceListResponse;
import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import com.gangwon.companion.domain.touristcongestion.repository.TouristCongestionRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HotplaceServiceTest {

    @Mock
    TouristCongestionRateRepository repository;

    @InjectMocks
    HotplaceService hotplaceService;

    @Test
    @DisplayName("searchHotplaces de-duplicates places and returns top 5 by congestion rate")
    void searchHotplaces_usesExpectedSort_andLimit() {
        TouristCongestionRate spotAOld = rate(1L, "20260701", 70.0, "A", "Spot A");
        TouristCongestionRate spotANew = rate(2L, "20260702", 90.0, "A", "Spot A");
        TouristCongestionRate spotB = rate(3L, "20260701", 80.0, "B", "Spot B");
        TouristCongestionRate spotC = rate(4L, "20260703", 60.0, "C", "Spot C");
        TouristCongestionRate spotD = rate(5L, "20260702", 50.0, "D", "Spot D");
        TouristCongestionRate spotE = rate(6L, "20260701", 40.0, "E", "Spot E");
        TouristCongestionRate spotF = rate(7L, "20260701", 30.0, "F", "Spot F");

        given(repository.findAll(any(Specification.class)))
                .willReturn(List.of(spotAOld, spotANew, spotB, spotC, spotD, spotE, spotF));

        HotplaceListResponse response = hotplaceService.searchHotplaces(
                new HotplaceSearchCriteria(null, null, "today", 3, 20)
        );

        verify(repository).findAll(any(Specification.class));
        assertThat(response.getTotalCount()).isEqualTo(6);
        assertThat(response.getItems()).hasSize(5);
        assertThat(response.getItems().stream().map(item -> item.getHotplaceId())).containsExactly(
                2L, 3L, 4L, 5L, 6L
        );
        assertThat(response.getItems().stream().map(item -> item.getCongestionRate())).containsExactly(
                90.0, 80.0, 60.0, 50.0, 40.0
        );
    }

    private TouristCongestionRate rate(Long id, String baseDate, Double congestionRate,
                                       String signguName, String attractionName) {
        TouristCongestionRate rate = TouristCongestionRate.builder()
                .areaCode("51")
                .signguCode("51110")
                .areaName("Gangwon")
                .signguName(signguName)
                .attractionName(attractionName)
                .congestionRate(congestionRate)
                .baseDate(baseDate)
                .rawPayload("{}")
                .externalKey("key-" + id)
                .build();
        try {
            var field = TouristCongestionRate.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(rate, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return rate;
    }
}
