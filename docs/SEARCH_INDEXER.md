# Search Indexer 및 검색 랭킹 개선

빈 DB 초기화부터 전체 재색인 및 AI E2E 검증까지의 절차는 [로컬 데이터 동기화 및 검색 통합 테스트 문제해결](LOCAL_DATA_SYNC_AND_SEARCH_TROUBLESHOOTING.md)을 참고한다.

## 1. 담당자 B 구현 범위

담당자 B 범위인 Kafka 기반 증분 색인과 Elasticsearch 검색 랭킹 개선을 구현했다.

```text
Kafka Debezium topic
  -> SearchIndexKafkaListener
  -> DebeziumPlaceChangeParser
  -> SearchIndexEventService
  -> PlaceSearchDocumentAssembler
  -> Elasticsearch upsert/delete
```

주요 구현 파일:

- `domain/search/indexer/SearchIndexKafkaListener.java`
- `domain/search/indexer/SearchIndexerKafkaConfiguration.java`
- `domain/search/indexer/DebeziumPlaceChangeParser.java`
- `domain/search/indexer/SearchIndexEventService.java`
- `domain/search/elasticsearch/PlaceSearchDocumentAssembler.java`
- `domain/search/elasticsearch/ElasticsearchIndexService.java`
- `domain/search/elasticsearch/ElasticsearchPlaceSearchEngine.java`

## 2. Search Indexer 동작

Indexer는 Debezium의 schema 미포함 JSON과 `payload` wrapper가 있는 JSON을 모두 파싱한다.

| 변경 테이블 | 장소 문서 | ID 필드 | DELETE 처리 |
|---|---|---|---|
| `destinations` | `DESTINATION:{id}` | `id` | 문서 삭제 |
| `destination_details` | `DESTINATION:{destination_id}` | `destination_id` | 최신 aggregate 재색인 |
| `pet_infos` | `DESTINATION:{destination_id}` | `destination_id` | 최신 aggregate 재색인 |
| `accessibility_infos` | `DESTINATION:{destination_id}` | `destination_id` | 최신 aggregate 재색인 |
| `restaurants` | `RESTAURANT:{id}` | `id` | 문서 삭제 |
| `lodgings` | `LODGING:{id}` | `id` | 문서 삭제 |

자식 테이블의 DELETE는 장소 자체의 삭제가 아니다. 따라서 ES 문서를 지우지 않고 RDB에 남은 최신 데이터를 다시 조립한다. INSERT, UPDATE, snapshot read도 동일하게 최신 aggregate를 조회해 upsert한다. aggregate가 이미 사라졌다면 오래된 ES 문서를 삭제한다.

정상 처리된 레코드만 offset commit 대상이 된다. 처리 실패 시 retry topic에서 기본 3회까지 재시도하고, 마지막 실패 이벤트는 원본 topic 이름 뒤에 `.DLT`가 붙은 topic으로 이동한다.

`SearchIndexerKafkaConfiguration`은 Indexer가 활성화된 경우에만 Kafka listener 인프라를 생성한다. Spring Kafka 4 환경에서 retry topic이 실제로 동작하도록 `@EnableKafkaRetryTopic`, 문자열 producer `KafkaTemplate`, record ack 기반 consumer/listener factory를 명시적으로 구성한다.

## 3. 실행 설정

기본 설정은 비활성화 상태다. Kafka와 Elasticsearch가 준비된 환경에서 다음 환경변수를 설정한다.

```text
SEARCH_ENGINE=elasticsearch
SEARCH_INDEXER_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
SEARCH_INDEXER_GROUP_ID=gangwon-search-indexer
SEARCH_INDEXER_RETRY_ATTEMPTS=3
```

Docker 네트워크 안에서 애플리케이션을 실행하면 bootstrap server는 `kafka:9092`를 사용한다.

대상 topic 기본값:

```text
gangwon.public.destinations
gangwon.public.destination_details
gangwon.public.pet_infos
gangwon.public.accessibility_infos
gangwon.public.restaurants
gangwon.public.lodgings
```

필요하면 `SEARCH_INDEXER_TOPICS`에 쉼표로 구분한 topic 목록을 전달한다.

## 4. 검색 정책 및 랭킹

RDB와 Elasticsearch의 hard filter 정책을 다음과 같이 통일했다.

```text
false -> 후보 제외
true  -> 후보 유지, evidence 제공
null  -> 후보 유지, INSUFFICIENT_EVIDENCE 표시
```

Elasticsearch에서는 요청 필드가 `false`인 문서만 `must_not`으로 제외하므로 값이 없는 문서는 후보로 남는다.

텍스트 랭킹 필드 boost:

```text
name^6
name.english^3
themeName^3
menuType^3
address^2
searchText^1.5
searchText.english
petInfoText^1.2
accessibilityInfoText^1.2
```

정확한 이름과 이름 구문 일치에는 별도 boost를 적용한다. `function_score`로 평점의 제곱근을 작은 가중치로 더하고, 위치 조건이 있으면 요청 중심점에 가까운 결과에 거리 감쇠 점수를 더한다. 텍스트 검색이 0건이면 기존과 동일하게 AND 검색에서 70% OR 검색으로 한 번 완화한다.

## 5. 검증

추가된 단위 테스트:

- 테이블별 place ID 및 root/child DELETE 매핑
- Debezium `payload` wrapper 호환
- 알 수 없는 테이블 거부 및 DLT 대상 예외 발생
- 최신 aggregate upsert
- root row 삭제 시 ES 문서 삭제
- ES hard filter의 `null` 유지 정책
- `function_score`, 평점 및 거리 랭킹 DSL 생성

검증 명령:

