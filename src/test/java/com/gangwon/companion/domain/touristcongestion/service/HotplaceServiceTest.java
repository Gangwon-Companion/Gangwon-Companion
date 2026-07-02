package com.gangwon.companion.domain.touristcongestion.service;

import com.gangwon.companion.domain.touristcongestion.dto.request.HotplaceSearchCriteria;
import com.gangwon.companion.domain.touristcongestion.dto.response.HotplaceListResponse;
import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import com.gangwon.companion.domain.touristcongestion.repository.TouristCongestionRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Captor
    ArgumentCaptor<Pageable> pageableCaptor;

    @InjectMocks
    HotplaceService hotplaceService;

    @Test
    @DisplayName("searchHotplaces uses congestionRate desc and limits to 5 items")
    void searchHotplaces_usesExpectedSort_andLimit() {
        TouristCongestionRate first = rate(1L, "20260702", 90.0, "A", "Spot A");
        TouristCongestionRate second = rate(2L, "20260701", 80.0, "B", "Spot B");
        given(repository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(first, second)));

        HotplaceListResponse response = hotplaceService.searchHotplaces(
                new HotplaceSearchCriteria(null, null, "today", 3, 20)
        );

        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort()).containsExactly(
                Sort.Order.desc("congestionRate"),
                Sort.Order.desc("baseDate"),
                Sort.Order.desc("id")
        );
        assertThat(response.getItems()).hasSize(2);
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
