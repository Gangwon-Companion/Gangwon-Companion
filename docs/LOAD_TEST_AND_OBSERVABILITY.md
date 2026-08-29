# 부하 테스트와 관측 환경

이 구성은 동일한 부하 조건에서 개선 전후의 p95, p99, 처리량, 오류율을 비교하고 첫 병목 지점을 찾기 위한 로컬 실험 환경이다.

## 구성 요소

- k6: 읽기 60%, 통합 장소 검색 40%의 단계 상승 부하
- Prometheus: Spring Boot, PostgreSQL, Kafka, Elasticsearch 및 k6 메트릭 수집
- Grafana: API, JVM, HikariCP, DB, Kafka lag, Elasticsearch, 이벤트 지연 대시보드
- Loki + Alloy: Spring, PostgreSQL, Kafka, Kafka Connect, Elasticsearch 컨테이너 로그 수집
- Micrometer: Debezium 원본 변경부터 Elasticsearch 반영 완료까지의 지연 측정

## 1. 관측 환경 실행

프로젝트 루트에서 다음 명령을 실행한다.

```powershell
docker compose -f compose.yaml -f compose.observability.yaml up -d --build
docker compose -f compose.yaml -f compose.observability.yaml ps
```

접속 주소:

- Grafana: http://localhost:3000 (`admin` / `admin`, 환경변수로 변경 가능)
- Prometheus: http://localhost:9090
- Loki: http://localhost:3100/ready
- Spring 메트릭: http://localhost:8080/actuator/prometheus

Grafana의 `Gangwon Companion / Gangwon Companion - Load Test` 대시보드는 자동 생성된다.

검색 인덱서까지 관측하려면 `.env`에 다음 값을 지정하고 스택을 다시 생성한다.

```dotenv
SEARCH_INDEXER_ENABLED=true
```

## 2. 기준 부하 실행

데이터 동기화나 배치가 끝나 정상 상태가 된 후 실행한다. 부하 중에는 데이터와 컨테이너 자원을 동일하게 유지한다.

```powershell
$env:TEST_RUN_ID="before"
$env:PEAK_VUS="100"
docker compose -f compose.yaml -f compose.observability.yaml --profile loadtest run --rm k6
```

기본 부하 형태는 30초 워밍업, 1분 상승, 3분 유지, 30초 하강이다. 필요하면 `WARMUP`, `RAMP`, `HOLD`, `COOLDOWN`, `PEAK_VUS`, `THINK_TIME` 환경변수로 바꿀 수 있다. 결과는 `performance/results/before.json`에 저장된다.

## 3. 첫 병목 판정

부하가 상승하는 동안 대시보드의 변화 시각을 비교한다.

| 관측 패턴 | 우선 의심 지점 |
|---|---|
| `hikaricp_connections_pending` 증가, active=max | 커넥션 풀 또는 PostgreSQL |
| Hikari 여유, CPU/GC와 HTTP p95 동시 증가 | 애플리케이션/JVM |
| API 정상, `kafka_consumergroup_lag` 지속 증가 | Kafka consumer 또는 indexer |
| Elasticsearch rejected 증가 | Elasticsearch thread pool/색인 |
| processing p95는 낮고 end-to-end p95만 증가 | CDC/Kafka 대기 구간 |
| 특정 시점에 5xx와 Loki 오류 로그 동시 증가 | 해당 로그의 예외/외부 의존성 |

주요 Prometheus 메트릭:

- `http_server_requests_seconds_bucket`
- `hikaricp_connections_active`, `hikaricp_connections_pending`, `hikaricp_connections_max`
- `jvm_gc_pause_seconds`, `process_cpu_usage`, `jvm_memory_used_bytes`
- `pg_stat_activity_count`
- `kafka_consumergroup_lag`
- `elasticsearch_thread_pool_rejected_count`
- `search_indexer_processing_seconds_bucket`
- `search_indexer_end_to_end_seconds_bucket`
- `search_indexer_events_total`

## 4. 개선 후 재실행과 비교

코드 또는 설정을 개선한 뒤 같은 데이터와 `PEAK_VUS`로 다시 실행한다.

```powershell
$env:TEST_RUN_ID="after"
$env:PEAK_VUS="100"
docker compose -f compose.yaml -f compose.observability.yaml --profile loadtest run --rm k6

.\performance\scripts\compare-k6-results.ps1 `
  -Before performance/results/before.json `
  -After performance/results/after.json `
  -Output performance/results/comparison.md
```

비교 보고서에는 p95, p99, 처리량, 오류율, 총 요청 수와 증감률이 기록된다. p95 감소만 보지 말고 동일 오류율 조건에서 처리량이 함께 개선됐는지 확인한다.

## 5. 종료

관측 데이터는 named volume에 유지된다.

```powershell
docker compose -f compose.yaml -f compose.observability.yaml down
```

볼륨까지 삭제하는 `down -v`는 이전 실험 데이터를 모두 지우므로 새 기준선을 만들 때만 사용한다.
