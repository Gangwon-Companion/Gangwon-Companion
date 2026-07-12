package com.gangwon.companion.domain.user.service;

import com.gangwon.companion.domain.user.dto.request.LoginRequest;
import com.gangwon.companion.domain.user.dto.request.SignUpRequest;
import com.gangwon.companion.domain.user.dto.request.PasswordChangeRequest;
import com.gangwon.companion.domain.user.dto.request.NicknameChangeRequest;
import com.gangwon.companion.domain.user.dto.response.MyPageResponse;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingReviewRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantReviewRepository;
import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import com.gangwon.companion.domain.visit.repository.VisitRecordRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.security.JwtTokenProvider;
import com.gangwon.companion.global.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final LodgingReviewRepository lodgingReviewRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final SavedCourseRepository savedCourseRepository;
    private final VisitRecordRepository visitRecordRepository;

    @Transactional
    public void signUp(SignUpRequest request) {
        validateDuplicatedUser(request);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);
    }

    public String login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        return jwtTokenProvider.generateToken(authentication.getName());
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        long reviewCount = restaurantReviewRepository.countByUserUsername(username)
                + lodgingReviewRepository.countByUserUsername(username);
        long savedCourseCount = savedCourseRepository.countByUserUsername(username);
        long visitedPlaceCount = visitRecordRepository.countByUserUsername(username);

        return MyPageResponse.of(user, savedCourseCount, visitedPlaceCount, reviewCount);
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = findUser(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void changeNickname(String username, NicknameChangeRequest request) {
        User user = findUser(username);
        if (!user.getNickname().equals(request.nickname())
                && userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        user.changeNickname(request.nickname());
    }

    public void logout(String token) {
        tokenBlacklistService.block(token, jwtTokenProvider.getExpirationTime(token));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateDuplicatedUser(SignUpRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
