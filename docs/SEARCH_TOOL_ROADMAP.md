# Search Tool 고도화 작업 분담안

## 1. 현재 상태 요약

현재 search tool은 Elasticsearch 전환을 검증하는 단계까지 구현되어 있다.

- PostgreSQL이 원본 데이터 저장소다.
- Elasticsearch는 검색 성능과 검색 품질을 높이기 위한 검색 전용 인덱스다.
- `SEARCH_ENGINE=rdb | elasticsearch` 설정으로 RDB 검색과 Elasticsearch 검색을 선택할 수 있다.
- `POST /internal/search/index/rebuild` API로 PostgreSQL 데이터를 전체 재색인할 수 있다.
- 여행지, 음식점, 숙소 데이터를 공통 검색 문서인 `PlaceSearchDocument`로 변환해 Elasticsearch에 저장한다.
- 현재 검색은 BM25 기반 키워드 검색, 지역 필터, 도메인 필터, Geo 필터, 반려동물/휠체어 hard filter를 사용한다.

현재 구조:

```text
PostgreSQL
  └─ 수동 전체 재색인
       └─ Elasticsearch index

검색 요청
  └─ SEARCH_ENGINE
       ├─ rdb           -> PostgreSQL
       └─ elasticsearch -> Elasticsearch
```

목표 구조:

```text
PostgreSQL
  └─ Debezium CDC
       └─ Kafka KRaft
            └─ Search Indexer
                 └─ Elasticsearch upsert/delete
```

## 2. 고도화 목표

이번 고도화는 크게 두 방향으로 나눈다.

### 2.1 정확도 고도화

검색 결과가 사용자 의도에 더 잘 맞도록 Elasticsearch 문서와 검색 랭킹을 개선한다.

핵심 목표:

- 검색 문서에 더 풍부한 정보를 넣는다.
- `searchText` 구성을 개선한다.
- 검색 쿼리와 field boost를 튜닝한다.
- hard filter 정책을 명확히 한다.
- 실제 검색어 기반 평가 세트로 개선 전후를 비교한다.

### 2.2 성능/운영 고도화

전체 재색인에만 의존하지 않고, DB 변경분을 Elasticsearch에 자동 반영할 수 있는 구조를 만든다.

핵심 목표:

- PostgreSQL 변경을 Debezium CDC로 감지한다.
- Kafka는 ZooKeeper가 아닌 KRaft 모드로 구성한다.
- Kafka topic에 쌓인 변경 이벤트를 Search Indexer가 소비한다.
- 변경된 장소 문서만 Elasticsearch에 upsert/delete한다.

## 3. 역할 분담

## 3.1 담당자 A: 검색 문서 품질 개선 + CDC/Kafka 앞단 구축

진행 상태:

```text
A-1 검색 문서 품질 개선: 완료
A-2 CDC/Kafka 앞단 구축: 완료
```

담당자 A는 "Elasticsearch가 검색할 재료를 좋게 만들고, DB 변경 이벤트가 Kafka까지 흐르게 만드는 부분"을 맡는다.

### A-1. 검색 문서 품질 개선

상태: 완료

주요 파일:

- `src/main/java/com/gangwon/companion/domain/search/elasticsearch/PlaceSearchDocument.java`
- `src/main/java/com/gangwon/companion/domain/search/elasticsearch/PlaceSearchDocumentAssembler.java`
- `src/main/java/com/gangwon/companion/domain/search/elasticsearch/ElasticsearchIndexService.java`

작업 내용:

- `PlaceSearchDocument`에 검색 결과와 랭킹에 필요한 필드를 추가한다.
- 여행지, 음식점, 숙소별 `searchText` 구성을 개선한다.
- 반려동물, 무장애, 테마, 카테고리 관련 텍스트를 검색 문서에 반영한다.
- 사용자가 자주 쓰는 표현을 검색 문서에 반영할 수 있는 정규화 로직을 검토한다.

추가 후보 필드:

