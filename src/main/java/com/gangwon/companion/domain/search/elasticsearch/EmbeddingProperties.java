package com.gangwon.companion.domain.search.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search.embedding")
public class EmbeddingProperties {
    private boolean enabled;
    private String url = "http://localhost:8091";
    private String model = "intfloat/multilingual-e5-small";
    private String revision = "614241f622f53c4eeff9890bdc4f31cfecc418b3";
    private int dimensions = 384;
    private Duration timeout = Duration.ofSeconds(2);
    private int rankWindow = 50;
    private int rankConstant = 60;
}
