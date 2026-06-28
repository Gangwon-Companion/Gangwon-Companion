package com.gangwon.companion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.secret=gangwon_test_jwt_secret_key_2024_very_long_string")
class CompanionApplicationTests {

	@Test
	void contextLoads() {
	}

}
