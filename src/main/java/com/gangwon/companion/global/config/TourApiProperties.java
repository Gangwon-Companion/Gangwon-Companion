package com.gangwon.companion.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external.tour-api")
@Getter
@Setter
public class TourApiProperties {
    private String serviceKey;
    private String baseUrl = "https://apis.data.go.kr/B551011/KorService2";
}
