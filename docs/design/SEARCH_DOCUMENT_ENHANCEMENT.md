# Elasticsearch 검색 문서 개선 진행상황

## 1. 작업 목적

AI 에이전트가 search tool을 호출했을 때 더 정확한 장소 후보를 받을 수 있도록
Elasticsearch 검색 문서(`PlaceSearchDocument`)를 1차 확장했다.

이번 작업은 검색 랭킹 자체를 크게 바꾸기보다, 팀원이 이후 ranking/boost 튜닝에서
활용할 수 있는 문서 필드를 준비하는 단계다.

## 2. 기존 문서 필드

기존 `PlaceSearchDocument`는 다음 필드로 구성되어 있었다.

```text
placeId
domain
name
address
regionCode
searchText
location
petAllowed
smallPetAllowed
mediumPetAllowed
largePetAllowed
wheelchairAccessible
source
evidenceFields
```

기존 구조는 공통 장소 검색에는 충분하지만, 테마, 메뉴, 평점, 가격, 반려동물 상세,
무장애 상세 같은 정보를 별도 필드로 다루기 어려웠다.

## 3. 1차 추가 필드

이번에 추가한 필드는 다음과 같다.

```text
themeName
menuType
rating
price
petInfoText
accessibilityInfoText
updatedAt
documentVersion
```

### themeName

여행지의 서비스 테마 이름이다.

- 대상: `DESTINATION`
- 원본: `Destination.theme.name`
- 용도: 자연, 체험, 역사 등 테마 기반 검색과 ranking 튜닝

`themeCode`, `contentTypeId`도 후보였지만 1차에서는 의미가 가장 직관적인
`themeName`만 추가했다.

### menuType

음식점의 음식 종류다.

- 대상: `RESTAURANT`
- 원본: `Restaurant.menuType`
- 예시: 한식, 중식, 일식, 서양식, 카페, 분식
- 용도: 음식 종류 검색과 ranking 튜닝

### rating

음식점과 숙소의 평점이다.

- 대상: `RESTAURANT`, `LODGING`
- 원본: `Restaurant.rating`, `Lodging.rating`
- 용도: 관련도가 비슷한 후보 사이에서 평점 기반 보조 ranking 후보

주의: 현재 TourAPI 동기화 코드에서는 음식점/숙소 생성 시 기본값이 `0.0`으로 들어간다.
실제 DB에 의미 있는 평점 분포가 있는지 확인한 뒤 ranking에 강하게 반영해야 한다.

### price

숙소 가격이다.

- 대상: `LODGING`
- 원본: `Lodging.price`
- 용도: 예산 기반 숙소 검색 또는 필터 확장 후보

주의: 현재 숙소 동기화 코드에서는 기본값이 `0L`로 들어간다. 실제 가격 데이터가 채워져
있는지 확인한 뒤 검색 필터나 ranking에 사용해야 한다.

### petInfoText

반려동물 상세 텍스트를 합친 필드다.

- 대상: `DESTINATION`
- 원본:
  - `PetInfo.accompanyType`
  - `PetInfo.needItems`
  - `PetInfo.petFacilities`
  - `PetInfo.caution`
  - `PetInfo.accidentRisk`
- 용도: 목줄, 케이지, 반려견 동반, 펫 시설 등 반려동물 관련 검색 품질 개선

### accessibilityInfoText

무장애/접근성 상세 텍스트를 합친 필드다.

- 대상: `DESTINATION`
- 원본:
  - `AccessibilityInfo.parking`
  - `AccessibilityInfo.route`
  - `AccessibilityInfo.entrance`
  - `AccessibilityInfo.elevator`
  - `AccessibilityInfo.restroom`
  - `AccessibilityInfo.wheelchair`
  - `AccessibilityInfo.braileBlock`
  - `AccessibilityInfo.helpDog`
  - `AccessibilityInfo.guideHuman`
- 용도: 휠체어, 엘리베이터, 장애인 화장실, 보조견 등 무장애 관련 검색 품질 개선

### updatedAt

검색 문서 원본의 갱신 시각이다.

