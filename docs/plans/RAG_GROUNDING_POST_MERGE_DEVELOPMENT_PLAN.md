# 머지 기준 RAG 기반 LLM 문구 생성 개발계획

작성일: 2026-09-11
상태: 머지 후 재설계 및 구현 계획

## 1. 작성 목적

`feature/RAG`와 `origin/main`의 머지 이후에는 현재 머지된 코드를 기준으로 RAG 기반 LLM 문구 생성을 다시 구현한다.

기존 작업에서 검증한 검색 근거와 환각 방지 정책을 그대로 복원하는 것이 아니라, 머지 후 유지된 검색 계약을 새로운 기준으로 삼는다. 검색 결과에 포함된 장소 정보와 `evidence`, `missing_fields`, `status`, `diagnostics`만을 LLM 입력의 근거로 사용하고, 생성된 문구가 실제 근거를 벗어나지 않는지 별도로 검증한다.

## 2. 머지 후 기준선

현재 백엔드에는 다음 기능이 기준선으로 남아 있다.

- `PlaceSearchResponse`가 장소 후보와 검색 진단 정보를 반환한다.
- 장소 후보는 `status`, `missing_fields`, `evidence`를 가진다.
- 근거가 부족한 후보는 `INSUFFICIENT_EVIDENCE` 상태로 구분된다.
- `Diagnostics`는 검색 결과 부족, 근거 부족, 운영시간 부족 및 추천 가능한 후속 행동을 제공한다.
- RDB 검색과 Elasticsearch 검색이 동일한 장소 검색 응답 계약을 사용한다.
- `AiTravelClient`는 AI 서버의 `/internal/travel/plan`을 호출한다.
- `CourseRecommendationService`는 AI 서버의 여행 코스 추천 결과를 그대로 전달한다.

현재 기준선에는 최종 LLM 문구에 대한 claim 단위의 구조화된 검증 계층이 아직 없으므로, AI 저장소의 현재 구현을 기반으로 이 부분을 단계적으로 강화한다.

## 2-1. Gangwon-AI 머지 후 확인 결과

`Gangwon-AI`는 `origin/main`을 fast-forward한 뒤, 머지 전에 작업하던 RAG 변경사항을 다음과 같이 다시 반영한 상태다.

| 영역 | 현재 반영 상태 | 비고 |
| --- | --- | --- |
| LLM 문구 생성 | 반영 | `response_llm.py`가 검증된 일정 데이터를 입력으로 자연어 문구 생성 |
| LLM 실패 fallback | 반영 | 호출 실패·빈 응답 시 deterministic answer 사용 |
| 최종 답변 grounding | 1차 반영 | 장소 ID·시간·숫자 기반 보수적 검증 후 실패 시 fallback |
| 숙소 grounding | 반영 | main에서 추가된 `accommodations`를 LLM 입력과 검증 대상에 포함 |
| 검색 계약 | main 기준 반영 | `place_subtype`, 지역 정보, 정책 필드, `diagnostics`를 AI 모델이 수용 |
| BE grounding 계약 검증 | 1차 반영 | 검색 응답의 `status`, `missing_fields`, `evidence.field/source` 일관성을 AI 전달 전에 검증 |
| 구조화 claim 출력 | 1차 반영 | JSON 응답을 선택적으로 파싱하고 자연어 응답과 하위 호환 |
| source/evidence 필드 직접 검증 | 반영 | `source_ids`, 장소 ID, 시간·숫자 및 claim의 evidence field/value 검증 |

따라서 다음 구현은 AI의 기존 문구 생성과 fallback을 폐기하지 않고, 현재 머지된 검색·일정 계약 위에 구조화된 claim 검증을 추가하는 방식으로 진행한다.

## 3. 목표

LLM이 검색 결과에 없는 사실을 추가하지 않고, 검증 가능한 장소 근거를 바탕으로 여행 설명과 추천 문구를 생성하도록 한다.

검증 대상은 다음과 같다.

- 검색 결과에 없는 장소명이나 장소 ID를 추가하지 않는가
- 검색 결과에 없는 주소, 운영시간, 거리, 가격, 수치를 생성하지 않는가
- `pet_allowed`, `max_pet_size`, `wheelchair_accessible` 등 정책 정보를 임의로 추정하지 않는가
- `INSUFFICIENT_EVIDENCE` 후보를 확정 추천처럼 표현하지 않는가
- 추천 이유가 `matched_preferences` 또는 `evidence`로 설명 가능한가
- 운영시간이 추정값인지 실제 값인지 구분하는가
- 근거가 부족할 때 확인 필요 문구 또는 안전한 대체 응답을 반환하는가

