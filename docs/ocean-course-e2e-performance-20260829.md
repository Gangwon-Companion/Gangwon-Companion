# 강원도 바다 2박 3일 코스 E2E·검색 성능 검증

검증일: 2026-08-29  
요청: `강원도 바다를 둘러볼 수 있는 2박 3일 코스를 짜줘`

## 결론

- JWT 회원가입·로그인부터 Spring → AI Supervisor → Spring 내부 검색 → Elasticsearch까지 실제 E2E가 `READY`로 완료됐다.
- Hard Validation은 `VALID`, Quality Validation은 `PASS`, 재시도는 0회이며 3일 13개 슬롯을 생성했다.
- 최초 결과의 내륙 워터파크 오탐을 발견해, 광역 강원도+바다 요청의 관광지·숙소 검색 범위를 동해안 6개 시군으로 제한했다.
- 동일한 코스 후보 검색 부하에서 ES는 RDB보다 p95가 75.66% 낮고 처리량이 25.42% 높았다.
- PostgreSQL 변경은 Debezium → Kafka → Spring indexer → ES로 521.6ms 안에 반영됐고, consumer lag은 0이었다.
- 다만 음식점 영업시간 데이터가 동해안에 매우 부족해 식사는 평창 후보가 섞인다. 현재 결과는 구조적으로 유효하지만 실제 해안 여행 동선 품질은 아직 운영 기준 미달이다.

## 생성된 일정

| 일차 | 시간 | 종류 | 장소 | 이전 장소에서 추정 이동 |
|---|---|---|---|---:|
| 1 | 10:00-12:00 | 관광지 | 외옹치해변(속초) | 0분 |
| 1 | 12:30-13:30 | 식사 | 밥먹을시간(평창) | 69분 |
| 1 | 18:00-19:30 | 식사 | 시래기밥상(평창) | 5분 |
| 1 | 20:00-21:00 | 숙소 | 강릉강변스테이 | 24분 |
| 2 | 08:00-09:00 | 식사 | 봉평식당(평창) | 59분 |
| 2 | 10:00-12:00 | 관광지 | 순포해변(강릉) | 61분 |
| 2 | 12:30-13:30 | 식사 | 이선생 중화요리(평창) | 27분 |
| 2 | 18:00-19:30 | 식사 | 돈감자탕(평창) | 5분 |
| 2 | 20:00-21:00 | 숙소 | 강릉강변스테이 | 24분 |
| 3 | 08:00-09:00 | 식사 | 뜰안채(평창) | 66분 |
| 3 | 10:00-12:00 | 관광지 | 동산포해변(양양) | 70분 |
| 3 | 12:30-13:30 | 식사 | 오대산 정가네 순메밀막국수(평창) | 39분 |
| 3 | 18:00-19:30 | 식사 | 프렘식당(평창) | 20분 |

관광지 3곳은 모두 `oceanView` 조건과 일치하며 검색 범위도 `GOSEONG, SOKCHO, YANGYANG, GANGNEUNG, DONGHAE, SAMCHEOK`으로 확인했다. 숙소도 같은 해안 범위를 사용한다. 음식점은 데이터 부족 때문에 강원 전체를 조회한다.

이동 시간은 도로 API 실측값이 아니라 좌표 거리 기반 추정값이다. 실제 사용자 노출 전에 Naver Directions 또는 동등한 도로 경로 API로 교체해야 한다.

## E2E 결과

| 항목 | 결과 |
|---|---|
| API 경로 | 회원가입 → 로그인/JWT → `/api/v1/courses/recommendations` |
| status | `completed` |
| response status | `READY` |
| itinerary status | `READY` |
| hard validation | `VALID` |
| quality validation | `PASS` |
| retry | 0 |
| itinerary | 13개 |
| ES 문서 | 3,143개 |

전체 응답: [course-e2e-ocean-3d-coastal-20260829.json](../performance/results/course-e2e-ocean-3d-coastal-20260829.json)

## RDB 대 Elasticsearch k6 비교

코스 생성 전체가 아니라 AI가 사용하는 관광지·음식점·숙소 후보 검색 API만 분리했다. 양쪽 모두 동일한 요청, 최대 20 VU, 5초 warm-up, 10초 ramp, 30초 hold, 5초 cool-down, 요청 간 0.1초 조건이다. 검색 측정 중 CDC indexer는 껐고 측정 후 다시 켰다.

| 지표 | RDB | Elasticsearch | 변화 |
|---|---:|---:|---:|
| p95 | 123.82ms | 30.14ms | -75.66% |
| p99 | 186.52ms | 56.40ms | -69.76% |
| 처리량 | 107.41 req/s | 134.71 req/s | +25.42% |
| 오류율 | 0% | 0% | 동일 |
| 요청 수 | 5,379 | 6,745 | +25.40% |

스크립트: [course-search-load.js](../performance/k6/course-search-load.js)  
원본: [RDB 결과](../performance/results/rdb-course-search-valid-20260829.json), [ES 결과](../performance/results/es-course-search-valid-20260829.json), [비교표](../performance/results/course-search-rdb-vs-es-20260829.md)

이 수치는 현재 로컬 Docker 데이터와 50초 부하의 결과다. 장기 soak test, 여러 회 반복의 중앙값, 서버 자원 제한을 고정한 CI 기준값은 별도로 필요하다.

## Kafka CDC 검증

`restaurants.id=64`의 rating을 `0 → 0.01`로 수정하고 ES 문서 `RESTAURANT:64`를 polling한 뒤 원래 값으로 복원했다.

| 항목 | 결과 |
|---|---|
| Debezium connector | `gangwon-postgres-connector`: RUNNING |
| connector task | RUNNING |
| consumer group | `gangwon-search-indexer` |
| 6개 원본 topic lag | 모두 0 |
| ES 갱신 확인 | 성공 |
| 관측 지연 | 521.6ms |
| DB·ES 원복 | 성공 |

원본: [kafka-cdc-es-20260829.json](../performance/results/kafka-cdc-es-20260829.json)

## 이번에 확인된 남은 과제

1. 동해안 음식점의 영업시간 데이터 보강이 최우선이다. 현재 확인 가능한 수가 3일 8끼를 해안 지역만으로 구성하기에 부족하다.
2. 식당 후보를 관광지·숙소 좌표 주변으로 순차 재검색하는 단계가 필요하다. 현재 Supervisor는 도메인 후보를 병렬 수집해 식당이 당일 관광지 위치를 미리 알지 못한다.
3. 좌표 직선거리 추정을 실제 도로 이동시간으로 교체해야 한다.
4. `oceanView`는 원천의 정식 속성보다 검색 텍스트 파생 태그 성격이 강하다. 해변 유형/해안 시군 같은 명시적 필드가 필요하다.
5. 반려동물·휠체어 정책 데이터는 음식점·숙소에서 거의 비어 있으므로, 해당 조건 요청은 현재 안전하게 추천 불가로 처리해야 한다.

## 최종 실행 상태

- Spring: healthy, `SEARCH_ENGINE=elasticsearch`, `SEARCH_INDEXER_ENABLED=true`
- AI: healthy, Spring 검색 주소 `http://host.docker.internal:8080`
- PostgreSQL, Kafka, Kafka Connect, Elasticsearch: healthy

