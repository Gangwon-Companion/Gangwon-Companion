# 로컬 데이터 동기화 및 검색 통합 테스트 문제해결

빈 PostgreSQL에서 관광 데이터를 수집하고 Elasticsearch를 초기화한 뒤 AI 여행 일정 E2E 테스트까지 실행하는 절차와 실제로 확인된 오류를 정리한다.

남은 구현 과제와 우선순위는 [검색·AI 통합 후속 작업](SEARCH_INTEGRATION_NEXT_STEPS.md)을 참고한다.

## 1. 전체 구조

```text
한국관광공사 API
  -> Spring 동기화 서비스
  -> PostgreSQL
  -> Debezium CDC
  -> Kafka
  -> Search Indexer
  -> Elasticsearch
  -> AI 내부 장소 검색 API
```

PostgreSQL은 원본 데이터 저장소이고 Elasticsearch는 검색용 인덱스다. 내부 검색 API는 `SEARCH_ENGINE` 설정에 따라 RDB 또는 Elasticsearch를 선택한다.

## 2. 구현 완료 내역

### AI용 내부 장소 검색

- `POST /internal/search/places` API 구현
- 관광지(`DESTINATION`), 음식점(`RESTAURANT`), 숙소(`LODGING`) 통합 검색
- 지역 코드, 자연어 검색어, 위치·반경 조건 지원
- 반려동물 동반, 반려동물 크기, 휠체어 접근 가능 여부 hard filter 지원
- 검색 결과에 점수, 거리, 충족한 선호 조건, 근거, 누락 필드, 근거 부족 상태 포함
- `SEARCH_ENGINE=rdb|elasticsearch` 설정을 통한 검색 엔진 전환

### Elasticsearch 검색 문서와 랭킹

- 장소 데이터를 공통 `PlaceSearchDocument`로 변환
- 관광지 기본정보와 상세정보, 반려동물 정보, 무장애 정보를 장소 단위 문서로 통합
- 테마, 카테고리, 이미지, 소개, 원본 출처, 갱신 시각 및 문서 버전 필드 추가
- 정규화된 키워드를 포함하도록 `searchText` 개선
- 이름, 검색 텍스트, 주소, 테마, 카테고리 등에 field boost 적용
- 거리, 이미지 존재 여부, 평점 등을 반영하는 `function_score` 적용
- 검색 결과가 0건일 때 완화된 조건으로 재검색하는 fallback 적용
- RDB와 Elasticsearch의 hard filter 의미를 통일

### PostgreSQL 변경의 Elasticsearch 자동 반영

- Kafka를 ZooKeeper 없는 KRaft 모드로 구성
- Kafka Connect와 Debezium PostgreSQL connector 구성
- PostgreSQL logical replication 기반 CDC 파이프라인 구성
- Spring Kafka 기반 Search Indexer 구현
- `destinations`, `destination_details`, `pet_infos`, `accessibility_infos`, `restaurants`, `lodgings` 변경 이벤트 처리
- INSERT·UPDATE 발생 시 최신 RDB aggregate를 조회해 Elasticsearch 문서 upsert
- 장소 DELETE 발생 시 Elasticsearch 문서 삭제
- 처리 실패 시 retry topic과 DLT 처리
- CDC 원본 발생 시각부터 Elasticsearch 반영 완료까지 end-to-end 지연시간 측정

### 전체 재색인

- `POST /internal/search/index/rebuild` API 구현
- PostgreSQL의 전체 검색 대상 데이터를 새 버전 인덱스에 bulk 적재
- 적재 완료 후 `gangwon-places` alias 전환
- 재색인 API 보호용 `X-Search-Reindex-Key` 지원

### 데이터 수집과 수동 동기화

- 관광지 일반·반려동물·무장애 목록 수동 동기화 API
- 관광지 일반·반려동물·무장애 상세정보 수동 동기화 API
- 음식점 목록 및 상세정보 수동 동기화 API
- 숙소 목록 및 상세정보 수동 동기화 API
- 액티비티 수동 동기화 API
- 관광 혼잡도 수동 동기화 API
- 관광 데이터 전체를 매일 새벽 2시에 갱신하는 스케줄러
- 외부 원본 ID를 이용한 음식점·숙소 중복 방지 및 갱신
- 관광지의 여러 외부 출처를 공통 장소와 `destination_sources`로 분리

