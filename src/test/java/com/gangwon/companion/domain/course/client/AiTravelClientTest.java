package com.gangwon.companion.domain.course.client;

import com.gangwon.companion.domain.course.dto.CourseRecommendationRequest;
import com.gangwon.companion.global.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiTravelClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsObjectResponse() throws Exception {
        AiTravelClient client = clientResponding(200, "{\"status\":\"completed\"}", Duration.ofSeconds(1));

        assertThat(client.recommend(request()).get("status").asText()).isEqualTo("completed");
    }

    @Test
    void mapsAi4xxWithoutForwardingItsBody() throws Exception {
        AiTravelClient client = clientResponding(422, "{\"secret\":\"must-not-leak\"}", Duration.ofSeconds(1));

        assertError(client, ErrorCode.AI_CLIENT_ERROR);
    }

    @Test
    void mapsAi5xx() throws Exception {
        AiTravelClient client = clientResponding(500, "failure", Duration.ofSeconds(1));

        assertError(client, ErrorCode.AI_SERVER_ERROR);
    }

    @Test
    void mapsMalformedJson() throws Exception {
        AiTravelClient client = clientResponding(200, "not-json", Duration.ofSeconds(1));

        assertError(client, ErrorCode.AI_INVALID_RESPONSE);
    }

    @Test
    void mapsReadTimeout() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/travel/plan", exchange -> {
            try {
                Thread.sleep(300);
                send(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();
        AiTravelClient client = new AiTravelClient(baseUrl(), Duration.ofSeconds(1), Duration.ofMillis(50));

        assertError(client, ErrorCode.AI_SERVICE_TIMEOUT);
    }

    @Test
    void mapsConnectionFailure() {
        AiTravelClient client = new AiTravelClient(
                "http://127.0.0.1:1",
                Duration.ofMillis(100),
                Duration.ofMillis(100)
        );

        assertError(client, ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    private AiTravelClient clientResponding(int status, String body, Duration readTimeout) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/travel/plan", exchange -> send(exchange, status, body));
        server.start();
        return new AiTravelClient(baseUrl(), Duration.ofSeconds(1), readTimeout);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static CourseRecommendationRequest request() {
        return new CourseRecommendationRequest(
                "강릉에서 하루 여행", "강릉", 1, 0, false,
                null, false, false, null, List.of()
        );
    }

    private static void assertError(AiTravelClient client, ErrorCode expected) {
        assertThatThrownBy(() -> client.recommend(request()))
                .isInstanceOf(AiTravelClientException.class)
                .extracting(exception -> ((AiTravelClientException) exception).getErrorCode())
                .isEqualTo(expected);
    }
}