## 4. 목표 아키텍처

```text
사용자 여행 요청
      ↓
CourseRecommendationService
      ↓
AI Server /internal/travel/plan
      ↓
Search Tool /internal/search/places
      ↓
PlaceSearchResponse
(results + evidence + missing_fields + diagnostics)
      ↓
RAG Context Builder
      ↓
LLM 여행 설명·추천 문구 생성
      ↓
Structured Claim Parser
      ↓
Grounding Validator
      ├─ 통과: 검증된 문구 반환
      ├─ 일부 실패: 실패 문구 제거 후 반환
      └─ 전체 실패: deterministic fallback 반환
```

검색은 사실의 원천이고, LLM은 검색 결과를 사용자에게 자연스럽게 설명하는 역할만 담당한다.

## 5. 구현 범위

### 5.1 검색 응답을 RAG 컨텍스트로 변환

AI 서버에서 `PlaceSearchResponse`를 그대로 프롬프트에 넣지 않고, LLM 입력용 구조로 변환한다.

```json
{
  "place_id": "DESTINATION:123",
  "name": "장소명",
  "address": "강원특별자치도 ...",
  "status": "OK",
  "facts": {
    "region_code": "SOKCHO",
    "pet_allowed": true,
    "wheelchair_accessible": false
  },
  "evidence": [
    {
      "field": "pet_allowed",
      "value": true,
      "source": "TOUR_API"
    }
  ],
  "missing_fields": []
}
```

규칙:

- `status=OK`와 `status=INSUFFICIENT_EVIDENCE`를 구분해 전달한다.
- `missing_fields`에 포함된 값은 사실값으로 만들지 않는다.
- `evidence`가 없는 필드는 LLM 컨텍스트에서 추정 금지 대상으로 표시한다.
- 검색 점수와 매칭 키워드는 추천 우선순위 참고용으로만 사용하고 사실값으로 표현하지 않는다.
- 장소 ID는 모든 생성 claim과 연결할 수 있도록 필수로 포함한다.

### 5.2 LLM 출력 계약

현재 AI는 호환성을 위해 자연어 `answer`를 반환하고 있다. 다음 단계에서는 기존 문자열 응답을 즉시 제거하지 않고, 내부적으로 구조화된 claim 결과를 먼저 받도록 확장한 뒤 최종 API에는 기존 `answer`를 유지한다.

```json
{
  "summary": "속초 해변 인근에서 반려동물 동반이 확인된 장소를 중심으로 구성했습니다.",
  "claims": [
    {
      "text": "이 장소는 반려동물 동반이 가능합니다.",
      "place_id": "DESTINATION:123",
      "evidence_fields": ["pet_allowed"],
      "confidence": "SUPPORTED"
    }
  ],
  "uncertainties": [],
  "fallback_used": false
}
```

LLM 프롬프트에는 다음 지침을 고정한다.

1. 제공된 장소와 근거만 사용한다.
2. 근거가 없는 숫자, 시간, 주소, 정책, 가격을 만들지 않는다.
3. `INSUFFICIENT_EVIDENCE` 후보는 확인 필요 또는 참고 후보로만 표현한다.
4. 검색 결과에 없는 추천 이유를 생성하지 않는다.
5. 확신할 수 없는 정보는 `uncertainties`에 기록한다.
6. JSON 계약을 지키지 못하면 정상 답변으로 간주하지 않는다.

초기 호환 단계에서는 JSON 응답을 받지 못하면 기존 자연어 응답을 사용하되, 현재 `answer_grounding.py`의 검증을 통과한 경우에만 반환한다. 구조화 응답 계약이 안정화되면 자연어만 반환한 응답은 fallback 처리한다.

### 5.3 Grounding Validator

LLM 응답을 사용자에게 전달하기 전에 claim 단위로 검증한다.

검증 순서:

1. JSON 스키마 검증
2. `place_id`가 검색 결과에 존재하는지 확인
3. `evidence_fields`가 해당 장소의 evidence에 존재하는지 확인
4. claim 안의 숫자·시간·주소·정책 값이 evidence 값과 일치하는지 확인
5. `INSUFFICIENT_EVIDENCE` 후보가 확정 표현으로 사용됐는지 확인
6. 검증 실패 claim 제거 또는 확인 필요 문구로 변환
7. 전체 claim이 실패하면 deterministic fallback 반환

