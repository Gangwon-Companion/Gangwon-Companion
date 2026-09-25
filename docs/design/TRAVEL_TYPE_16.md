# 여행 전용 성향 체계 — Travel Type 16

## 문서 상태

- 상태: FE·BE·AI 구현 완료, 통합 검증 진행 중
- 현재 분석 버전: `travel-type-16-v1`
- 방식: 별도 설문 없이 검색·방문·저장 코스·리뷰 기록으로 자동 분석
- 반려동물 정보: 유형 축에 포함하지 않고 여행 추천의 필수 조건으로 별도 처리
- 공통 유형 코드의 기준 문서: 이 문서

## 4개 여행 축

코드는 반드시 `공간 → 활동 → 일정 → 장소` 순서로 조합한다.

| 순서 | 축 | 성향 1 | 성향 2 | 의미 |
|---:|---|---|---|---|
| 1 | 공간 | `C` City 도시형 | `N` Nature 자연형 | 도시와 자연 중 선호하는 공간 |
| 2 | 활동 | `A` Active 체험형 | `R` Rest 휴식형 | 활동적인 체험과 편안한 휴식 중 선호 방식 |
| 3 | 일정 | `P` Planned 계획형 | `S` Spontaneous 즉흥형 | 사전 계획과 현장 결정 중 가까운 방식 |
| 4 | 장소 | `F` Famous 명소형 | `H` Hidden 로컬형 | 대표 명소와 숨은 장소 중 선호 공간 |

예시:

- `CAPF`: 도시 + 체험 + 계획 + 유명 명소
- `NRSH`: 자연 + 휴식 + 즉흥 + 숨은 로컬

## 16가지 유형

| 코드 | 유형명 | 설명 |
|---|---|---|
| `CAPF` | 도시 정복 플래너 | 도시의 대표 명소와 체험을 촘촘한 일정으로 즐긴다. |
| `CAPH` | 히든시티 전략가 | 숨은 도시 콘텐츠를 미리 조사해 경험한다. |
| `CASF` | 랜드마크 액션러 | 유명한 도심 명소와 체험을 자유롭게 누빈다. |
| `CASH` | 골목 모험 스카우트 | 발길 닿는 골목에서 새로운 활동을 발견한다. |
| `CRPF` | 도심 힐링 가이드 | 대표 도시 공간을 여유로운 계획으로 즐긴다. |
| `CRPH` | 골목 감성 큐레이터 | 조용한 카페와 숨은 공간을 코스로 엮는다. |
| `CRSF` | 도심 여유 산책가 | 유명 도시 공간을 부담 없이 산책하듯 즐긴다. |
| `CRSH` | 골목 낭만 유랑자 | 계획 없이 걷다가 마음에 드는 공간에 머문다. |
| `NAPF` | 자연 원정대장 | 유명 자연 명소와 액티비티를 계획적으로 공략한다. |
| `NAPH` | 자연 탐험 플래너 | 숨은 자연 명소와 활동을 미리 조사해 찾아간다. |
| `NASF` | 자연 액티비티 헌터 | 유명 자연 명소와 레포츠를 즉흥적으로 즐긴다. |
| `NASH` | 야생 모험 개척자 | 정해진 코스 없이 숨은 자연에 뛰어든다. |
| `NRPF` | 절경 힐링 설계자 | 대표적인 자연 명소에서 완벽한 휴식을 계획한다. |
| `NRPH` | 숲속 힐링 설계자 | 한적한 자연 속 숨은 휴식처를 계획해 찾아간다. |
| `NRSF` | 풍경 따라 쉼표 여행자 | 유명한 자연 풍경을 따라 자유롭게 쉬어간다. |
| `NRSH` | 자연 속 은둔 유랑자 | 사람 적은 자연에서 즉흥적으로 머물 곳을 정한다. |

## 전체 동작 흐름

```text
사용자 활동 생성
  → BE가 검색·방문·저장 코스·리뷰 수집
  → AI가 네 축 점수와 4글자 유형 산출
  → BE가 결과 검증 및 DB 저장
  → FE가 유형·축별 비율·근거 표시
  → 저장된 유형과 점수를 AI 코스 추천 컨텍스트로 전달
```

## 자동 분석 정책

- 서로 다른 유효 활동 신호가 3건 미만이면 `INSUFFICIENT_DATA`를 반환한다.
- 검색은 기본 가중치 `1.0`, 방문은 `3.0`, 저장 장소는 `2.5`를 사용한다.
- 리뷰는 평점에 따라 최대 `3.0`까지 반영한다.
- 최근 활동일수록 더 크게 반영하며 약 90일 반감 가중치를 사용한다.
- 검색·코스 저장은 계획형 근거, 실제 방문은 즉흥형의 보조 근거로 사용한다.
- 장소명·카테고리·검색어의 축별 키워드를 점수화한다.
- 각 축 점수는 합계 100으로 정규화한다.
- 근거가 없는 축은 `50:50`이며 동점이면 코드의 왼쪽 성향을 선택한다.
- LLM은 확정된 유형을 변경하지 않고 사용자용 설명을 다듬는 데만 사용한다.

