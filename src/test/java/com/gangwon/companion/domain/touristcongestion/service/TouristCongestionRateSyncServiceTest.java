package com.gangwon.companion.domain.touristcongestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.touristcongestion.entity.TouristCongestionRate;
import com.gangwon.companion.domain.touristcongestion.repository.TouristCongestionRateRepository;
import com.gangwon.companion.global.external.tourapi.tatscnctrate.TatsCnctrRateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TouristCongestionRateSyncServiceTest {

    @Mock TouristCongestionRateRepository repository;
    @Mock TatsCnctrRateClient client;
    @Captor ArgumentCaptor<TouristCongestionRate> rateCaptor;

    @InjectMocks TouristCongestionRateSyncService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void sync_savesNewRecords_whenApiReturnsData() throws Exception {
        given(client.fetchRates(anyString(), anyString(), anyInt(), anyInt()))
                .willReturn(List.of());
        given(client.fetchRates(eq("51"), eq("51110"), eq(1), eq(100)))
                .willReturn(List.of(objectMapper.readTree("""
                        {
                          "signguCd": "51110",
                          "areaNm": "Gangwon",
                          "signguNm": "Chuncheon",
                          "cnctrRate": "42.5",
                          "cnctrGrade": "HIGH",
                          "baseYmd": "20260630",
                          "baseTm": "1200"
                        }
                        """)));

        service.sync();

        verify(repository).save(rateCaptor.capture());
        TouristCongestionRate saved = rateCaptor.getValue();
        assertThat(saved.getAreaCode()).isEqualTo("51");
        assertThat(saved.getSignguCode()).isEqualTo("51110");
        assertThat(saved.getCongestionRate()).isEqualTo(42.5);
    }

    @Test
    void sync_updatesExistingRecord_whenExternalKeyMatches() throws Exception {
        given(client.fetchRates(anyString(), anyString(), anyInt(), anyInt()))
                .willReturn(List.of());
        given(client.fetchRates(eq("51"), eq("51110"), eq(1), eq(100)))
                .willReturn(List.of(objectMapper.readTree("""
                        {
                          "signguCd": "51110",
                          "areaNm": "Gangwon",
                          "signguNm": "Chuncheon",
                          "cnctrRate": "42.5",
                          "cnctrGrade": "HIGH",
                          "baseYmd": "20260630",
                          "baseTm": "1200"
                        }
                        """)));

        given(repository.findByExternalKey(anyString()))
                .willReturn(Optional.of(TouristCongestionRate.builder()
                        .areaCode("51")
                        .signguCode("51110")
                        .externalKey("existing")
                        .build()));

        service.sync();

        verify(repository).findByExternalKey(anyString());
    }
}
