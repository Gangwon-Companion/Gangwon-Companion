# RAG Grounding 및 최종 답변 환각 방지 구현 계획

작성일: 2026-09-07

## 결정 사항

Hard Validator와 Quality Validator는 유지한다. RAG grounding은 이를 대체하지
않고, 검증된 일정 데이터를 LLM이 자연어로 설명하는 마지막 단계의 안전장치로
추가한다.

## 목표

LLM 최종 답변에 다음 정보가 근거 없이 포함되지 않도록 한다.

- 검색 결과에 없는 장소
- 검증된 일정에 없는 시간과 일정 변경
- 근거 없는 숫자·가격·거리
- 근거 없는 운영시간·반려동물·접근성·주차 정책
- 원본 데이터에 없는 추천 이유

## 목표 파이프라인

```text
검색 후보/evidence
  -> 일정 구성
  -> Hard Validator
  -> Quality Validator
  -> 구조화된 응답 데이터
  -> LLM 자연어 렌더링
  -> Final Answer Grounding Guard
  -> 통과 답변 또는 deterministic fallback
```

## 구현 원칙

1. LLM은 장소·사실·시간을 새로 만들지 않고 검증된 입력만 설명한다.
2. 장소 ID와 구조화된 일정 데이터가 사실의 source of truth다.
3. LLM 응답은 publish 전에 검증한다.
4. 검증 실패 또는 LLM 장애 시 deterministic fallback을 반환한다.
5. 근거가 없는 값은 추정하지 않고 삭제하거나 확인 필요 문구로 표시한다.
6. Hard Validator 실패 결과는 계속 PENDING/FAILED로 처리한다.

## Final Answer Grounding Guard 검사 항목

- 답변의 장소 ID가 검증된 itinerary에 존재하는가
- 답변의 숫자·시간·주소가 구조화된 응답 또는 evidence에 존재하는가
- 정책 주장이 null/미확인 필드에 대해 생성되지 않았는가
- LLM이 장소·일정·시간·추천 이유를 새로 추가하지 않았는가
- 검증되지 않은 값이 안전한 notice로 표시되었는가

## 개발 순서

- [x] 구현 계획 추가
- [x] Gangwon-AI에 최종 답변 grounding 검사 계약 추가
- [x] 최종 답변 검증기와 deterministic fallback 연결
- [x] 장소·숫자·시간 adversarial test 추가
- [ ] 정책 claim 및 추천 이유의 구조화된 claim 검증 추가 (운영 이슈 발생 시 후속)

## 과적합 방지 원칙

- 장소 도메인별 ID 목록이나 특정 문장 표현을 코드에 하드코딩하지 않는다.
- 새로운 도메인 ID와 새로운 자연어 표현을 포함한 테스트를 추가한다.
- 테스트는 정상 케이스, 근거 누락, 잘못된 값, 새로운 도메인, LLM 장애를 분리한다.
- 정규식 검사는 1차 방어선으로만 사용하고, 최종적으로는 구조화된 claim/evidence
  계약을 source of truth로 삼는다.
- 오프라인 테스트 통과만으로 안전성을 주장하지 않고 실제 E2E 응답에서 fallback과
  READY/FAILED 상태를 함께 확인한다.
- [ ] 기존 Hard Validator 및 E2E 회귀 테스트 실행
- [ ] 회귀 통과 후 기본 활성화

## 완료 기준

- 근거 없는 장소 ID가 사용자 답변에 포함되지 않는다.
- 근거 없는 숫자·시간·정책 주장은 제거되거나 fallback 처리된다.
- LLM 장애가 있어도 deterministic 답변이 있으면 안전하게 응답한다.
- Hard Validator의 INVALID 결과가 READY로 승격되지 않는다.
- 정상 답변과 악의적/오류 LLM 답변 모두 자동 테스트된다.
