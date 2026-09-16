# AI 여행 취향 프로필 — FE 구현 명세

## 현재 구현 상태

**마이페이지 여행 취향 카드 MVP 구현 완료**

- 인증 기반 저장 프로필 조회
- 분석 Job 생성과 1.5초 간격 Polling
- 미분석·분석 중·완료·데이터 부족·실패 상태 표시
- 완료 프로필의 제목·설명·태그·근거 표시
- 재분석과 완료 결과 즉시 갱신
- TypeScript 타입 검사 통과
- Expo 실제 빌드·브라우저/기기 UI 검증과 컴포넌트 테스트는 남아 있음

## 목표

마이페이지에서 사용자의 AI 여행 취향 프로필을 조회하고, 분석 전·완료·데이터 부족·오류 상태에 맞는 UI를 제공한다.

FE는 BE 공개 API만 호출한다. AI 서버를 직접 호출하거나 AI 내부 요청·응답 타입을 정의하지 않는다.

## MVP 화면 범위

- 마이페이지 여행 취향 요약 카드
- 최초 분석 버튼
- 분석 Job 생성과 Polling 기반 로딩 상태
- 완료된 취향 제목·설명·태그 표시
- 분석 근거 표시 영역 또는 모달
- 다시 분석하기 버튼
- 데이터 부족 안내 및 탐색 CTA
- API 오류와 재시도 처리

사용자 취향 직접 수정과 취향 변화 이력 UI는 MVP에서 제외한다.

## BE API

### 저장된 프로필 조회

```http
GET /api/v1/users/me/travel-profile
Authorization: Bearer {token}
```

### 분석 또는 재분석 Job 생성

```http
POST /api/v1/users/me/travel-profile/analysis-jobs
Authorization: Bearer {token}
```

- 요청 본문 없음
- 성공 시 `202 Accepted`와 `{ jobId, status: 'PENDING' }` 반환

### 분석 Job 상태 조회

```http
GET /api/v1/users/me/travel-profile/analysis-jobs/{jobId}
Authorization: Bearer {token}
```

- `PENDING` 또는 `RUNNING`이면 Polling 유지
- `COMPLETED`이면 Polling을 중단하고 `profile` 반영
- `FAILED`이면 Polling을 중단하고 오류 UI 표시
- 저장된 프로필 GET은 결과가 없어도 `404`가 아니라 `NOT_ANALYZED` 반환

## 응답 타입

```ts
type TravelProfileStatus =
  | 'NOT_ANALYZED'
  | 'COMPLETED'
  | 'INSUFFICIENT_DATA';

type TravelerType =
  | 'NATURE_HEALING'
  | 'PET_COMPANION'
  | 'LOCAL_FOOD_EXPLORER'
  | 'ACTIVITY_ADVENTURE'
  | 'CULTURE_EXPLORER'
  | 'BALANCED_TRAVELER';

interface TravelProfile {
  status: TravelProfileStatus;
  travelerType: TravelerType | null;
  title: string | null;
  description: string | null;
  tags: string[];
  evidences: string[];
  confidence: number | null;
  analyzedAt: string | null;
}

type TravelProfileJobStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED';

interface TravelProfileAnalysisJobSubmitted {
  jobId: string;
  status: 'PENDING';
}

interface TravelProfileAnalysisJob {
  jobId: string;
  status: TravelProfileJobStatus;
  profile: TravelProfile | null;
  errorCode: string | null;
  message: string | null;
  createdAt: string;
  updatedAt: string;
}
```

분석 완료 응답 예시:

```json
{
  "status": "COMPLETED",
  "travelerType": "NATURE_HEALING",
  "title": "한적한 자연을 즐기는 힐링 여행자",
  "description": "최근 자연 관광지와 산책하기 좋은 장소를 자주 선택했어요.",
  "tags": ["자연", "한적한 곳", "산책"],
  "evidences": [
    "최근 방문한 장소 중 자연 관광지의 비중이 높아요.",
    "저장한 코스에 산책하기 좋은 장소가 자주 포함됐어요."
  ],
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

데이터 부족 응답:

```json
{
  "status": "INSUFFICIENT_DATA",
  "travelerType": null,
  "title": null,
  "description": "검색하거나 장소를 방문하면 여행 취향을 분석할 수 있어요.",
  "tags": [],
  "evidences": [],
  "confidence": null,
  "analyzedAt": "2026-09-13T14:30:00+09:00"
}
```

## 상태별 UI

### 최초 로딩

- 카드 영역에 스켈레톤 표시
- 이전 사용자 데이터가 잠시 보이지 않도록 사용자별 캐시 키 사용
- 인증 오류는 기존 전역 인증 처리 적용

### `NOT_ANALYZED`

- 제목: `내 여행 취향을 알아볼까요?`
- 안내: `검색과 방문 기록을 바탕으로 AI가 여행 취향을 분석해 드려요.`
- 버튼: `내 여행 취향 분석하기`

### 분석 요청 중

- 버튼 비활성화로 중복 요청 방지
- 문구: `여행 취향을 분석하고 있어요.`
- 기존 결과가 있으면 제거하지 않고 재분석 중 표시만 추가
- POST 응답의 `jobId`로 1~2초 간격 Polling
- `PENDING`, `RUNNING` 동안만 Polling 유지
- `COMPLETED`, `FAILED` 수신 즉시 Polling 중단
- 앱이 백그라운드 상태일 때 Polling을 일시 중지하고 복귀 시 재개
- 컴포넌트가 언마운트되면 Polling 타이머를 정리
- 작업 생성 후 5분이 지나면 Polling을 중단하고 재확인 안내 제공

### Job 완료

- `COMPLETED`이며 `profile.status === 'COMPLETED'`이면 새 프로필로 화면과 캐시 갱신
- `COMPLETED`이며 `profile.status === 'INSUFFICIENT_DATA'`이면 데이터 부족 UI 표시
- Job의 `COMPLETED`와 프로필의 `COMPLETED`는 서로 다른 상태이므로 타입과 분기를 분리
- 완료 후 저장 프로필 GET Query도 invalidate하여 서버 상태와 동기화

### Job 실패

- `FAILED`이면 서버가 준 사용자용 `message` 또는 공통 실패 문구 표시
- 기존 프로필이 있으면 그대로 유지
- 실패한 Job을 자동으로 다시 생성하지 않고 사용자의 재시도 동작을 기다림

### `COMPLETED`

요약 카드:

- 서버의 `title`을 주 제목으로 표시
- `description` 표시
- `tags`를 서버가 준 순서대로 최대 5개 표시
- 첫 번째 `evidence`만 표시하거나 `자세히 보기`로 연결
- `analyzedAt` 표시
- `다시 분석하기` 버튼 제공

상세 영역 또는 모달:

- 모든 `evidences` 표시
- 최근 활동을 기반으로 분석했다는 안내 표시
- `confidence`는 MVP에서 사용자에게 숫자로 직접 노출하지 않는 것을 권장

FE가 `travelerType`을 자체 문구로 변환하지 않는다. 사용자 노출 문구는 서버의 `title`과 `description`을 사용하고, 유형 코드는 분석 이벤트 또는 아이콘 선택 정도에만 사용한다.

### `INSUFFICIENT_DATA`

- 제목: `아직 분석할 여행 활동이 부족해요.`
- 보조 문구는 서버의 `description` 사용
- 검색, 관광지 탐색 또는 코스 생성 화면으로 이동하는 CTA 제공
- 재분석 버튼을 계속 노출할 수 있으나 빠른 반복 클릭은 방지

### 오류

- 기존 성공 결과가 있으면 해당 결과를 유지하고 재분석 실패 토스트 표시
- 저장 결과가 없는 초기 조회 실패라면 카드 내부에 재시도 UI 표시
- AI 서버, 내부 예외, 스택 트레이스 등 기술적인 오류 내용을 사용자에게 노출하지 않음
- 네트워크 오류와 서버 오류 모두 재시도 가능 상태로 처리

## 데이터 처리 규칙

- `tags`와 `evidences`가 빈 배열이어도 오류로 처리하지 않는다.
- nullable 필드는 상태에 맞게 안전하게 처리한다.
- `analyzedAt`은 사용자 로컬 시간 형식으로 표시한다.
- 서버 응답 순서를 유지하고 FE에서 태그를 임의 재정렬하지 않는다.
- Job 조회에서 완료 프로필을 받으면 저장 프로필 GET 캐시를 갱신하거나 해당 결과로 즉시 교체한다.
- 새 분석 Job ID를 메모리 또는 화면 상태에 보관하고, 서버가 동일 진행 Job을 반환하면 기존 Polling에 연결한다.
- 로그아웃 시 여행 프로필 관련 사용자 캐시를 제거한다.

## 접근성과 반응형

- 태그는 작은 화면에서 자연스럽게 줄바꿈한다.
- 긴 제목과 설명이 카드 밖으로 넘치지 않게 처리한다.
- 로딩 상태를 색상만으로 표현하지 않는다.
- 버튼에 명확한 접근성 이름을 제공한다.
- 모달을 사용한다면 포커스 이동과 ESC/뒤로 가기 동작을 지원한다.

## 분석 이벤트 권장안

서비스가 기존 분석 도구를 사용한다면 다음 이벤트를 남길 수 있다.

- `travel_profile_viewed`
- `travel_profile_analysis_requested`
- `travel_profile_analysis_completed`
- `travel_profile_analysis_failed`
- `travel_profile_insufficient_data_shown`

이벤트에 제목·설명·검색어 같은 사용자 콘텐츠는 넣지 않고 `travelerType`과 상태 코드만 사용한다.

## 구현 체크리스트

- [x] API Client와 응답 타입 정의
- [x] 사용자별 조회 상태 관리 구현
- [x] 마이페이지 여행 취향 카드 구현
- [x] `NOT_ANALYZED` UI 구현
- [x] Job 생성과 Polling 구현
- [x] 분석 중 UI, 중복 요청 방지, Polling 정리 구현
- [x] `COMPLETED` UI 구현
- [x] 근거 상세 UI 구현
- [x] `INSUFFICIENT_DATA` 안내 구현
- [x] 오류 표시와 재분석 구현
- [x] 완료 후 화면 상태 갱신 구현
- [x] TypeScript 타입 검사
- [ ] 데이터 부족 상태의 탐색 CTA
- [ ] Expo 브라우저·기기 반응형 및 접근성 검증
- [ ] 컴포넌트·API 테스트 작성

## 테스트 조건

- 세 가지 서버 상태별 렌더링
- 분석 요청 중 중복 클릭 방지
- PENDING/RUNNING Polling 유지 및 완료/실패 시 중단
- 분석 성공 후 프로필 결과와 캐시 갱신
- 5분 Polling 제한과 앱 백그라운드 처리
- 기존 결과가 있을 때 재분석 실패 처리
- 빈 태그·빈 근거 배열 처리
- nullable 필드 안전 처리
- 긴 제목·설명과 태그 줄바꿈
- 로그아웃 후 사용자 캐시 제거

## 완료 조건

- 사용자가 마이페이지에서 현재 분석 상태를 확인할 수 있다.
- 최초 분석과 재분석 Job을 중복 생성 없이 실행하고 완료까지 Polling할 수 있다.
- 완료된 프로필의 제목·설명·태그·근거가 올바르게 표시된다.
- 데이터 부족이 오류가 아닌 안내 UI로 표현된다.
- 재분석 실패 시 기존 결과가 유지된다.
- FE-BE API 계약 테스트가 통과한다.
