# AI Agent Issue Log

기준일: 2026-09-03

이 문서는 프론트/Swagger에서 AI 여행 추천 플로우를 테스트하며 확인한 문제를 정리한다. 목적은 특정 검색어 하나를 고치는 것이 아니라, AI Agent가 사용자가 준 정보만으로 최대한 자연스럽고 안정적으로 성공하도록 개선 지점을 모으는 것이다.

## 현재 관찰된 흐름

- 프론트의 AI 추천 화면에서 사용자가 자연어로 여행 조건을 입력한다.
- Spring API는 AI 서버 `/internal/travel/plan`으로 요청을 전달한다.
- AI 서버는 입력 파싱, 선호 추출, Supervisor 분기, 장소 후보 검색, 일정 구성, 검증을 수행한다.
- 최종 응답은 프론트에서 일정 카드 또는 실패 메시지로 표시된다.

## 이슈 1. 실패 메시지가 내부 검증 용어를 그대로 노출함

### 재현 상황

사용자가 지역/기간만 입력하거나 조건이 부족한 상태에서 추천을 시도했을 때 다음과 같은 메시지가 표시됐다.

```text
요청 조건을 확인할 근거가 부족해 안전한 여행 일정을 확정하지 못했습니다.
- 필수 근거 데이터가 없습니다: petAllowed
- 필수 근거 데이터가 없습니다: operatingHours, petAllowed

여행 일정을 완성하지 못했습니다. 검색 및 검증 결과를 확인해 주세요.
```

### 문제

- `petAllowed`, `operatingHours` 같은 내부 필드명이 사용자에게 그대로 노출된다.
- 사용자가 반려동물을 언급하지 않았는데 `petAllowed`가 실패 이유로 보이면 혼란스럽다.
- 실패 원인은 개발자에게는 유용하지만, 사용자에게는 다음에 무엇을 입력해야 하는지 알려주지 못한다.

### 기대 동작

- 내부 실패 사유는 state/debug 정보로 유지한다.
- 사용자 메시지는 자연어 질문 또는 조건 완화 제안으로 바꾼다.

예시:

```text
강릉 4일 여행으로 이해했어요.
반려동물 동반 여부를 알려주시면 추천 기준을 더 정확히 잡을 수 있어요.
```

또는:

```text
영업시간이 확인된 음식점 후보가 부족합니다.
일부 식사 장소는 후보 추천 형태로 포함해도 괜찮을까요?
```

### 분류

- UX
- VALIDATOR
- RESPONSE

## 이슈 2. 후보가 있는데도 같은 장소를 과도하게 반복함

### 재현 상황

강릉 4일 여행 일정 생성 결과가 `READY/PASS`로 완료됐지만, 실제 일정에는 같은 장소가 반복 배치됐다.

### 확인된 응답 상태

```text
status=completed
input_complete=true
itinerary_status=READY
final_response.response_status=READY
hard_validation=VALID
quality_validation=PASS
destination_candidates=10
restaurant_candidates=32
lodging_candidates=15
itinerary=18
```

### 반복된 장소

```text
강릉항여객터미널       4회
커피씨엘             4회
송정해변막국수        4회
신라모노그램 강릉     3회
순두부젤라또 1호점    3회
```

### 문제

- 후보 수는 충분했지만 일정 생성기가 상위 후보 몇 개를 반복 사용했다.
- 4일 일정인데 관광지가 사실상 같은 곳으로 반복된다.
- 숙소는 같은 곳을 여러 박 사용하는 것이 자연스러울 수 있지만, 관광지와 식당 반복은 품질 저하가 크다.
- 그런데 Validator와 Quality Validator가 모두 통과 처리했다.

### 기대 동작

- `LODGING` 계열 숙소 반복은 허용 가능하다.
- `DESTINATION`은 기본적으로 중복 배치하지 않는다.
- `RESTAURANT`는 같은 날 중복 금지, 다일 반복도 제한한다.
- 후보가 부족할 때만 제한적으로 반복을 허용하고, 그 이유를 응답에 남긴다.

### 개선 방향

- Itinerary Agent에서 `used_place_ids`를 관리한다.
- 카테고리별 중복 정책을 둔다.
- Quality Validator가 일정 다양성을 검사한다.
- 후보 수가 충분한데도 반복이 심하면 `PASS`가 아니라 `REPAIR_REQUIRED` 또는 `FAIL`로 처리한다.

### 분류

- ITINERARY
- VALIDATOR
- QUALITY

## 이슈 3. 수정 요청을 부분 수정으로 처리하지 못함

### 재현 상황

기존 4일 일정이 있는 상태에서 사용자가 다음과 같이 입력했다.

```text
3일차에 해산물을 먹고 싶어
```

### 문제

- 기존 일정을 유지하면서 3일차 식사 슬롯만 바꾸는 것이 아니라, 전체 일정을 다시 만드는 형태로 보인다.
- `3일차`, `먹고 싶어`, 음식 선호 조건이 명확히 edit intent로 처리되는지 불분명하다.
- 수정 요청이 실제로 반영됐는지 Validator가 확인하지 않는다.
- 기존 일정의 중복 문제도 그대로 유지된다.

### 기대 동작

1. 기존 itinerary를 state/context로 받는다.
2. 사용자 발화를 edit intent로 분류한다.
3. `day=3`, `category=RESTAURANT`, `preference=해산물` 같은 수정 의도를 추출한다.
4. `D3_LUNCH` 또는 `D3_DINNER`를 변경 대상 슬롯으로 좁힌다.
5. Restaurant Agent만 필요한 범위에서 재검색한다.
6. 기존 일정은 최대한 유지하고 대상 슬롯만 교체한다.
7. 전체 일정을 다시 검증한다.
8. 최종 응답에 변경된 슬롯을 표시한다.

### 분류

- EDIT_INTENT
- STATE
- ITINERARY
- VALIDATOR

## 이슈 4. 후보 부족 시 재시도 전략이 단순함

### 재현 상황

일부 조건에서 Restaurant 후보가 0건으로 반환됐고, 재시도 후에도 같은 유형의 실패가 반복됐다.

### 문제

- 재시도할 때 어떤 조건을 완화했는지 명확하지 않다.
- 같은 조건으로 Agent를 다시 실행하면 결과가 바뀌지 않는다.
- 실패 원인이 `NO_TEXT_MATCH`, `NO_REGION_MATCH`, `EVIDENCE_MISSING`, `NOT_ENOUGH_UNIQUE_CANDIDATES` 중 무엇인지 구조화되어 있지 않다.

### 기대 동작

Search Agent는 후보 부족 시 단계적으로 조건을 완화해야 한다.

```text
1차: region + query_text + hard filters
2차: region + expanded query terms
3차: region + soft preference only
4차: nearby/coastal region 확장
5차: 필수 슬롯이면 query_text 제거 후 지역 인기순
```

각 시도는 state에 기록한다.

```json
{
  "agent": "restaurant",
  "failure_reason": "NO_TEXT_MATCH",
  "tried_relaxations": ["STRICT_TEXT", "RELAXED_TEXT"],
  "next_action": "DROP_QUERY_TEXT_KEEP_REGION"
}
```

### 분류

- SEARCH
- RETRY
- SUPERVISOR

## 이슈 5. 사용자 선호와 검색 조건의 역할이 섞여 있음

### 관찰

Input Parser는 사용자의 선호 키워드를 `preferences`와 `soft_preferences`로 추출한다. 예를 들어 `바다`는 `oceanView`, 음식 관련 키워드는 `food`로 매핑된다.

### 문제

- 선호 조건은 기본적으로 soft preference인데, 특정 도메인 SearchRequest의 `query_text`로 강하게 들어가면 후보를 0건으로 만들 수 있다.
- Spring Search Tool에서 `soft_preferences`는 점수화/랭킹에 쓰는 것이 자연스럽고, 결과 제거 조건으로 쓰면 안 된다.
- AI Agent는 선호 키워드를 도메인별 검색 전략으로 번역해야 한다.

