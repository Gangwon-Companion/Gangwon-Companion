package com.gangwon.companion.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external.tats-cnctr-rate")
@Getter
@Setter
public class TatsCnctrRateProperties {
    private String serviceKey;
    private String baseUrl = "https://apis.data.go.kr/B551011/TatsCnctrRateService";
}
