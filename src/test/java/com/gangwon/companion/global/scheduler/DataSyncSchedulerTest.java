package com.gangwon.companion.global.scheduler;

import com.gangwon.companion.domain.destination.service.DestinationDetailSyncService;
import com.gangwon.companion.domain.destination.service.DestinationSyncService;
import com.gangwon.companion.domain.lodging.service.LodgingSyncService;
import com.gangwon.companion.domain.restaurant.service.RestaurantSyncService;
import com.gangwon.companion.domain.touristcongestion.service.TouristCongestionRateSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataSyncSchedulerTest {

    @Mock
    DestinationSyncService destinationSyncService;

    @Mock
    DestinationDetailSyncService destinationDetailSyncService;

    @Mock
    RestaurantSyncService restaurantSyncService;

    @Mock
    LodgingSyncService lodgingSyncService;

    @Mock
    TouristCongestionRateSyncService touristCongestionRateSyncService;

    DataSyncScheduler dataSyncScheduler;

    @BeforeEach
    void setUp() {
        dataSyncScheduler = new DataSyncScheduler(
                destinationSyncService,
                destinationDetailSyncService,
                restaurantSyncService,
                lodgingSyncService,
                touristCongestionRateSyncService
        );
        ReflectionTestUtils.setField(dataSyncScheduler, "destinationSyncEnabled", true);
        ReflectionTestUtils.setField(dataSyncScheduler, "destinationDetailSyncEnabled", true);
        ReflectionTestUtils.setField(dataSyncScheduler, "destinationDetailSyncLimit", 50);
    }

    @Test
    void syncAllCallsEverySyncService() {
        given(destinationSyncService.syncKoreanDestinations()).willReturn(1);
        given(destinationSyncService.syncPetDestinations()).willReturn(2);
        given(destinationSyncService.syncAccessibilityDestinations()).willReturn(3);

        dataSyncScheduler.syncAll();

        verify(destinationSyncService).syncKoreanDestinations();
        verify(destinationSyncService).syncPetDestinations();
        verify(destinationSyncService).syncAccessibilityDestinations();
        verify(destinationDetailSyncService).syncKoreanDestinationDetails(50);
        verify(destinationDetailSyncService).syncPetDestinationDetails(50);
        verify(destinationDetailSyncService).syncAccessibilityDestinationDetails(50);
        verify(restaurantSyncService).sync();
        verify(lodgingSyncService).sync();
        verify(touristCongestionRateSyncService).sync();
        verify(lodgingSyncService).enrichDetails();
    }

    @Test
    void syncAllSkipsDestinationSyncsWhenDisabled() {
        ReflectionTestUtils.setField(dataSyncScheduler, "destinationSyncEnabled", false);
        ReflectionTestUtils.setField(dataSyncScheduler, "destinationDetailSyncEnabled", false);

        dataSyncScheduler.syncAll();

        verify(destinationSyncService, never()).syncKoreanDestinations();
        verify(destinationSyncService, never()).syncPetDestinations();
        verify(destinationSyncService, never()).syncAccessibilityDestinations();
        verify(destinationDetailSyncService, never()).syncKoreanDestinationDetails(50);
        verify(destinationDetailSyncService, never()).syncPetDestinationDetails(50);
        verify(destinationDetailSyncService, never()).syncAccessibilityDestinationDetails(50);
        verify(restaurantSyncService).sync();
        verify(lodgingSyncService).sync();
        verify(touristCongestionRateSyncService).sync();
        verify(lodgingSyncService).enrichDetails();
    }
}
