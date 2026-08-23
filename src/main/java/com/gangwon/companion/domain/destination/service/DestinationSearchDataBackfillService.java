package com.gangwon.companion.domain.destination.service;

import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DestinationSearchDataBackfillService {
    private final PetInfoRepository petInfoRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;
    private final DestinationSearchDataNormalizer normalizer;

    @Transactional
    public BackfillResult backfill() {
        int petUpdated = 0;
        for (PetInfo info : petInfoRepository.findAll()) {
            if (normalizer.normalize(info)) {
                petUpdated++;
            }
        }

        int accessibilityUpdated = 0;
        for (AccessibilityInfo info : accessibilityInfoRepository.findAll()) {
            if (normalizer.normalize(info)) {
                accessibilityUpdated++;
            }
        }
        return new BackfillResult(petUpdated, accessibilityUpdated);
    }

    public record BackfillResult(int petUpdated, int accessibilityUpdated) {
    }
}
