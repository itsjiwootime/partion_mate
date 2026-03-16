# Changelog

이 문서는 `Partition Mate`의 주요 기능 변경과 검증 결과를 릴리즈 단위로 정리한다.

## v0.2.0 - 2026-03-16

### 요약
- 에픽 1~3을 반영해 파티 참여 안정성, 위치 기반 조회 성능, 자동 마감 및 앱 내 알림 흐름을 완성했다.
- 공동구매 파티 생성부터 참여, 대기열, 자동 종료, 알림 확인까지 한 사이클을 서비스 안에서 처리할 수 있게 됐다.

### 추가
- 파티 참여 대기열과 FIFO 자동 승격 흐름을 추가했다.
- 참여 취소 API와 내 파티 화면의 대기 상태 노출을 추가했다.
- 파티 `deadline`, `closedAt`, `closeReason`, `CLOSED` 상태를 추가했다.
- 자동 마감 스케줄러와 `outbox_event`, `user_notification` 기반 앱 내 알림 파이프라인을 추가했다.
- 알림 조회 API와 `/notifications` 화면, 하단 네비게이션 진입점을 추가했다.

### 변경
- 주변 지점 조회를 거리 계산 쿼리 + bounding box 기반으로 최적화했다.
- 지점별 파티 조회를 projection 기반 조회로 단순화하고 `Spring Cache + Caffeine` 캐시를 적용했다.
- 파티 상세, 내 파티, 참여 화면이 종료 상태와 마감 시간을 공통 필드로 처리하도록 응답 DTO와 프론트 정규화를 맞췄다.
- 파티 생성 시 마감 시간이 없으면 서버에서 기본값 `현재 시각 + 1일`을 적용하도록 변경했다.

### 수정
- 동시 참여 시 초과 모집이 발생하지 않도록 참여 락과 수량 검증을 보강했다.
- 같은 outbox 이벤트가 다시 처리돼도 사용자 알림이 중복 생성되지 않도록 `externalKey` 기반 idempotency 처리를 추가했다.
- 개발/테스트 환경을 분리해 로컬 MySQL 없이도 백엔드 테스트를 실행할 수 있게 정리했다.

### 검증
- 백엔드 전체 테스트: `./mvnw test`
- 프론트 빌드 검증: `cd frontend && npm ci && npm run build`
- 파티 참여 부하 측정: `docs/benchmarks/E1-5-party-join-load-benchmark.md`
- 위치 조회 성능 비교: `docs/benchmarks/E2-5-store-query-performance-comparison.md`

### 참고 문서
- 로드맵 상태: `docs/agent-roadmap.md`
- 에픽 1 ADR: `docs/adr/ADR-20260315-party-participation-schema.md` 외 4건
- 에픽 2 ADR: `docs/adr/ADR-20260315-store-query-baseline-method.md` 외 4건
- 에픽 3 ADR: `docs/adr/ADR-20260316-party-lifecycle-model.md` 외 4건

### 다음 예정
- 에픽 4 실시간 모집 현황 반영
- 에픽 5 소분 도메인 상세화
