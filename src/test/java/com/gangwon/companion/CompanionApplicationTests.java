package com.gangwon.companion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jwt.secret=gangwon_test_jwt_secret_key_2026_at_least_32_bytes_long",
        "tour-api.service-key=test-tour-api-key"
})
class CompanionApplicationTests {

    @Test
    void contextLoads() {
    }

}