- `DESTINATION`: `Destination.updatedAt`
- `RESTAURANT`: `Restaurant.createdAt`
- `LODGING`: `Lodging.createdAt`
- 용도: 이후 CDC/Search Indexer 단계에서 최신성 판단 후보

현재 음식점/숙소 엔티티에는 `updatedAt`이 없어서 `createdAt`을 사용한다.

### documentVersion

Elasticsearch 검색 문서 스키마 버전이다.

- 현재 값: `2`
- 용도: 이후 문서 스키마 변경, 재색인, 증분 색인 과정에서 문서 버전 식별

## 4. searchText 구성 변경

`searchText`는 여러 텍스트 필드를 한 번에 검색하기 위한 범용 검색 필드다.
별도 필드는 정교한 boost와 필터에 좋고, `searchText`는 넓게 후보를 잡는 데 좋다.

### DESTINATION

기존:

```text
title + addr1 + addr2 + themeName + overview
```

변경:

```text
title + addr1 + addr2 + themeName + overview + petInfoText + accessibilityInfoText
```

반려동물/무장애 상세 텍스트를 `searchText`에도 포함해 자연어 검색에서 더 잘 걸리도록 했다.

### RESTAURANT

기존:

```text
name + menuType + address
```

변경:

```text
name + menuType + region + address
```

지역명도 검색 텍스트에 포함했다.

### LODGING

기존:

```text
name + description + address
```

변경:

```text
name + description + region + address
```

지역명도 검색 텍스트에 포함했다.

## 5. Elasticsearch mapping 변경

새 필드는 strict mapping에 추가했다.

```text
themeName               text
menuType                text
rating                  double
price                   long
petInfoText             text
accessibilityInfoText   text
updatedAt               date
documentVersion         integer
```

`themeName`, `menuType`, `petInfoText`, `accessibilityInfoText`는 기존 `name`,
`searchText`와 같은 Nori analyzer 기반 text mapping을 사용한다.

## 6. 이번에 제외한 후보

### imageUrl

대표 이미지는 검색 정확도보다는 프론트 표시용 성격이 강해서 1차 검색 문서 품질 개선에서는 제외했다.

### overview 별도 필드

여행지 overview는 이미 `searchText`에 포함되어 있다. 1차에서는 별도 필드로 분리하지 않았다.
나중에 overview만 별도 boost를 주고 싶다면 추가할 수 있다.

### themeCode, contentTypeId

1차에서는 `themeName`만 추가했다. 코드 기반 필터가 필요해지면 `themeCode` 또는
`contentTypeId`를 추가한다.

## 7. 팀원이 이어서 활용할 수 있는 부분

ranking/query 튜닝에서 다음 필드를 활용할 수 있다.

```text
themeName
menuType
rating
price
petInfoText
accessibilityInfoText
```

예시 방향:

```text
themeName 매칭은 여행지 테마 의도가 강할 때 boost
menuType 매칭은 음식점 검색에서 boost
petInfoText 매칭은 반려동물 요청일 때 boost
accessibilityInfoText 매칭은 무장애 요청일 때 boost
rating은 실제 값 분포 확인 후 보조 점수로 사용
price는 실제 값 분포 확인 후 예산 필터로 사용
```

## 8. 다음 단계

추천 다음 작업:

```text
1. ElasticsearchPlaceSearchEngine에서 field boost 후보 검토
2. hard filter null 정책 통일
3. 검색 품질 평가 세트 작성
4. Kafka KRaft + Debezium CDC 앞단 구축
```

## 9. 재색인 검증 결과

현재 코드로 Docker 환경에서 재색인을 실행했고, Elasticsearch alias 전환과 새 필드 저장을 확인했다.

실행 환경:

```text
Spring container: spring_gangwon
PostgreSQL container: postgres_gangwon
Elasticsearch container: elasticsearch_gangwon
SEARCH_ENGINE=elasticsearch
```

재색인 API:

```text
POST /internal/search/index/rebuild
```

재색인 결과:

```text
index: gangwon-places-v1-20260824113202323
sourceCount: 3561
indexedCount: 3561
failedIds: []
retriedIds: []
aliasSwitched: true
retryCount: 0
```

alias 확인:

```text
gangwon-places -> gangwon-places-v1-20260824113202323
is_write_index: true
```

문서 수 확인:

```text
gangwon-places count: 3561
```

mapping 확인:

```text
themeName               text
menuType                text
rating                  double
price                   long
petInfoText             text
accessibilityInfoText   text
updatedAt               date
documentVersion         integer
```

샘플 문서 확인:

```text
DESTINATION
- themeName 저장 확인
- updatedAt 저장 확인
- documentVersion=2 확인

RESTAURANT
- menuType 저장 확인
- rating 저장 확인
- updatedAt 저장 확인
- documentVersion=2 확인

LODGING
- rating 저장 확인
- price 저장 확인
- updatedAt 저장 확인
- documentVersion=2 확인
```

반려동물/무장애 텍스트 확인:

```text
petInfoText 보유 문서 수: 84
accessibilityInfoText 보유 문서 수: 346
```

예시:

```text
옥계5일장
- petInfoText: 일부구역 동반가능
- searchText에도 petInfoText 포함 확인

가원습지 생태자연공원
- accessibilityInfoText: 출입구까지 턱이 없어 휠체어 접근 가능함 동반가능_시각장애인 편의시설
- searchText에도 accessibilityInfoText 포함 확인
```

주의:

```text
rating은 음식점/숙소 문서에 들어가지만 현재 샘플은 0.0이었다.
price는 숙소 문서에 들어가지만 현재 샘플은 0이었다.
ranking이나 예산 필터에 사용하기 전 실제 DB 값 분포를 먼저 확인해야 한다.
```

DB 분포 확인 결과:

```text
restaurants
- total: 1713
- rating_present: 1713
- rating_positive: 0
- min_rating: 0
- max_rating: 0
- avg_rating: 0

lodgings
- total: 702
- rating_present: 702
- rating_positive: 0
- min_rating: 0
- max_rating: 0
- avg_rating: 0
- price_present: 702
- price_positive: 0
- min_price: 0
- max_price: 0
- avg_price: 0
```

결론:

```text
rating, price 필드는 문서 스키마에 포함했지만 현재 데이터로는 ranking이나 예산 필터에
바로 사용하면 안 된다. 실제 평점/가격 적재 로직이 추가된 뒤 활용하는 것이 안전하다.
```

## 10. 검색 샘플 검증 결과

확장된 필드와 변경된 `searchText`가 실제 검색 결과에 반영되는지
`POST /internal/search/places`로 확인했다.

주의:

```text
현재 ElasticsearchPlaceSearchEngine의 multi_match 대상 필드는
name, name.english, searchText, searchText.english, address다.

즉 themeName, menuType, petInfoText, accessibilityInfoText는 별도 boost 필드로
직접 검색되는 것이 아니라, 현재 단계에서는 searchText에 포함되어 검색된다.

팀원이 ranking 개선을 할 때 이 필드들을 multi_match 대상이나 should boost 대상으로
추가하면 더 정교하게 활용할 수 있다.
```

### themeName 검색

요청:

```json
{
  "domain": "DESTINATION",
  "slot": "VERIFY_THEME",
  "query_text": "체험관광",
  "limit": 3
}
```

결과:

```text
1. 오호어촌체험마을
2. 화랑도체험장
3. 장호어촌체험마을
```

추가 확인:

```text
themeName=쇼핑 직접 조회 시 77개 문서 확인
예시: 간성전통시장, 강릉 농산물도매시장, 강릉 동부시장
```

판단:

```text
themeName은 ES 문서에 저장되어 있고 searchText에도 포함되어 있다.
테마 의도 검색에서 ranking boost 후보로 사용할 수 있다.
```

### menuType 검색

요청:

```json
{
  "domain": "RESTAURANT",
  "slot": "VERIFY_MENU",
  "query_text": "한식",
  "limit": 3
}
```

결과:

```text
1. 썬한식
2. 사임당한식뷔페
3. 곰배령
```

원문 확인:

```text
곰배령
- menuType: 한식
- searchText: 곰배령 한식 춘천 강원특별자치도 춘천시 춘천로 19
```

판단:

```text
음식점 menuType은 문서 필드와 searchText에 모두 반영된다.
음식 종류 검색에서 boost 후보로 사용할 수 있다.
```

### petInfoText 검색

요청:

```json
{
  "domain": "DESTINATION",
  "slot": "VERIFY_PET_LEASH",
  "query_text": "목줄",
  "limit": 3
}
```

결과:

```text
1. 별빛 반려견놀이터
2. 애견전용해수욕장 멍비치
3. 거진어촌체험휴양마을
```

원문 확인:

```text
별빛 반려견놀이터
- petInfoText: 일부구역 동반가능 목줄 착용 펜스, 안전문, 산책로 대형견 가능 (25kg이상) 없음
- searchText에도 petInfoText 포함 확인
```

판단:

```text
petInfoText는 반려동물 상세 조건 검색에 효과가 있다.
petAllowed hard filter와는 역할이 다르다.

petAllowed:
- 반려동물 동반 가능 여부 판단

petInfoText:
- 목줄, 케이지, 시설, 주의사항 같은 상세 검색어 매칭
```

### accessibilityInfoText 검색

요청:

```json
{
  "domain": "DESTINATION",
  "slot": "VERIFY_ACCESSIBILITY",
  "query_text": "휠체어 접근",
  "limit": 3
}
```

결과:

```text
1. 주문진 관광안내센터
2. 강릉종합사회복지관
3. 강릉갤럭시
```

원문 확인:

```text
주문진 관광안내센터
- accessibilityInfoText: 출입구에 턱이 없어 휠체어 접근 가능 300번 버스 작은다리 하차 후 도보 5분 대여위치 : 건물 입구, 휠체어 종류: 수동 휠체어, 개수 : 1대
- searchText에도 accessibilityInfoText 포함 확인
```

추가 요청:

```json
{
  "domain": "DESTINATION",
  "slot": "VERIFY_ACCESSIBILITY_ELEVATOR",
  "query_text": "엘리베이터",
  "limit": 3
}
```

결과:

```text
1. 강릉교회
2. 연세대학교 원주박물관
3. 주문진문화교육센터
```

판단:

```text
accessibilityInfoText는 휠체어, 엘리베이터, 화장실, 보조견 같은 무장애 상세 검색에 활용 가능하다.
wheelchairAccessible hard filter와는 역할이 다르다.

wheelchairAccessible:
- 휠체어 접근 가능 여부 판단

accessibilityInfoText:
- 엘리베이터, 화장실, 보조견, 출입구 같은 상세 검색어 매칭
```

## 11. 팀원에게 넘길 boost 후보

현재 문서 스키마 기준으로 ranking/query 튜닝에서 검토할 수 있는 후보는 다음과 같다.

```text
DESTINATION
- themeName: 테마 의도가 명확한 검색에서 boost 후보
- petInfoText: 반려동물 상세 요청에서 boost 후보
- accessibilityInfoText: 무장애 상세 요청에서 boost 후보

RESTAURANT
- menuType: 음식 종류 검색에서 boost 후보

LODGING
- rating: 현재 데이터가 모두 0이라 보류
- price: 현재 데이터가 모두 0이라 보류
```

추천 방향:

```text
1. name은 계속 가장 높은 가중치 유지
2. themeName/menuType은 domain별 의도와 맞을 때 별도 should boost 후보
3. petInfoText/accessibilityInfoText는 hard filter와 함께 쓰일 때 보조 boost 후보
4. rating/price는 실제 데이터 적재 전까지 ranking/filter에 사용하지 않음
```

## 12. A-1 완료 기준

담당자 A의 검색 문서 품질 개선 작업은 다음 기준을 충족했다.

```text
PlaceSearchDocument 확장 완료
Elasticsearch strict mapping 확장 완료
PlaceSearchDocumentAssembler 문서 생성 로직 수정 완료
searchText 구성 개선 완료
Elasticsearch 관련 테스트 통과
Docker 환경 재색인 성공
alias 전환 성공
새 필드 샘플 문서 저장 확인
새 필드 기반 검색 샘플 검증 완료
rating/price 데이터 분포 확인 완료
팀원 인수인계 문서 작성 완료
```
