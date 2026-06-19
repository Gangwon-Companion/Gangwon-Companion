package com.gangwon.companion.domain.user.controller;

import com.gangwon.companion.domain.user.service.UserService;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import com.gangwon.companion.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("POST /api/auth/signup -> HTTP 200")
    void signUp_returnsOk_whenRequestValid() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!",
                  "email": "test@test.com",
                  "nickname": "tester"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/signup invalid password -> HTTP 400 VALIDATION_FAILED")
    void signUp_returnsBadRequest_whenPasswordInvalid() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "1234",
                  "email": "test@test.com",
                  "nickname": "tester"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    @DisplayName("POST /api/auth/signup invalid email -> HTTP 400 VALIDATION_FAILED")
    void signUp_returnsBadRequest_whenEmailInvalid() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!",
                  "email": "invalid-email",
                  "nickname": "tester"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    @DisplayName("POST /api/auth/login -> HTTP 200 token")
    void login_returnsToken_whenCredentialsValid() throws Exception {
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
    @DisplayName("POST /api/auth/login invalid credentials -> HTTP 401 BAD_CREDENTIALS")
    void login_returnsUnauthorized_whenCredentialsInvalid() throws Exception {
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
    @DisplayName("POST /api/auth/signup duplicate username -> HTTP 400 DUPLICATE_USERNAME")
    void signUp_returnsBadRequest_whenUsernameDuplicated() throws Exception {
        String body = """
                {
                  "username": "testuser1",
                  "password": "Test1234!",
                  "email": "test@test.com",
                  "nickname": "tester"
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
    @DisplayName("GET /api/auth/check/username/{username} available -> HTTP 200 available=true")
    void checkUsername_returnsAvailable_whenUsernameNotExists() throws Exception {
        given(userService.existsByUsername("newuser")).willReturn(false);

        mockMvc.perform(get("/api/auth/check/username/newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("GET /api/auth/check/username/{username} exists -> HTTP 200 available=false")
    void checkUsername_returnsUnavailable_whenUsernameExists() throws Exception {
        given(userService.existsByUsername("existinguser")).willReturn(true);

        mockMvc.perform(get("/api/auth/check/username/existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("GET /api/auth/check/nickname/{nickname} available -> HTTP 200 available=true")
    void checkNickname_returnsAvailable_whenNicknameNotExists() throws Exception {
        given(userService.existsByNickname("newnick")).willReturn(false);

        mockMvc.perform(get("/api/auth/check/nickname/newnick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("GET /api/auth/check/nickname/{nickname} exists -> HTTP 200 available=false")
    void checkNickname_returnsUnavailable_whenNicknameExists() throws Exception {
        given(userService.existsByNickname("oldnick")).willReturn(true);

        mockMvc.perform(get("/api/auth/check/nickname/oldnick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