## API

- `GET /api/v1/users/me/travel-profile`
- `POST /api/v1/users/me/travel-profile/analysis-jobs`
- `GET /api/v1/users/me/travel-profile/analysis-jobs/{jobId}`
- `POST /internal/travel/profile/analyze` — BE에서 AI로 보내는 내부 API

완료 결과 예시:

```json
{
  "status": "COMPLETED",
  "travelerType": "NRPH",
  "title": "숲속 힐링 설계자",
  "description": "한적한 자연 속 숨은 휴식처를 계획적으로 찾아가는 여행자예요.",
  "confidence": 0.73,
  "axisScores": {
    "space": { "C": 22, "N": 78 },
    "activity": { "A": 31, "R": 69 },
    "schedule": { "P": 61, "S": 39 },
    "place": { "F": 35, "H": 65 }
  }
}
```

## 화면 구성

- 마이페이지에 유형 코드·이름·설명 요약 카드 표시
- 상세 화면에서 자동 분석 또는 재분석 실행
- 코드, 유형명, 설명, 태그, 축별 비율과 분석 근거 표시
- 네 알파벳 축의 의미 설명
- 16가지 전체 유형 목록과 현재 `나의 유형` 표시
- `GET` 프로필 응답은 캐시하지 않고 진입할 때마다 최신 결과 조회

## 기존 6유형 호환 정책

과거 유형은 신규 분석에 사용하지 않지만 기존 DB 행을 안전하게 읽기 위해 BE Enum과 DB 체크 제약조건에는 한동안 남긴다.

- `NATURE_HEALING`
- `PET_COMPANION`
- `LOCAL_FOOD_EXPLORER`
- `ACTIVITY_ADVENTURE`
- `CULTURE_EXPLORER`
- `BALANCED_TRAVELER`

`travel-profile-llm-v1` 프로필은 화면에서 미분석으로 처리하고 새 버전으로 재분석한다. 신규 AI 결과는 16유형만 반환한다.

## 트러블슈팅 요약

### `AI_SERVICE_UNAVAILABLE`

- 의미: BE가 AI 서버에 연결하지 못함
- 로컬 BE: AI를 `127.0.0.1:8000`에서 실행
- Docker BE: 현재 Compose 기본 주소가 `host.docker.internal:8001`이므로 AI를 `0.0.0.0:8001`에서 실행
- Windows 방화벽은 필요한 경우 개인 네트워크만 허용

### `travel_profiles_traveler_type_check` 위반

- 의미: DB가 기존 6개 유형만 허용해 `CAPF` 등 신규 유형 저장 거부
- 해결: 기존 6개와 신규 16개를 모두 허용하도록 체크 제약조건 마이그레이션
- 운영 RDS에도 애플리케이션 배포 전 동일한 마이그레이션 필요

### 분석 직후 보이지만 재진입하면 `NOT_ANALYZED`

- 확인 사항: DB의 `status`, `traveler_type`, `analysis_version`
- 원인 1: 구버전 BE 이미지가 새 분석 버전을 현재 버전으로 인식하지 못함
- 원인 2: 브라우저가 최초 `NOT_ANALYZED` GET 응답을 재사용
- 해결: 최신 BE 재빌드, 지원 버전 코드 고정, BE `Cache-Control: no-store`, FE `cache: no-store`

### 실패 Job 재사용

`FAILED` Job은 재사용하지 않는다. 원인 해결 후 `POST /analysis-jobs`로 새 Job을 생성한다.

## 검증 명령

BE 컴파일:

```powershell
.\gradlew.bat compileJava compileTestJava
```

AI 관련 테스트:

```powershell
.\.venv\Scripts\python.exe -B -m pytest -p no:cacheprovider tests\test_travel_profile.py tests\test_input_parser.py
```

FE 타입 검사:

```powershell
npm run typecheck
```

## 남은 개선 과제

- `P/S` 정확도를 높이기 위한 검색·저장·방문 간 시간 관계 분석
- `F/H` 정확도를 높이기 위한 관광지 인기도·방문량 메타데이터 추가
- 근거가 부족한 축이 있을 때 유형 확정을 보류하는 정책 검토
- 수동 DB 변경 대신 Flyway 또는 Liquibase 마이그레이션 도입
