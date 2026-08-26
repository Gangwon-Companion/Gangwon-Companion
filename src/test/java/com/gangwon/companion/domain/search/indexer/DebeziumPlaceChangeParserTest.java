package com.gangwon.companion.domain.search.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebeziumPlaceChangeParserTest {
    private final DebeziumPlaceChangeParser parser = new DebeziumPlaceChangeParser(new ObjectMapper());

    @Test
    void mapsDestinationChildDeleteToAggregateRefresh() {
        DebeziumPlaceChange change = parser.parse("""
                {"before":{"id":7,"destination_id":42},"after":null,
                 "source":{"table":"pet_infos"},"op":"d"}
                """);

        assertThat(change).isEqualTo(new DebeziumPlaceChange("DESTINATION", 42, false));
    }

    @Test
    void mapsRootDeleteToDocumentDeleteAndSupportsPayloadWrapper() {
        DebeziumPlaceChange change = parser.parse("""
                {"payload":{"before":{"id":9},"after":null,
                 "source":{"table":"restaurants"},"op":"d"}}
                """);

        assertThat(change.placeId()).isEqualTo("RESTAURANT:9");
        assertThat(change.rootDeleted()).isTrue();
    }

    @Test
    void rejectsUnknownTables() {
        assertThatThrownBy(() -> parser.parse("""
                {"after":{"id":1},"source":{"table":"users"},"op":"u"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Debezium table");
    }
}