### 성능 테스트와 관측 환경

- Spring Actuator와 Micrometer Prometheus registry 구성
- 검색 인덱서 처리시간 및 end-to-end 지연시간 메트릭 추가
- Prometheus 수집 대상 구성
- Grafana 데이터소스와 검색 부하 테스트 대시보드 자동 프로비저닝
- Loki와 Alloy 기반 컨테이너 로그 수집
- PostgreSQL, Kafka, Kafka Connect, Elasticsearch exporter 구성
- k6 기반 검색 API 단계별 부하 테스트 시나리오 추가
- 개선 전후 p95, p99, 처리량, 오류율 비교 스크립트 추가

### 테스트와 문서

- 검색 요청·응답 JSON 계약 테스트
- RDB 및 Elasticsearch 검색 엔진 테스트
- Elasticsearch 색인 및 검색 통합 테스트
- Debezium 이벤트 파싱과 Search Indexer 이벤트 처리 테스트
- CDC INSERT·UPDATE·DELETE의 Elasticsearch 반영 검증
- 검색 문서, CDC/Kafka, Search Indexer, 부하 테스트 및 관측 환경 실행 문서 작성

## 3. 필수 환경변수와 Docker 주소

실제 비밀값은 저장소에 기록하지 않는다. `.env.example`을 참고해 로컬 환경에 설정한다.

```dotenv
TOUR_API_KEY=<한국관광공사 API 키>
ELASTICSEARCH_URL=http://elasticsearch:9200
SEARCH_ENGINE=elasticsearch
SEARCH_INDEXER_ENABLED=true
ELASTICSEARCH_API_KEY=
ELASTICSEARCH_REINDEX_KEY=<로컬 재색인 보호 키>
```

- 애플리케이션이 참조하는 이름은 `TOUR_API_KEY`다. `TOUR_API_SERVICE_KEY`는 현재 코드와 Compose에서 사용하지 않는다.
- 로컬 Docker Elasticsearch는 보안이 비활성화되어 있어 `ELASTICSEARCH_API_KEY`를 비워둘 수 있다.
- `ELASTICSEARCH_REINDEX_KEY`는 Elasticsearch 인증 키가 아니라 전체 재색인 API 보호용 키다.

호출 위치에 따라 Elasticsearch 주소가 다르다.

| 호출 위치 | 주소 |
|---|---|
| 호스트 브라우저, PowerShell | `http://localhost:9200` |
| Spring Docker 컨테이너 | `http://elasticsearch:9200` |
| 동일 Compose 내부 서비스 | `http://elasticsearch:9200` |

Spring 컨테이너에서 `localhost:9200`을 사용하면 Spring 컨테이너 자신을 가리켜 연결이 거부되고 `/internal/search/places`가 HTTP 500을 반환한다.

환경변수만 변경한 경우:

```powershell
docker compose up -d --force-recreate spring
```

Java 코드도 변경한 경우:

```powershell
docker compose up -d --build spring
```

## 4. 빈 DB 초기화

### 자동 수집 시점

데이터 수집은 서버 시작 즉시 실행되지 않는다. `DataSyncScheduler.syncAll()`은 기본적으로 Asia/Seoul 기준 매일 새벽 2시에 실행된다.

자동 동기화 대상:

- 관광지 목록과 상세정보
- 액티비티
- 음식점 목록과 상세정보
- 숙소 목록과 상세정보
- 관광 혼잡도

빈 DB를 즉시 테스트하려면 수동 동기화 API를 호출한다.

### themes 기준 데이터

관광지는 `themes` 테이블을 참조한다. 관광 API의 대분류 코드에 해당하는 테마가 없으면 `THEME_NOT_FOUND`가 발생하고 동기화 트랜잭션이 실패한다.

필요한 코드는 `NA`, `HS`, `EX`, `EV`, `LS`, `SH`, `VE`다. 자동 초기화 구현 전까지 다음 멱등 SQL로 등록할 수 있다.

