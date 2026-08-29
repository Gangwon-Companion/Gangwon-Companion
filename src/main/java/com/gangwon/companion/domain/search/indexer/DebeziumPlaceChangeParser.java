package com.gangwon.companion.domain.search.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DebeziumPlaceChangeParser {
    private final ObjectMapper objectMapper;

    public DebeziumPlaceChangeParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DebeziumPlaceChange parse(String json) {
        try {
            JsonNode event = objectMapper.readTree(json);
            if (event.has("payload") && !event.path("payload").isNull()) event = event.path("payload");
            String table = requiredText(event.path("source"), "table");
            String operation = requiredText(event, "op");
            long sourceTimestampMillis = event.path("source").path("ts_ms").asLong(event.path("ts_ms").asLong(0));
            JsonNode row = "d".equals(operation) ? event.path("before") : event.path("after");
            if (row.isMissingNode() || row.isNull()) throw new IllegalArgumentException("Debezium row is missing");
            return switch (table) {
                case "destinations" -> change("DESTINATION", row, "id", "d".equals(operation), sourceTimestampMillis);
                case "destination_details", "pet_infos", "accessibility_infos" ->
                        change("DESTINATION", row, "destination_id", false, sourceTimestampMillis);
                case "restaurants" -> change("RESTAURANT", row, "id", "d".equals(operation), sourceTimestampMillis);
                case "lodgings" -> change("LODGING", row, "id", "d".equals(operation), sourceTimestampMillis);
                default -> throw new IllegalArgumentException("Unsupported Debezium table: " + table);
            };
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Debezium event", exception);
        }
    }

    private DebeziumPlaceChange change(String domain, JsonNode row, String idField, boolean rootDeleted,
                                       long sourceTimestampMillis) {
        JsonNode id = row.path(idField);
        if (!id.canConvertToLong()) throw new IllegalArgumentException("Missing numeric field: " + idField);
        return new DebeziumPlaceChange(domain, id.longValue(), rootDeleted, sourceTimestampMillis);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) throw new IllegalArgumentException("Missing field: " + field);
        return value;
    }
}
