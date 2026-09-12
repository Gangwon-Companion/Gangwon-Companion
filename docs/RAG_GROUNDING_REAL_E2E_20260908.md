# RAG Grounding 실제 데이터 E2E 검증
- 검증일: 2026-09-08 (Asia/Seoul)
- 대상: PostgreSQL·Elasticsearch·Spring 검색 스택
- 목적: 실제 장소 데이터에서 검색 결과, 근거 snapshot, 근거 부족 상태가 일관되게 연결되는지 확인

## 1. 실행 환경

Docker Compose로 다음 서비스를 기동했다.

| 서비스 | 상태 |
|---|---|
| PostgreSQL | healthy |
| Kafka | healthy |
| Kafka Connect | healthy |
| Elasticsearch | healthy |
| Spring Boot | healthy |

Spring health endpoint도 `status=UP`을 반환했다.

기동 명령:

```powershell
docker compose up -d --build
```

## 2. 실제 검색 E2E 결과

모든 요청은 `X-Grounding-Trace: true`로 호출해 검색 결과와 `trace.snapshots`를 함께 확인했다.

| 시나리오 | 결과 수 | Snapshot 수 | 결과 상태 | 검증 결과 |
|---|---:|---:|---|---|
| 강릉 관광지 기본 검색 | 5 | 5 | 모두 `OK` | 통과 |
| 강릉 음식점 + 반려동물·휠체어 조건 | 5 | 5 | 모두 `INSUFFICIENT_EVIDENCE` | 통과 |
| 강릉 숙소 + 반려동물 조건 | 5 | 5 | 모두 `INSUFFICIENT_EVIDENCE` | 통과 |

### 확인한 Grounding 계약

- 검색 결과의 장소 ID와 snapshot의 `place_id`가 일치했다.
- 모든 결과에 `source`, `content_version`, `facts`, `evidence`, `missing_fields`가 포함됐다.
- 근거가 없는 음식점 정책 필드는 `pet_allowed`, `wheelchair_accessible`로 표시됐다.
- 근거가 없는 숙소 반려동물 정책은 `pet_allowed`로 표시됐다.
- 근거가 없는 정책값을 임의로 `true` 또는 `false`로 생성하지 않았다.
- 검색 결과가 없어지는 대신 후보를 유지하고 `INSUFFICIENT_EVIDENCE`로 전달했다.

검색 API 호출 예시:

```http
POST /internal/search/places
X-Grounding-Trace: true
Content-Type: application/json
```

## 3. 저장된 실제 AI E2E 응답 검증

대상 파일: `performance/rag_grounding/live-e2e-response.json`

| 항목 | 결과 |
|---|---:|
| 최종 상태 | `READY` |
| 일정 슬롯 | 3개 |
| 관광지 후보 | 5개 |
| 음식점 후보 | 7개 |
| 누락 슬롯 | 0개 |
| 재시도 | 0회 |
| 품질 점수 | 100점 |
| 최종 source ID | 3개 |
| 근거 부족 안내문 | 12건 |

최종 일정의 관광지 1개와 음식점 2개 모두 후보 데이터의 source ID와 연결됐고, 이름·주소·영업시간이 근거 데이터와 일치했다. 반려동물·휠체어·실내 동반 여부처럼 확인되지 않은 값은 최종 응답에서 확인 필요 안내로 표시됐다.

## 4. 자동 테스트 결과

실행 명령:

```powershell
$env:PYTHONPATH = "performance/rag_grounding"
python -m unittest discover -s performance/rag_grounding -p "test_*.py" -v
```

결과:

- 6개 테스트 통과
- 60개 합성 adversarial grounding 케이스 통과
- 숫자·문서 ID·출처 누락·정책 반전·문서 간 혼합 케이스 탐지 통과

단, 60개 케이스 평가는 합성 데이터 기반 계약 검증이며 실제 운영 LLM의 환각률을 의미하지 않는다.

## 5. 범위와 제한

이번 Compose 구성에는 FastAPI AI 서비스가 포함되어 있지 않아 새 컨테이너에서 `POST /internal/travel/plan`을 재실행하지는 못했다. 따라서 이번 검증은 다음 두 범위로 구분한다.

1. 새로 실행한 실제 데이터 검증: Spring → Elasticsearch 검색 및 Grounding Snapshot
2. 기존 저장 응답 검증: AI 여행 코스 결과의 `READY`, source 연결, 근거 부족 안내

전체 Spring–FastAPI E2E를 재현하려면 별도 `Gangwon-AI` 서비스와 AI 컨테이너를 함께 기동해야 한다.
