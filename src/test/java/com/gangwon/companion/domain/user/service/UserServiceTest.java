package com.gangwon.companion.domain.user.service;

import com.gangwon.companion.domain.user.dto.LoginRequest;
import com.gangwon.companion.domain.user.dto.SignUpRequest;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks UserService userService;

    @Test
    void 회원가입_성공() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getNickname()).willReturn("테스트");
        given(request.getPassword()).willReturn("Test1234!");
        given(userRepository.existsByUsername("testuser1")).willReturn(false);
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(userRepository.existsByNickname("테스트")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encodedPassword");

        assertThatCode(() -> userService.signUp(request)).doesNotThrowAnyException();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 회원가입_중복_아이디_예외() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(userRepository.existsByUsername("testuser1")).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_USERNAME.getMessage());
    }

    @Test
    void 회원가입_중복_이메일_예외() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(request.getEmail()).willReturn("test@test.com");
        given(userRepository.existsByUsername("testuser1")).willReturn(false);
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    void 회원가입_중복_닉네임_예외() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getNickname()).willReturn("테스트");
        given(userRepository.existsByUsername("testuser1")).willReturn(false);
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(userRepository.existsByNickname("테스트")).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    void 로그인_성공() {
        LoginRequest request = mock(LoginRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(request.getPassword()).willReturn("Test1234!");

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn("testuser1");
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.generateToken("testuser1")).willReturn("mock.jwt.token");

        String token = userService.login(request);

        assertThat(token).isEqualTo("mock.jwt.token");
    }

    @Test
    void 아이디_존재여부_확인() {
        given(userRepository.existsByUsername("testuser1")).willReturn(true);
        assertThat(userService.existsByUsername("testuser1")).isTrue();

        given(userRepository.existsByUsername("newuser")).willReturn(false);
        assertThat(userService.existsByUsername("newuser")).isFalse();
    }

    @Test
    void 닉네임_존재여부_확인() {
        given(userRepository.existsByNickname("테스트")).willReturn(true);
        assertThat(userService.existsByNickname("테스트")).isTrue();

        given(userRepository.existsByNickname("새닉")).willReturn(false);
        assertThat(userService.existsByNickname("새닉")).isFalse();
    }
}
