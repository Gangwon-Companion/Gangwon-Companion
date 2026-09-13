package com.gangwon.companion.domain.travelprofile.client;

import com.gangwon.companion.domain.travelprofile.dto.AiTravelProfileRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiTravelProfileClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsInternalApiKeyHeader() throws Exception {
        AtomicReference<String> receivedKey = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/travel/profile/analyze", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("X-Internal-API-Key"));
            byte[] body = """
                    {"status":"INSUFFICIENT_DATA","traveler_type":null,"title":null,
                     "description":null,"tags":[],"evidences":[],"confidence":null,
                     "analysis_version":"travel-profile-v1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        var client = new AiTravelProfileClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1), Duration.ofSeconds(1), "local-dev-key"
        );
        client.analyze(new AiTravelProfileRequest("1.0", Instant.now(), List.of(), List.of(), List.of(), List.of()));

        assertThat(receivedKey.get()).isEqualTo("local-dev-key");
    }
}
