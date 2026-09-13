package com.gangwon.companion.domain.travelprofile.service;
import com.gangwon.companion.domain.travelprofile.dto.*;
import com.gangwon.companion.domain.travelprofile.entity.TravelProfile;
import com.gangwon.companion.domain.travelprofile.repository.TravelProfileRepository;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class TravelProfileService {
 private final TravelProfileRepository repository; private final UserRepository userRepository;
 @Transactional(readOnly=true) public TravelProfileResponse get(String username) { return repository.findByUserUsername(username).map(TravelProfileResponse::from).orElseGet(TravelProfileResponse::notAnalyzed); }
 @Transactional public TravelProfile save(String username, AiTravelProfileResponse response) {
  var profile=repository.findByUserUsername(username).orElseGet(() -> TravelProfile.forUser(userRepository.findByUsername(username).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
  profile.apply(response); return repository.save(profile);
 }
 @Transactional(readOnly=true) public TravelProfileResponse getById(Long id) { return id==null ? null : repository.findById(id).map(TravelProfileResponse::from).orElse(null); }
}
