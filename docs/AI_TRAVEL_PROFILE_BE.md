# AI 여행 취향 프로필 — BE 구현 명세

## 현재 구현 상태

**BE MVP와 여행 추천 개인화 연결 구현 완료**

- 활동 수집, AI 호출, 사용자별 결과 upsert 구현
- 분석 Job 생성·Polling·중복 실행 방지·완료 Job 정리 구현
- 최근 7일 이내이며 현재 분석 버전과 일치하는 `COMPLETED` 프로필만 추천에 사용
- FE 공개 요청과 BE→AI 내부 추천 DTO를 분리해 클라이언트의 프로필 위조 방지
- 동기·비동기 코스추천 모두 저장 프로필을 선택적 `travel_profile`로 전달
- 프로필 분석과 코스추천 AI Client 모두 `X-Internal-API-Key` 전송
- 메인·테스트 소스 컴파일 완료. 실행 환경을 사용한 최종 E2E는 남아 있음

## 목표

사용자의 검색·방문·저장 코스·리뷰 데이터를 수집해 AI 서버에 분석을 요청하고, 검증된 여행 취향 프로필을 저장하여 FE에 제공한다.

BE는 전체 기능의 오케스트레이터이자 분석 결과의 최종 저장소다. AI 서버는 BE DB에 직접 접근하지 않고, FE는 AI 서버를 직접 호출하지 않는다.

## MVP 범위

- 저장된 여행 취향 프로필 조회
- 활동 데이터 수집 및 AI 요청 형식으로 정규화
- 사용자가 요청하는 비동기 분석·재분석 Job
- AI 응답 검증 및 DB upsert
- 미분석·분석 완료·데이터 부족 상태 제공
- AI 장애 시 기존 결과 보존

다음 기능은 MVP에서 제외한다.

- 사용자 취향 직접 수정
- 활동 발생 시 자동 재분석
- 정기 분석 배치
- 분석 이력과 취향 변화 조회
- AI 코스 생성 요청에 취향 자동 적용

## 권장 패키지 구조

```text
domain/travelprofile/
├── client/
├── controller/
├── dto/
├── entity/
├── repository/
└── service/
```

주요 컴포넌트:

- `TravelProfileController`: FE 공개 API
- `TravelProfileService`: 조회, 분석, 결과 저장 트랜잭션
- `TravelProfileDataCollector`: 여러 도메인의 활동 데이터 수집 및 정규화
- `AiTravelProfileClient`: AI 내부 API 호출
- `TravelProfile`: 최신 분석 결과 엔티티
- `TravelProfileRepository`: 사용자별 결과 조회 및 저장
- `TravelProfileAnalysisJob`: 비동기 분석 작업 엔티티
- `TravelProfileAnalysisJobService`: 작업 생성, 백그라운드 실행, 상태 조회
- `TravelProfileAnalysisJobRepository`: 작업 저장 및 만료 작업 삭제

기존 `AiTravelClient`의 `RestClient`, 타임아웃, 예외 변환 방식을 참고한다. 신규 Client는 `JsonNode` 대신 명시적인 요청·응답 DTO를 사용한다.

`CourseRecommendationJob`, `CourseRecommendationJobService`, `AsyncTaskConfiguration`에 추가된 Polling 구조를 기준으로 구현한다. 프로필 분석은 코스 추천과 실행 시간·부하가 다를 수 있으므로 별도의 `travelProfileAnalysisExecutor`를 사용한다.

## 분석 입력 데이터

| 데이터 | 현재 원천 | AI 전달 정보 |
|---|---|---|
| 검색 기록 | `SearchHistory` | 검색어, 지역, 검색 시각 |
| 방문 기록 | `VisitRecord` | 장소 유형·ID, 방문 시각, 조회 가능한 장소 메타데이터 |
| 저장 코스 | `SavedCourse`, `CoursePlace` | 코스명, 장소 유형·ID·이름·주소, 저장 시각 |
| 리뷰 | 관광지·음식점·숙박 리뷰 | 장소 유형·ID·이름, 평점, 작성 시각 |

수집 기준:

- 최근 90일 데이터를 우선 사용한다.
- 종류별 최대 50건으로 제한한다.
- 데이터가 부족하면 90일 이전 최신 데이터까지 포함할 수 있다.
- 리뷰 본문은 개인정보와 불필요한 자유 텍스트가 포함될 수 있어 MVP에서 전달하지 않는다.
- 이름, 이메일, 닉네임 등 분석에 필요 없는 사용자 개인정보를 AI 서버에 전달하지 않는다.

## DB 모델

테이블명: `travel_profiles`

| 컬럼 | 타입 예시 | 설명 |
|---|---|---|
| `id` | BIGINT PK | 식별자 |
| `user_id` | BIGINT UNIQUE FK | 사용자당 최신 프로필 1개 |
| `status` | VARCHAR(30) | 분석 상태 |
| `traveler_type` | VARCHAR(50), nullable | 여행자 유형 코드 |
| `title` | VARCHAR(100), nullable | 화면 표시 제목 |
| `description` | VARCHAR(500), nullable | 화면 표시 설명 |
| `tags` | JSON/TEXT, nullable | 태그 배열, 최대 5개 |
| `evidences` | JSON/TEXT, nullable | 분석 근거 배열, 최대 3개 |
| `confidence` | DECIMAL(4,3), nullable | 0 이상 1 이하 |
| `analysis_version` | VARCHAR(50), nullable | AI 분석 버전 |
| `analyzed_at` | TIMESTAMP, nullable | 분석 완료 시각 |
| `created_at` | TIMESTAMP | 생성 시각 |
| `updated_at` | TIMESTAMP | 수정 시각 |

사용자 활동 원문이나 AI 요청 전체는 중복 저장하지 않는다. 분석 이력이 필요해지면 별도 이력 테이블을 추가한다.

### 분석 Job 모델

테이블명: `travel_profile_analysis_jobs`

| 컬럼 | 타입 예시 | 설명 |
|---|---|---|
| `id` | UUID PK | FE Polling용 작업 ID |
| `username` | VARCHAR(20) | 작업 소유 사용자 |
| `status` | VARCHAR(20) | `PENDING`, `RUNNING`, `COMPLETED`, `FAILED` |
| `error_code` | VARCHAR(100), nullable | 실패 시 공개 가능한 오류 코드 |
| `error_message` | VARCHAR(500), nullable | 실패 시 사용자용 오류 메시지 |
| `created_at` | TIMESTAMP | 작업 생성 시각 |
| `updated_at` | TIMESTAMP | 마지막 상태 변경 시각 |

- Job에는 활동 데이터나 AI 전체 응답을 저장하지 않는다.
- 완료 결과는 `travel_profiles`에 저장하고 Job 조회 시 해당 프로필을 반환한다.
- 다른 사용자의 `jobId` 조회는 리소스 존재 여부가 드러나지 않도록 `404`로 처리한다.
- 완료·실패 Job은 기본 24시간 보관 후 스케줄러로 삭제한다.

## FE 공개 API

기존 마이페이지 경로인 `/api/v1/users/me` 하위에 둔다.

### 저장된 프로필 조회

```http
GET /api/v1/users/me/travel-profile
Authorization: Bearer {token}
```

저장 결과가 없어도 `404` 대신 `200 OK`와 `NOT_ANALYZED`를 반환한다.

분석 완료 응답:

```json
{
  "status": "COMPLETED",
  "travelerType": "NATURE_HEALING",
  "title": "한적한 자연을 즐기는 힐링 여행자",
  "description": "최근 자연 관광지와 산책하기 좋은 장소를 자주 선택했어요.",
  "tags": ["자연", "한적한 곳", "산책"],
  "evidences": ["최근 방문한 장소 중 자연 관광지의 비중이 높아요."],
  "confidence": 0.86,
  "analyzedAt": "2026-09-13T14:30:00+09:00"
}
```

미분석 응답:

```json
{
  "status": "NOT_ANALYZED",
  "travelerType": null,
  "title": null,
  "description": null,
  "tags": [],
  "evidences": [],
  "confidence": null,
  "analyzedAt": null
}
```

### 분석 또는 재분석 Job 생성

```http
POST /api/v1/users/me/travel-profile/analysis-jobs
Authorization: Bearer {token}
```

