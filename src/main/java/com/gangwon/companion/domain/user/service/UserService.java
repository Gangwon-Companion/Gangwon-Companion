package com.gangwon.companion.domain.user.service;

import com.gangwon.companion.domain.user.dto.request.LoginRequest;
import com.gangwon.companion.domain.user.dto.request.SignUpRequest;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.security.JwtTokenProvider;
import com.gangwon.companion.global.security.PersonalDataCrypto;
import com.gangwon.companion.global.security.CaptchaVerifier;
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
    private final PersonalDataCrypto personalDataCrypto;
    private final CaptchaVerifier captchaVerifier;

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
}
