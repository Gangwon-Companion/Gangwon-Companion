package com.gangwon.companion.domain.search.indexer;

import com.gangwon.companion.domain.search.elasticsearch.ElasticsearchIndexService;
import com.gangwon.companion.domain.search.elasticsearch.PlaceSearchDocumentAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchIndexEventService {
    private final DebeziumPlaceChangeParser parser;
    private final PlaceSearchDocumentAssembler assembler;
    private final ElasticsearchIndexService indexService;

    public void handle(String eventJson) {
        DebeziumPlaceChange change = parser.parse(eventJson);
        if (change.rootDeleted()) {
            indexService.delete(change.placeId());
            return;
        }
        assembler.loadOne(change.domain(), change.id())
                .ifPresentOrElse(indexService::upsert, () -> indexService.delete(change.placeId()));
    }
}
