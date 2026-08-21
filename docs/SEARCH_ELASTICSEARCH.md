# Elasticsearch 장소 검색

## 실행

```powershell
docker compose build elasticsearch
docker compose up -d elasticsearch
```

로컬 컨테이너는 Elasticsearch 9.5.0과 `analysis-nori` 플러그인을 사용한다.
로컬에서만 보안 기능을 끈다. 운영 환경은 TLS와 API Key를 사용한다.

## 설정

```text
SEARCH_ENGINE=rdb | elasticsearch
ELASTICSEARCH_URL=http://localhost:9200
ELASTICSEARCH_API_KEY=
ELASTICSEARCH_REINDEX_KEY=
```

기본 엔진은 `rdb`다. 전체 색인과 alias 전환이 끝난 뒤
`SEARCH_ENGINE=elasticsearch`로 변경한다.

## 검색 문서

인덱스 이름은 `gangwon-places-v1-<UTC timestamp>`, 검색 alias는
`gangwon-places`다. 문서는 Destination, Restaurant, Lodging aggregate를
공통 형태로 저장한다.

- `placeId`, `domain`, `regionCode`, `source`: `keyword`
- `name`, `address`, `searchText`: Nori 분석기와 영문 standard multi-field
- `location`: `geo_point`
- 반려동물·휠체어 파생값: nullable `boolean`
- `embedding`: 차원 자동 추론 `dense_vector` 예약 필드

현재 Phase 4 검색은 BM25, exact filter, Geo만 사용한다. `embedding` 생성과 kNN,
RRF는 Phase 6에서 구현한다.

## 전체 재색인

```text
POST /internal/search/index/rebuild
X-Search-Reindex-Key: <ELASTICSEARCH_REINDEX_KEY>
```

재색인은 다음 순서로 동작한다.

1. 새 버전 인덱스 생성
2. RDB aggregate 전체 조회
3. 500건 단위 Bulk 색인
4. 실패 문서 1회 재시도
5. RDB aggregate 수와 Elasticsearch 문서 수 비교
6. 수가 일치할 때만 alias 원자 전환

실패 문서가 남거나 문서 수가 다르면 alias를 전환하지 않는다. 이전 인덱스는 자동으로
삭제하지 않으므로 확인 후 별도 보존 정책에 따라 제거한다.

## 테스트

일반 전체 테스트에서는 실제 Elasticsearch 통합 테스트를 제외한다. 실제 컨테이너를
사용하는 검증은 다음 환경변수를 설정하고 실행한다.

```powershell
$env:ELASTICSEARCH_INTEGRATION_URL = "http://localhost:9200"
.\gradlew.bat test --tests com.gangwon.companion.domain.search.elasticsearch.*
```