```powershell
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test --tests "com.gangwon.companion.domain.search.indexer.*" `
  --tests "com.gangwon.companion.domain.search.elasticsearch.ElasticsearchPlaceSearchEngineTest" `
  --tests "com.gangwon.companion.domain.search.elasticsearch.ElasticsearchIndexServiceTest" `
  --tests "com.gangwon.companion.domain.search.service.RdbPlaceSearchEngineTest"
```

통합 검증은 Kafka, PostgreSQL, Elasticsearch를 실행하고 전체 재색인으로 alias를 만든 다음 CDC UPDATE/DELETE를 발생시켜 해당 `_id` 문서를 조회한다.

## 6. Docker 통합 검증 절차

PowerShell에서 검증용 환경변수를 설정하고 전체 서비스를 실행한다.

```powershell
$env:SEARCH_ENGINE = "elasticsearch"
$env:SEARCH_INDEXER_ENABLED = "true"
$env:ELASTICSEARCH_REINDEX_KEY = "local-reindex-key"
docker compose up -d --build
.\scripts\register-search-cdc-connector.ps1
```

서비스와 connector 상태를 확인한다.

```powershell
docker compose ps
curl.exe -fsS http://localhost:8083/connectors/gangwon-postgres-connector/status
curl.exe -fsS http://localhost:9200/_cluster/health
curl.exe -fsS http://localhost:8080/actuator/health
```

최초 alias를 만들기 위해 전체 재색인을 한 번 실행한다.

```powershell
curl.exe -fsS -X POST `
  -H "X-Search-Reindex-Key: local-reindex-key" `
  http://localhost:8080/internal/search/index/rebuild

curl.exe -fsS http://localhost:9200/_alias/gangwon-places
```

검증할 destination ID와 변경 전 ES 문서를 확인한다.

```powershell
$destinationId = docker exec postgres_gangwon psql -U gangwon_user -d gangwon -tA `
  -c "select id from destinations order by id limit 1;"

curl.exe -fsS "http://localhost:9200/gangwon-places/_doc/DESTINATION:$destinationId"
```

DB 제목을 변경하면 Debezium, Kafka, Indexer를 거쳐 같은 ES 문서가 갱신되어야 한다.

```powershell
docker exec postgres_gangwon psql -U gangwon_user -d gangwon `
  -c "update destinations set title = title || ' [CDC검증]', updated_at = now() where id = $destinationId;"

Start-Sleep -Seconds 3
curl.exe -fsS "http://localhost:9200/gangwon-places/_doc/DESTINATION:$destinationId"
docker logs spring_gangwon --since 2m
```

응답의 `_source.name` 끝에 `[CDC검증]`이 있으면 UPDATE 검증 성공이다. 검증 후 DB 제목을 복구하고 ES 반영도 확인한다.

```powershell
docker exec postgres_gangwon psql -U gangwon_user -d gangwon `
  -c "update destinations set title = replace(title, ' [CDC검증]', ''), updated_at = now() where id = $destinationId;"

Start-Sleep -Seconds 3
curl.exe -fsS "http://localhost:9200/gangwon-places/_doc/DESTINATION:$destinationId"
```

DELETE 통합 검증은 테스트 전용 restaurant/lodging fixture를 먼저 INSERT한 뒤 해당 ID가 ES에 생성된 것을 확인하고, 같은 row를 DELETE해 ES 조회가 404가 되는지 확인한다. 실제 컬럼 제약조건이 환경마다 다를 수 있으므로 운영·공용 DB의 기존 row를 삭제해서 검증하지 않는다. 트랜잭션을 rollback하면 CDC 이벤트가 발행되지 않으므로 DELETE 검증이 되지 않는다.

DLT와 consumer group 상태는 다음 명령으로 확인한다.

```powershell
docker exec kafka_gangwon kafka-topics --bootstrap-server kafka:9092 --list
docker exec kafka_gangwon kafka-consumer-groups --bootstrap-server kafka:9092 `
  --group gangwon-search-indexer --describe
```

## 7. 2026-08-26 통합 검증 결과

로컬 Docker 통합 환경에서 다음 구성을 실제로 실행해 검증했다.

```text
PostgreSQL 16
Debezium Connect 3.0
Kafka 7.8.0 KRaft
Spring Boot 4.0.6 / Spring Kafka 4.0.5
Elasticsearch 9.5.0 + analysis-nori
```

검증 결과:

- PostgreSQL, Kafka, Kafka Connect, Elasticsearch, Spring 컨테이너 healthy 확인
- Debezium connector 및 task `RUNNING` 확인
- `SEARCH_INDEXER_ENABLED=true`, `SEARCH_ENGINE=elasticsearch` 적용 확인
- `gangwon-search-indexer`, retry, DLT consumer group 생성 확인
- 빈 DB 전체 재색인과 `gangwon-places` alias 전환 확인
- 고유 `external_id`를 사용한 테스트 전용 restaurant INSERT
- Kafka 이벤트 소비 후 ES `RESTAURANT:1` 문서 `200 OK`, `found=true` 확인
- 테스트 row 한 건만 `DELETE 1`로 삭제
- Kafka 이벤트 소비 후 같은 ES 문서 `404`, `found=false` 확인
- 테스트용 `codex-cdc-test-%` restaurant 잔여 row 0건 확인

최종 확인 흐름:

```text
PostgreSQL INSERT
  -> Debezium CDC
  -> Kafka
  -> Search Indexer
  -> Elasticsearch upsert

PostgreSQL DELETE
  -> Debezium CDC
  -> Kafka
  -> Search Indexer
  -> Elasticsearch delete
```

검증 과정에서 retry listener가 시작되려면 명시적인 retry topic 활성화, `KafkaTemplate`, listener container factory가 필요함을 확인했고 `SearchIndexerKafkaConfiguration`에 반영했다.
