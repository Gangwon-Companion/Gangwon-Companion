package com.gangwon.companion.domain.travelprofile.controller;

import com.gangwon.companion.domain.travelprofile.dto.TravelProfileJobResponses.Submitted;
import com.gangwon.companion.domain.travelprofile.dto.TravelProfileResponse;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfileAnalysisJob;
import com.gangwon.companion.domain.travelprofile.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TravelProfileControllerTest {
    @Mock TravelProfileService profileService;
    @Mock TravelProfileAnalysisJobService jobService;
    @InjectMocks TravelProfileController controller;
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("owner", "n/a");

    @Test void returnsNotAnalyzedProfile() {
        given(profileService.get("owner")).willReturn(TravelProfileResponse.notAnalyzed());
        assertThat(controller.get(auth).status()).isEqualTo("NOT_ANALYZED");
    }

    @Test void submitsAnalysisJobWithAcceptedStatus() {
        UUID id = UUID.randomUUID();
        given(jobService.submit("owner")).willReturn(new Submitted(id, TravelProfileAnalysisJob.Status.PENDING));
        var response = controller.submit(auth);
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody().jobId()).isEqualTo(id);
        verify(jobService).submit("owner");
    }

}
