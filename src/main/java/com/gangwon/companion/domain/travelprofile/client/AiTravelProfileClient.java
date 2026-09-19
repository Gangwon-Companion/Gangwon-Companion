package com.gangwon.companion.domain.travelprofile.client;

import com.gangwon.companion.domain.course.client.AiTravelClientException;
import com.gangwon.companion.domain.travelprofile.dto.AiTravelProfileRequest;
import com.gangwon.companion.domain.travelprofile.dto.AiTravelProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.net.SocketTimeoutException;
import java.time.Duration;
import static com.gangwon.companion.global.exception.ErrorCode.*;

@Component
public class AiTravelProfileClient {
    private final RestClient client;
    @Autowired
    public AiTravelProfileClient(@Value("${ai.base-url}") String baseUrl,
        @Value("${ai.connect-timeout:2s}") String connectTimeout,
        @Value("${ai.read-timeout:30s}") String readTimeout,
        @Value("${ai.internal-api-key:}") String internalApiKey) {
        this(baseUrl, DurationStyle.detectAndParse(connectTimeout), DurationStyle.detectAndParse(readTimeout), internalApiKey);
    }
    AiTravelProfileClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        this(baseUrl, connectTimeout, readTimeout, "");
    }
    AiTravelProfileClient(String baseUrl, Duration connectTimeout, Duration readTimeout, String internalApiKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout); factory.setReadTimeout(readTimeout);
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            builder.defaultHeader("X-Internal-API-Key", internalApiKey);
        }
        client = builder.build();
    }
    public AiTravelProfileResponse analyze(AiTravelProfileRequest request) {
        try {
            AiTravelProfileResponse response = client.post().uri("/internal/travel/profile/analyze")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(request).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> { throw new AiTravelClientException(AI_CLIENT_ERROR, null); })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> { throw new AiTravelClientException(AI_SERVER_ERROR, null); })
                .body(AiTravelProfileResponse.class);
            validate(response); return response;
        } catch (AiTravelClientException e) { throw e;
        } catch (ResourceAccessException e) {
            if (hasCause(e, SocketTimeoutException.class)) throw new AiTravelClientException(AI_SERVICE_TIMEOUT, e);
            throw new AiTravelClientException(AI_SERVICE_UNAVAILABLE, e);
        } catch (RestClientResponseException e) {
            throw new AiTravelClientException(e.getStatusCode().is4xxClientError() ? AI_CLIENT_ERROR : AI_SERVER_ERROR, e);
        } catch (RestClientException | IllegalArgumentException e) { throw new AiTravelClientException(AI_INVALID_RESPONSE, e); }
    }
    private static void validate(AiTravelProfileResponse r) {
        if (r == null || r.status() == null || r.analysisVersion() == null || r.analysisVersion().isBlank()) invalid();
        if (r.tags().size() > 5 || r.evidences().size() > 3 || r.tags().stream().anyMatch(v -> invalidLength(v, 30))
            || r.evidences().stream().anyMatch(v -> invalidLength(v, 200))) invalid();
        if (r.status() == AiTravelProfileResponse.Status.COMPLETED && (r.travelerType() == null || invalidLength(r.title(), 100)
            || invalidLength(r.description(), 500) || r.confidence() == null || r.confidence() < 0 || r.confidence() > 1
            || r.axisScores() == null)) invalid();
        if (r.axisScores() != null) try { r.axisScores().validate(); } catch (IllegalArgumentException e) { invalid(); }
    }
    private static boolean invalidLength(String v, int max) { return v == null || v.isBlank() || v.length() > max; }
    private static void invalid() { throw new AiTravelClientException(AI_INVALID_RESPONSE, null); }
    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) { for (Throwable c=t; c!=null; c=c.getCause()) if(type.isInstance(c)) return true; return false; }
}