- 요청 본문 없음
- 성공 시 `202 Accepted`와 Job ID 반환
- 같은 사용자의 동시 분석 요청 방지
- 이미 `PENDING` 또는 `RUNNING`인 작업이 있으면 새 Job 대신 진행 중인 Job을 반환
- 최근 완료 결과가 1분 이내라면 새 분석 생성 대신 정책에 따라 기존 결과를 안내하거나 `429` 처리

```json
{
  "jobId": "2c7d7c1c-4ea6-4fc9-8db1-f9b5da2e1bc3",
  "status": "PENDING"
}
```

### 분석 Job 상태 조회

```http
GET /api/v1/users/me/travel-profile/analysis-jobs/{jobId}
Authorization: Bearer {token}
```

처리 중 응답:

```json
{
  "jobId": "2c7d7c1c-4ea6-4fc9-8db1-f9b5da2e1bc3",
  "status": "RUNNING",
  "profile": null,
  "errorCode": null,
  "message": null,
  "createdAt": "2026-09-13T14:30:00Z",
  "updatedAt": "2026-09-13T14:30:01Z"
}
```

완료 응답은 `status: COMPLETED`와 `profile`을 함께 반환한다. AI가 데이터 부족을 판단한 경우에도 Job 자체는 정상 완료이므로 Job 상태는 `COMPLETED`이고, `profile.status`가 `INSUFFICIENT_DATA`가 된다.

데이터 부족 응답:

```json
{
  "jobId": "2c7d7c1c-4ea6-4fc9-8db1-f9b5da2e1bc3",
  "status": "COMPLETED",
  "profile": {
    "status": "INSUFFICIENT_DATA",
    "travelerType": null,
    "title": null,
    "description": "검색하거나 장소를 방문하면 여행 취향을 분석할 수 있어요.",
    "tags": [],
    "evidences": [],
    "confidence": null,
    "analyzedAt": "2026-09-13T14:30:00+09:00"
  },
  "errorCode": null,
  "message": null,
  "createdAt": "2026-09-13T14:30:00Z",
  "updatedAt": "2026-09-13T14:30:03Z"
}
```

실패 응답은 `status: FAILED`, `profile: null`, 공개 가능한 `errorCode`와 `message`를 반환한다. 실패한 분석으로 기존 `travel_profiles` 결과를 변경하지 않는다.

## BE → AI 내부 API

```http
POST /internal/travel/profile/analyze
Content-Type: application/json
```

배포 환경에서는 내부 인증 헤더를 추가하는 것을 권장한다.

BE 설정:

```env
AI_SERVER_URL=http://localhost:8000
AI_INTERNAL_API_KEY=local-dev-key
```

`AI_INTERNAL_API_KEY`가 비어 있지 않으면 BE는 모든 프로필 분석 요청에 다음 헤더를 전송한다.

```http
X-Internal-API-Key: {AI_INTERNAL_API_KEY}
```

AI 서버에도 동일한 로컬 키를 설정해야 한다. 키가 비어 있으면 BE는 헤더를 전송하지 않으므로, 이 경우 AI 서버도 내부 인증 검사를 비활성화해야 한다.

요청은 `snake_case`를 사용한다.

```json
{
  "schema_version": "1.0",
  "reference_time": "2026-09-13T14:30:00+09:00",
  "searches": [
    {
      "keyword": "강아지 산책",
      "region": "정선",
      "searched_at": "2026-09-10T10:20:00+09:00"
    }
  ],
  "visits": [
    {
      "place_type": "ATTRACTION",
      "place_id": 101,
      "name": "정선 아우라지",
      "category": "NATURE",
      "region": "정선",
      "visited_at": "2026-09-01T12:00:00+09:00"
    }
  ],
  "saved_courses": [],
  "reviews": []
}
```

정상 AI 응답:

```json
{
  "status": "COMPLETED",
  "traveler_type": "NATURE_HEALING",
  "title": "한적한 자연을 즐기는 힐링 여행자",
  "description": "최근 자연 관광지와 산책하기 좋은 장소를 자주 선택했어요.",
  "tags": ["자연", "한적한 곳", "산책"],
  "evidences": [
    "최근 방문한 장소 중 자연 관광지의 비중이 높아요."
  ],
  "confidence": 0.86,
  "analysis_version": "travel-profile-llm-v1"
}
```

