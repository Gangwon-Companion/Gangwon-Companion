# 여행 코스추천 E2E 현황

기준일: 2026-08-29

## 완료 기준

```text
사용자
  -> Spring POST /api/v1/courses/recommendations
  -> AI POST /internal/travel/plan
  -> Supervisor
  -> Destination / Restaurant / Lodging 검색 Agent
  -> Spring POST /internal/search/places
  -> Candidate Collector
  -> Itinerary
  -> Hard Validator
  -> Quality Validation
  -> Response
  -> Spring 최종 응답
```

HTTP 200 또는 `FAILED` 종단 응답만으로 완료 처리하지 않는다. 실제 DB 후보로 일정이 생성되고 `final_response.response_status=READY`가 반환돼야 성공이다.

## 지금까지 완료한 작업

### 서비스 연결

- Docker Desktop 기동
- PostgreSQL, Elasticsearch, Kafka, Kafka Connect, Spring 컨테이너 정상 기동
- `Gangwon-AI` 이미지 빌드 및 AI 컨테이너 8000 포트 기동
- AI 컨테이너에서 `host.docker.internal:8080`의 Spring 호출 확인
- AI와 Spring health 상태 확인

### Agent 그래프 및 Search Tool

- Form Binder, Preference Extractor, Conflict Checker, Supervisor 연결
- Destination, Restaurant, Lodging Agent 병렬 분기 연결
- 각 검색 Agent에서 공통 `BeSearchClient`를 통해 Spring Search Tool 호출
- Candidate Collector, Itinerary, Hard Validator, Quality Validation, Response 연결
- 후보 부족 및 검증 실패 시 검색 Agent 또는 Itinerary 재시도 연결
- 최대 재시도 초과 시 `FAILED` 응답 연결
- 실제 호출에서 후보 검색, 병합, 일정 생성, 검증, 재시도 경로 실행 확인

### 영업시간 근거

- 외부 API 실시간 조회가 아닌 DB 저장값을 사용
- 관광지: `destination_details.usage_time`
- 음식점: `restaurants.open_time`
- 숙소: `lodgings.check_in_time`, `lodgings.check_out_time`
- 검색 응답 evidence에 `opens_at`, `closes_at` 추가
- RDB와 Elasticsearch 문서에 영업시간 필드 연결
- `HH:mm`, 한국어 시·분, `상시 개방`, `24시간`, `24:00` 파싱 지원
- 파싱할 수 없는 빈 값, `점포별 상이`, `전화 문의`는 근거 부족으로 유지
- 영업시간 파서 단위 테스트 추가

2026-08-29 로컬 DB에서 확인한 영업시간 보유 건수:

| 도메인 | 보유 건수 |
|---|---:|
| 관광지 상세 | 1,204 |
| 음식점 | 73 |
| 숙소 | 48 |

### 성공한 실제 E2E

요청:

```json
{
  "message": "강릉에서 하루 여행하고 싶어요",
  "region": "강릉",
  "travel_days": 1,
  "nights": 0,
  "pet_allowed": false,
  "preferences": []
}
```

검증된 결과:

```text
status             = completed
responseStatus     = READY
retryCount         = 0
itineraryStatus    = READY
hardValidation     = VALID
qualityValidation  = PASS
itineraryCount     = 3
```

다음 두 진입점에서 모두 성공했다.

1. AI `POST /internal/travel/plan` 직접 호출
2. 회원가입 → 로그인 → JWT 인증 → Spring `POST /api/v1/courses/recommendations` 호출

Spring 요청 DTO의 snake_case 전달과 AI 최종 JSON의 무손실 반환도 실제 호출로 확인했다.

## 재현 방법

Spring과 AI 컨테이너가 실행 중일 때 Companion 루트에서 실행한다.

```powershell
.\performance\scripts\run-course-recommendation-e2e.ps1
```

스크립트는 테스트 사용자를 생성하고 JWT를 발급받아 사용자용 코스추천 API를 호출한다. 다음 조건 중 하나라도 만족하지 않으면 실패 코드로 종료한다.

- `status=completed`
- `final_response.response_status=READY`
- `itinerary_status=READY`
- `hard_validation.status=VALID`
- `quality_validation.status=PASS`
- 일정 항목이 한 개 이상 존재

## 확인된 실패 시나리오

`바다`, `카페`, 반려동물 허용과 크기를 함께 요구한 강릉 1일 요청은 전체 노드 연결과 재시도는 정상 실행됐지만 최종 `FAILED`가 됐다.

원인:

- 선택된 강릉 카페 후보들의 `open_time`이 비어 있음
- 음식점 반려동물 허용 및 크기 근거가 없음
- Hard Validator가 임의 추정을 허용하지 않음
- 동일 조건으로 검색 Agent를 4회 재실행해도 더 나은 근거가 없어 종료

이는 노드나 Search Tool 연결 문제가 아니라 원천 데이터 및 후보 선택 문제다.

## 남은 작업