```text
themeName
themeCode
contentTypeId
firstImage
overview
category
sourceTypes
petInfoText
accessibilityInfoText
updatedAt
documentVersion
```

`searchText` 개선 예시:

```text
기존:
title + addr1 + addr2 + theme + overview

개선:
title + regionName + themeName + category + overview
+ petInfoText + accessibilityInfoText
+ normalized keywords
```

정규화 예시:

```text
애견동반 -> 반려견 동반, 반려동물 가능
오션뷰 -> 바다전망, 바다 전망, 해변 근처
무장애 -> 휠체어 접근, 장애인 편의
```

완성 기준:

- 재색인 시 확장된 `PlaceSearchDocument`가 Elasticsearch에 저장된다.
- 여행지, 음식점, 숙소의 `searchText` 구성 기준이 문서화된다.
- 기존 테스트가 통과한다.
- 팀원이 랭킹 튜닝에 사용할 수 있도록 변경된 문서 스키마를 공유한다.

완료 내용:

```text
PlaceSearchDocument 확장 완료
Elasticsearch mapping 확장 완료
searchText 구성 개선 완료
Docker 재색인 검증 완료
gangwon-places alias 전환 검증 완료
새 필드 기반 검색 샘플 검증 완료
rating/price 데이터 분포 확인 완료
```

상세 문서:

```text
docs/SEARCH_DOCUMENT_ENHANCEMENT.md
```

### A-2. CDC/Kafka 앞단 구축

상태: 완료

담당자 A는 PostgreSQL 변경 이벤트가 Kafka topic에 들어오는 것까지 구현한다.

주요 파일:

- `compose.yaml`
- `.env.example`
- Debezium connector 설정 문서 또는 스크립트
- `docs/SEARCH_ELASTICSEARCH.md`

작업 내용:

- Kafka를 KRaft 모드로 Docker Compose에 추가한다.
- Kafka Connect와 Debezium PostgreSQL connector를 구성한다.
- PostgreSQL logical replication 설정을 추가한다.
- Debezium connector를 등록한다.
- `destinations`, `destination_details`, `pet_infos`, `accessibility_infos`, `restaurants`, `lodgings` 변경 이벤트가 Kafka topic에 쌓이는지 확인한다.

1차 완성 기준:

```text
PostgreSQL에서 destinations row를 INSERT/UPDATE/DELETE 하면
Kafka topic에 Debezium CDC 이벤트가 생성된다.
```

완료 내용:

```text
PostgreSQL logical replication 설정 완료
Kafka KRaft 구성 완료
Kafka Connect + Debezium PostgreSQL connector 구성 완료
대상 테이블별 Kafka topic 생성 확인 완료
snapshot read, insert, update, delete 이벤트 검증 완료
```

상세 문서:

```text
docs/SEARCH_CDC_KAFKA_PIPELINE.md
```

1차 범위 추천:

```text
destinations
destination_details
restaurants
lodgings
```

2차 확장 후보:

```text
pet_infos
accessibility_infos
destination_images
themes
restaurant_reviews
lodging_reviews
```

팀원에게 넘길 산출물:

- Kafka KRaft 실행 방법
- Debezium connector 등록 방법
- topic 이름 규칙
- INSERT/UPDATE/DELETE 이벤트 샘플 JSON
- 어떤 table 변경이 어떤 placeId에 영향을 주는지 정리

## 3.2 담당자 B: 검색 랭킹 개선 + Search Indexer 구현

담당자 B는 "Elasticsearch가 더 좋은 결과를 고르게 만들고, Kafka 이벤트를 Elasticsearch 증분 색인으로 반영하는 부분"을 맡는다.

구현 상태: **완료**

- Spring Kafka consumer, Debezium 파서, 최신 aggregate 재조회 구현
- Elasticsearch 문서 단위 upsert/delete 구현
- retry topic 및 `.DLT` 처리 구현
- RDB/ES의 `false 제외, null 유지` hard filter 정책 통일
- field boost, 평점 및 거리 기반 `function_score`, 0건 fallback 구현
- Docker 통합 환경에서 CDC INSERT → ES upsert 및 CDC DELETE → ES delete 검증 완료
- 상세 설계와 실행 방법: [`SEARCH_INDEXER.md`](SEARCH_INDEXER.md)

