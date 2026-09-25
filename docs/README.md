# 프로젝트 작업 문서

## AI 여행 취향 프로필

- [통합 구현 및 E2E 체크리스트](design/AI_TRAVEL_PROFILE_INTEGRATION.md): BE·AI·FE 공통 계약, 갱신 정책, 추천 개인화와 남은 검증
- [BE 구현 명세](design/AI_TRAVEL_PROFILE_BE.md): 활동 수집, 비동기 분석 Job, 저장·만료 및 추천 주입
- [AI 구현 명세](design/AI_TRAVEL_PROFILE_AI.md): LLM-first 분석, 검증과 결정론적 fallback
- [FE 구현 명세](design/AI_TRAVEL_PROFILE_FE.md): 마이페이지 카드, 상태별 UI와 Polling

## 코스추천 및 관광 데이터

- [여행 코스추천 E2E 현황](reports/course-recommendation-e2e-status.md): 완료된 연결·검증 결과, 재현 방법, 실패 원인, 남은 작업
- [티맵 기반 인기 관광지·핫플레이스 설계](design/tmap-tourism-rankings.md): 인기 관광지와 핫플레이스 산식, 데이터 모델 및 API 제안

## 검색 및 Elasticsearch

- [하이브리드 검색 및 RAG 환각 방지 개발 계획](plans/RAG_GROUNDING_DEVELOPMENT_PLAN.md): 벡터 검색·근거 검증 설계, A/B/C/D 전후 비교와 평가 기준 (개발 전)
- [RAG Grounding 실제 데이터 E2E 검증](reports/RAG_GROUNDING_REAL_E2E_20260908.md): Docker 기반 실제 검색·근거 snapshot·AI E2E 검증 결과
- [RAG Grounding 실제 데이터 E2E HTML 리포트](reports/RAG_GROUNDING_REAL_E2E_20260908.html): 브라우저용 검증 대시보드
- [검색·AI 통합 후속 작업](plans/SEARCH_INTEGRATION_NEXT_STEPS.md): 코드 기준 통합 현황과 후속 작업
- [Elasticsearch 장소 검색](design/SEARCH_ELASTICSEARCH.md): 실행 설정, 재색인, alias 및 테스트 절차
- [검색 문서 개선 진행상황](design/SEARCH_DOCUMENT_ENHANCEMENT.md): 검색 문서와 ranking 개선 내역
- [Kafka 기반 검색 색인](design/SEARCH_INDEXER.md): CDC 증분 색인 구현 및 검증 결과
- [검색 CDC 파이프라인](design/SEARCH_CDC_KAFKA_PIPELINE.md): PostgreSQL부터 Elasticsearch까지의 이벤트 흐름
- [로컬 동기화 및 검색 문제해결](operations/LOCAL_DATA_SYNC_AND_SEARCH_TROUBLESHOOTING.md): 로컬 통합 실행 절차와 장애 대응
