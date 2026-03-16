# ADR-20260315-store-query-performance-evaluation

## 배경
- Epic 2는 이력서와 포트폴리오에 사용할 수 있는 성능 수치를 남기는 것이 목표라, 기준값과 개선값을 같은 방식으로 비교할 필요가 있었다.
- E2-1에서 이미 H2 + MockMvc + 300개 지점/1800개 파티 데이터셋 기준 베이스라인을 확보했기 때문에, E2-5에서도 같은 데이터셋과 실행 방식을 유지해야 했다.
- 또한 이번 배치는 쿼리 최적화와 캐시가 함께 들어가므로 cold path와 warm path를 분리해서 해석해야 했다.

## 결정사항
- E2-5는 `StoreQueryOptimizedBenchmarkTest`와 전용 스크립트로 측정하고, 같은 데이터셋에서 cold path와 warm path를 모두 기록한다.
- 주변 지점 조회는 `nearbyCold`, `nearbyWarm`, 지점별 파티 조회는 `storePartiesCold`, `storePartiesWarm`으로 나눠 평균과 P95를 기록한다.
- 캐시 효과는 Caffeine stats 기준 hit ratio로 함께 남긴다.
- 전후 비교 문서에는 baseline 대비 개선폭과, cold path/ warm path 각각의 의미를 분리해서 적는다.

## 대안
- MySQL/Docker 환경만 기준으로 재측정
  - 장점: 운영 DB에 더 가깝다.
  - 단점: 현재 CI와 로컬 재현성이 떨어지고, Epic 2 내부 비교 기준이 흐려진다.
- warm path만 측정
  - 장점: 캐시 효과를 강하게 보여주기 쉽다.
  - 단점: 쿼리 최적화 자체의 효과와 cold path 특성이 가려진다.
- JMeter나 k6로 별도 부하 시나리오 작성
  - 장점: 더 다양한 관점의 수치를 얻을 수 있다.
  - 단점: Epic 2 범위 대비 준비 비용이 커지고, E1과 달리 조회 벤치마크를 빠르게 반복하기 어렵다.

## 결과
- 긍정적
  - baseline, cold path, warm path를 한 문맥에서 비교할 수 있어 개선 효과를 설명하기 쉬워졌다.
  - cache hit ratio를 함께 남겨 warm path 수치가 우연한 값이 아니라는 근거를 추가했다.
- 부정적
  - H2 + MockMvc 측정은 실제 MySQL/AWS 운영 환경과 완전히 같지 않다.
  - 지점별 파티 조회는 cold path 평균이 baseline보다 소폭 증가해, 모든 지표가 일괄 개선된 것은 아니다.
- 중립적
  - Epic 2에서는 재현성과 비교 가능성을 우선했고, 이후 CI/CD 또는 AWS 단계에서 운영 환경 벤치마크를 별도 문서로 확장하면 된다.