### B-1. 검색 쿼리/랭킹 개선

주요 파일:

- `src/main/java/com/gangwon/companion/domain/search/elasticsearch/ElasticsearchPlaceSearchEngine.java`
- `src/main/java/com/gangwon/companion/domain/search/service/RdbPlaceSearchEngine.java`
- `src/test/java/com/gangwon/companion/domain/search/elasticsearch/ElasticsearchPlaceSearchEngineTest.java`
- `src/test/java/com/gangwon/companion/domain/search/service/RdbPlaceSearchEngineTest.java`

작업 내용:

- `name`, `searchText`, `address`, `themeName`, `category` 등 field boost를 조정한다.
- `function_score`를 사용해 거리, 이미지 여부, 리뷰 수, 평점, 인기도 등을 점수에 반영할지 검토한다.
- 검색 결과가 0건일 때 fallback 전략을 개선한다.
- hard filter 정책을 RDB와 Elasticsearch에서 동일하게 맞춘다.
- `NULL` 정책을 명확히 한다.

현재 중요한 정책 이슈:

```text
petAllowed=true 요청 시 값이 null인 후보를 어떻게 처리할 것인가?
wheelchairAccessible=true 요청 시 값이 null인 후보를 어떻게 처리할 것인가?
petSize 요청 시 값이 null인 후보를 어떻게 처리할 것인가?
```

추천 정책:

```text
false  -> 제외
true   -> 통과 + evidence 제공
null   -> 후보 유지 + INSUFFICIENT_EVIDENCE 표시
```

단, 최종 정책은 팀 합의 후 RDB와 ES 양쪽에 동일하게 적용한다.

완성 기준:

- RDB와 ES의 hard filter 정책이 일치한다.
- hard-filter violation rate가 0%다.
- 주요 검색 시나리오에서 개선 전후 결과를 비교할 수 있다.
- 검색 쿼리 변경 이유와 기대 효과가 문서화된다.

### B-2. Search Indexer 구현

담당자 B는 Kafka topic의 Debezium 이벤트를 소비해 Elasticsearch 문서를 증분 갱신한다.

작업 내용:

- Spring Kafka consumer를 추가한다.
- Debezium 이벤트 JSON을 파싱한다.
- 이벤트의 `table`, `op`, `before`, `after` 정보를 읽는다.
- 변경된 row가 어떤 `placeId`에 영향을 주는지 계산한다.
- 최신 RDB aggregate를 다시 조회한다.
- `PlaceSearchDocument`를 재생성한다.
- Elasticsearch에 문서 단위 upsert/delete를 수행한다.
- 실패 시 retry와 DLQ 전략을 검토한다.

table별 placeId 매핑 예시:

```text
destinations 변경             -> DESTINATION:{id}
destination_details 변경      -> DESTINATION:{destination_id}
pet_infos 변경                -> DESTINATION:{destination_id}
accessibility_infos 변경      -> DESTINATION:{destination_id}
restaurants 변경              -> RESTAURANT:{id}
lodgings 변경                 -> LODGING:{id}
```

완성 기준:

```text
Kafka topic에 UPDATE 이벤트가 들어오면
Search Indexer가 해당 장소 문서를 Elasticsearch에 자동 upsert한다.

Kafka topic에 DELETE 이벤트가 들어오면
Search Indexer가 해당 장소 문서를 Elasticsearch에서 삭제한다.
```

## 4. 공통 작업: 검색 품질 평가 세트

정확도 개선은 감으로 판단하지 않고 평가 세트를 기반으로 비교한다.

둘이 함께 할 작업:

- 실제 사용자가 입력할 만한 검색어 50~100개를 만든다.
- 각 검색어에 대해 기대 결과를 사람이 점수화한다.
- 개선 전후 Elasticsearch 결과를 비교한다.

