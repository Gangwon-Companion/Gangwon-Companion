# RAG Grounding 실제 검증 기록

- 검증일: 2026-09-11
- 실행 환경: Windows PowerShell, Asia/Seoul
- Backend 저장소: `Gangwon-Companion`, branch `feature/RAG`
- AI 저장소: `Gangwon-AI`, branch `main`
- Java: 17.0.20 LTS
- Python: `Gangwon-AI/.venv/Scripts/python.exe` (Python 3.12 계열)
- 주요 구성: Spring Boot, PostgreSQL 16, Elasticsearch, FastAPI, LangGraph

## 1. 검증 대상

다음 흐름을 실제 코드와 테스트로 확인했다.

```text
사용자 요청
  -> AI LangGraph
  -> Destination / Restaurant / Lodging 검색 Agent
  -> Backend /internal/search/places
  -> RDB 또는 Elasticsearch 하이브리드 검색
  -> evidence / missing_fields / status 계약 검증
  -> Itinerary 생성
  -> Hard Validator
  -> Quality Validator
  -> Response Agent
  -> LLM answer grounding 검증
  -> 실패 시 deterministic fallback
  -> final_response
```

## 2. 실행 중인 환경

Docker Desktop을 기동한 뒤 다음 서비스의 상태를 확인했다.

| 서비스 | 확인 방법 | 결과 |
| --- | --- | --- |
| Backend | `GET http://localhost:8080/actuator/health` | `liveness/readiness: UP` |
| AI Server | `GET http://localhost:8000/health` | `ok` |
| PostgreSQL | Docker Compose 컨테이너 | 실행 확인 |
| Elasticsearch | Docker Compose 컨테이너 | 컨테이너 기동 확인. 기동 직후 HTTP 연결 안정화가 필요했음 |
| Redis / Celery | 기존 컨테이너 | 실행 확인 |

Docker 엔진은 처음에는 꺼져 있었으며 `docker desktop start`로 기동했다. 실호출 테스트에서는 비용이 발생하는 OpenAI API를 사용하지 않도록 `GANGWON_RESPONSE_LLM_ENABLED=false`를 지정했다.

## 3. AI 서버 전체 테스트

실행 위치: `C:\Users\5131\Desktop\Gangwon\Gangwon-AI`

```powershell
$env:RUN_BE_INTEGRATION_TESTS = "1"
$env:GANGWON_RESPONSE_LLM_ENABLED = "false"
.\.venv\Scripts\python.exe -B -m unittest
```

결과:

```text
Ran 112 tests in 0.751s
OK
```

이 실행에서는 기존에 skip 처리되던 Backend 연동 테스트도 포함됐다. LLM 관련 테스트는 mock client와 fallback 경로를 사용하며, 다음 동작을 확인했다.

- 검색 계약 파싱 및 변환
- 실제 Backend 검색 호출
- 1일·2일·3일 일정 생성
- 반려동물·휠체어 조건의 근거 부족 처리
- Hard Validator와 Quality Validator 연결
- Response Agent의 `final_response` 생성
- LLM answer의 장소 ID·시간·숫자 검증
- claim의 `place_id`, `evidence_fields`, evidence 값 검증
- grounding 실패 시 fallback 답변 전환

별도로 Backend 연동 테스트만 실행한 결과도 다음과 같다.

```text
$env:RUN_BE_INTEGRATION_TESTS = "1"
$env:GANGWON_RESPONSE_LLM_ENABLED = "false"
.\.venv\Scripts\python.exe -B -m unittest tests.test_be_e2e

Ran 6 tests in 1.765s
OK
```

## 4. Backend 테스트

실행 위치: `C:\Users\5131\Desktop\Gangwon\Gangwon-Companion`

```powershell
.\gradlew.bat test
```

첫 실행 결과:

```text
BUILD SUCCESSFUL
5 actionable tasks: 5 up-to-date
```

RAG 검색 근거 계약, 임베딩 문서 처리, Elasticsearch 인덱스 구조 및 기존 도메인 테스트가 통과했다.

이후 재실행에서는 코드 실패가 아니라 다음 로컬 권한 문제가 발생했다.

```text
Could not create parent directory for lock file C:\.gradle\wrapper\dists\...
AccessDeniedException: ...\build\reports\problems\problems-report.html
```

따라서 최종 판정은 첫 번째 성공 실행과 AI 서버의 실제 Backend 연동 테스트 결과를 기준으로 했다. 해당 오류는 테스트 assertion 실패가 아니라 Gradle wrapper 캐시 및 report 파일 권한 문제다.

## 5. 실제 API 연동 시나리오

AI 테스트의 `tests.test_be_e2e`는 `TestClient`로 AI의 `/internal/travel/plan`을 호출하고, AI 검색 클라이언트가 `http://localhost:8080` Backend의 검색 API를 호출하는 구조다.

검증한 요청 유형:

- 강릉 1일 일반 여행
- 강릉 1박 2일 여행
- 강원도 바다 중심 2박 3일 여행
- 반려동물 동반 조건으로 근거가 부족한 요청
- 휠체어 접근성 조건으로 근거가 부족한 요청

성공 일정은 `completed`, `READY`, `VALID`, `PASS`, `final_response=READY`를 확인하고, 조건 부족 일정은 `failed` 또는 `final_response=FAILED`와 부족 슬롯을 확인한다.

## 6. OpenAI 실호출 범위

이번 자동 검증에서는 API 비용과 비밀키 노출을 방지하기 위해 OpenAI 실호출을 사용하지 않았다. LLM 계층은 mock 성공, mock 실패, 잘못된 claim, 잘못된 장소 ID, fallback 경로로 검증했다.

실제 모델 문장 생성 확인은 기존 기록에 있는 `gpt-4.1` 수동 테스트 결과를 참고한다. 운영 전에는 키를 저장소나 문서에 남기지 않고 Secret 관리 시스템에서 주입해야 한다.

## 7. 결론 및 잔여 사항

RAG 검색·근거 계약·AI 그래프·최종 답변 grounding·fallback까지 구현되어 있으며, 로컬 Docker 환경에서 Backend 연동 테스트를 포함한 AI 전체 테스트가 통과했다.

잔여 사항은 기능 미완료가 아니라 운영 검증 항목이다.

- Elasticsearch 기동 완료 후 alias 및 색인 상태를 별도로 확인할 것
- 배포 환경에서 Secret 주입 및 LLM 모델 권한 확인
- Gradle 실행 계정의 `C:\.gradle` 및 `build/reports` 쓰기 권한 정리
- 비용 승인 후 운영용 OpenAI 모델 실호출 smoke test 수행
