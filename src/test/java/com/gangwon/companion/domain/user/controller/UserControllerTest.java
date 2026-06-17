package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @InjectMocks UserController userController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 회원가입_성공() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!",
                  "email": "test@test.com",
                  "nickname": "테스트"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    void 회원가입_유효성_검사_실패_비밀번호() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "1234",
                  "email": "test@test.com",
                  "nickname": "테스트"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    void 회원가입_유효성_검사_실패_이메일형식() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!",
                  "email": "invalid-email",
                  "nickname": "테스트"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    void 로그인_성공() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!"
                }
                """;

        given(userService.login(any())).willReturn("mock.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));
    }

    @Test
    void 로그인_실패_잘못된_비밀번호() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Wrong1234!"
                }
                """;

        given(userService.login(any())).willThrow(new BadCredentialsException(""));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_CREDENTIALS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.BAD_CREDENTIALS.getMessage()));
    }

    @Test
    void 회원가입_중복_아이디_400반환() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!",
                  "email": "test@test.com",
                  "nickname": "테스트"
                }
                """;

        willThrow(new BusinessException(ErrorCode.DUPLICATE_USERNAME)).given(userService).signUp(any());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_USERNAME.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_USERNAME.getMessage()));
    }

    @Test
    void 아이디_중복확인_사용가능() throws Exception {
        given(userService.existsByUsername("newuser")).willReturn(false);

        mockMvc.perform(get("/api/auth/check/username/newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 아이디_중복확인_사용불가() throws Exception {
        given(userService.existsByUsername("existinguser")).willReturn(true);

        mockMvc.perform(get("/api/auth/check/username/existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void 닉네임_중복확인_사용가능() throws Exception {
        given(userService.existsByNickname("새닉")).willReturn(false);

        mockMvc.perform(get("/api/auth/check/nickname/새닉"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 닉네임_중복확인_사용불가() throws Exception {
        given(userService.existsByNickname("기존닉")).willReturn(true);

        mockMvc.perform(get("/api/auth/check/nickname/기존닉"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