평가 점수 예시:

```text
3점: 매우 적합
2점: 적합
1점: 약간 관련
0점: 부적합
```

추천 지표:

```text
Precision@5
Recall@10
NDCG@10
zero-result rate
hard-filter violation rate
```

검색 시나리오 예시:

```text
강릉 바다 산책
속초 아이와 갈만한 여행지
반려견 동반 가능한 관광지
휠체어 접근 가능한 여행지
비 오는 날 실내 여행지
오션뷰 숙소
초당 순두부 맛집
조용한 자연 명소
카페가 있는 해변
가족 여행 코스
```

## 5. 추천 진행 순서

### Step 1. 검색 정책 합의

- hard filter와 `NULL` 처리 정책을 먼저 정한다.
- RDB와 ES가 같은 정책으로 동작하도록 기준을 맞춘다.

### Step 2. 검색 문서 스키마 확장

- 담당자 A가 `PlaceSearchDocument`와 `searchText`를 개선한다.
- 담당자 B가 랭킹 개선에 사용할 필드를 확인한다.

### Step 3. 검색 랭킹 개선

- 담당자 B가 ES query DSL과 boost를 조정한다.
- 담당자 A가 문서 구성 변경으로 검색 결과가 어떻게 달라지는지 확인한다.

### Step 4. 평가 세트로 개선 전후 비교

- 둘이 함께 검색어와 정답 세트를 만든다.
- Precision@5, NDCG@10 등으로 개선 여부를 판단한다.

### Step 5. Kafka KRaft + Debezium CDC 구성

- 담당자 A가 PostgreSQL 변경 이벤트가 Kafka topic에 들어오는 것까지 구현한다.
- 이벤트 샘플과 topic 규칙을 담당자 B에게 공유한다.

### Step 6. Search Indexer 구현

- 담당자 B가 Kafka 이벤트를 소비해 Elasticsearch 문서를 upsert/delete한다.
- 전체 재색인과 증분 색인의 관계를 정리한다.

### Step 7. 통합 검증

- PostgreSQL row 변경
- Debezium CDC 이벤트 생성
- Kafka topic 적재
- Search Indexer 소비
- Elasticsearch 문서 upsert/delete
- 검색 결과 반영 확인

## 6. 최종 완료 기준

정확도 측면:

- 주요 검색 시나리오에서 검색 결과 품질이 개선된다.
- hard-filter violation rate가 0%다.
- zero-result rate가 과도하게 높지 않다.
- 개선 전후 결과를 리포트로 설명할 수 있다.

성능/운영 측면:

- 전체 재색인 API는 유지된다.
- PostgreSQL 변경 이벤트가 Kafka KRaft topic으로 발행된다.
- Search Indexer가 변경된 장소만 Elasticsearch에 증분 반영한다.
- INSERT/UPDATE/DELETE 이벤트가 모두 검증된다.
- 실패 시 retry 또는 DLQ 전략이 문서화된다.

포트폴리오 설명 포인트:

```text
PostgreSQL을 source of truth로 유지하고,
Elasticsearch를 검색 전용 인덱스로 분리했다.

Debezium CDC와 Kafka KRaft를 사용해
DB 변경 이벤트를 스트리밍했다.

Search Indexer consumer를 통해
Elasticsearch 문서를 증분 upsert/delete하도록 설계했다.

검색 정확도 개선을 위해
검색 문서 스키마, searchText, field boost, 평가 지표를 개선했다.
```

## 7. 역할 요약

```text
담당자 A
- 검색 문서 품질 개선
- searchText 개선
- Kafka KRaft + Debezium CDC 앞단 구축
- DB 변경 이벤트가 Kafka topic에 들어오는 것까지 검증

담당자 B
- Elasticsearch query/ranking 개선
- hard filter 정책 통일
- Spring Kafka 기반 Search Indexer 구현
- Kafka 이벤트를 ES upsert/delete로 반영

공통
- 검색 품질 평가 세트 작성
- 개선 전후 리포트 작성
- 통합 검증
```
