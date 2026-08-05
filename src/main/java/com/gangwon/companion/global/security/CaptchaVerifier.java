package com.gangwon.companion.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.global.exception.BusinessException;
import com.gangwon.companion.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class CaptchaVerifier {
    private final boolean enabled; private final String secret; private final double minScore; private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newHttpClient();
    public CaptchaVerifier(@Value("${captcha.enabled:false}") boolean enabled, @Value("${captcha.secret:}") String secret, @Value("${captcha.min-score:0.5}") double minScore) { this.enabled = enabled; this.secret = secret; this.minScore = minScore; this.mapper = new ObjectMapper(); }
    public void verify(String token, String remoteIp) {
        if (!enabled) return;
        if (token == null || token.isBlank() || secret.isBlank()) throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
        try {
            String body = "secret=" + enc(secret) + "&response=" + enc(token);
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.google.com/recaptcha/api/siteverify")).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            JsonNode result = mapper.readTree(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
            if (!result.path("success").asBoolean(false) || result.path("score").asDouble(0) < minScore) throw new BusinessException(ErrorCode.CAPTCHA_FAILED);
        } catch (BusinessException e) { throw e; } catch (Exception e) { throw new BusinessException(ErrorCode.CAPTCHA_FAILED); }
    }
    private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
