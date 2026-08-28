package com.gangwon.companion.domain.search.indexer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.indexer.enabled", havingValue = "true")
public class SearchIndexKafkaListener {
    private final SearchIndexEventService eventService;

    @RetryableTopic(
            attempts = "${search.indexer.retry-attempts:3}",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "#{'${search.indexer.topics}'.split(',')}",
            groupId = "${search.indexer.group-id:gangwon-search-indexer}")
    public void consume(String payload) {
        eventService.handle(payload);
    }

    @DltHandler
    public void deadLetter(String payload, @Header(RECEIVED_TOPIC) String topic) {
        log.error("Search indexing event moved to DLT. topic={}, payload={}", topic, payload);
    }
}