```sql
INSERT INTO themes (code, name, display_order)
VALUES
    ('NA', '자연', 1),
    ('HS', '역사', 2),
    ('EX', '체험', 3),
    ('EV', '축제·공연', 4),
    ('LS', '레저·스포츠', 5),
    ('SH', '쇼핑', 6),
    ('VE', '휴양', 7)
ON CONFLICT (code) DO NOTHING;
```

```sql
SELECT id, code, name, display_order
FROM themes
ORDER BY display_order;
```

장기적으로는 Flyway/Liquibase 마이그레이션이나 멱등 초기화 코드로 대체해야 한다.

## 5. 인증과 Swagger

관리자 동기화 API는 인증된 사용자만 호출할 수 있다. 별도의 관리자 역할 검사는 아직 적용되지 않았다.

1. `/api/v1/auth/signup`으로 회원가입한다.
2. `/api/v1/auth/login`으로 로그인한다.
3. 응답의 `token`을 Swagger `Authorize`에 `Bearer <token>` 형식으로 등록한다.

개발 환경에서 CAPTCHA가 비활성화되어 있다면 `captchaToken`은 `null`로 전달한다. Swagger의 `"string"`은 예시 문자열이다.

```json
{
  "captchaToken": null,
  "username": "testuser1",
  "password": "Test1234!"
}
```

회원가입 비밀번호는 8~20자이며 대문자, 소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 한다. JWT는 로그, 문서, 채팅, 이슈에 기록하지 않으며 노출된 토큰은 로그아웃 처리한다.

## 6. 개별 수동 동기화 API

관광지 목록:

```http
POST /api/v1/admin/destinations/sync/korean
POST /api/v1/admin/destinations/sync/pet
POST /api/v1/admin/destinations/sync/accessibility
```

관광지 상세정보:

```http
POST /api/v1/admin/destinations/details/sync/korean?limit=50
POST /api/v1/admin/destinations/details/sync/pet?limit=50
POST /api/v1/admin/destinations/details/sync/accessibility?limit=50
```

음식점, 숙소, 액티비티:

```http
POST /api/v1/admin/restaurants/sync
POST /api/v1/admin/restaurants/details/sync
POST /api/v1/admin/lodgings/sync
POST /api/v1/admin/lodgings/details/sync
POST /api/v1/admin/activities/sync
```

관광 혼잡도:

```http
POST /api/v1/promotions/hotplace/sync
```

권장 순서:

```text
themes 초기화
-> 관광지 목록과 상세
-> 음식점 목록과 상세
-> 숙소 목록과 상세
-> 액티비티
-> Elasticsearch 전체 재색인
```

## 7. Elasticsearch 최초 적재

`SEARCH_INDEXER_ENABLED=true`는 Kafka의 신규 변경 이벤트를 Elasticsearch에 반영한다. 기존 PostgreSQL 데이터 전체가 항상 자동 적재된다는 의미는 아니다. 최초 구축이나 인덱스 초기화 후에는 전체 재색인을 실행한다.

```http
POST /internal/search/index/rebuild
X-Search-Reindex-Key: <ELASTICSEARCH_REINDEX_KEY>
```

호스트에서 alias와 문서 수를 확인한다.

```powershell
Invoke-RestMethod http://localhost:9200/_cat/aliases?v
Invoke-RestMethod http://localhost:9200/gangwon-places/_count
```

정상 상태:

- `gangwon-places` alias가 실제 버전 인덱스를 가리킨다.
- 문서 수가 0보다 크다.
- `DESTINATION`, `RESTAURANT`, `LODGING` domain 문서가 존재한다.

alias가 존재해도 문서 수가 0이면 검색 결과는 비어 있다. 빈 인덱스 자체는 HTTP 500의 원인이 아니며 정상적으로 빈 `results`를 반환해야 한다.

## 8. 내부 검색 검증

AI를 실행하기 전에 BE 검색 API를 직접 확인한다.

```http
POST /internal/search/places
Content-Type: application/json
```

```json
{
  "domain": "RESTAURANT",
  "slot": "D1_LUNCH",
  "region_codes": ["GANGNEUNG"],
  "query_text": "강릉 점심 맛집",
  "hard_filters": {},
  "soft_preferences": {},
  "limit": 5
}
```

