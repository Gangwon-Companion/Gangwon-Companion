# AI 코스 추천 비동기 Polling

기존 `POST /api/v1/courses/recommendations` 동기 API는 유지하고, 오래 걸릴 수 있는 AI 추천에는 별도 비동기 API를 제공한다.

## 흐름

```text
Frontend
  -> POST /api/v1/courses/recommendations/jobs
  <- 202 { jobId, status: PENDING }

Frontend
  -> GET /api/v1/courses/recommendations/jobs/{jobId}
  <- PENDING 또는 RUNNING

Backend worker
  -> AI server
  -> PostgreSQL course_recommendation_jobs 업데이트

Frontend
  -> GET /api/v1/courses/recommendations/jobs/{jobId}
  <- COMPLETED + result 또는 FAILED + errorCode
```

작업은 요청한 사용자에게 귀속되며 다른 사용자의 `jobId`는 `404`로 처리한다. 결과는 PostgreSQL에 저장되므로 서버 재시작이나 여러 백엔드 인스턴스에서도 조회할 수 있다.

## API 계약

### 작업 생성

```http
POST /api/v1/courses/recommendations/jobs
Authorization: Bearer <token>
Content-Type: application/json
```

응답: `202 Accepted`

```json
{
  "jobId": "2c7d7c1c-4ea6-4fc9-8db1-f9b5da2e1bc3",
  "status": "PENDING"
}
```

### 상태 조회

```http
GET /api/v1/courses/recommendations/jobs/{jobId}
Authorization: Bearer <token>
```

처리 중:

```json
{
  "jobId": "2c7d7c1c-4ea6-4fc9-8db1-f9b5da2e1bc3",
  "status": "RUNNING",
  "result": null,
  "errorCode": null,
  "message": null
}
```

완료:

```json
{
  "jobId": "2c7d7c1c-4ea6-4fc9-8db1-f9b5da2e1bc3",
  "status": "COMPLETED",
  "result": { "status": "completed" },
  "errorCode": null,
  "message": null
}
```

프론트엔드는 `COMPLETED` 또는 `FAILED`를 받으면 Polling을 중단한다. 권장 조회 간격은 1~2초이며, 작업 생성 후 5분이 지나면 프론트에서 타임아웃 UX를 제공한다.

완료·실패 작업은 기본 24시간 보관 후 스케줄러가 삭제한다. 환경변수 `COURSE_RECOMMENDATION_JOB_RETENTION`과 `COURSE_RECOMMENDATION_JOB_CLEANUP_INTERVAL`로 조정할 수 있다.