### 기대 동작

예시:

```text
preferences=["바다", "음식"]

destination:
  query_text="바다"
  soft_preferences={"oceanView": 0.9}

lodging:
  query_text="바다"
  soft_preferences={"oceanView": 0.9}

restaurant:
  query_text는 비우거나 음식 도메인에 맞게 확장
  soft_preferences={"food": 0.9, "oceanView": 0.9}
```

### 분류

- PARSER
- SEARCH
- SUPERVISOR

## 이슈 6. 다일 일정 품질 기준이 부족함

### 문제

- 1일 일정 기준의 검증을 4일 일정에도 그대로 적용하는 느낌이 있다.
- 다일 일정은 슬롯 수, 후보 다양성, 숙소 연속성, 동선 권역, 식사 다양성을 별도로 평가해야 한다.

### 기대 동작

- `travel_days`에 따라 필요한 최소 unique 후보 수를 계산한다.
- 숙소는 동일 숙소 연박을 허용하되, 다른 카테고리와 분리한다.
- 일자별 권역이 지나치게 흔들리지 않도록 한다.
- 같은 식당이 여러 번 반복되면 감점 또는 repair 대상이 된다.

예시 기준:

```text
4일 일정:
- unique destination >= 3
- unique restaurant >= 식사 슬롯의 50~70%
- same destination repeated: 원칙적으로 불가
- same restaurant repeated: 최대 1~2회
- same lodging repeated: 허용 가능
```

### 분류

- QUALITY
- ITINERARY
- VALIDATOR

## 이슈 7. 병렬 후보 검색만으로는 동선 기반 식당 추천이 어려움

### 관찰

현재 실행 계획은 Destination, Restaurant, Lodging Agent를 병렬로 실행한 뒤 Itinerary Agent가 일정을 구성한다.

### 문제

- Restaurant Agent가 어느 관광지 근처에서 점심/저녁을 먹을지 모르는 상태에서 후보를 모은다.
- 다일 일정에서는 식당이 실제 일차별 동선과 맞지 않을 수 있다.

### 기대 동작

초기 후보 수집은 병렬로 하더라도, 일정 초안 이후 식사 후보는 동선 기반으로 보정한다.

```text
1. Destination 후보 수집
2. Lodging 후보 수집
3. 일자별 권역 또는 대표 좌표 결정
4. Restaurant 후보를 각 일차/식사 슬롯 기준으로 재검색
5. Itinerary 구성 또는 repair
```

### 분류

- SUPERVISOR
- SEARCH
- ITINERARY

## 이슈 8. `pet_allowed=false` 의미는 확인되었지만 용도 분리가 필요함

### 확인된 내용

Input Parser에서 사용자가 `반려동물은 안 데려가`라고 입력하면 `pet_allowed=false`로 추출된다. 이는 자연어 파싱상 정상이다.

Spring Search Tool 코드 기준으로도 `pet_allowed=true`일 때만 반려동물 조건을 강하게 적용하고, `false/null`은 필터로 적용하지 않는 구조로 보인다.

### 남은 문제

- `pet_allowed=false`는 사용자 상태값으로는 필요하지만, 사용자-facing 실패 이유로 나오면 어색하다.
- Validator에서도 사용자가 반려동물을 데려가지 않는다면 pet evidence 부족을 실패 이유로 삼지 않아야 한다.

### 기대 동작

```text
pet_allowed=true
→ 반려동물 동반 가능한 장소 근거 필요

pet_allowed=false
→ 반려동물 조건은 검색/검증에서 제외

pet_allowed=null
→ 필요하면 추가 질문, 아니면 기본 미동반으로 진행할지 정책 결정
```

### 분류

- PARSER
- VALIDATOR
- UX

## 이슈 9. 요청하지 않은 정책 필드 notice가 과도하게 생성됨

### 재현 상황

다음 3턴 흐름에서 속초 3일 일정이 `READY/PASS`로 생성됐다.

```text
1. 안녕
2. 속초 생각중이고 기간은 3일정도?
3. 반려동물 없어.
```

### 확인된 응답 상태

```text
status=completed
input_complete=true
itinerary_status=READY
final_response.response_status=READY
hard_validation=VALID
quality_validation=PASS
destination_candidates=15
restaurant_candidates=24
lodging_candidates=10
itinerary=13
notices=40
```

### 문제

- 사용자는 반려동물을 데려가지 않는다고 답했는데, 최종 응답에 `pet_allowed`, `indoor_pet_allowed`, `max_pet_size` 미확인 notice가 다수 생성됐다.
- 사용자는 휠체어/무장애 조건을 요청하지 않았는데, `wheelchair_accessible` 미확인 notice도 모든 장소 수준으로 생성됐다.
- 사용자가 신경 쓰지 않는 정책 필드까지 경고로 노출되어 일정이 불안정하거나 위험해 보인다.

### 기대 동작

- 사용자가 명시적으로 요청한 정책 조건만 필수 검증과 notice 대상으로 삼는다.
- `pet_allowed=false`이면 pet 관련 evidence 부족 notice는 생성하지 않는다.
- `wheelchair_accessible=null`이면 무장애 관련 notice를 기본 노출하지 않는다.
- 내부 디버깅용 `unverified_fields`와 사용자-facing `notices`를 분리한다.

예시:

```text
pet_allowed=false
→ pet_allowed, indoor_pet_allowed, max_pet_size notice 제외

wheelchair_accessible=null
→ wheelchair_accessible notice 제외

wheelchair_accessible=true
→ 무장애 접근성 근거가 없으면 notice 또는 실패 사유로 표시
```

### 분류

- VALIDATOR
- RESPONSE
- UX

## 이슈 10. 무장애 여부는 묻지 않지만 검증/notice에는 등장함

### 관찰

초기 필수 질문은 다음 세 가지에 집중되어 있다.

```text
어느 지역으로 여행하시나요?
여행 기간이 며칠인가요?
반려동물과 함께 가시는지 알려주세요.
```

반면 최종 응답과 notice에는 `wheelchair_accessible` 미확인 정보가 계속 등장한다.

### 문제

- 무장애 접근성을 필수 검증 또는 사용자-facing notice 대상으로 삼을 거라면 사전에 질문해야 한다.
- 사전에 묻지 않을 거라면 기본값은 `조건 없음`으로 처리하고 notice에서도 숨겨야 한다.
- 현재는 질문 정책과 검증/응답 정책이 서로 맞지 않는다.

### 기대 동작

정책을 둘 중 하나로 명확히 정해야 한다.

```text
옵션 A: 무장애를 선택 질문으로 둔다.
- "휠체어 접근성이나 무장애 동선이 필요하신가요?"를 필요 시 질문한다.
- true일 때만 Search/Validator에서 강하게 다룬다.

옵션 B: 무장애를 기본 필수 조건에서 제외한다.
- 사용자가 언급하지 않으면 wheelchair_accessible=null을 조건 없음으로 처리한다.
- 사용자-facing notice에도 노출하지 않는다.
```

### 분류

- QUESTION
- VALIDATOR
- UX

## 이슈 11. 최종 응답의 시간 표시가 내부 itinerary와 다름

### 재현 상황

속초 3일 일정 응답에서 내부 `itinerary`에는 `start_time`, `end_time`이 존재했다.

예시:

```text
D1_DESTINATION 10:00-12:00
D1_LUNCH       12:30-13:30
D1_DINNER      18:00-19:30
```

하지만 `final_response.days[].visits[].time`에는 다음처럼 표시됐다.

```text
시간 미확인-시간 미확인
```

### 문제

- 내부 일정에는 시간이 있는데 최종 응답 변환 과정에서 시간이 누락된다.
- 프론트는 최종 응답을 기준으로 표시하므로 사용자에게는 시간 없는 일정처럼 보일 수 있다.

