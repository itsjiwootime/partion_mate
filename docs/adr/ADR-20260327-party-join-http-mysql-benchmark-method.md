# ADR-20260327-party-join-http-mysql-benchmark-method

- 날짜: 2026-03-27
- 상태: Accepted
- 관련 작업: `E1-5`
- 관련 파일: `src/test/java/com/project/partition_mate/service/PartyJoinHttpMysqlBenchmarkTest.java`, `scripts/run_party_join_http_mysql_benchmark.sh`, `docs/benchmarks/E1-5-party-join-http-mysql-benchmark.md`

## 배경
- 기존 동시 참여 수치는 `PartyService.joinParty`를 직접 호출하는 H2 in-memory 서비스 벤치마크 기준이었다.
- 이력서와 블로그에 정량 수치를 남기려면, 실제 HTTP 계층과 MySQL 락 비용까지 포함한 운영 유사 측정값이 추가로 필요했다.
- 다만 일반 테스트 스위트에 MySQL 의존성을 섞으면 로컬과 CI 실행 안정성이 떨어지므로, opt-in 방식의 재현 경로가 필요했다.

## 결정사항
- `POST /party/{id}/join`을 실제로 호출하는 `RANDOM_PORT` 기반 HTTP 벤치마크 테스트를 별도로 추가한다.
- 이 테스트는 `RUN_MYSQL_HTTP_BENCHMARK=true` 환경변수로만 실행되게 두고, 별도 실행 스크립트에서 Docker Compose MySQL 8.4를 `3307` 포트로 띄운 뒤 수행한다.
- 측정 대상은 `300건 동시 요청`, `총 수량 20`, `호스트 선점 1`, `사용자별 JWT 인증` 시나리오로 고정하고, 성공/실패 건수와 평균/P95/P99를 함께 기록한다.

## 대안
- 기존 H2 서비스 벤치마크만 유지한다.
  - 장점: 빠르고 안정적이며 테스트 환경이 단순하다.
  - 단점: HTTP 직렬화, 서블릿 필터, JWT 인증, MySQL 락 비용이 빠져 운영 유사 수치로 설명하기 어렵다.
- MockMvc + H2 기반으로 컨트롤러까지 확장한다.
  - 장점: HTTP 요청/응답 형태는 검증할 수 있고 Docker 의존성이 없다.
  - 단점: 실제 네트워크 소켓과 MySQL 동시성 비용이 빠져 핵심 보강 포인트가 남는다.
- Testcontainers 기반 MySQL 테스트를 도입한다.
  - 장점: 테스트 격리와 재현성이 좋고 포트 충돌 관리가 쉽다.
  - 단점: 현재 저장소는 Docker Compose 기반 로컬 DB 운영 문서를 이미 사용 중이며, 이번 목적은 빠른 운영 유사 측정 경로 확보라서 설정 비용이 더 크다.

## 결과
- 긍정적 트레이드오프를 작성한다.
  - 이력서와 문서에 H2 서비스 수치와 HTTP + MySQL 수치를 분리해 설명할 수 있게 됐다.
  - 실제 `Authorization` 헤더, JWT 필터, 컨트롤러, 트랜잭션, MySQL 락까지 포함한 재현 경로가 생겼다.
- 부정적 트레이드오프를 작성한다.
  - Docker와 MySQL이 필요하므로 일반 테스트보다 느리고, 포트 충돌 같은 로컬 환경 변수의 영향을 받는다.
  - 애플리케이션 전체 스키마를 MySQL에 올리면서 join 경로와 무관한 DDL 경고까지 같이 드러난다.
- 중립적 트레이드오프 또는 후속 영향 사항을 작성한다.
  - 벤치마크 테스트는 일반 CI 스위트에 넣지 않고, 필요 시 명시적으로 실행하는 운영형 검증 도구로 관리한다.
  - 추후 멀티 인스턴스나 외부 네트워크 구간까지 포함한 부하 검증이 필요하면 별도 시나리오를 추가해야 한다.