### P0 — 장애 응답 계약

- [x] AI 서버 중단 시 Spring 오류 응답 검증
- [x] AI 응답 timeout 검증 및 RestClient timeout 명시
- [x] AI 4xx 응답 전달 정책 검증
- [x] AI 5xx 및 잘못된 JSON 응답 검증
- [ ] `GlobalExceptionHandler`의 외부 API 오류 코드와 로그 검증
- [x] 위 항목의 자동화 테스트 추가

### P0 — AI 저장소 E2E 강화

- [x] 기존 live test의 `READY 또는 FAILED` 허용을 성공 시나리오에서는 `READY` 필수로 변경
- [ ] 깨진 한글 테스트 문자열과 fixture를 UTF-8로 정리
- [x] 기본 성공 시나리오와 의도된 `FAILED` 시나리오 분리
- [ ] CI에서 Spring 의존 E2E를 선택적으로 실행할 수 있도록 명령 정리

### P1 — 확정된 Elasticsearch 운영 경로 검증

- [x] 운영 검색 엔진을 Elasticsearch로 확정
- [x] 현재 alias의 문서 3,143건이 모두 문서 버전 3임을 확인
- [x] `opensAt`, `closesAt`이 있는 문서 450건 확인
- [x] `DESTINATION` 1,030건, `RESTAURANT` 1,548건, `LODGING` 565건 확인
- [x] 최신 영업시간 파서 변경을 반영하도록 전체 재색인
- [x] 현재 RDB 기본값으로 확인한 성공 시나리오를 Elasticsearch 모드에서 `READY`로 재검증
- [ ] Kafka/Debezium 변경 후 영업시간 문서 갱신 확인

### P1 — 일정 범위 확장

- [x] 1박 2일 요청에서 Lodging Agent 포함 `READY` (평창, 일정 8개)
- [ ] 2박 3일 이상 다일 일정 검증
- [x] 같은 장소 중복 배치 방지 검증
- [x] 방문 시간이 실제 영업시간 안에 있는지 검증
- [x] 이동시간과 지역 반경 검증
- [ ] 숙소 체크인·체크아웃 시간 의미를 일반 영업시간과 분리할지 결정

### P1 — 정책 조건 확장

- [ ] 반려동물 허용 성공 시나리오용 데이터 확보
- [ ] 반려동물 크기 및 실내 동반 근거 모델 확정
- [ ] 휠체어 접근성 성공·실패 시나리오
- [ ] 최대 가격 조건을 검색과 Validator에 연결
- [ ] 근거 부족 후보를 재시도할 때 동일 후보만 반복하지 않도록 제외 또는 우선순위 조정

### P2 — 운영 및 품질

- [ ] E2E 테스트 계정 정리 또는 전용 seed 사용자 사용
- [ ] 한 명령으로 전체 인프라와 AI를 기동하는 Compose 구성 검토
- [ ] 요청별 trace ID를 Spring과 AI 로그에 공통 적용
- [ ] Agent별 실행 시간, 재시도 횟수, READY/FAILED 비율 메트릭 추가
- [ ] API 응답 DTO를 raw `JsonNode` 대신 명시적 계약으로 전환할지 결정

## 권장 진행 순서

1. AI timeout·중단·4xx·5xx에 대한 Spring 테스트
2. AI 저장소 live E2E를 `READY` 필수로 강화
3. Elasticsearch 전체 재색인 후 동일한 1일 코스 검증
4. 1박 2일 숙소 포함 성공 경로 확보
5. 반려동물과 접근성 데이터 보강 및 성공 경로 추가
6. CI와 관측성 연결

## 현재 판단

2026-08-29 최신 Spring·AI 이미지와 `SEARCH_ENGINE=elasticsearch` 구성에서 강릉 1일 코스와 평창 1박 2일 코스가 각각 `READY`, `VALID`, `PASS`로 통과했다. 전체 재색인은 원본 3,143건과 색인 3,143건이 일치하고 alias 전환도 성공했다. 구체 검색어가 0건이면 지역·도메인·정책·영업시간 hard filter를 유지한 채 텍스트만 제거하는 최종 fallback을 적용한다.

반려동물·휠체어 근거는 현재 Elasticsearch 기준 관광지에만 존재한다. 음식점과 숙소는 두 정책 필드가 모두 0건이므로 조건부 전체 코스는 임의 추정 없이 `EVIDENCE_MISSING`으로 실패한다. 네이버 Maps/Local API는 주소·좌표·경로 보강에는 사용할 수 있지만 이 정책 필드를 제공하지 않으므로, 성공 경로 확보에는 별도의 신뢰 가능한 음식점·숙소 정책 데이터가 필요하다.
# 2026-08-29 바다 2박 3일 검증

해안 범위 보정, 최종 JWT E2E, Kafka CDC, k6 RDB/ES 비교 결과는 [강원도 바다 2박 3일 코스 E2E·검색 성능 검증](ocean-course-e2e-performance-20260829.md)에 정리했다.
