package com.gangwon.companion.domain.user.service;

import com.gangwon.companion.domain.destination.entity.DestinationReview;
import com.gangwon.companion.domain.lodging.entity.LodgingReview;
import com.gangwon.companion.domain.restaurant.entity.RestaurantReview;
import com.gangwon.companion.domain.user.dto.request.LoginRequest;
import com.gangwon.companion.domain.user.dto.request.NicknameChangeRequest;
import com.gangwon.companion.domain.user.dto.request.PasswordChangeRequest;
import com.gangwon.companion.domain.user.dto.request.ProfileImageChangeRequest;
import com.gangwon.companion.domain.user.dto.request.SignUpRequest;
import com.gangwon.companion.domain.user.dto.response.MyPageResponse;
import com.gangwon.companion.domain.user.dto.response.MyReviewResponse;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.domain.destination.repository.DestinationReviewRepository;
import com.gangwon.companion.domain.lodging.repository.LodgingReviewRepository;
import com.gangwon.companion.domain.restaurant.repository.RestaurantReviewRepository;
import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import com.gangwon.companion.domain.visit.repository.VisitRecordRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.security.JwtTokenProvider;
import com.gangwon.companion.global.security.PersonalDataCrypto;
import com.gangwon.companion.global.security.CaptchaVerifier;
import com.gangwon.companion.global.security.TokenBlacklistService;
import com.gangwon.companion.global.storage.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PersonalDataCrypto personalDataCrypto;
    private final CaptchaVerifier captchaVerifier;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final LodgingReviewRepository lodgingReviewRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final SavedCourseRepository savedCourseRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final S3FileService s3FileService;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (captchaVerifier != null) captchaVerifier.verify(request.getCaptchaToken(), null);
        validateDuplicatedUser(request);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .emailHash(emailHash(request.getEmail()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);
    }

    public String login(LoginRequest request) {
        if (captchaVerifier != null) captchaVerifier.verify(request.getCaptchaToken(), null);
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
                + lodgingReviewRepository.countByUserUsername(username)
                + destinationReviewRepository.countByUserUsername(username);
        long savedCourseCount = savedCourseRepository.countByUserUsername(username);
        long visitedPlaceCount = visitRecordRepository.countByUserUsername(username);

        return MyPageResponse.of(user, savedCourseCount, visitedPlaceCount, reviewCount, profileImageUrl(user));
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

    @Transactional
    public void changeProfileImage(String username, ProfileImageChangeRequest request) {
        String profileImageS3Key = request.profileImageS3Key();
        findUser(username).changeProfileImage(profileImageS3Key == null || profileImageS3Key.isBlank() ? null : profileImageS3Key);
    }

    @Transactional(readOnly = true)
    public List<MyReviewResponse> getMyReviews(String username) {
        findUser(username);

        return Stream.concat(
                        Stream.concat(
                                destinationReviewRepository.findAllByUserUsername(username).stream()
                                        .map(this::destinationReviewResponse),
                                restaurantReviewRepository.findAllByUserUsername(username).stream()
                                        .map(this::restaurantReviewResponse)
                        ),
                        lodgingReviewRepository.findAllByUserUsername(username).stream()
                                .map(this::lodgingReviewResponse)
                )
                .sorted(Comparator.comparing(MyReviewResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public void withdraw(String username, String token) {
        findUser(username).withdraw();
        if (token != null && !token.isBlank()) {
            tokenBlacklistService.block(token, jwtTokenProvider.getExpirationTime(token));
        }
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
        if (userRepository.existsByEmail(request.getEmail())
                || userRepository.existsByEmailHash(emailHash(request.getEmail()))) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private String emailHash(String email) {
        return personalDataCrypto == null ? null : personalDataCrypto.hash(email);
    }

    private String profileImageUrl(User user) {
        String key = user.getProfileImageS3Key();
        return key == null || key.isBlank() ? null : s3FileService.createDownloadUrl(key);
    }

    private MyReviewResponse destinationReviewResponse(DestinationReview review) {
        return new MyReviewResponse("DESTINATION", review.getDestination().getId(), review.getDestination().getTitle(),
                review.getId(), review.getContent(), review.getRating(), review.getCreatedAt());
    }

    private MyReviewResponse restaurantReviewResponse(RestaurantReview review) {
        return new MyReviewResponse("RESTAURANT", review.getRestaurant().getId(), review.getRestaurant().getName(),
                review.getId(), review.getContent(), review.getRating(), review.getCreatedAt());
    }

    private MyReviewResponse lodgingReviewResponse(LodgingReview review) {
        return new MyReviewResponse("LODGING", review.getLodging().getId(), review.getLodging().getName(),
                review.getId(), review.getContent(), review.getRating(), review.getCreatedAt());
    }
}
