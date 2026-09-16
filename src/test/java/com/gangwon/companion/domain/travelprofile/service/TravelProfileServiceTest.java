package com.gangwon.companion.domain.travelprofile.service;

import com.gangwon.companion.domain.travelprofile.entity.TravelProfile;
import com.gangwon.companion.domain.travelprofile.repository.TravelProfileRepository;
import com.gangwon.companion.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TravelProfileServiceTest {
    @Mock TravelProfileRepository repository;
    @Mock UserRepository userRepository;
    @Mock TravelProfile profile;
    private TravelProfileService service;

    @BeforeEach
    void setUp() {
        service = new TravelProfileService(repository, userRepository);
        ReflectionTestUtils.setField(service, "currentVersion", "travel-profile-llm-v1");
        ReflectionTestUtils.setField(service, "completedTtl", Duration.ofDays(7));
    }

    @Test
    void returnsRecentCompletedProfileAsRecommendationContext() {
        given(repository.findByUserUsername("owner")).willReturn(Optional.of(profile));
        given(profile.getStatus()).willReturn(TravelProfile.ProfileStatus.COMPLETED);
        given(profile.getAnalyzedAt()).willReturn(Instant.now().minus(Duration.ofDays(1)));
        given(profile.getAnalysisVersion()).willReturn("travel-profile-llm-v1");
        given(profile.getTravelerType()).willReturn(TravelProfile.TravelerType.NATURE_HEALING);
        given(profile.getTags()).willReturn(List.of("자연", "산책"));
        given(profile.getConfidence()).willReturn(0.82);

        var context = service.findUsableContext("owner");

        assertThat(context).isPresent();
        assertThat(context.orElseThrow().travelerType()).isEqualTo("NATURE_HEALING");
    }

    @Test
    void excludesExpiredOrOldVersionProfile() {
        given(repository.findByUserUsername("owner")).willReturn(Optional.of(profile));
        given(profile.getStatus()).willReturn(TravelProfile.ProfileStatus.COMPLETED);
        given(profile.getAnalyzedAt()).willReturn(Instant.now().minus(Duration.ofDays(8)));

        assertThat(service.findUsableContext("owner")).isEmpty();
    }
}
