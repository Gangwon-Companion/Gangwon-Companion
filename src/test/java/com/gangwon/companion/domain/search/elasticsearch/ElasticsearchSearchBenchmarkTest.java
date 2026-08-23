package com.gangwon.companion.domain.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangwon.companion.domain.lodging.entity.Lodging;
import com.gangwon.companion.domain.lodging.repository.LodgingRepository;
import com.gangwon.companion.domain.restaurant.entity.Restaurant;
import com.gangwon.companion.domain.restaurant.repository.RestaurantRepository;
import com.gangwon.companion.domain.search.dto.PlaceSearchRequest;
import com.gangwon.companion.domain.search.service.PlaceSearchEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ELASTICSEARCH_PERFORMANCE_URL", matches = ".+")
class ElasticsearchSearchBenchmarkTest {
    private static final int ROWS = integerProperty("performance.rows-per-domain", 2_000);
    private static final int WARM_UP = integerProperty("performance.warm-up-requests", 100);
    private static final int REQUESTS = integerProperty("performance.measured-requests", 300);
    private static final int REPETITIONS = integerProperty("performance.repetitions", 3);

    @DynamicPropertySource
    static void elasticsearch(DynamicPropertyRegistry registry) {
        registry.add("search.engine", () -> "elasticsearch");
        registry.add("search.elasticsearch.url", () -> required("elasticsearch.performance.url"));
        registry.add("search.elasticsearch.alias", () -> "gangwon-places-performance");
        registry.add("search.elasticsearch.index-prefix", () -> "gangwon-places-performance-v1");
        registry.add("search.elasticsearch.request-timeout", () -> "10s");
    }

    @Autowired RestaurantRepository restaurantRepository;
    @Autowired LodgingRepository lodgingRepository;
    @Autowired ElasticsearchIndexService indexService;
    @Autowired PlaceSearchEngine engine;
    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() {
        restaurantRepository.deleteAllInBatch();
        lodgingRepository.deleteAllInBatch();
        seedRestaurants();
        seedLodgings();
    }

