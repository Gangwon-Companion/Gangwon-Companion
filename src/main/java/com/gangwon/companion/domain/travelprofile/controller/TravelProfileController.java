package com.gangwon.companion.domain.travelprofile.controller;
import com.gangwon.companion.domain.travelprofile.dto.TravelProfileJobResponses.*;
import com.gangwon.companion.domain.travelprofile.dto.TravelProfileResponse;
import com.gangwon.companion.domain.travelprofile.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.UUID;
@RestController @RequestMapping("/api/v1/users/me/travel-profile") @RequiredArgsConstructor
public class TravelProfileController {
 private final TravelProfileService profileService; private final TravelProfileAnalysisJobService jobService;
 @GetMapping public TravelProfileResponse get(Authentication a){return profileService.get(a.getName());}
 @PostMapping("/analysis-jobs") public ResponseEntity<Submitted> submit(Authentication a){return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobService.submit(a.getName()));}
 @GetMapping("/analysis-jobs/{jobId}") public Detail getJob(@PathVariable UUID jobId,Authentication a){return jobService.get(jobId,a.getName());}
}
