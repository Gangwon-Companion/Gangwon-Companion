# Travel Type 16 — BE 구현 및 운영 명세

## 구현 상태

- 상태: 구현 완료
- 지원 분석 버전: `travel-type-16-v1`
- 데이터 수집, 비동기 분석 Job, 결과 검증·저장·조회, 추천 컨텍스트 전달 구현
- 프로필 GET 응답에 `Cache-Control: no-store` 적용

## 책임

1. 검색·방문·저장 코스·리뷰 기록 수집
2. AI 내부 분석 API 호출
3. 16유형·축별 점수·신뢰도 검증
4. 사용자별 최신 프로필 저장
5. FE 조회·Job API 제공
6. 유효한 프로필을 코스 추천 AI에 전달

## 허용 유형

신규 분석 결과는 다음 16개만 허용한다.

```text
CAPF CAPH CASF CASH CRPF CRPH CRSF CRSH
NAPF NAPH NASF NASH NRPF NRPH NRSF NRSH
```

엔티티에는 기존 DB 행 조회 호환을 위해 과거 6개 Enum도 유지한다. AI 응답 DTO에는 신규 16개만 선언해 신규 분석에서 과거 유형이 들어오지 못하게 한다.

## 저장 모델

기존 `travel_profiles`에 다음 nullable 컬럼을 추가한다.

```text
space_city_score
space_nature_score
activity_active_score
activity_rest_score
schedule_planned_score
schedule_spontaneous_score
place_famous_score
place_hidden_score
```

각 점수는 0~100이고 축별 합계가 100이어야 한다.

## API 계약

### 프로필 조회

```http
GET /api/v1/users/me/travel-profile
```

- 지원 버전 `travel-type-16-v1` 결과만 노출한다.
- 응답 캐시를 금지한다.
- 과거 버전만 있으면 `NOT_ANALYZED`를 반환한다.

### 분석 Job 생성

```http
POST /api/v1/users/me/travel-profile/analysis-jobs
```

### 분석 Job 조회

```http
GET /api/v1/users/me/travel-profile/analysis-jobs/{jobId}
```

### AI 응답 검증

- 완료 결과에 유형, 제목, 설명, 신뢰도, 네 축 점수 필수
- 키는 `C/N`, `A/R`, `P/S`, `F/H` 정확히 두 개씩 필요
- 점수 범위 0~100, 축별 합계 100
- 알 수 없는 유형은 역직렬화 단계에서 거부

## 코스 추천 연동

다음 조건을 모두 만족하는 프로필만 `travel_profile`로 전달한다.

- 상태 `COMPLETED`
- 분석 시각이 TTL 이내
- 분석 버전 `travel-type-16-v1`
- 유형과 신뢰도 존재

내부 JSON에서는 `traveler_type`, `axis_scores`, `analysis_version`처럼 snake_case를 사용한다.

## DB 마이그레이션

`ddl-auto: update`는 새 점수 컬럼을 추가할 수 있지만 기존 Enum 체크 제약조건을 자동 확장하지 않는다. 기존 DB에는 아래 SQL을 한 번 적용해야 한다.

```sql
BEGIN;

ALTER TABLE travel_profiles
DROP CONSTRAINT IF EXISTS travel_profiles_traveler_type_check;

ALTER TABLE travel_profiles
ADD CONSTRAINT travel_profiles_traveler_type_check
CHECK (
    traveler_type IN (
        'CAPF', 'CAPH', 'CASF', 'CASH',
        'CRPF', 'CRPH', 'CRSF', 'CRSH',
        'NAPF', 'NAPH', 'NASF', 'NASH',
        'NRPF', 'NRPH', 'NRSF', 'NRSH',
        'NATURE_HEALING', 'PET_COMPANION',
        'LOCAL_FOOD_EXPLORER', 'ACTIVITY_ADVENTURE',
        'CULTURE_EXPLORER', 'BALANCED_TRAVELER'
    )
);

COMMIT;
```

로컬과 AWS RDS의 기존 DB 모두 동일하게 적용한다. 운영에서는 적용 전 스냅샷을 생성하고 장기적으로 Flyway 도입을 권장한다.

## 실제 장애 기록

### AI 연결 실패

```text
errorCode=AI_SERVICE_UNAVAILABLE
```

Docker BE는 Compose 기본 설정상 `http://host.docker.internal:8001`로 AI를 호출한다. AI를 `127.0.0.1:8000`으로만 열면 컨테이너가 접근할 수 없다.

해결:

```powershell
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

### 신규 유형 저장 실패

```text
SQLState: 23514
violates check constraint "travel_profiles_traveler_type_check"
```

원인: 기존 6유형 체크 제약조건이 `CAPF`를 거부했다. 위 DB 마이그레이션으로 해결한다.

### 저장됐지만 재진입 시 미분석 표시

DB 확인 예시:

```sql
SELECT id, status, traveler_type, analysis_version, analyzed_at
FROM travel_profiles;
```

DB가 `COMPLETED / CAPF / travel-type-16-v1`인데 API가 `NOT_ANALYZED`라면 구버전 이미지 또는 분석 버전 필터를 확인한다. 현재 코드는 지원 버전을 상수로 확인해 외부 설정의 구버전 값이 신규 결과를 숨기지 않도록 한다.

프로필 GET은 `Cache-Control: no-store`를 반환한다.

## 실행 및 검증

```powershell
.\gradlew.bat compileJava compileTestJava
docker compose up -d --build spring
docker logs --tail 100 spring_gangwon
```

기존 실패 Job은 원인 해결 후 새로 생성한다.