    @Test
    void measureElasticsearchSearch() throws Exception {
        long indexingStarted = System.nanoTime();
        var reindex = indexService.reindex();
        double indexingMs = round((System.nanoTime() - indexingStarted) / 1_000_000.0);
        List<Scenario> scenarios = scenarios();
        for (int i = 0; i < WARM_UP; i++) engine.search(scenarios.get(i % scenarios.size()).request());

        List<Map<String, Object>> measurements = new ArrayList<>();
        for (int run = 1; run <= REPETITIONS; run++) {
            for (int concurrency : List.of(1, 10, 50)) {
                for (Scenario scenario : scenarios) measurements.add(measure(scenario, concurrency, run));
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema_version", 1);
        report.put("measured_at", OffsetDateTime.now().toString());
        report.put("engine", "elasticsearch-bm25");
        report.put("rows_per_domain", ROWS);
        report.put("total_indexed_documents", reindex.indexedCount());
        report.put("initial_indexing_ms", indexingMs);
        report.put("query_set_version", "rdb-v1");
        report.put("warm_up_requests", WARM_UP);
        report.put("measured_requests_per_scenario", REQUESTS);
        report.put("repetitions", REPETITIONS);
        report.put("java_version", System.getProperty("java.version"));
        report.put("available_processors", Runtime.getRuntime().availableProcessors());
        report.put("max_heap_bytes", Runtime.getRuntime().maxMemory());
        report.put("jvm", ManagementFactory.getRuntimeMXBean().getVmName());
        report.put("measurements", measurements);
        Path output = Path.of(required("performance.output"));
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
    }

    private Map<String, Object> measure(Scenario scenario, int concurrency, int run) throws Exception {
        var executor = Executors.newFixedThreadPool(concurrency);
        List<Callable<Sample>> tasks = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) tasks.add(() -> {
            long started = System.nanoTime();
            int count = engine.search(scenario.request()).results().size();
            return new Sample(System.nanoTime() - started, count);
        });
        long wallStarted = System.nanoTime();
        var futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.MINUTES);
        double wallSeconds = (System.nanoTime() - wallStarted) / 1_000_000_000.0;
        List<Double> timings = new ArrayList<>();
        long resultCount = 0;
        int errors = 0;
        for (var future : futures) {
            try { Sample sample = future.get(); timings.add(sample.nanos() / 1_000_000.0); resultCount += sample.resultCount(); }
            catch (Exception exception) { errors++; }
        }
        timings.sort(Double::compareTo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("run", run);
        result.put("id", scenario.id());
        result.put("type", scenario.type());
        result.put("concurrency", concurrency);
        result.put("requests", REQUESTS);
        result.put("errors", errors);
        result.put("error_rate", round((double) errors / REQUESTS));
        result.put("throughput_rps", round(REQUESTS / wallSeconds));
        result.put("p50_ms", percentile(timings, .50));
        result.put("p95_ms", percentile(timings, .95));
        result.put("p99_ms", percentile(timings, .99));
        result.put("max_ms", percentile(timings, 1));
        result.put("average_result_count", round((double) resultCount / Math.max(1, timings.size())));
        return result;
    }

    private List<Scenario> scenarios() {
        var none = new PlaceSearchRequest.HardFilters(null, null, null);
        var policies = new PlaceSearchRequest.HardFilters(true, PlaceSearchRequest.PetSize.SMALL, true);
        var geo = new PlaceSearchRequest.GeoConstraint(new PlaceSearchRequest.GeoCenter(37.75, 128.90), 2.0);
        return List.of(
                scenario("restaurant-region", "simple_filter", PlaceSearchRequest.Domain.RESTAURANT, "", none, null),
                scenario("restaurant-keyword", "keyword", PlaceSearchRequest.Domain.RESTAURANT, "한식", none, null),
                scenario("restaurant-multi-filter", "multi_filter", PlaceSearchRequest.Domain.RESTAURANT, "카페", policies, null),
                scenario("restaurant-geo", "geo", PlaceSearchRequest.Domain.RESTAURANT, "", none, geo),
                scenario("lodging-keyword", "keyword", PlaceSearchRequest.Domain.LODGING, "바다", none, null),
                scenario("lodging-sparse", "sparse_result", PlaceSearchRequest.Domain.LODGING, "존재하지않는희소검색어", none, null));
    }

    private Scenario scenario(String id, String type, PlaceSearchRequest.Domain domain, String query,
                              PlaceSearchRequest.HardFilters filters, PlaceSearchRequest.GeoConstraint geo) {
        return new Scenario(id, type, new PlaceSearchRequest(domain, "D1_BENCHMARK",
                List.of(PlaceSearchRequest.RegionCode.GANGNEUNG), query, filters, Map.of(), geo, 10));
    }

    private double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) return 0;
        return round(values.get(Math.min(values.size() - 1, (int) Math.ceil(values.size() * percentile) - 1)));
    }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private void seedRestaurants() {
        List<Restaurant> rows = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) rows.add(Restaurant.builder().externalId("ES-PERF-R-" + i)
                .name(i % 2 == 0 ? "강릉 한식당 " + i : "강릉 카페 " + i).menuType(i % 2 == 0 ? "한식" : "카페")
                .region("강릉").rating(0.0).thumbnailUrl("").address("강원특별자치도 강릉시 성산면 " + i)
                .latitude(37.75 + i % 100 * .0001).longitude(128.90 + i % 100 * .0001).build());
        restaurantRepository.saveAll(rows);
    }

    private void seedLodgings() {
        List<Lodging> rows = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) rows.add(Lodging.builder().externalId("ES-PERF-L-" + i)
                .name(i % 2 == 0 ? "강릉 바다 숙소 " + i : "강릉 시내 호텔 " + i)
                .description(i % 2 == 0 ? "바다 전망 숙소" : "시내 숙박").region("강릉").price(0L).rating(0.0)
                .thumbnailUrl("").address("강원특별자치도 강릉시 주문진읍 " + i)
                .latitude(37.75 + i % 100 * .0001).longitude(128.90 + i % 100 * .0001).build());
        lodgingRepository.saveAll(rows);
    }

    private static int integerProperty(String name, int defaultValue) { return Integer.parseInt(property(name, Integer.toString(defaultValue))); }
    private static String required(String name) {
        String value = property(name, null);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
    private static String property(String name, String defaultValue) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) value = System.getenv(name.toUpperCase().replace('.', '_').replace('-', '_'));
        return value == null || value.isBlank() ? defaultValue : value;
    }
    private record Scenario(String id, String type, PlaceSearchRequest request) {}
    private record Sample(long nanos, int resultCount) {}
}
