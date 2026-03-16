# E2-5 Store Query Performance Comparison

## 측정 조건
- 실행 일시: 2026-03-15
- baseline 명령: `./scripts/run_store_query_baseline.sh`
- optimized 명령: `./scripts/run_store_query_optimized_benchmark.sh`
- 프로필: `test`
- 실행 방식: `MockMvc`
- DB: H2 in-memory
- 데이터셋:
  - 지점 300개
  - 지점당 파티 6개
  - 총 파티 1800개
- 반복 횟수:
  - baseline: 워밍업 5회 + 측정 20회
  - optimized: cold 20회 + warm 20회

## 측정 결과
### 주변 지점 조회 API
- baseline
  - 평균 응답시간: `71.78ms`
  - P95 응답시간: `81.07ms`
- optimized cold path
  - 평균 응답시간: `12.49ms`
  - P95 응답시간: `12.20ms`
  - baseline 대비 평균 `82.60%` 감소
  - baseline 대비 P95 `84.95%` 감소
- optimized warm path
  - 평균 응답시간: `1.64ms`
  - P95 응답시간: `2.26ms`
  - 캐시 hit ratio: `0.95`
  - baseline 대비 평균 `97.72%` 감소
  - baseline 대비 P95 `97.21%` 감소

### 지점별 파티 조회 API
- baseline
  - 평균 응답시간: `1.78ms`
  - P95 응답시간: `2.69ms`
- optimized cold path
  - 평균 응답시간: `2.12ms`
  - P95 응답시간: `1.79ms`
  - baseline 대비 평균 `19.10%` 증가
  - baseline 대비 P95 `33.46%` 감소
- optimized warm path
  - 평균 응답시간: `0.48ms`
  - P95 응답시간: `0.86ms`
  - 캐시 hit ratio: `0.95`
  - baseline 대비 평균 `73.03%` 감소
  - baseline 대비 P95 `68.03%` 감소

## 해석
- Epic 2의 핵심 병목이던 주변 지점 조회는 쿼리화와 인덱스 적용만으로도 cold path 평균을 `71.78ms -> 12.49ms`로 크게 줄였다.
- 캐시가 붙은 warm path에서는 같은 좌표 재조회 기준 평균 `1.64ms`, P95 `2.26ms`까지 내려가 홈/지점 선택 화면 재진입 성능이 크게 개선됐다.
- 지점별 파티 조회는 cold path 평균이 baseline보다 소폭 증가했지만, P95는 개선됐고 warm path에서는 평균 `0.48ms`까지 줄었다.
- 따라서 Epic 2의 성과는 `주변 지점 조회 병목 제거`와 `반복 조회 warm path 최적화`에 가장 크게 나타났다고 보는 것이 정확하다.
