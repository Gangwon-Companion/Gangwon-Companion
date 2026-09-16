package com.gangwon.companion.domain.search.indexer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
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
    private final DebeziumPlaceChangeParser parser;
    private final SearchIndexDltEventRepository dltEventRepository;

    @RetryableTopic(
            attempts = "${search.indexer.retry-attempts:7}",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 30000),
            exclude = {IllegalArgumentException.class},
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
        try {
            DebeziumPlaceChange change = parser.parse(payload);
            SearchIndexDltEvent event = dltEventRepository.findByDomainAndPlaceId(change.domain(), change.id())
                    .orElseGet(() -> SearchIndexDltEvent.pending(change.domain(), change.id(),
                            "Moved to DLT from " + topic));
            dltEventRepository.save(event);
            log.error("Search indexing event moved to DLT. topic={}, domain={}, placeId={}",
                    topic, change.domain(), change.id());
        } catch (RuntimeException exception) {
            log.error("Unprocessable search indexing event moved to DLT. topic={}", topic, exception);
        }
    }
}
