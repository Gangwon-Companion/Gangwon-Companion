package com.gangwon.companion.domain.course.client;

import tools.jackson.databind.JsonNode;
import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.io.IOException;
import java.time.Duration;

import static com.gangwon.companion.global.exception.ErrorCode.*;

@Component
public class AiTravelClient {
    private final RestClient client;

    @Autowired
    public AiTravelClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.connect-timeout:2s}") String connectTimeout,
            @Value("${ai.read-timeout:30s}") String readTimeout
    ) {
        this(baseUrl, DurationStyle.detectAndParse(connectTimeout), DurationStyle.detectAndParse(readTimeout));
    }

    AiTravelClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public JsonNode recommend(CourseRecommendationRequest request) {
        try {
            JsonNode response = client.post()
                    .uri("/internal/travel/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, httpResponse) -> {
                        throw new AiTravelClientException(AI_CLIENT_ERROR, null);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (httpRequest, httpResponse) -> {
                        throw new AiTravelClientException(AI_SERVER_ERROR, null);
                    })
                    .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new AiTravelClientException(AI_INVALID_RESPONSE, null);
            }
            return response;
        } catch (AiTravelClientException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (hasCause(exception, ConnectException.class)) {
                throw new AiTravelClientException(AI_SERVICE_UNAVAILABLE, exception);
            }
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new AiTravelClientException(AI_SERVICE_TIMEOUT, exception);
            }
            throw new AiTravelClientException(AI_SERVICE_UNAVAILABLE, exception);
        } catch (RestClientResponseException exception) {
            throw new AiTravelClientException(
                    exception.getStatusCode().is4xxClientError() ? AI_CLIENT_ERROR : AI_SERVER_ERROR,
                    exception
            );
        } catch (RestClientException exception) {
            if (hasCause(exception, ConnectException.class)) {
                throw new AiTravelClientException(AI_SERVICE_UNAVAILABLE, exception);
            }
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new AiTravelClientException(AI_SERVICE_TIMEOUT, exception);
            }
            if (hasCause(exception, IOException.class)) {
                throw new AiTravelClientException(AI_SERVICE_UNAVAILABLE, exception);
            }
            throw new AiTravelClientException(AI_INVALID_RESPONSE, exception);
        }
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