### 기대 동작

- `itinerary.start_time/end_time`을 `final_response.days[].visits[].time`에 정상 매핑한다.
- 내부 시간 필드와 사용자-facing 시간 필드의 계약을 명확히 한다.

예시:

```text
start_time="12:30", end_time="13:30"
→ time="12:30-13:30"
```

### 분류

- RESPONSE_MAPPING
- FRONTEND
- UX

## 이슈 12. 오류 또는 실패 후 대화 복구 기능이 부족함

### 관찰

일정 생성 실패 또는 이상한 일정 생성 후 사용자가 다시 시도하려면 어떤 상태가 남아 있는지 알기 어렵다.

### 문제

- 실패한 대화 state가 남아 있으면 이후 입력이 수정 요청인지 새 요청인지 모호해질 수 있다.
- 사용자가 오류 상황에서 대화를 명확히 초기화할 방법이 부족하다.
- 새로고침, 대화 초기화, 새 추천 시작 같은 복구 UX가 필요하다.

### 기대 동작

- 프론트에 `대화 초기화` 또는 `새 추천 시작` 기능을 제공한다.
- 실패 응답에는 사용자가 이어서 고칠 수 있는 버튼/선택지를 제공한다.
- 새로고침 시 기존 AI 추천 state를 유지할지 버릴지 정책을 정한다.

예시:

```text
새 추천 시작
→ 현재 conversation state, itinerary, pending clarification을 초기화

다시 시도
→ 같은 request로 Search/Itinerary만 재실행

조건 수정
→ 기존 request를 유지하고 사용자의 수정 발화만 반영
```

### 분류

- FRONTEND
- STATE
- UX

## 이슈 13. 반려동물 동반 요청에서 근거 부족으로 반복 실패함

### 재현 상황

다음 대화 흐름에서 2차 응답까지는 `pet_allowed`만 누락된 상태로 정상 질문했다.

```text
1. 속초 생각중이고 2박3일 갈거야.
2. 반려동물 있어.
```

2차 입력 전 상태:

```json
{
  "request": {
    "region": "속초",
    "travel_days": 3,
    "nights": 2,
    "pet_allowed": null
  },
  "missing_fields": ["pet_allowed"],
  "clarification_questions": ["반려동물과 함께 가시는지 알려주세요."]
}
```

최종 실패 응답에서는 다음처럼 `pet_allowed=true`가 정상 반영됐다.

```json
{
  "request": {
    "region": "속초",
    "travel_days": 3,
    "nights": 2,
    "pet_allowed": true
  },
  "status": "failed"
}
```

### 문제

- 반려동물 동반 요청이므로 SearchRequest의 `hard_filters.pet_allowed=true`가 활성화되는 것은 맞다.
- 후보들은 대부분 `pet_allowed=null`이라 `INSUFFICIENT_EVIDENCE`가 되었고, Validator가 모든 슬롯에서 `petAllowed` 근거 부족을 필수 조건 위반으로 판단했다.
- 같은 후보 수집과 같은 검증 실패가 4회 반복되며 종료됐다.
- 반려동물 가능 근거가 부족한 지역/도메인에서는 조건을 완화할지, 대안 지역/장소를 제안할지, 부분 일정을 제안할지에 대한 복구 전략이 부족하다.

### 확인된 실패 패턴

```text
retry_count=4
itinerary_status는 생성됐지만 최종 status=failed
필수 조건 위반 13건 반복
retry_actions 대부분: 필수 근거 데이터가 없습니다: petAllowed
final_response.response_status=FAILED
```

### 기대 동작

```text
사용자: 반려동물 있어.
→ pet_allowed=true
→ 반려동물 동반 가능 근거가 있는 후보를 우선 검색
→ 근거 있는 후보가 부족하면 조건 완화/대안 제안/부분 추천 중 하나로 복구
→ 같은 조건으로 반복 재시도하지 않음
```

### 확인 필요

- 반려동물 동반 가능 근거가 있는 후보 수를 도메인별로 확인한다.
- `pet_allowed=true`일 때 모든 슬롯에 동일 강도의 pet evidence를 요구할지 정책을 정한다.
- 관광지, 음식점, 숙소 중 어떤 도메인은 근거 부족을 허용하고 notice로 낮출지 결정한다.
- 재시도 시 같은 후보를 반복 수집하지 않도록 조건 완화 또는 대안 제안을 넣는다.
- 실패 응답에서 `petAllowed` 같은 내부 필드명 대신 사용자가 이해할 수 있는 문구를 제공한다.

### 분류

- PARSER
- STATE
- VALIDATOR
- P0

## 우선순위 제안

### P0

- 반려동물 동반 요청에서 근거 부족으로 같은 재시도가 반복되는 문제를 해결한다.
- 기존 일정 수정 요청을 edit intent로 분류하고 부분 수정으로 처리한다.
- 후보가 충분한데도 같은 장소가 반복되는 문제를 막는다.
- Validator/Quality Validator에 중복 장소와 다양성 검사를 추가한다.
- 요청하지 않은 pet/accessibility notice를 사용자에게 노출하지 않는다.
- 내부 itinerary 시간과 final_response 시간 매핑을 맞춘다.

### P1

- 후보 부족 시 재시도 전략을 조건 완화 ladder로 바꾼다.
- 실패 원인과 재시도 이력을 state에 구조화한다.
- 사용자 메시지에서 내부 필드명을 제거하고 자연어 질문/제안으로 바꾼다.
- 무장애 조건을 질문할지, 기본 조건 없음으로 둘지 정책을 정한다.
- 오류/실패 후 대화 초기화 또는 새 추천 시작 기능을 제공한다.

### P2

- 식당 검색을 일차별 동선 기반으로 재검색/보정한다.
- 선호 키워드를 도메인별 검색 전략으로 번역하는 계층을 강화한다.
- 다일 일정 품질 지표를 별도로 설계한다.

## 테스트 시나리오 기록 템플릿

```text
시나리오:
사용자 입력:
기대 동작:
실제 동작:
응답 상태:
후보 수:
반복 장소:
실패/이상 메시지:
분류:
우선순위:
관련 원본 JSON:
관련 화면 캡처:
```

## 현재 결론

현재 AI Agent의 주요 문제는 단순한 자연어 파싱 실패가 아니다. 더 큰 문제는 다음과 같다.

1. 검색 실패 또는 후보 부족 시 조건을 바꿔가며 복구하는 전략이 약하다.
2. 일정 생성기가 후보를 다양하게 사용하는 제약이 부족하다.
3. Validator가 사용자 의도 반영 여부와 다일 일정 품질을 충분히 검증하지 못한다.
4. 사용자에게 묻지 않은 정책 필드가 검증/notice에 섞여 UX를 혼란스럽게 만든다.
5. 최종 응답 변환과 대화 state 복구 정책이 아직 안정적이지 않다.

따라서 다음 개선은 Input Parser보다 Supervisor, Search Agent retry, Itinerary Agent, Validator/Quality Validator 쪽에 집중하는 것이 좋다.

## 이슈 14. 요청 지역보다 식당 검색 지역이 너무 일찍 확장됨

### 재현 상황

1차 개선 후 프론트에서 다음 요청을 테스트했다.

```text
강릉으로 4일 여행 갈 거야. 바다, 카페, 해산물 위주로 가고 싶어. 반려동물은 안 데려가.
```

응답은 `completed`였고 일정 생성 자체는 성공했다.

하지만 `restaurant_search_request.region_codes`가 처음부터 다음처럼 동해안 전체로 생성됐다.

```json
[
  "GOSEONG",
  "SOKCHO",
  "YANGYANG",
  "GANGNEUNG",
  "DONGHAE",
  "SAMCHEOK"
]
```

### 문제

