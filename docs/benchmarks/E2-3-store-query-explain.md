# E2-3 Store Query Explain

## 측정 조건
- 실행 일시: 2026-03-15
- 실행 명령: `./mvnw -Dtest=StoreQueryIndexIntegrationTest test`
- 프로필: `test`
- DB: H2 in-memory
- 확인 대상:
  - `store(latitude, longitude)` 범위 조회
  - `party(store_id, party_status)` 조건 조회

## EXPLAIN 결과 요약
- `store` 조회 계획
  - `IDX_STORE_LATITUDE_LONGITUDE` 인덱스를 사용했다.
  - 범위 조건: `latitude between 37.4 and 37.7`, `longitude between 127.0 and 127.3`
- `party` 조회 계획
  - `IDX_PARTY_STORE_STATUS` 인덱스를 사용했다.
  - 조건: `store_id = 1`, `party_status = 'RECRUITING'`

## 원문 일부
```text
STORE_QUERY_EXPLAIN storePlan=... PUBLIC.IDX_STORE_LATITUDE_LONGITUDE ...
STORE_QUERY_EXPLAIN partyPlan=... PUBLIC.IDX_PARTY_STORE_STATUS ...
```

## 해석
- Epic 2에서 추가한 두 복합 인덱스가 테스트 환경의 `EXPLAIN` 결과에 직접 노출되어, 조회 쿼리가 인덱스 경로를 타고 있음을 확인했다.
- 이 결과는 E2-2 쿼리화 이후 cold path 성능 개선이 단순 캐시 효과만이 아니라, 인덱스 설계와 함께 작동하고 있음을 보여준다.
