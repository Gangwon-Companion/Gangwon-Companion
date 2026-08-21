package com.gangwon.companion.domain.search.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search.elasticsearch")
public class ElasticsearchProperties {
    private String url = "http://localhost:9200";
    private String apiKey = "";
    private String alias = "gangwon-places";
    private String indexPrefix = "gangwon-places-v1";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration requestTimeout = Duration.ofSeconds(3);
    private int bulkSize = 500;
    private String reindexKey = "";
}