- 사용자가 `강릉`을 명확히 지정했는데 식당 검색이 첫 검색부터 동해안 전체로 확장된다.
- 그 결과 강릉 여행 일정에 고성, 속초, 양양, 동해 식당이 많이 섞인다.
- 후보 부족을 막기 위한 확장 정책은 필요하지만, 현재는 후보 부족 여부를 확인하기 전에 너무 빠르게 확장된다.
- 사용자 입장에서는 “강릉 여행을 요청했는데 왜 다른 지역까지 이동해야 하지?”라고 느낄 수 있다.

### 확인된 예시

```text
보배진 - 고성
카페 긷 - 속초
레드인블루커피 - 고성
베이커리카페 클램 - 동해
카페 로그 - 양양
앤커피스토리 - 속초
```

### 개선 방향

- 식당도 1차 검색은 사용자가 요청한 지역만 대상으로 한다.
- 후보 부족이 발생하거나 Search Tool diagnostics에서 `NO_REGION_MATCH`, `LOW_RESULT_COUNT`, `NOT_ENOUGH_UNIQUE_CANDIDATES`가 내려올 때 지역 확장을 검토한다.
- 지역 확장은 단계적으로 수행한다.

```text
1차: 요청 지역만 검색
2차: 요청 지역 + 가까운 인접 지역
3차: 동해안 권역 전체
```

- 사용자가 “강릉 안에서만”, “멀리 이동하기 싫어”처럼 지역 고정을 명시한 경우 자동 확장하지 않는다.
- 다른 지역 후보를 일정에 넣는 경우 `search_relaxations` 또는 최종 응답에 확장 이유를 남긴다.

예시:

```text
강릉 내 해산물 식당 후보가 부족해 인접 동해안 지역까지 검색 범위를 넓혔습니다.
```

### 분류

- SEARCH
- REGION_POLICY
- ITINERARY
- UX
- P1

## 이슈 15. 카페와 식사 장소를 구분하지 않아 식사 슬롯이 카페 위주로 채워짐

### 재현 상황

1차 개선 후 프론트에서 다음 요청을 테스트했다.

```text
강릉으로 4일 여행 갈 거야. 바다, 카페, 해산물 위주로 가고 싶어. 반려동물은 안 데려가.
```

응답은 `completed`였고 Quality Validator도 `PASS`였다.

하지만 실제 일정에서는 아침/점심/저녁 식사 슬롯에 카페 후보가 다수 배치됐다.

### 문제

- 현재 구조에서는 카페가 별도 도메인이나 슬롯으로 분리되어 있지 않다.
- 카페 후보도 `RESTAURANT` 후보로 취급되어 `BREAKFAST`, `LUNCH`, `DINNER` 슬롯에 들어갈 수 있다.
- 사용자는 “카페도 가고 싶다”는 의미로 말했지만, 결과는 “식사 대부분이 카페”인 일정이 될 수 있다.
- 해산물/맛집 같은 식사 의도보다 카페 선호가 과하게 반영될 수 있다.

### 확인된 예시

```text
카페 긷
커피씨엘
카페 뤼미에르
보사노바 커피로스터스 강릉점
카페기와
레드인블루커피
베이커리카페 클램
카페 로그
앤커피스토리
```

### 개선 방향

- 카페를 일반 식사 슬롯과 분리해서 다룬다.
- 가능한 정책 예시는 다음과 같다.

```text
옵션 A: CAFE 슬롯을 별도로 추가한다.
옵션 B: RESTAURANT 후보 중 카페는 식사 슬롯 배치 횟수를 제한한다.
옵션 C: BREAKFAST에는 일부 카페 허용, LUNCH/DINNER에는 일반 식당 우선 배치한다.
```

- 사용자 요청이 “카페 투어”, “카페 위주”처럼 명확할 때만 카페 비중을 높인다.
- `카페도 가고 싶어` 수준이면 하루 1개 이하 또는 전체 일정 중 일부 슬롯으로 제한한다.
- Search Tool은 후보의 세부 타입을 AI Agent가 구분할 수 있게 내려주는 것이 좋다.

예시 필드:

```text
place_subtype=CAFE
matched_keywords=["카페", "오션뷰"]
```

### 분류

- SEARCH
- ITINERARY
- SLOT_POLICY
- QUALITY
- P1

## 이슈 16. 세부 선호 키워드 근거가 부족해 Quality Validator가 반복 실패함

### 재현 상황

1차 개선 후 프론트에서 다음 요청을 테스트했다.

```text
속초로 2일 여행 갈래. 물회랑 대게 먹고 싶고 오션뷰 카페도 가고 싶어. 반려동물은 없어.
```

또 다른 후보 부족 테스트도 수행했다.

```text
속초로 3일 여행 갈 거야. 없는없는해산물키워드 맛집 가고 싶어. 반려동물은 안 데려가.
```

두 경우 모두 일정은 한 번 생성됐지만, Quality Validator에서 `PREFERENCE_UNDERREFLECTED`가 반복되어 최종 실패했다.

### 문제

- Input Parser는 `물회`, `대게`, `오션뷰`, `카페`, `해산물`, `맛집` 같은 키워드를 잘 추출한다.
- 하지만 Search Tool의 `matched_conditions`는 `food`, `oceanView`처럼 큰 범주로 내려오는 경우가 많다.
- AI Agent는 사용자의 세부 선호가 실제 후보에서 충족됐는지 판단하기 어렵다.
- 그 결과 일정이 88점 수준으로 만들어져도 “사용자 선호 일부만 반영”으로 판단하고 같은 검증 재시도를 반복한다.

### 확인된 실패 패턴

```text
itinerary_status=READY
quality_validation.status=REVISE
quality_validation.score=88
issue=PREFERENCE_UNDERREFLECTED
retry_count=4
final_response.response_status=FAILED
```

예시 reason:

```text
사용자 선호 4개 중 2개만 일정에 반영되었습니다.
사용자 선호 2개 중 1개만 일정에 반영되었습니다.
```

### 추가로 확인된 문제

`해산물`, `맛집`처럼 음식 계열 선호가 부족한데도 retry action이 `destination` Agent로 잡히는 경우가 있었다.

```json
{
  "agent": "destination",
  "instruction": "사용자 선호 2개 중 1개만 일정에 반영되었습니다."
}
```

음식 선호 미반영은 우선 `restaurant` Agent 재검색 또는 식당 후보 재평가로 연결되는 것이 자연스럽다.

### 개선 방향

Search Tool 개선:

- `matched_conditions`에 큰 범주만 넣지 말고 실제 매칭된 원본 키워드를 함께 내려준다.

```text
현재 예시:
matched_conditions=["food"]

개선 예시:
matched_conditions=["food", "대게", "해산물"]
matched_keywords=["대게"]
```

AI Agent 개선:

- `food`, `cafe`, `oceanView` 같은 상위 범주를 세부 선호의 부분 충족으로 인정한다.
- 장소명, matched_conditions, recommendation_reason, tags를 함께 보고 세부 선호 반영 여부를 판단한다.
- Quality score가 충분히 높고 itinerary가 READY인 경우 무한 재시도하지 않고, 부족한 선호를 안내하면서 일정을 반환하는 fallback 정책을 둔다.
- 선호 미반영 retry action은 키워드 도메인에 맞춰 라우팅한다.

```text
food/cafe 계열
→ restaurant Agent

nature/oceanView/관광 계열
→ destination Agent

숙소/호텔/펜션/오션뷰 숙소 계열
→ lodging Agent
```

### 분류

- SEARCH
- QUALITY
- VALIDATION_RETRY
- RESPONSE
- P0

## 이슈 17. 반려동물/무장애 필수 조건 evidence 부족으로 검증 재시도가 반복 실패함

### 재현 상황

1차 개선 후 반려동물 조건 테스트를 수행했다.

