# Search CDC/Kafka 파이프라인

## 1. 목표

Elasticsearch 전체 재색인만으로는 DB 변경사항을 실시간에 가깝게 반영하기 어렵다.
이번 파이프라인의 목표는 PostgreSQL 변경 이벤트를 Kafka topic까지 전달하는 것이다.

담당자 A 범위:

```text
PostgreSQL
  -> Debezium CDC
  -> Kafka KRaft topic
```

담당자 B 범위:

```text
Kafka topic
  -> Search Indexer
  -> Elasticsearch upsert/delete
```

## 2. 구성 요소

```text
postgres_gangwon
- PostgreSQL source of truth
- logical replication 활성화

kafka_gangwon
- Kafka broker
- ZooKeeper 없는 KRaft 모드

kafka_connect_gangwon
- Debezium PostgreSQL connector 실행
- PostgreSQL 변경 이벤트를 Kafka topic으로 발행
```

## 3. Docker Compose 변경

`compose.yaml`에 다음 구성을 추가했다.

PostgreSQL CDC 설정:

```text
wal_level=logical
max_replication_slots=8
max_wal_senders=8
```

Kafka:

```text
image: confluentinc/cp-kafka:7.8.0
mode: KRaft
internal bootstrap: kafka:9092
host bootstrap: localhost:29092
```

Kafka Connect:

```text
image: quay.io/debezium/connect:3.0
REST API: http://localhost:8083
bootstrap servers: kafka:9092
JSON converter schemas.enable=false
```

## 4. 실행 순서

컨테이너 실행:

```powershell
docker compose up -d --build
```

상태 확인:

```powershell
docker ps
curl.exe -fsS http://localhost:8083/connectors
```

Debezium connector 등록:

```powershell
.\scripts\register-search-cdc-connector.ps1
```

직접 등록하고 싶으면:

```powershell
curl.exe -X POST `
  -H "Content-Type: application/json" `
  --data-binary "@scripts/debezium-postgres-connector.json" `
  http://localhost:8083/connectors
```

connector 상태 확인:

```powershell
curl.exe -fsS http://localhost:8083/connectors/gangwon-postgres-connector/status
```

## 5. Connector 설정

설정 파일:

```text
scripts/debezium-postgres-connector.json
```

감시 대상 테이블:

```text
public.destinations
public.destination_details
public.pet_infos
public.accessibility_infos
public.restaurants
public.lodgings
```

topic prefix:

```text
gangwon
```

예상 topic:

```text
gangwon.public.destinations
gangwon.public.destination_details
gangwon.public.pet_infos
gangwon.public.accessibility_infos
gangwon.public.restaurants
gangwon.public.lodgings
```

## 6. Topic 확인

topic 목록:

```powershell
docker exec kafka_gangwon kafka-topics --bootstrap-server kafka:9092 --list
```

특정 topic 이벤트 읽기:

```powershell
docker exec -it kafka_gangwon kafka-console-consumer `
  --bootstrap-server kafka:9092 `
  --topic gangwon.public.destinations `
  --from-beginning `
  --max-messages 5
```

## 7. CDC 이벤트 테스트

테스트용 UPDATE:

```powershell
docker exec postgres_gangwon psql -U gangwon_user -d gangwon `
  -c "update destinations set updated_at = updated_at where id = (select id from destinations order by id limit 1);"
```

이후 `gangwon.public.destinations` topic에서 이벤트가 보이면 앞단 파이프라인이 동작하는 것이다.

Debezium 이벤트에서 중요한 필드:

```text
before
after
op
source.table
source.lsn
ts_ms
```

operation 값:

```text
c: create
u: update
d: delete
r: snapshot read
```

현재 Kafka Connect는 JSON converter의 `schemas.enable=false` 설정을 사용한다.
그래서 메시지 최상위에 `schema/payload` 래퍼가 붙지 않고 다음처럼 바로 읽힌다.

```json
{
  "before": null,
  "after": {
    "id": 2318,
    "title": "간현관광지"
  },
  "source": {
    "schema": "public",
    "table": "destinations"
  },
  "op": "u"
}
```

DELETE 이벤트는 PostgreSQL 기본 replica identity 정책 때문에 `before.id` 중심으로 사용한다.
삭제된 row의 모든 컬럼 값이 항상 완전하게 들어온다고 가정하면 안 된다.

## 8. Search Indexer가 사용할 placeId 매핑

여러 RDB 테이블이 하나의 ES 문서를 구성하므로 CDC row 이벤트를 그대로 ES에 넣으면 안 된다.
Search Indexer는 이벤트에서 영향받은 장소를 식별한 뒤 최신 aggregate를 다시 조회해야 한다.

매핑 규칙:

```text
public.destinations
-> DESTINATION:{after.id 또는 before.id}

public.destination_details
-> DESTINATION:{after.destination_id 또는 before.destination_id}

public.pet_infos
-> DESTINATION:{after.destination_id 또는 before.destination_id}

public.accessibility_infos
-> DESTINATION:{after.destination_id 또는 before.destination_id}

public.restaurants
-> RESTAURANT:{after.id 또는 before.id}

public.lodgings
-> LODGING:{after.id 또는 before.id}
```

Search Indexer 권장 흐름:

```text
1. Debezium 이벤트 수신
2. table, op 확인
3. 영향받은 placeId 계산
4. delete 이벤트면 ES 문서 삭제
5. insert/update 이벤트면 RDB에서 최신 aggregate 조회
6. PlaceSearchDocument 재생성
7. Elasticsearch upsert
8. 성공 후 offset commit
```

검증 완료 상태:

```text
Kafka KRaft healthy 확인 완료
Kafka Connect healthy 확인 완료
Debezium PostgreSQL connector RUNNING 확인 완료
대상 테이블별 topic 생성 확인 완료
snapshot read 이벤트(op=r) 확인 완료
UPDATE 이벤트(op=u) 확인 완료
INSERT 이벤트(op=c) 확인 완료
DELETE 이벤트(op=d) 확인 완료
```

## 9. 운영상 주의

현재 connector 설정은 로컬 개발용이다.

```text
database.user/database.password가 JSON 파일에 들어 있다.
운영에서는 secret 또는 환경변수 기반 등록 방식으로 바꿔야 한다.
```

처음 connector를 등록하면 `snapshot.mode=initial` 때문에 대상 테이블의 현재 row가
`op=r` 이벤트로 먼저 발행된다. 이후 변경분은 `c/u/d` 이벤트로 발행된다.

전체 재색인 API는 CDC 도입 후에도 유지한다.

```text
전체 재색인:
- mapping 변경
- 장애 복구
- 데이터 불일치 복구

CDC 증분 색인:
- 일반 insert/update/delete 반영
```

## 10. 담당자 B 구현 연결

Kafka topic 이후 Search Indexer와 Elasticsearch 증분 upsert/delete가 구현되었다.
2026-08-26 로컬 Docker 환경에서 테스트 전용 restaurant의 INSERT 및 DELETE가
Elasticsearch 문서의 `found=true` 및 `found=false`로 반영되는 것까지 검증했다.
실행 설정, DELETE 정책, retry/DLT, 검색 랭킹 정책은
[`SEARCH_INDEXER.md`](SEARCH_INDEXER.md)를 참고한다.
