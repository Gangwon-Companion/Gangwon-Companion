package com.gangwon.companion.domain.travelprofile.service;
import com.gangwon.companion.domain.course.client.AiTravelClientException;
import com.gangwon.companion.domain.travelprofile.client.AiTravelProfileClient;
import com.gangwon.companion.domain.travelprofile.dto.TravelProfileJobResponses.*;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfileAnalysisJob;
import com.gangwon.companion.domain.travelprofile.repository.TravelProfileAnalysisJobRepository;
import com.gangwon.companion.global.exception.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*; import java.util.concurrent.Executor;
@Service
public class TravelProfileAnalysisJobService {
 private final TravelProfileAnalysisJobRepository repository; private final TravelProfileDataCollector collector; private final AiTravelProfileClient client; private final TravelProfileService profileService; private final Executor executor;
 @Value("${travel-profile.analysis.jobs.retention:24h}") private Duration retention;
 public TravelProfileAnalysisJobService(TravelProfileAnalysisJobRepository r, TravelProfileDataCollector c, AiTravelProfileClient a, TravelProfileService p, @Qualifier("travelProfileAnalysisExecutor") Executor e) { repository=r; collector=c; client=a; profileService=p; executor=e; }
 public synchronized Submitted submit(String username) {
  var active=repository.findFirstByUsernameAndStatusInOrderByCreatedAtDesc(username,List.of(TravelProfileAnalysisJob.Status.PENDING,TravelProfileAnalysisJob.Status.RUNNING));
  if(active.isPresent()) return new Submitted(active.get().getId(),active.get().getStatus());
  var job=repository.save(TravelProfileAnalysisJob.pending(username));
  try { executor.execute(() -> process(job.getId())); } catch(RuntimeException e) { job.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),ErrorCode.INTERNAL_SERVER_ERROR.getMessage()); repository.save(job); throw e; }
  return new Submitted(job.getId(),TravelProfileAnalysisJob.Status.PENDING);
 }
 public Detail get(UUID id,String username) {
  var job=repository.findById(id).filter(v->v.getUsername().equals(username)).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  var profile=job.getStatus()==TravelProfileAnalysisJob.Status.COMPLETED ? profileService.getById(job.getProfileId()) : null;
  return new Detail(job.getId(),job.getStatus(),profile,job.getErrorCode(),job.getErrorMessage(),job.getCreatedAt(),job.getUpdatedAt());
 }
 void process(UUID id) {
  var job=repository.findById(id).orElse(null); if(job==null)return; job.markRunning(); repository.save(job);
  try { var profile=profileService.save(job.getUsername(),client.analyze(collector.collect(job.getUsername()))); job.complete(profile.getId()); }
  catch(AiTravelClientException e){job.fail(e.getErrorCode().getCode(),e.getErrorCode().getMessage());}
  catch(RuntimeException e){job.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),ErrorCode.INTERNAL_SERVER_ERROR.getMessage());}
  repository.save(job);
 }
 @Scheduled(fixedDelayString="${travel-profile.analysis.jobs.cleanup-interval:3600000}") @Transactional public void cleanupExpiredJobs(){repository.deleteByStatusInAndUpdatedAtBefore(List.of(TravelProfileAnalysisJob.Status.COMPLETED,TravelProfileAnalysisJob.Status.FAILED),Instant.now().minus(retention));}
}