```text
강릉으로 2일 여행 갈 거야. 소형견이랑 같이 가고 싶고 바다 산책하고 싶어.
```

추가 답변:

```text
응, 소형견이랑 같이 가.
```

무장애 조건 테스트도 수행했다.

```text
강릉으로 1일 여행 갈 거야. 휠체어로 이동하기 편한 곳 위주로 바다 보고 싶어. 반려동물은 안 데려가.
```

### 문제

- AI Agent는 `pet_allowed=true`, `pet_size=SMALL`, `wheelchair_accessible=true`를 hard filter로 유지했다.
- 후보 부족이나 재검색 과정에서도 이 필수 조건을 자동 완화하지 않았다.
- 이 정책 자체는 정상이다.
- 하지만 Search Tool이 반환한 후보에 필수 조건을 증명할 evidence가 부족해서 Hard Validator가 모두 `EVIDENCE_MISSING`으로 막았다.
- 같은 검증 실패가 반복되다가 최대 재시도 횟수를 초과했다.

### 확인된 실패 패턴

반려동물 조건:

```text
필수 근거 데이터가 없습니다: petAllowed, maxPetSize
```

무장애 조건:

```text
필수 근거 데이터가 없습니다: wheelchairAccessible
```

공통 흐름:

```text
SearchRequest hard_filters에는 필수 조건이 유지됨
→ 후보는 반환됨
→ 후보 evidence에 필수 조건 근거가 부족함
→ Hard Validator INVALID
→ 같은 유형의 재검색/검증 반복
→ retry_count=4에서 failed
```

### 개선 방향

Search Tool 개선:

- hard filter를 만족한다고 판단해 후보를 반환했다면 해당 근거 evidence를 함께 내려준다.

```text
pet_allowed=true 요청
→ evidence.field=pet_allowed 필요

pet_size=SMALL 요청
→ evidence.field=max_pet_size 또는 pet_size 필요

wheelchair_accessible=true 요청
→ evidence.field=wheelchair_accessible 필요
```

- 근거가 부족한 후보는 `status=INSUFFICIENT_EVIDENCE`와 `missing_fields`를 명확히 내려준다.
- diagnostics에 `NO_POLICY_EVIDENCE`를 포함해 AI Agent가 “필수 조건 근거 부족”으로 인식할 수 있게 한다.

AI Agent 개선:

- 같은 `EVIDENCE_MISSING`이 반복되면 무한 재검색하지 않고 조기 종료하거나 사용자 선택지를 반환한다.
- 반려동물/무장애 조건은 자동 완화하지 않되, 사용자가 선택할 수 있는 대안을 제공한다.

예시:

```text
요청하신 조건을 확실히 확인할 수 있는 후보가 부족합니다.
1. 조건 근거가 확실한 장소만 다시 찾기
2. 근거가 부족한 후보도 방문 전 확인 안내와 함께 포함하기
3. 인접 지역까지 넓혀 찾기
```

### 추가 관찰

다음 문장에서 `pet_size=SMALL`은 추출됐지만 `pet_allowed=true`는 바로 추출되지 않았다.

```text
소형견이랑 같이 가고 싶어.
```

이 경우 불필요한 추가 질문이 발생했다.

### 개선 방향

- `소형견/중형견/대형견 + 같이 가다/데려가다/동반` 표현이 있으면 `pet_allowed=true`도 함께 추출한다.

예시:

```text
소형견이랑 같이 가고 싶어
→ pet_allowed=true
→ pet_size=SMALL
```

### 분류

- SEARCH
- VALIDATOR
- RETRY
- PARSER
- UX
- P0

## 2차 개선 후 3차 개선 대상 정리

### 배경

2차 개선에서는 Search Tool의 diagnostics와 세부 매칭 필드를 AI Agent가 받아서 활용하도록 연결했다.

AI Agent는 다음 정보를 받을 수 있게 되었다.

```text
matched_keywords
matched_preference_details
place_subtype
region_code
region_match
under_matched_preferences
unmatched_query_terms
missing_evidence_fields
```

이후 프론트에서 실제 자연어 시나리오를 테스트한 결과, Search Tool 개선 방향은 전체적으로 의도에 맞게 반영되었지만 AI Agent 쪽에서 추가로 정리해야 할 문제가 확인되었다.

### 이슈 18. 여행 기간 자연어 표현을 충분히 파싱하지 못함

#### 재현 상황

다음과 같은 표현을 포함한 요청에서 `travel_days`가 채워지지 않았다.

```text
강릉으로 하루 여행 갈 거야.
강릉으로 이틀 여행 갈 거야.
강릉으로 1일 여행 갈 거야.
```

#### 문제

- `2일`, `4일`처럼 숫자 기반 표현은 일부 처리된다.
- 하지만 `하루`, `이틀`, `1일` 같은 실제 사용자가 자주 쓰는 표현이 누락된다.
- 그 결과 프론트에서는 사용자가 이미 기간을 말했는데도 “여행 기간이 며칠인가요?”라고 다시 묻게 된다.

#### 개선 방향

- Input Parser에서 한국어 기간 표현을 보강한다.
- 최소한 다음 표현은 `travel_days`로 변환한다.

```text
하루, 당일, 당일치기, 1일 → travel_days=1
이틀, 2일 → travel_days=2
사흘, 3일 → travel_days=3
나흘, 4일 → travel_days=4
```

#### 분류

- PARSER
- UX
- P0

### 이슈 19. 반려동물 크기 표현은 잡지만 동반 여부를 함께 추론하지 못함

#### 재현 상황

```text
강릉으로 2일 여행 갈 거야. 소형견이랑 같이 갈 수 있는 바다 산책 코스로 추천해줘.
```

#### 문제

- `pet_size=SMALL`은 추출됐다.
- 하지만 `pet_allowed=true`가 함께 채워지지 않았다.
- 그래서 사용자가 이미 “소형견이랑 같이”라고 말했는데도 `반려동물과 함께 가시는지 알려주세요.`라는 추가 질문이 발생했다.

#### 개선 방향

- `소형견`, `중형견`, `대형견`, `강아지`, `반려견`, `반려동물` 표현이 `같이`, `동반`, `데려`, `함께` 같은 표현과 함께 나오면 `pet_allowed=true`도 같이 채운다.
- `pet_size`는 사용자 요청 조건으로 유지하되, 장소의 `max_pet_size` 근거가 없다는 이유만으로 전체 일정을 실패시키지는 않는다.
- 장소별 허용 크기 근거가 부족하면 최종 응답에서 방문 전 확인 안내로 처리한다.

#### 분류

- PARSER
- VALIDATOR
- UX
- P0

### 이슈 20. 가격 조건은 데이터 근거가 없어 검색/검증 조건으로 사용하기 어려움

#### 재현 상황

사용자가 다음과 같이 가격 조건을 요청할 수 있다.

```text
저렴한 곳 위주로 추천해줘.
3만원 이하로 갈 수 있는 곳으로 짜줘.
가성비 좋은 곳으로 부탁해.
```

#### 문제

- 현재 Search Tool/ES 데이터에는 가격을 안정적으로 비교하거나 검증할 수 있는 필드가 없다.
- `max_price`를 hard filter나 검증 조건으로 사용하면 근거 부족 실패가 발생할 가능성이 높다.
- 가격 요청을 완전히 무시하면 사용자 입장에서는 요청이 반영되지 않은 것처럼 보인다.

#### 개선 방향

- Input Parser는 가격 관련 요청을 인식해도 된다.
- 다만 현재 단계에서는 `max_price`를 Search Tool hard filter나 Hard Validator 실패 조건으로 사용하지 않는다.
- 최종 응답에서는 가격 정보를 정확히 비교하기 어렵다는 점을 사용자 눈높이에 맞게 안내한다.

예시:

