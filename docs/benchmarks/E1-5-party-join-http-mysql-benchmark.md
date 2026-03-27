# E1-5 Party Join HTTP + MySQL Benchmark

## 측정 조건
- 실행 일시: 2026-03-27
- 실행 명령: `./scripts/run_party_join_http_mysql_benchmark.sh`
- 측정 대상: `POST /party/{id}/join`
- 서버 구동 방식: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- DB: Docker Compose MySQL 8.4 (`localhost:3307`)
- DB URL: `jdbc:mysql://localhost:3307/partition_mate_http_benchmark?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8`
- 시나리오:
  - 총 동시 요청 300건
  - 파티 총 수량 20개
  - 호스트 선점 수량 1개
  - 일반 사용자 요청 수량은 모두 1개
  - 각 요청은 서로 다른 JWT access token으로 인증

## 측정 결과
- 즉시 참여 성공: 19건
- 잔여 수량 부족으로 거절: 281건
- 초과 모집: 0건
- 중복 참여: 0건
- 평균 응답시간: 557.45ms
- P95 응답시간: 795.11ms
- P99 응답시간: 907.52ms

## 해석
- 실제 HTTP 계층과 MySQL 락 비용을 포함해도 총수량 20개를 넘는 초과 모집은 발생하지 않았다.
- 서비스 계층 + H2 기준 수치보다 응답시간은 커졌지만, 실패 요청도 모두 `409 CONFLICT` 비즈니스 예외로 정리됐다.
- 이번 수치는 로컬 Docker MySQL과 단일 애플리케이션 인스턴스 기준이며, 네트워크 홉과 멀티 인스턴스 경쟁은 포함하지 않는다.
- 애플리케이션 부팅 시 MySQL DDL 로그에 `Specified key was too long; max key length is 3072 bytes` 경고가 남았다. 참여 경로 검증은 성공했지만, 별도 스키마 정리 과제로 추적할 필요가 있다.

## 재실행 메모
- 다른 작업용 MySQL 컨테이너가 `3306`을 사용 중일 수 있어 벤치마크는 기본 포트를 `3307`로 고정했다.
- 실행 스크립트가 `partition-mate-db` 컨테이너를 재생성하고 MySQL health check 이후 테스트를 시작한다.
- 일반 테스트 스위트에는 포함하지 않도록 `RUN_MYSQL_HTTP_BENCHMARK=true` 환경변수가 있을 때만 실행된다.
