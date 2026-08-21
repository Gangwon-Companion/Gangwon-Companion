package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchIndexServiceTest {
    private final ElasticsearchHttpClient client = mock(ElasticsearchHttpClient.class);
    private final PlaceSearchDocumentAssembler assembler = mock(PlaceSearchDocumentAssembler.class);
    private final ElasticsearchProperties properties = new ElasticsearchProperties();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ElasticsearchIndexService service = new ElasticsearchIndexService(client, properties, assembler, mapper);

    @Test
    void createsVersionedIndexBulkIndexesAndSwitchesAlias() throws Exception {
        PlaceSearchDocument document = new PlaceSearchDocument("RESTAURANT:1", "RESTAURANT", "강릉 카페", "강릉",
                "GANGNEUNG", "강릉 카페", new PlaceSearchDocument.Location(37.75, 128.90),
                null, null, null, null, null, "TOUR_API", List.of());
        when(assembler.loadAll()).thenReturn(List.of(document));
        when(client.postNdjson(startsWith("/gangwon-places-v1-"), any())).thenReturn(mapper.readTree("{\"errors\":false}"));
        when(client.post(startsWith("/gangwon-places-v1-"))).thenReturn(mapper.createObjectNode());
        when(client.get(startsWith("/gangwon-places-v1-"))).thenReturn(mapper.readTree("{\"count\":1}"));
        when(client.exists("/_alias/gangwon-places")).thenReturn(false);

        var report = service.reindex();

        assertThat(report.sourceCount()).isEqualTo(1);
        assertThat(report.indexedCount()).isEqualTo(1);
        assertThat(report.aliasSwitched()).isTrue();
        ArgumentCaptor<Object> definition = ArgumentCaptor.forClass(Object.class);
        verify(client).put(startsWith("/gangwon-places-v1-"), definition.capture());
        assertThat(mapper.writeValueAsString(definition.getValue()))
                .contains("nori_tokenizer", "geo_point", "dense_vector", "dynamic");
        verify(client).post("/_aliases", Map.of("actions", List.of(Map.of("add", Map.of(
                "index", report.index(), "alias", "gangwon-places", "is_write_index", true)))));
    }
}