초기 구현은 문자열 규칙과 구조화된 값 비교를 사용한다. 현재는 구조화 claim의 evidence 값까지 비교하며, 자연어 전체의 의미적 함의 판정은 별도 groundedness 모델 없이 범위를 제한한다.

### 5.4 Deterministic Fallback

LLM 장애, JSON 파싱 실패, 근거 불일치, 타임아웃이 발생해도 안전한 응답을 반환한다.

fallback은 다음 정보만 사용한다.

- 검색 결과의 장소명
- 주소
- `matched_preferences`
- 검증된 `evidence`
- `status`
- `missing_fields`

예시:

```text
검색 조건에 맞는 장소를 찾았습니다. 반려동물 동반 여부는 관광 데이터에서 확인되었습니다.
다만 운영시간 정보가 부족하므로 방문 전 운영 여부를 확인해 주세요.
```

## 6. API 및 모듈 변경 계획

### Backend

| 대상 | 변경 내용 |
| --- | --- |
| `AiTravelClient` | 검색 근거가 포함된 여행 추천 요청 및 구조화된 LLM 응답 처리 |
| `CourseRecommendationService` | AI 응답 검증 결과와 fallback 상태를 서비스 응답에 반영 |
| `CourseResponse` | 생성 문구, claim, uncertainty, fallback 여부의 응답 계약 정의 |
| 검색 응답 DTO | 현재 `evidence`, `missing_fields`, `diagnostics` 계약 유지 |
| 공통 예외 처리 | AI 응답 파싱 실패·타임아웃·검증 실패를 안전한 응답으로 변환 |

### AI Server

| 대상 | 변경 내용 |
| --- | --- |
| Search Tool | 현재 `/internal/search/places` 계약 사용 |
| RAG Context Builder | 검색 응답을 장소별 facts/evidence 구조로 변환 |
| Prompt Builder | 근거 사용 범위와 금지 규칙을 시스템 프롬프트로 고정 |
| LLM Response Parser | JSON 스키마와 claim 구조 검증 |
| Grounding Validator | claim과 evidence의 일치 여부 검증 |
| Fallback Renderer | 검증된 근거만으로 안전한 문구 생성 |

## 7. 단계별 개발계획

| 단계 | 작업 | 완료 기준 |
| --- | --- | --- |
| 1 | 머지된 검색 응답 계약 고정 | `PlaceSearchResponse` fixtures와 API 계약 확정 |
| 2 | RAG 컨텍스트 스키마 정의 | 장소별 facts/evidence/missing_fields 및 숙소 accommodations 구조 확정 |
| 3 | AI 서버 검색 결과 변환 | 머지된 BE 검색 응답을 AI 검색 모델로 검증하고 LLM 입력 컨텍스트 생성 |
| 4 | 기존 문구 생성 안정화 | `days`와 `accommodations`를 모두 프롬프트에 포함하고 실패 시 fallback |
| 5 | 1차 grounding 검증 | 장소 ID·근거에 포함된 시간·숫자 검증 및 숙소 검증 |
| 6 | 구조화된 LLM 출력 추가 | summary/claims/uncertainties JSON 응답을 선택적으로 지원 |
| 7 | claim grounding 검증 | 장소 ID·근거 필드·값을 claim 단위로 일치 검증 |
| 8 | adversarial 테스트 | 환각 유도 입력과 근거 부족 입력 차단 |
| 9 | 실제 데이터 E2E | Spring → AI → Search Tool → LLM → Validator 전체 검증 |
| 10 | 운영 지표 추가 | 검증 실패율, fallback율, latency, 토큰 비용 기록 |

## 8. 테스트 계획

### 단위 테스트

- 검색 결과를 RAG 컨텍스트로 변환하는 테스트
- `missing_fields`가 있는 후보를 추정하지 않는 테스트
- claim의 `place_id` 검증 테스트
- evidence 필드 불일치 테스트
- 숫자·시간·주소 불일치 테스트
- `INSUFFICIENT_EVIDENCE` 확정 표현 차단 테스트
- LLM JSON 파싱 실패 및 fallback 테스트

### 통합 테스트

- 실제 `/internal/search/places` 결과를 AI 컨텍스트로 변환
- RDB 검색과 Elasticsearch 검색 결과의 동일 계약 검증
- AI 서버 타임아웃 및 5xx 대응
- 후보가 없는 검색 요청 대응
- 반려동물·무장애·운영시간 조건별 evidence 검증

### E2E 평가셋

최소 다음 유형을 포함한다.