데이터 부족 AI 응답:

```json
{
  "status": "INSUFFICIENT_DATA",
  "traveler_type": null,
  "title": null,
  "description": null,
  "tags": [],
  "evidences": [],
  "confidence": null,
  "analysis_version": "travel-profile-llm-v1"
}
```

배열 값이 없으면 `null` 대신 빈 배열을 전송한다.

## 공통 Enum

분석 상태:

- `NOT_ANALYZED`: 아직 저장된 결과 없음. BE 공개 API에서만 사용
- `COMPLETED`: 분석 완료
- `INSUFFICIENT_DATA`: 분석 데이터 부족

Job 상태는 프로필 상태와 별도로 다음 값을 사용한다.

- `PENDING`
- `RUNNING`
- `COMPLETED`
- `FAILED`

여행자 유형:

- `NATURE_HEALING`
- `PET_COMPANION`
- `LOCAL_FOOD_EXPLORER`
- `ACTIVITY_ADVENTURE`
- `CULTURE_EXPLORER`
- `BALANCED_TRAVELER`

## AI 응답 검증

- `title`: 1~100자
- `description`: 1~500자
- `tags`: 최대 5개, 항목당 1~30자, 중복 제거
- `evidences`: 최대 3개, 항목당 1~200자
- `confidence`: 0~1
- `traveler_type`: 공통 Enum 값만 허용
- 계약 위반 응답은 저장하지 않고 AI 잘못된 응답 오류로 변환

## 구현 체크리스트

- [x] `TravelProfile` 엔티티와 Repository 구현
- [x] 사용자별 최신 결과 조회 구현
- [x] 검색·방문·코스·리뷰 데이터 Collector 구현
- [x] AI 요청·응답 DTO 구현
- [x] `AiTravelProfileClient` 구현
- [x] AI 응답 검증 및 결과 upsert 구현
- [x] `TravelProfileAnalysisJob`과 Repository 구현
- [x] 전용 Executor와 Job Service 구현
- [x] 프로필 GET, Job POST/GET 공개 API 구현
- [x] 동일 인스턴스 내 활성 분석 Job 중복 요청 방지
- [x] 완료·실패 Job 정리 스케줄러 구현
- [x] Springdoc 기반 API 노출
- [x] Client·Controller·프로필 유효성 단위 테스트 작성
- [x] 유효 프로필의 코스추천 내부 요청 주입
- [x] 코스추천 내부 API 인증 헤더 적용
- [ ] 다중 인스턴스 중복 실행 방지용 DB 제약 또는 분산 락
- [ ] 실제 DB·AI·인증 설정을 사용한 통합 테스트

## 테스트 조건

- 저장 결과 조회 및 미분석 응답
- 인증되지 않은 요청 `401`
- 데이터 수집 기간과 최대 건수
- AI 요청 필드 매핑
- 정상 결과 upsert
- 데이터 부족 결과 처리
- AI 타임아웃, 4xx, 5xx, 잘못된 응답 처리
- AI 장애 시 기존 성공 결과 보존
- 동일 사용자의 동시 요청 제어
- 다른 사용자의 Job 조회 차단
- PENDING → RUNNING → COMPLETED/FAILED 상태 전이
- 만료 Job 정리

## 완료 조건

- 최초 사용자는 `NOT_ANALYZED`를 조회할 수 있다.
- 분석 요청 시 `202`와 소유 사용자에게 귀속된 Job ID가 반환된다.
- 데이터가 충분한 사용자의 분석 결과가 백그라운드에서 DB에 저장된다.
- 재조회 시 AI를 호출하지 않고 저장 결과를 반환한다.
- 재분석 성공 시 기존 행이 최신 결과로 갱신된다.
- 데이터 부족은 실패 Job이 아니라 완료 Job의 `INSUFFICIENT_DATA` 프로필로 제공된다.
- AI 장애가 기존 프로필에 영향을 주지 않는다.
- 개인정보가 AI 요청에 포함되지 않는다.