관광지, 음식점, 숙소를 각각 확인한다. 1일 일정은 숙소가 없어도 만들 수 있지만 `D1_LUNCH`, `D1_DINNER`를 채우려면 음식점 후보가 필요하다.

## 9. AI E2E 성공 기준

HTTP 200은 종단 응답 도달만 의미한다. 실패 상태도 HTTP 200으로 반환될 수 있으므로 다음을 함께 검증한다.

```text
final_response.response_status == SUCCESS
일정 상태가 완성 상태
D1_LUNCH 후보 1개 이상
D1_DINNER 후보 1개 이상
missing_slots가 비어 있음
Hard Validator 실행 완료
Validation Agent 실행 완료
```

`NEEDS_CANDIDATES`, 음식점 검색 재시도 후 `FAILED`, Validator 미실행, 최종 `FAILED`는 종단 도달에는 성공했지만 일정 생성에는 실패한 상태다.

## 10. 확인된 오류와 해결

| 증상 | 원인 | 해결 |
|---|---|---|
| DB 데이터가 0건 | 서버 시작 즉시 수집하지 않고 새벽 2시에 실행 | 수동 동기화 API 호출 |
| 관광지 동기화 404 `THEME_NOT_FOUND` | `themes` 기준 데이터 누락 | 기준 테마 초기화 후 재호출 |
| 관광지는 검색되지만 ES 문서는 0건 | `SEARCH_ENGINE=rdb`로 RDB 검색 사용 | 재색인 후 `SEARCH_ENGINE=elasticsearch` 적용 |
| 음식점·숙소 검색 0건 | 해당 원본 데이터가 PostgreSQL에 없음 | 개별 목록 동기화 API 호출 |
| Elasticsearch 검색 API HTTP 500 | 컨테이너에서 `localhost:9200` 사용 | `http://elasticsearch:9200`으로 변경 후 Spring 재생성 |
| alias는 있지만 문서 수 0건 | 전체 재색인 미실행 또는 실패 | 재색인 API 실행 후 `_count` 확인 |
| 로그인 401 | 사용자 미존재 또는 비밀번호 불일치 | 회원가입 성공과 사용자 존재 여부 확인 |
| CAPTCHA에 `string` 사용 | Swagger 예시 문자열을 실제 토큰으로 오인 | 비활성화된 개발 환경에서는 `null` 사용 |
| BE HTTP 500인데 로그에 스택 없음 | 전역 예외 처리기가 예외를 응답으로만 변환 | 전역 예외 로깅 추가 필요 |
| E2E가 HTTP 200이지만 일정은 FAILED | 테스트가 종단 도달만 검증 | 최종 상태, 슬롯, Validator 실행까지 검증 |

## 11. Gradle 테스트 실행 관련 확인 사항

수동 동기화 컨트롤러 추가 후 `compileJava`, `compileTestJava`는 성공했다. 현재 환경에서 `test` 실행 시 신규 테스트뿐 아니라 기존 테스트 전체가 로딩 단계에서 `ClassNotFoundException`으로 중단되는 현상이 확인됐다.

이는 assertion 실패가 아니라 Gradle 테스트 런타임의 공통 클래스패스/실행 환경 문제다. Gradle 캐시, 테스트 워커 클래스패스, 한글과 공백이 포함된 작업 경로, Gradle/Spring Boot 조합을 별도로 점검해야 한다.

## 12. 전체 체크리스트

```text
[ ] TOUR_API_KEY 설정
[ ] Docker 내부 ELASTICSEARCH_URL=http://elasticsearch:9200 설정
[ ] SEARCH_ENGINE=elasticsearch 설정
[ ] 필요 시 SEARCH_INDEXER_ENABLED=true 설정
[ ] Spring 컨테이너 재생성 또는 코드 변경 시 재빌드
[ ] BE health UP 확인
[ ] themes 기준 데이터 확인
[ ] 관광지·음식점·숙소 목록 동기화
[ ] 필요한 상세정보 동기화
[ ] Elasticsearch 전체 재색인
[ ] gangwon-places 문서 수 확인
[ ] 도메인별 내부 검색 API 확인
[ ] AI E2E 최종 성공 상태와 필수 슬롯 검증
```
