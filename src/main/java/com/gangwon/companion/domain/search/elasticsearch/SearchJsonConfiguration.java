package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchJsonConfiguration {
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper searchObjectMapper() {
        return new ObjectMapper();
    }
}