```text
가격 정보는 장소마다 확인 가능한 범위가 달라서 정확한 예산 기준으로 걸러내기는 어려웠어요.
방문 전에 메뉴 가격이나 이용 요금을 한 번 더 확인해 주세요.
```

#### 분류

- PARSER
- VALIDATOR
- UX
- P1

### 이슈 21. 음식 선호 키워드가 관광지/숙소 검색에도 섞임

#### 재현 상황

```text
강릉으로 2일 여행 갈 거야. 바다 보고 물회나 회를 먹고 싶어.
```

#### 문제

- 사용자 선호가 `바다`, `물회`, `회`로 추출됐다.
- 이 중 `물회`, `회`는 식당 검색에 적합한 키워드다.
- 하지만 관광지/숙소 검색에서도 음식 키워드가 query text에 섞이는 흐름이 확인됐다.
- 이 경우 Search Tool diagnostics에서 관광지/숙소 후보가 부족하거나 선호 키워드를 만족하지 못한다고 판단할 수 있다.

#### 개선 방향

- AI Agent에서 도메인별 검색 키워드를 분리한다.

```text
DESTINATION → 바다, 산책, 자연, 전망, 박물관, 체험 등
RESTAURANT → 해산물, 회, 물회, 대게, 막국수, 순두부, 카페 등
LODGING → 오션뷰, 숙소, 호텔, 펜션, 리조트 등
```

- 음식 키워드는 기본적으로 식당 검색 요청에 우선 반영한다.
- 관광지/숙소 검색에는 해당 도메인과 관련 있는 키워드만 보낸다.

#### 분류

- SEARCH_REQUEST
- ROUTING
- QUALITY
- P0

### 이슈 22. 부정/비교 선호 표현을 긍정 선호로 잘못 해석함

#### 재현 상황

```text
카페보다는 관광지 위주로 보고 싶어.
```

#### 문제

- 위 문장에서 사용자의 핵심 의도는 `관광지 위주`다.
- 하지만 `카페`라는 단어가 포함되어 있어 `preferences=["카페", "해산물"]`처럼 카페가 긍정 선호로 들어간 사례가 있었다.
- 그 결과 슬롯 구성에 `CAFE`가 포함되었다.

#### 개선 방향

- Input Parser에서 다음과 같은 표현을 부정/비교 문맥으로 처리한다.

```text
A보다는 B
A보다 B
A 말고 B
A는 빼고 B
A는 적게
A 위주 아님
```

- `카페보다는 관광지`처럼 비교 표현이 있으면 카페는 긍정 선호에서 제외하거나 낮은 우선순위로 처리한다.
- 향후에는 `negative_preferences` 또는 `deprioritized_preferences` 필드를 별도로 두는 것도 고려한다.

#### 분류

- PARSER
- SLOT
- UX
- P0

### 이슈 23. 미확인 안내가 너무 많이 노출됨

#### 재현 상황

일정 생성은 성공했지만 `notices`가 30개 이상 생성되는 케이스가 있었다.

#### 문제

- 사용자가 요청하지 않은 조건까지 미확인 안내로 많이 노출된다.
- 예를 들어 반려동물이나 무장애를 요청하지 않았는데 관련 필드 미확인 안내가 반복될 수 있다.
- 프론트에서 그대로 보여줄 경우 사용자에게 과하게 복잡한 응답이 된다.

#### 개선 방향

- 사용자가 요청한 조건과 관련된 notice를 우선 노출한다.
- 요청하지 않은 정책 필드의 미확인 안내는 숨기거나 낮은 우선순위로 둔다.
- 같은 유형의 notice는 장소별로 반복하지 않고 묶어서 보여준다.

예시:

```text
일부 장소는 운영시간이나 이용 가능 여부가 바뀔 수 있어 방문 전에 확인해 주세요.
```

#### 분류

- RESPONSE
- UX
- P1

### 이슈 24. 실패 응답에 개발자용 필드명이 그대로 노출됨

#### 재현 상황

반려동물/무장애 조건 실패 시 다음과 같은 내부 필드명이 응답에 포함됐다.

```text
pet_allowed
pet_size
maxPetSize
wheelchairAccessible
```

#### 문제

- 내부 필드명은 개발자에게는 유용하지만 일반 사용자에게는 이해하기 어렵다.
- 실패 원인이 맞더라도 표현이 딱딱해 보여 서비스 응답 품질이 떨어진다.

#### 개선 방향

- Response Agent에서 내부 필드명을 사용자 친화 표현으로 변환한다.

```text
pet_allowed → 반려동물 동반 가능 여부
pet_size / maxPetSize → 허용 가능한 반려동물 크기
wheelchairAccessible / wheelchair_accessible → 휠체어 접근 가능 여부
operating_hours → 운영시간
```

- 실패 응답은 “무엇이 부족했는지”와 “사용자가 다음에 어떻게 하면 되는지”를 짧게 안내한다.

#### 분류

- RESPONSE
- UX
- P0

### 이슈 25. 필수 근거 부족 상황에서도 동일한 재검색이 반복됨

#### 재현 상황

반려동물/무장애 조건 시나리오에서 다음 흐름이 반복됐다.

```text
후보 반환
→ Hard Validator EVIDENCE_MISSING
→ 재검색
→ 다시 EVIDENCE_MISSING
→ retry_count=4
→ failed
```

#### 문제

- 후보 수 부족이면 재검색이 의미가 있다.
- 하지만 `wheelchair_accessible`, `pet_allowed`, `max_pet_size`처럼 필수 근거 데이터 자체가 계속 없으면 같은 방식의 재검색만으로 해결되기 어렵다.
- 특히 반려동물/무장애 조건은 자동 완화하면 안 되는 조건이라, 재검색보다 “근거 부족 안내” 또는 “사용자 선택지 제공”이 더 적절하다.

#### 개선 방향

- diagnostics의 `missing_evidence_fields`가 반복되면 retry strategy를 바꾼다.
- 같은 필수 evidence 부족이 2회 이상 반복되면 조기 종료하거나 사용자에게 선택지를 제공한다.
- 자동으로 조건을 완화하지 않는다.

예시:

```text
요청하신 조건을 확실히 확인할 수 있는 장소가 부족했어요.
조건을 유지해서 다시 찾거나, 방문 전 확인 안내와 함께 후보를 받아볼 수 있어요.
```

#### 분류

- RETRY
- VALIDATOR
- UX
- P0

## 3차 개선 우선순위

### AI Agent 우선 개선

1. 여행 기간 파싱 보강
2. 반려동물 크기 표현에서 `pet_allowed=true` 함께 추론
3. 부정/비교 선호 표현 처리
4. 도메인별 검색 키워드 분리
5. notice 필터링 및 묶음 처리
6. 실패 응답의 내부 필드명 사용자 친화 변환
7. 필수 evidence 부족 반복 시 retry 조기 종료 또는 선택지 제공

### Search Tool/ES 우선 개선

1. 반려동물 동반 가능 여부 evidence 제공
2. 허용 가능한 반려동물 크기 evidence 제공
3. 휠체어 접근 가능 여부 evidence 제공
4. hard filter를 만족한다고 판단한 근거를 candidate와 함께 반환
5. 가격 데이터가 없다면 가격 필터는 지원 불가 조건으로 명확히 분리

### 현재 정책 정리

- 가격 조건은 현재 데이터 근거가 없으므로 hard filter/검증 실패 조건으로 사용하지 않는다.
- 반려동물 동반 여부는 사용자가 요청하면 중요한 조건으로 유지한다.
- 반려동물 크기는 사용자 요청으로는 파싱하되, 장소의 허용 크기 근거가 없다는 이유만으로 전체 일정을 실패시키지 않는다.
- 무장애 조건은 중요 조건이지만, 숙소/식당에 근거 데이터가 부족한 경우 사용자 안내와 Search Tool evidence 보강이 함께 필요하다.

## 3차 개선 후 4차 개선 대상 정리

