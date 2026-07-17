package com.gangwon.companion.domain.user.service;

import com.gangwon.companion.domain.user.dto.request.LoginRequest;
import com.gangwon.companion.domain.user.dto.request.SignUpRequest;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("signUp valid request -> user saved")
    void signUp_savesUser_whenRequestValid() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getNickname()).willReturn("tester");
        given(request.getPassword()).willReturn("Test1234!");
        given(userRepository.existsByUsername("testuser1")).willReturn(false);
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(userRepository.existsByNickname("tester")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encodedPassword");

        assertThatCode(() -> userService.signUp(request)).doesNotThrowAnyException();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("signUp duplicate username -> DUPLICATE_USERNAME exception")
    void signUp_throwsDuplicateUsername_whenUsernameExists() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(userRepository.existsByUsername("testuser1")).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_USERNAME.getMessage());
    }

    @Test
    @DisplayName("signUp duplicate email -> DUPLICATE_EMAIL exception")
    void signUp_throwsDuplicateEmail_whenEmailExists() {
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
    @DisplayName("signUp duplicate nickname -> DUPLICATE_NICKNAME exception")
    void signUp_throwsDuplicateNickname_whenNicknameExists() {
        SignUpRequest request = mock(SignUpRequest.class);
        given(request.getUsername()).willReturn("testuser1");
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getNickname()).willReturn("tester");
        given(userRepository.existsByUsername("testuser1")).willReturn(false);
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(userRepository.existsByNickname("tester")).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("login valid credentials -> JWT token")
    void login_returnsToken_whenCredentialsValid() {
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
    @DisplayName("existsByUsername called -> repository result")
    void existsByUsername_returnsRepositoryResult_whenCalled() {
        given(userRepository.existsByUsername("testuser1")).willReturn(true);
        assertThat(userService.existsByUsername("testuser1")).isTrue();

        given(userRepository.existsByUsername("newuser")).willReturn(false);
        assertThat(userService.existsByUsername("newuser")).isFalse();
    }

    @Test
    @DisplayName("existsByNickname called -> repository result")
    void existsByNickname_returnsRepositoryResult_whenCalled() {
        given(userRepository.existsByNickname("tester")).willReturn(true);
        assertThat(userService.existsByNickname("tester")).isTrue();

        given(userRepository.existsByNickname("newbie")).willReturn(false);
        assertThat(userService.existsByNickname("newbie")).isFalse();
    }
}
