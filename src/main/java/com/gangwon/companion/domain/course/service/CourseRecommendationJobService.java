package com.gangwon.companion.domain.course.service;

import com.gangwon.companion.domain.course.client.AiTravelClientException;
import com.gangwon.companion.domain.course.client.AiTravelClient;
import com.gangwon.companion.domain.course.dto.CourseRecommendationJobResponse;
import com.gangwon.companion.domain.course.dto.CourseRecommendationJobSubmittedResponse;
import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import com.gangwon.companion.domain.course.dto.AiCourseRecommendationRequest;
import com.gangwon.companion.domain.travelprofile.service.TravelProfileService;
import com.gangwon.companion.domain.course.entity.CourseRecommendationJob;
import com.gangwon.companion.domain.course.repository.CourseRecommendationJobRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class CourseRecommendationJobService {
    private final CourseRecommendationJobRepository repository;
    private final AiTravelClient aiTravelClient;
    private final ObjectMapper objectMapper;
    private final TravelProfileService travelProfileService;
    @Qualifier("courseRecommendationExecutor")
    private final Executor executor;
    @Value("${course.recommendation.jobs.retention:24h}")
    private Duration retention;

    public CourseRecommendationJobService(
            CourseRecommendationJobRepository repository,
            AiTravelClient aiTravelClient,
            ObjectMapper objectMapper,
            TravelProfileService travelProfileService,
            @Qualifier("courseRecommendationExecutor") Executor executor
    ) {
        this.repository = repository;
        this.aiTravelClient = aiTravelClient;
        this.objectMapper = objectMapper;
        this.travelProfileService = travelProfileService;
        this.executor = executor;
    }

    public CourseRecommendationJobSubmittedResponse submit(CourseRecommendationRequest request, String username) {
        CourseRecommendationJob job = repository.save(CourseRecommendationJob.pending(username));
        try {
            executor.execute(() -> process(job.getId(), request, username));
        } catch (RuntimeException exception) {
            job.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            repository.save(job);
            throw exception;
        }
        return new CourseRecommendationJobSubmittedResponse(job.getId(), job.getStatus());
    }

    public CourseRecommendationJobResponse get(UUID jobId, String username) {
        CourseRecommendationJob job = repository.findById(jobId)
                .filter(candidate -> candidate.getUsername().equals(username))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        JsonNode result = job.getResultJson() == null ? null : objectMapper.readTree(job.getResultJson());
        return new CourseRecommendationJobResponse(
                job.getId(), job.getStatus(), result, job.getErrorCode(), job.getErrorMessage(),
                job.getCreatedAt(), job.getUpdatedAt()
        );
    }

    private void process(UUID jobId, CourseRecommendationRequest request, String username) {
        CourseRecommendationJob job = repository.findById(jobId).orElse(null);
        if (job == null) return;
        job.markRunning();
        repository.save(job);
        try {
            var profile = travelProfileService.findUsableContext(username).orElse(null);
            JsonNode result = aiTravelClient.recommend(AiCourseRecommendationRequest.from(request, profile));
            job.complete(result.toString());
        } catch (AiTravelClientException exception) {
            job.fail(exception.getErrorCode().getCode(), exception.getErrorCode().getMessage());
        } catch (RuntimeException exception) {
            job.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        }
        repository.save(job);
    }

    @Scheduled(fixedDelayString = "${course.recommendation.jobs.cleanup-interval:3600000}")
    @Transactional
    public void cleanupExpiredJobs() {
        Instant cutoff = Instant.now().minus(retention);
        repository.deleteByStatusInAndUpdatedAtBefore(
                List.of(CourseRecommendationJob.Status.COMPLETED, CourseRecommendationJob.Status.FAILED), cutoff
        );
    }
}
