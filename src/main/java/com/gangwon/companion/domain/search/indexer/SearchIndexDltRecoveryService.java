package com.gangwon.companion.domain.search.indexer;

import com.gangwon.companion.domain.search.elasticsearch.ElasticsearchIndexService;
import com.gangwon.companion.domain.search.elasticsearch.PlaceSearchDocumentAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.indexer.enabled", havingValue = "true")
public class SearchIndexDltRecoveryService {
    private final SearchIndexDltEventRepository eventRepository;
    private final PlaceSearchDocumentAssembler assembler;
    private final ElasticsearchIndexService indexService;

    @Scheduled(fixedDelayString = "${search.indexer.dlt-recovery-interval:30000}")
    public void recoverPendingEvents() {
        eventRepository.findTop100ByOrderByCreatedAtAsc().forEach(this::recoverOne);
    }

    private void recoverOne(SearchIndexDltEvent event) {
        try {
            assembler.loadOne(event.getDomain(), event.getPlaceId())
                    .ifPresentOrElse(indexService::upsert, () -> indexService.delete(event.getDomain() + ":" + event.getPlaceId()));
            eventRepository.delete(event);
            log.info("Reprocessed search index DLT event. domain={}, placeId={}", event.getDomain(), event.getPlaceId());
        } catch (RuntimeException exception) {
            recordFailure(event, exception);
        }
    }

    @Transactional
    protected void recordFailure(SearchIndexDltEvent event, RuntimeException exception) {
        event.recordFailure(exception.getMessage());
        eventRepository.save(event);
        log.warn("Search index DLT recovery failed. domain={}, placeId={}, attempts={}",
                event.getDomain(), event.getPlaceId(), event.getAttempts(), exception);
    }
}
