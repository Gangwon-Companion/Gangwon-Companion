# 검색·AI 통합 후속 작업

현재까지 구현한 수동 데이터 동기화 API, Elasticsearch 검색, CDC 증분 색인 및 로컬 통합 테스트 환경을 기준으로 남은 작업을 정리한다.

실행 및 장애 대응 절차는 [로컬 데이터 동기화 및 검색 통합 테스트 문제해결](LOCAL_DATA_SYNC_AND_SEARCH_TROUBLESHOOTING.md)을 참고한다.

## 1. 최우선: AI evidence 계약 확정

현재 AI는 장소 운영시간을 검색 응답의 `evidence`에서 찾지만 BE 검색 문서와 응답에는 운영시간 필드가 없다.

AI가 요구하는 필드:

```text
opens_at
closes_at
pet_allowed
max_pet_size
indoor_pet_allowed
wheelchair_accessible
```

현재 BE 상태:

| 필드 | 상태 |
|---|---|
| `opens_at`, `closes_at` | DB 일부 엔티티에 원문은 있으나 검색 문서·응답에는 없음 |
| `pet_allowed` | 지원 |
| `max_pet_size` | BE는 현재 `pet_size` 이름 사용 |
| `indoor_pet_allowed` | 구조화 필드 없음 |
| `wheelchair_accessible` | 지원 |

해야 할 일:

- AI와 BE가 사용할 evidence 필드명과 타입 확정
- `PlaceSearchRequest`에 `required_evidence` 추가 여부 결정
- `PlaceSearchDocument`에 `opensAt`, `closesAt` 등 필요한 구조화 필드 추가
- Elasticsearch mapping에 신규 필드 추가
- RDB 검색과 Elasticsearch 검색에서 동일한 evidence 생성
- 요청된 필수 근거가 없을 때만 `INSUFFICIENT_EVIDENCE`와 `missing_fields` 반환
- 운영시간 원문을 파싱할 수 없는 경우 `operating_hours_raw` 보존
- 검색 문서 버전 증가 및 전체 재색인

완료 기준:

```json
{
  "status": "OK",
  "missing_fields": [],
  "evidence": [
    {"field": "opens_at", "value": "09:00", "source": "TOUR_API"},
    {"field": "closes_at", "value": "22:00", "source": "TOUR_API"}
  ]
}
```

## 2. 운영시간 정규화 정책

관광공사 원문은 반드시 `HH:mm~HH:mm` 형태가 아니다.

예시:

```text
09:00~18:00
상시 이용
일출부터 일몰까지
매장별 상이
```

정책 결정 항목:

- 파싱 가능한 시간만 `opens_at`, `closes_at`으로 저장
- 파싱 불가능한 값은 원문 필드에 보존
- 24시간 운영 표현 방식 확정
- 자정을 넘기는 영업시간 표현 방식 확정
- 요일별 운영시간이 다른 경우 데이터 구조 확정
- 음식점, 관광지, 숙소 체크인·체크아웃을 같은 의미로 취급하지 않도록 분리

## 3. 기준 테마 자동 초기화

현재 빈 DB에서는 `themes` 기준 데이터가 없어 관광지 동기화가 `THEME_NOT_FOUND`로 실패한다.

해야 할 일:

- Flyway 또는 Liquibase 도입 여부 결정
- `NA`, `HS`, `EX`, `EV`, `LS`, `SH`, `VE` 기준 데이터 마이그레이션 작성
- 이미 데이터가 있어도 중복되지 않는 멱등 방식 적용
- 새 PostgreSQL 볼륨에서 자동 초기화 검증

완료 기준:

```text
빈 DB 실행
-> 별도 수동 SQL 없이 themes 생성
-> 관광지 동기화 성공
```

## 4. Elasticsearch 초기 적재 자동화

현재 `SEARCH_INDEXER_ENABLED=true`는 신규 CDC 이벤트를 처리하지만 초기 전체 데이터 적재는 재색인 API에 의존한다.

해야 할 일:

- 신규 환경에서 전체 재색인이 필요한 시점을 정의
- 배포 절차에 재색인 단계를 포함하거나 안전한 부트스트랩 작업 구현
- 재색인 중 검색 가능한 기존 alias 유지
- bulk 실패 시 alias를 전환하지 않도록 검증
- 재색인 결과에 도메인별 적재 건수 제공
- 재색인 완료 후 refresh 및 문서 수 검증

## 5. 수동 동기화 API 개선

현재 추가된 API:

```http
POST /api/v1/admin/restaurants/sync
POST /api/v1/admin/restaurants/details/sync
POST /api/v1/admin/lodgings/sync
POST /api/v1/admin/lodgings/details/sync
POST /api/v1/admin/activities/sync
```

해야 할 일:

- 음식점·숙소 목록 동기화 응답에 신규·갱신·건너뜀·실패 건수 제공
- 긴 외부 API 호출의 timeout과 중복 실행 방지
- 외부 API 호출 한도 초과 시 진행 상태와 중단 사유 제공
- 필요하면 비동기 작업 ID와 상태 조회 API 도입
- 수동 동기화와 새벽 2시 스케줄의 동시 실행 방지

## 6. 관리자 API 권한 강화

현재 `/api/v1/admin/**`는 로그인한 사용자라면 호출할 수 있고 별도의 관리자 역할 검사가 없다.

해야 할 일:

- 사용자 권한 모델에 `ADMIN` 역할 추가
- `/api/v1/admin/**`를 관리자에게만 허용
- 재색인 및 대량 동기화 API 호출 감사 로그 추가
- 운영 환경에서 호출 횟수 제한 또는 내부 네트워크 제한 검토

## 7. 예외 로깅과 관측 보완

현재 전역 예외 처리기는 HTTP 500 응답을 만들지만 원본 예외 스택을 로그로 남기지 않아 장애 원인을 찾기 어렵다.

해야 할 일:

- 예상하지 못한 예외에 request path와 stack trace 로깅
- 민감한 요청 본문, JWT, API 키는 로그에서 제외
- `ElasticsearchOperationException` 전용 처리와 원인 구분
- Elasticsearch 연결 실패, 검색 실패, 역직렬화 실패 메트릭 분리
- 재색인 성공·실패·소요시간 메트릭 추가

## 8. E2E 성공 조건 강화

현재 E2E는 HTTP 200과 종단 응답 도달만으로 통과할 수 있어 최종 일정이 `FAILED`여도 성공으로 판정될 수 있다.

추가할 검증:

```text
final_response.response_status == SUCCESS
일정 상태가 완성 상태
missing_slots가 비어 있음
D1_LUNCH, D1_DINNER 후보 존재
Hard Validator 실행 완료
Validation Agent 실행 완료
응답 장소가 BE 검색 결과에 존재
필수 evidence가 누락되지 않음
```

시나리오:

- 강릉 1일 일정: 관광지, 점심, 저녁 슬롯 완성
- 반려동물 동반 일정: 허용 여부와 크기 evidence 확인
- 휠체어 접근 일정: 접근성 evidence 확인
- 운영시간 부족 장소: `INSUFFICIENT_EVIDENCE` 처리
- 검색 결과 0건: 재시도 후 명확한 실패 응답
- Elasticsearch 장애: 임의 장소 생성 없이 실패 처리

## 9. Gradle 테스트 실행 환경 수정

현재 `compileJava`, `compileTestJava`는 성공하지만 Gradle `test` 실행 시 기존 테스트를 포함한 전체 테스트 클래스 로딩 단계에서 `ClassNotFoundException`이 발생한다.

확인할 항목:

- 테스트 runtime classpath 출력 및 클래스 파일 존재 확인
- Gradle wrapper와 Spring Boot 4 조합 확인
- Gradle daemon 및 캐시 초기화 후 재검증
- 한글과 공백이 없는 경로에서 재현 여부 확인
- CI 환경에서 동일 증상 여부 확인
- 특정 테스트만 실행해도 전체 테스트가 탐색되는 원인 확인

완료 기준:

```text
./gradlew test 성공
신규 수동 동기화 컨트롤러 테스트 성공
기존 회귀 테스트 성공
```

## 10. 문서 및 배포 체크리스트 정리

- `.env.example`의 필수·선택 환경변수 설명 보완
- 호스트와 Docker 내부 URL 차이 명시
- 초기 테마 데이터와 최초 재색인 절차 자동화 후 문서 갱신
- 관리자용 수동 동기화 API를 Swagger에서 구분
- RDB 검색과 Elasticsearch 검색 전환 절차 문서화
- 실제 AI 저장소의 실행 방법과 E2E 명령 연결

## 우선순위 요약

```text
P0  AI evidence 계약 확정 및 운영시간 전달
P0  테마 기준 데이터 자동 초기화
P0  성공 일정 기준의 AI E2E 검증
P1  Elasticsearch 초기 적재 자동화
P1  예외 로깅 및 검색 장애 관측
P1  Gradle 테스트 실행 문제 해결
P2  관리자 권한 강화
P2  수동 동기화 비동기화 및 진행 상태 제공
P2  문서·배포 절차 최종 정리
```