| 유형 | 검증 내용 |
| --- | --- |
| 정상 추천 | 검색 결과와 evidence에 근거한 문구 생성 |
| 장소 환각 | 검색 결과에 없는 장소명 생성 차단 |
| 정책 환각 | 반려동물·무장애 정보를 근거 없이 확정하는 문장 차단 |
| 운영시간 환각 | 근거 없는 운영시간과 24시간 표현 차단 |
| 수치 환각 | 거리·가격·시간 등 숫자 불일치 차단 |
| 근거 부족 | `INSUFFICIENT_EVIDENCE`와 확인 필요 문구 검증 |
| LLM 장애 | timeout·빈 응답·잘못된 JSON fallback 검증 |

## 9. 평가 지표

| 지표 | 목표 |
| --- | --- |
| Unsupported claim rate | 0%에 가깝게 유지 |
| Evidence coverage | 생성 claim의 근거 연결 비율 100% |
| Place hallucination rate | 0% |
| Policy hallucination rate | 0% |
| Fallback success rate | LLM 실패 시 안전한 응답 반환 100% |
| Search-to-answer latency | 검색·생성·검증 단계별 p50/p95 측정 |
| Token cost | 요청 1건당 입력·출력 토큰과 비용 기록 |

품질 평가는 LLM의 문장 자연스러움만으로 판단하지 않는다. 근거 없는 문장을 만들지 않는지와 실패 상황에서 안전하게 제한하는지를 우선 평가한다.

## 10. 완료 기준

- 머지된 검색 계약을 변경하지 않고 RAG 컨텍스트를 생성한다.
- 모든 생성 claim이 장소 ID와 evidence 필드에 연결된다.
- 근거 없는 장소·수치·시간·정책 정보가 사용자 응답에 노출되지 않는다.
- `INSUFFICIENT_EVIDENCE` 후보가 확정 추천으로 표현되지 않는다.
- LLM 장애 시 deterministic fallback이 반환된다.
- 정상·환각·근거 부족·장애 E2E 테스트가 자동화된다.
- grounding 실패율과 fallback율을 운영 로그와 메트릭으로 확인할 수 있다.

## 11. 기존 문서와의 관계

- `RAG_GROUNDING_DEVELOPMENT_PLAN.md`: 머지 전 검색 및 RAG 설계 배경
- `RAG_GROUNDING_IMPLEMENTATION_PLAN.md`: 기존 최종 답변 grounding 정책
- 본 문서: 머지 후 현재 백엔드 계약을 기준으로 다시 구현하기 위한 실행계획
- `RAG_GROUNDING_REAL_E2E_20260908.md`: 기존 실제 데이터 검증 결과 및 회귀 테스트 참고자료

## 12. 현재 구현에서 확인된 후속 수정사항

- `source_ids`를 LLM 입력에 포함해 문구의 출처 범위를 명시한다.
- main에서 추가된 `accommodations`도 일정 장소와 동일한 grounding 검증 대상에 포함한다.
- 숫자·시간 검증만으로는 임의의 장소명·주소·정책 설명을 완전히 차단할 수 없으므로, 구조화 claim의 `place_id`, `evidence_fields`, evidence 값 검증을 추가했다. 자연어 전체의 의미적 함의 판정은 오탐·비용을 고려해 현재 범위에서 제외한다.
- `INSUFFICIENT_EVIDENCE` 후보는 claim 생성 대상에서 제외하거나 반드시 확인 필요 문구로 표시한다.
- AI 검색 모델의 `extra=forbid` 계약과 BE 응답 필드가 계속 일치하는지 검색 계약 fixture로 회귀 검증한다.
- BE 내부 검색 응답은 `X-Grounding-Contract: v1`과 `X-Grounding-Status` 헤더로 계약 버전과 근거 상태를 관측할 수 있게 한다.

## 13. 실제 E2E 검증 결과

- AI live integration test 6건 통과: 검색 계약, 정상 일정, 후보 부족 및 정책 조건 실패 케이스를 BE와 연결해 검증했다.
- AI 전체 회귀 테스트 112건 통과했다.
- BE JUnit 전체 테스트가 `BUILD SUCCESSFUL`로 완료됐다.
- K6 RAG 검색 부하 테스트: 488 requests, p95 12.60ms, 오류율 0%.
- K6 혼합 검색 부하 테스트: 256 requests, p95 16.33ms, 오류율 0%.
- LLM이 생성한 claim이 현재 검색 결과와 맞지 않는 경우에는 로그를 남기고 deterministic fallback으로 전환되는 것을 확인했다.
