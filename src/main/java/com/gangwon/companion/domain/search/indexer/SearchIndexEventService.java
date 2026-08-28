package com.gangwon.companion.domain.search.indexer;

import com.gangwon.companion.domain.search.elasticsearch.ElasticsearchIndexService;
import com.gangwon.companion.domain.search.elasticsearch.PlaceSearchDocumentAssembler;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

@Service
@RequiredArgsConstructor
public class SearchIndexEventService {
    private final DebeziumPlaceChangeParser parser;
    private final PlaceSearchDocumentAssembler assembler;
    private final ElasticsearchIndexService indexService;
    private final MeterRegistry meterRegistry;

    public void handle(String eventJson) {
        long startedAt = System.nanoTime();
        DebeziumPlaceChange change = null;
        try {
            change = parser.parse(eventJson);
            if (change.rootDeleted()) {
                indexService.delete(change.placeId());
            } else {
                DebeziumPlaceChange parsedChange = change;
                assembler.loadOne(change.domain(), change.id())
                        .ifPresentOrElse(indexService::upsert, () -> indexService.delete(parsedChange.placeId()));
            }
            meterRegistry.counter("search.indexer.events", "result", "success", "domain", change.domain()).increment();
            recordEndToEndLatency(change);
        } catch (RuntimeException exception) {
            String domain = change == null ? "UNKNOWN" : change.domain();
            meterRegistry.counter("search.indexer.events", "result", "failure", "domain", domain).increment();
            throw exception;
        } finally {
            String domain = change == null ? "UNKNOWN" : change.domain();
            Timer.builder("search.indexer.processing")
                    .description("Time spent processing one search indexing event")
                    .tag("domain", domain)
                    .register(meterRegistry)
                    .record(System.nanoTime() - startedAt, NANOSECONDS);
        }
    }

    private void recordEndToEndLatency(DebeziumPlaceChange change) {
        if (change.sourceTimestampMillis() <= 0) return;
        long latencyMillis = Math.max(0, System.currentTimeMillis() - change.sourceTimestampMillis());
        Timer.builder("search.indexer.end.to.end")
                .description("Time from the database change to Elasticsearch indexing completion")
                .tag("domain", change.domain())
                .register(meterRegistry)
                .record(Duration.ofMillis(latencyMillis));
    }
}
