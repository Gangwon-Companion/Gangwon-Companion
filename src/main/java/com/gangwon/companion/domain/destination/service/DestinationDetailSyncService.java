package com.gangwon.companion.domain.destination.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.destination.dto.DestinationDetailSyncResponseDto;
import com.gangwon.companion.domain.destination.entity.AccessibilityInfo;
import com.gangwon.companion.domain.destination.entity.DestinationDetail;
import com.gangwon.companion.domain.destination.entity.DestinationImage;
import com.gangwon.companion.domain.destination.entity.DestinationSource;
import com.gangwon.companion.domain.destination.entity.PetInfo;
import com.gangwon.companion.domain.destination.entity.SourceType;
import com.gangwon.companion.domain.destination.external.client.DestinationDetailApiClient;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailApiResponse;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailCommonItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailImageItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailIntroItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailPetTourItem;
import com.gangwon.companion.domain.destination.external.dto.detailApi.DetailWithTourItem;
import com.gangwon.companion.domain.destination.repository.AccessibilityInfoRepository;
import com.gangwon.companion.domain.destination.repository.DestinationDetailRepository;
import com.gangwon.companion.domain.destination.repository.DestinationImageRepository;
import com.gangwon.companion.domain.destination.repository.DestinationSourceRepository;
import com.gangwon.companion.domain.destination.repository.PetInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationDetailSyncService {

    private final DestinationDetailApiClient destinationDetailApiClient;
    private final DestinationSourceRepository destinationSourceRepository;
    private final DestinationDetailRepository destinationDetailRepository;
    private final DestinationImageRepository destinationImageRepository;
    private final PetInfoRepository petInfoRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DestinationDetailSyncResponseDto syncKoreanDestinationDetails(int limit) {
        return syncDestinationDetails(SourceType.KOREAN, limit);
    }

    @Transactional
    public DestinationDetailSyncResponseDto syncPetDestinationDetails(int limit) {
        return syncDestinationDetails(SourceType.PET, limit);
    }

    @Transactional
    public DestinationDetailSyncResponseDto syncAccessibilityDestinationDetails(int limit) {
        return syncDestinationDetails(SourceType.ACCESSIBILITY, limit);
    }

    private DestinationDetailSyncResponseDto syncDestinationDetails(SourceType sourceType, int limit) {
        List<DestinationSource> destinationSources = destinationSourceRepository.findBySourceType(sourceType);
        int savedCount = 0;
        int processedCount = 0;

        for (DestinationSource destinationSource : destinationSources) {
            if (processedCount >= limit) {
                break;
            }

            boolean detailExists = destinationDetailRepository.existsBySourceTypeAndContentId(
                    destinationSource.getSourceType(),
                    destinationSource.getContentId()
            );
            boolean additionalInfoExists = additionalInfoExists(destinationSource);

            if (detailExists && additionalInfoExists) {
                continue;
            }

            try {
                processedCount++;
                boolean saved = false;
                if (!detailExists && syncDestinationDetail(destinationSource)) {
                    saved = true;
                }
                syncDestinationImages(destinationSource);
                if (syncAdditionalInfo(destinationSource)) {
                    saved = true;
                }
                if (saved) {
                    savedCount++;
                }
            } catch (HttpClientErrorException.TooManyRequests e) {
                return DestinationDetailSyncResponseDto.builder()
                        .savedCount(savedCount)
                        .processedCount(processedCount)
                        .stoppedReason("API_RATE_LIMIT")
                        .build();
            } catch (RestClientException | IllegalStateException e) {
                return DestinationDetailSyncResponseDto.builder()
                        .savedCount(savedCount)
                        .processedCount(processedCount)
                        .stoppedReason("EXTERNAL_API_ERROR")
                        .build();
            }
        }

        return DestinationDetailSyncResponseDto.builder()
                .savedCount(savedCount)
                .processedCount(processedCount)
                .build();
    }

    private boolean syncDestinationDetail(DestinationSource destinationSource) {
        SourceType sourceType = destinationSource.getSourceType();
        Long contentId = destinationSource.getContentId();
        Integer contentTypeId = destinationSource.getContentTypeId();

        DetailApiResponse<DetailCommonItem> commonResponse =
                destinationDetailApiClient.fetchDetailCommon(sourceType, contentId);
        DetailApiResponse<DetailIntroItem> introResponse =
                destinationDetailApiClient.fetchDetailIntro(sourceType, contentId, contentTypeId);

        DetailCommonItem commonItem = firstItem(commonResponse);
        DetailIntroItem introItem = firstItem(introResponse);

        if (commonItem == null && introItem == null) {
            return false;
        }

        destinationDetailRepository.save(DestinationDetail.builder()
                .destination(destinationSource.getDestination())
                .sourceType(sourceType)
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .overview(commonItem == null ? null : commonItem.getOverview())
                .homepage(commonItem == null ? null : commonItem.getHomepage())
                .usageTime(introItem == null ? null : introItem.getUsageTime())
                .restDate(introItem == null ? null : introItem.getRestDate())
                .parking(introItem == null ? null : introItem.getParking())
                .inquiry(introItem == null ? null : introItem.getInquiry())
                .rawCommonJson(toJson(commonResponse.getItems()))
                .rawIntroJson(toJson(introResponse.getItems()))
                .build());

        return true;
    }

    private void syncDestinationImages(DestinationSource destinationSource) {
        SourceType sourceType = destinationSource.getSourceType();
        Long contentId = destinationSource.getContentId();

        DetailApiResponse<DetailImageItem> imageResponse =
                destinationDetailApiClient.fetchDetailImages(sourceType, contentId);

        for (DetailImageItem imageItem : imageResponse.getItems()) {
            if (imageItem.getSerialNum() == null || imageItem.getSerialNum().isBlank()) {
                continue;
            }

            if (destinationImageRepository.existsBySourceTypeAndContentIdAndSerialNum(
                    sourceType,
                    contentId,
                    imageItem.getSerialNum()
            )) {
                continue;
            }

            destinationImageRepository.save(DestinationImage.builder()
                    .destination(destinationSource.getDestination())
                    .sourceType(sourceType)
                    .contentId(contentId)
                    .originImgUrl(imageItem.getOriginImgUrl())
                    .smallImgUrl(imageItem.getSmallImgUrl())
                    .serialNum(imageItem.getSerialNum())
                    .build());
        }
    }

    private boolean additionalInfoExists(DestinationSource destinationSource) {
        Long destinationId = destinationSource.getDestination().getId();
        Long contentId = destinationSource.getContentId();

        return switch (destinationSource.getSourceType()) {
            case KOREAN -> true;
            case PET -> petInfoRepository.existsByDestinationIdAndContentId(destinationId, contentId);
            case ACCESSIBILITY -> accessibilityInfoRepository.existsByDestinationIdAndContentId(destinationId, contentId);
        };
    }

    private boolean syncAdditionalInfo(DestinationSource destinationSource) {
        return switch (destinationSource.getSourceType()) {
            case KOREAN -> false;
            case PET -> syncPetInfo(destinationSource);
            case ACCESSIBILITY -> syncAccessibilityInfo(destinationSource);
        };
    }

    private boolean syncPetInfo(DestinationSource destinationSource) {
        Long destinationId = destinationSource.getDestination().getId();
        Long contentId = destinationSource.getContentId();

        if (petInfoRepository.existsByDestinationIdAndContentId(destinationId, contentId)) {
            return false;
        }

        DetailApiResponse<DetailPetTourItem> petResponse =
                destinationDetailApiClient.fetchDetailPetTour(contentId);
        DetailPetTourItem petItem = firstItem(petResponse);

        if (petItem == null) {
            return false;
        }

        petInfoRepository.save(PetInfo.builder()
                .destination(destinationSource.getDestination())
                .contentId(contentId)
                .accompanyType(petItem.getAccompanyType())
                .needItems(petItem.getNeedItems())
                .petFacilities(petItem.getPetFacilities())
                .caution(petItem.getCaution())
                .accidentRisk(petItem.getAccidentRisk())
                .rawPetJson(toJson(petResponse.getItems()))
                .build());

        return true;
    }

    private boolean syncAccessibilityInfo(DestinationSource destinationSource) {
        Long destinationId = destinationSource.getDestination().getId();
        Long contentId = destinationSource.getContentId();

        if (accessibilityInfoRepository.existsByDestinationIdAndContentId(destinationId, contentId)) {
            return false;
        }

        DetailApiResponse<DetailWithTourItem> accessibilityResponse =
                destinationDetailApiClient.fetchDetailWithTour(contentId);
        DetailWithTourItem accessibilityItem = firstItem(accessibilityResponse);

        if (accessibilityItem == null) {
            return false;
        }

        accessibilityInfoRepository.save(AccessibilityInfo.builder()
                .destination(destinationSource.getDestination())
                .contentId(contentId)
                .parking(accessibilityItem.getParking())
                .route(accessibilityItem.getRoute())
                .entrance(accessibilityItem.getEntrance())
                .elevator(accessibilityItem.getElevator())
                .restroom(accessibilityItem.getRestroom())
                .wheelchair(accessibilityItem.getWheelchair())
                .braileBlock(accessibilityItem.getBraileBlock())
                .helpDog(accessibilityItem.getHelpDog())
                .guideHuman(accessibilityItem.getGuideHuman())
                .rawAccessibilityJson(toJson(accessibilityResponse.getItems()))
                .build());

        return true;
    }

    private <T> T firstItem(DetailApiResponse<T> response) {
        List<T> items = response.getItems();
        if (items.isEmpty()) {
            return null;
        }
        return items.get(0);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize detail API response.", e);
        }
    }
}
