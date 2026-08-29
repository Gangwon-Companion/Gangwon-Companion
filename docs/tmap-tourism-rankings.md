# 티맵 기반 인기 관광지·핫플레이스 설계 메모

## 목적

현재 관광 혼잡도(`tourist_congestion_rates`)와 티맵 내비게이션 검색량 기반 순위를 분리한다.

- 인기 관광지: 조회연월의 연령대별 내비게이션 검색건수 합계가 높은 관광지
- 핫플레이스: 조회연월 기준 최근 3개월 검색건수 합계의 전년 동기 대비 증가율이 높은 관광지
- 분석 대상: 문화관광, 자연관광, 역사관광, 레저스포츠, 체험관광

## 산식

인기 관광지의 월 검색량은 관광지별 모든 연령대 검색건수를 합산한다.

```text
monthlySearchCount = sum(searchCountByAgeGroup)
popularityRate = monthlySearchCount / maxMonthlySearchCount * 100
```

핫플레이스 증가율은 조회연월을 포함한 최근 3개월과 전년도 같은 3개월을 비교한다.

```text
current = sum(M, M-1, M-2)
previous = sum(M-12, M-13, M-14)
growthRate = (current - previous) / previous * 100
```

전년도 합계가 0인 데이터는 별도 신규 급상승 규칙을 적용하고, 작은 표본이 상위권을 왜곡하지 않도록 최소 검색건수 기준을 둔다.

## 권장 저장 구조

원천 월별·연령대별 통계와 계산된 순위를 분리한다.

```text
tourism_navigation_stats
- destination_id (nullable)
- external_place_id
- place_name
- region_code
- category_code
- base_month (YYYYMM)
- age_group
- navigation_search_count
- source
- raw_payload

tourism_rankings
- destination_id
- base_month
- ranking_type (POPULAR, HOT)
- rank
- score
- current_search_count
- previous_search_count
- growth_rate
- calculated_at
```

외부 관광지와 내부 `destinations` 연결은 공통 ID, 장소명과 시군구의 정확 일치, 장소명 유사도와 좌표 거리 순으로 수행한다. 낮은 신뢰도의 자동 매칭은 확정하지 않는다.

## API 제안

```text
GET /api/v1/tourism-rankings/popular?baseMonth=202608&region=강릉&limit=10
GET /api/v1/tourism-rankings/hot?baseMonth=202608&region=강릉&limit=10
GET /api/v1/tourism-rankings/{destinationId}/history
```

기존 `/api/v1/promotions/hotplace`의 관광 혼잡도와 홈 화면의 하드코딩 데이터는 이 통계와 의미가 다르므로 API와 표시 명칭을 구분한다.

## 구현 전 확인 사항

- 티맵 데이터가 원본 검색건수인지 계산된 비율·증가율인지 확인
- 실제 응답 필드, 관광지 식별자, 좌표, 갱신 주기 확인
- 티맵 카테고리와 TourAPI `lclsSystem` 코드의 호환 여부 확인
- 최소 검색건수 및 전년도 검색량 0 처리 정책 결정