### 배경

3차 개선에서는 AI Agent와 Search Tool 사이의 응답 계약을 맞추고, 자연어 입력과 SearchRequest 생성 흐름을 보강했다.

확인된 개선 결과:

```text
1일, 하루, 이틀 표현을 travel_days로 파싱함
소형견이랑 같이 간다는 표현에서 pet_allowed=true와 pet_size=SMALL을 함께 추론함
카페보다는 관광지 표현에서 카페 슬롯을 만들지 않음
음식 키워드가 관광지/숙소 검색어로 섞이지 않음
Search Tool 후보가 AI Agent 후보로 정상 변환됨
기본 시나리오에서 일정 생성 completed 확인
```

하지만 3차 개선 후 프론트 시나리오를 다시 테스트하면서 다음 문제가 남아 있음을 확인했다.

### 이슈 26. Search Tool hard filter 요청에도 null 정책 후보가 반환됨

#### 재현 상황

무장애 요청:

```text
강릉으로 하루 여행 갈 거야. 휠체어로 이동하기 편한 바다 코스로 추천해줘. 반려동물은 없어.
```

AI Agent가 Search Tool에 보낸 hard filter:

```json
{
  "wheelchair_accessible": true
}
```

하지만 반환 후보와 최종 일정 슬롯에는 다음 값이 포함됐다.

```json
{
  "wheelchair_accessible": null
}
```

#### 문제

- AI Agent는 `wheelchair_accessible=true`를 필수 조건으로 요청했다.
- Search Tool은 후보를 반환했지만, 일부 후보에는 `wheelchair_accessible` 근거가 없었다.
- Itinerary Agent가 해당 후보를 일정에 넣었다.
- Hard Validator가 최종 일정 슬롯에서 `wheelchair_accessible=null`을 보고 `EVIDENCE_MISSING`으로 실패 처리했다.

흐름:

```text
hard_filters.wheelchair_accessible=true
→ Search Tool이 wheelchair_accessible=null 후보 반환
→ 일정 생성
→ Hard Validator EVIDENCE_MISSING
→ 조기 실패
```

#### 개선 방향

Search Tool에서 hard filter가 true로 들어온 경우 후보 반환 정책을 명확히 해야 한다.

권장 정책:

```text
hard filter를 확실히 만족하는 후보
→ OK 후보로 반환

조건 만족 여부를 알 수 없는 후보
→ 기본적으로 hard filter 결과에서 제외
또는 INSUFFICIENT_EVIDENCE 후보로 반환하되 AI Agent가 확정 일정에 넣지 않도록 명확히 표시
```

AI Agent 쪽에서도 보완 가능하다.

```text
필수 조건값이 null인 후보는 itinerary 후보 선택에서 감점하거나 제외
INSUFFICIENT_EVIDENCE 후보는 필수 조건 요청 시 확정 일정에 넣지 않음
```

#### 분류

- SEARCH
- VALIDATOR
- ITINERARY
- P0

### 이슈 27. 반려동물/무장애 정보가 식당·숙소 후보에서 부족함

#### 재현 상황

반려동물 요청:

```text
강릉으로 2일 여행 갈 거야. 소형견이랑 같이 갈 수 있는 바다 산책 코스로 추천해줘.
```

확인된 결과:

```text
destination_candidates > 0
restaurant_candidates > 0
lodging_candidates > 0
```

하지만 식당과 숙소 후보 대부분은 다음 상태였다.

```json
{
  "pet_allowed": null,
  "max_pet_size": null
}
```

무장애 요청에서도 식당 후보 대부분은 다음 상태였다.

```json
{
  "wheelchair_accessible": null
}
```

#### 문제

- 후보 수집과 일정 생성은 정상 동작한다.
- 하지만 식당/숙소 후보의 정책 근거가 부족해 Hard Validator에서 실패한다.
- 특히 반려동물/무장애 조건은 사용자의 안전과 실제 방문 가능성에 직접 연결되므로 AI Agent가 임의로 통과시키기 어렵다.

#### 개선 방향

Search Tool/ES:

- 식당/숙소 문서에도 가능한 범위에서 정책 정보를 보강한다.
- `pet_allowed`, `wheelchair_accessible`을 알 수 있으면 candidate top-level과 evidence에 함께 내려준다.
- 알 수 없으면 `null`로 유지하되, 후보별 `missing_fields`와 diagnostics를 구분해 내려준다.

AI Agent 정책 결정:

- 서비스 정책상 식당/숙소까지 필수 검증할지 결정해야 한다.
- 안전 우선이면 현재처럼 근거 없을 때 실패 처리한다.
- 일정 제공 우선이면 식당/숙소는 방문 전 확인 안내로 낮추고 관광지 중심으로 강하게 검증한다.

현재 권장:

```text
반려동물/무장애 조건은 기본적으로 필수 조건으로 유지한다.
다만 데이터 부족이 반복되면 실패 이유를 명확히 안내하고, 추후 사용자 선택형 완화 옵션을 제공한다.
```

#### 분류

- SEARCH
- DATA
- VALIDATOR
- UX
- P0

### 이슈 28. 일정 생성 시 후보 품질 반영이 아직 약함

#### 재현 상황

음식 선호 요청:

```text
강릉으로 1일 여행 갈 거야. 바다 보고 물회나 회를 먹고 싶어. 반려동물은 안 데려가.
```

확인된 결과:

```text
점심: 신대게나라
저녁: 송정해변막국수
```

`신대게나라`는 `물회`, `회`, `대게`, `홍게` 키워드와 잘 맞았다.

하지만 `송정해변막국수`는 사용자가 말한 `물회`, `회`와 직접 매칭되지는 않았다.

#### 문제

- Search Tool은 `matched_keywords`를 내려주고 있다.
- AI Agent도 이 값을 후보에 보존하고 있다.
- 하지만 Itinerary Optimizer가 세부 음식 키워드 일치도를 충분히 강하게 반영하지 못하면, 일반 음식점이나 넓은 바다/해변 매칭 후보가 일정에 들어갈 수 있다.

#### 개선 방향

AI Agent:

- Itinerary Optimizer에서 사용자 선호 키워드와 후보 `matched_keywords`의 직접 일치도를 더 강하게 반영한다.
- 식당 슬롯에서는 `food` 같은 넓은 preference보다 `물회`, `회`, `해산물`, `대게` 같은 세부 키워드 매칭을 우선한다.
- 같은 식사 슬롯에서 세부 키워드 매칭 후보가 있으면 일반 음식점보다 우선 선택한다.

Search Tool:

- `matched_keywords`와 `matched_preference_details`를 계속 제공한다.
- 한 글자 키워드 `회`, `산`은 오탐 방지 규칙을 유지한다.

#### 분류

- ITINERARY
- QUALITY
- SEARCH
- P1

### 이슈 29. 대화형 상태가 새 요청과 섞일 수 있음

#### 재현 상황

이전 요청 상태에 `travel_days=2`가 남아 있는 상태에서 사용자가 새로 다음 요청을 보냈다.

```text
강릉으로 하루 여행 갈 거야. 휠체어로 이동하기 편한 바다 코스로 추천해줘. 반려동물은 없어.
```

결과적으로 다음 상태가 섞였다.

```json
{
  "travel_days": 2,
  "nights": 0
}
```

ConflictChecker는 이를 다음 충돌로 판단했다.

```text
숙박 일수와 여행 일수가 일치하지 않습니다.
```

#### 문제

- 현재 테스트/프론트 흐름에서는 이전 state와 새 자연어 요청이 섞일 수 있다.
- 새 여행 요청인지, 기존 여행 요청의 수정인지 구분하는 정책이 아직 명확하지 않다.
- 사용자는 새로 말한 것인데 시스템은 이전 값을 유지할 수 있다.

#### 개선 방향

