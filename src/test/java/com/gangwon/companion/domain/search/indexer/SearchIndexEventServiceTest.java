package com.gangwon.companion.domain.search.indexer;

import com.gangwon.companion.domain.search.elasticsearch.ElasticsearchIndexService;
import com.gangwon.companion.domain.search.elasticsearch.PlaceSearchDocument;
import com.gangwon.companion.domain.search.elasticsearch.PlaceSearchDocumentAssembler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchIndexEventServiceTest {
    private final DebeziumPlaceChangeParser parser = mock(DebeziumPlaceChangeParser.class);
    private final PlaceSearchDocumentAssembler assembler = mock(PlaceSearchDocumentAssembler.class);
    private final ElasticsearchIndexService indexService = mock(ElasticsearchIndexService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SearchIndexEventService service = new SearchIndexEventService(parser, assembler, indexService, meterRegistry);

    @Test
    void upsertsLatestAggregateForCreateOrUpdate() {
        PlaceSearchDocument document = document("DESTINATION:3");
        when(parser.parse("event")).thenReturn(new DebeziumPlaceChange("DESTINATION", 3, false, System.currentTimeMillis()));
        when(assembler.loadOne("DESTINATION", 3)).thenReturn(Optional.of(document));

        service.handle("event");

        verify(indexService).upsert(document);
        verify(indexService, never()).delete("DESTINATION:3");
        assertThat(meterRegistry.get("search.indexer.events").counter().count()).isEqualTo(1);
    }

    @Test
    void deletesRootDocumentWithoutQueryingAggregate() {
        when(parser.parse("event")).thenReturn(new DebeziumPlaceChange("LODGING", 4, true, 0));

        service.handle("event");

        verify(indexService).delete("LODGING:4");
        verify(assembler, never()).loadOne("LODGING", 4);
    }

    private PlaceSearchDocument document(String id) {
        return new PlaceSearchDocument(id, "DESTINATION", "장소", null, null, "장소", null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, 2, "TOUR_API", List.of());
    }
}