대화형 일정 수정 기능을 설계할 때 다음을 분리해야 한다.

```text
새 여행 요청
→ 기존 request/state 초기화 후 새로 파싱

기존 일정 수정 요청
→ 기존 state를 유지하고 변경된 슬롯/조건만 반영
```

AI Agent 개선:

- 새 요청으로 판단되는 표현을 감지한다.
- 예: “강릉으로 하루 여행 갈 거야”, “속초로 다시 추천해줘”
- 새 요청이면 `travel_days`, `nights`, `preferences`, 후보, 일정 상태를 초기화한다.
- 수정 요청이면 기존 itinerary를 유지한 채 변경 요청만 반영한다.

#### 분류

- CONVERSATION
- STATE
- PARSER
- UX
- P1

## 4차 개선 우선순위

### 1순위: Search Tool hard filter/null 후보 정책 정리

가장 먼저 봐야 할 문제다.

```text
hard_filters.pet_allowed=true
hard_filters.wheelchair_accessible=true
```

위 조건이 들어왔을 때 `null` 후보를 어떻게 처리할지 정해야 한다.

권장:

```text
true 근거가 있는 후보만 OK로 반환
근거가 없는 후보는 제외하거나 INSUFFICIENT_EVIDENCE로 표시
AI Agent는 필수 조건 요청 시 INSUFFICIENT_EVIDENCE 후보를 확정 일정에 넣지 않음
```

### 2순위: 식당/숙소 정책 데이터 보강

반려동물/무장애 여행이 서비스 핵심 요구라면 식당/숙소에도 정책 근거가 필요하다.

```text
restaurant.pet_allowed
restaurant.wheelchair_accessible
lodging.pet_allowed
lodging.wheelchair_accessible
```

데이터가 부족하면 실패가 반복될 수밖에 없다.

### 3순위: Itinerary Optimizer 품질 개선

기본 일정 생성은 가능해졌으므로, 이제는 “어떤 후보를 고르느냐”를 개선해야 한다.

```text
세부 matched_keywords 직접 일치 후보 우선
식당 슬롯에서 음식 키워드 매칭 강화
일반 음식점보다 요청 음식과 직접 맞는 음식점 우선
```

### 4순위: 대화형 새 요청/수정 요청 분리

프론트에서 자연어 대화를 이어갈 계획이라면 반드시 필요하다.

```text
새 여행 요청인지
기존 일정 수정 요청인지
```

이 둘을 구분하지 않으면 이전 state와 새 요청이 섞여 불필요한 충돌이 생길 수 있다.

### 현재 종료선

3차 개선까지로 다음은 확인됐다.

```text
AI Agent 입력 파싱 개선 완료
SearchRequest 도메인별 분리 완료
Search Tool 응답 계약 보강 완료
후보 변환 정상화 완료
기본 일정 생성 completed 확인
반려동물/무장애 실패 원인 명확화 완료
```

따라서 현재 기능은 기본 자연어 여행 추천 흐름까지 연결된 상태다.

4차 개선은 필수 연결 작업이라기보다 다음 완성도 개선 단계다.

### 이슈 30. 식당/숙소에는 반려동물·무장애 데이터가 없어 필수 검증 대상으로 보면 안 됨

#### 재현 상황

반려동물 또는 무장애 조건이 포함된 요청에서 관광지 후보는 조건 근거를 가진 후보가 내려왔지만, 식당/숙소 후보는 다음처럼 정책 필드가 `null`로 내려왔다.

```json
{
  "category": "RESTAURANT",
  "pet_allowed": null,
  "wheelchair_accessible": null,
  "missing_fields": ["pet_allowed", "wheelchair_accessible"]
}
```

#### 문제

- 현재 DB에서 반려동물/무장애 정보는 관광지 도메인에만 존재한다.
- 식당과 숙소에는 해당 데이터가 없기 때문에 Search Tool이 값을 채울 수 없다.
- 그런데 AI Agent가 모든 도메인에 동일하게 정책 필수 조건을 적용하면 식당/숙소 슬롯을 채우지 못해 일정 생성이 실패한다.
- 이는 Search Tool 검색 실패라기보다 AI Agent가 도메인별 데이터 보유 범위를 구분하지 못한 문제다.

#### 개선 방향

AI Agent에서 도메인별 정책을 분리한다.

```text
DESTINATION:
  반려동물/무장애 데이터가 있으므로 검색과 검증에 필수 조건으로 적용한다.

RESTAURANT / LODGING:
  현재 데이터가 없으므로 검색 hard filter와 Hard Validator 필수 검증에서 제외한다.
  대신 최종 응답에서 방문 전 직접 확인이 필요하다는 안내 대상으로 처리한다.
```

#### Search Tool 처리 여부

이번 문제는 Search Tool에서 추가로 고칠 문제가 아니다.

```text
Search Tool:
  DB에 없는 값을 임의로 만들 수 없음

AI Agent:
  도메인별로 어떤 조건을 검증 가능한지 알고 다르게 적용해야 함
```

#### 기대 동작

- 관광지는 요청 조건에 맞는 후보만 일정에 사용한다.
- 식당/숙소는 반려동물/무장애 값이 `null`이어도 일정 생성 실패 사유로 보지 않는다.
- 사용자에게는 식당/숙소 방문 전 반려동물 동반 가능 여부와 이동 편의성을 확인하라고 안내한다.

#### 분류

- AI_AGENT
- DOMAIN_POLICY
- VALIDATOR
- ITINERARY
- UX
- P0

### 이슈 31. 여러 숙소 요청 처리와 숙소 중복 선택 정책이 분리되어 있지 않음

#### 재현 상황

사용자가 다음처럼 3일 여행에서 첫째 날과 둘째 날 숙소를 다르게 쓰고 싶다고 요청했다.

```text
강릉으로 3일 여행 갈거야. 바다가 보고 싶고 맛있는 해산물도 많이 먹고 싶어.
반려동물은 없어. 첫째날이랑 둘째날 머물 숙소를 다르게 하고 싶어
```

#### 문제

- 기본 다일 여행에서는 한 숙소 연박이 일반적이므로 숙소 1개만 추천하는 것이 자연스럽다.
- 반대로 사용자가 숙소를 다르게 쓰고 싶다고 명시하면 숙박일 수만큼 서로 다른 숙소가 필요하다.
- 기존 구조에서는 여행 일수에 따라 숙소 슬롯이 여러 개 생기거나, 슬롯은 여러 개 생겨도 Optimizer가 같은 숙소를 반복 선택할 수 있었다.
- 그 결과 `final_response.accommodations`가 추천 후보 목록인지, 숙박일별 배정 숙소인지 FE에서 해석하기 어려웠다.

#### 개선 방향

AI Agent에서 숙소 정책을 다음처럼 분리한다.

```text
기본 다일 여행:
  숙소 슬롯은 D1_LODGING 하나만 생성
  한 숙소 연박으로 간주

사용자가 여러 숙소를 요청:
  숙박일 수만큼 숙소 슬롯 생성
  같은 숙소 반복 선택 금지
```

여러 숙소 요청으로 볼 수 있는 표현 예시:

```text
매일 다른 숙소
숙소를 다르게
머물 숙소를 다르게
숙소 이동
각각 다른 숙소
```

#### 기대 동작

- 3일 기본 여행이면 `final_response.accommodations`에는 숙소 1개만 내려간다.
- 3일 여행에서 "숙소를 다르게" 요청하면 `D1_LODGING`, `D2_LODGING`이 생성된다.
- 여러 숙소 요청일 때는 `accommodations`에 서로 다른 숙소가 내려간다.
- FE는 `accommodations.length === 1`이면 추천 숙소 1개로, 2개 이상이면 1박차/2박차 숙소로 표시한다.

#### 분류

- AI_AGENT
- SUPERVISOR
- ITINERARY
- FE_CONTRACT
- UX
- P1
