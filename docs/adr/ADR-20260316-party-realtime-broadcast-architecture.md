# ADR-20260316-party-realtime-broadcast-architecture

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E4-2`
- 관련 파일: `src/main/java/com/project/partition_mate/service/PartyRealtimeService.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`, `src/main/java/com/project/partition_mate/service/PartyLifecycleService.java`

## 배경
- 실시간 이벤트는 파티 생성, 참여 확정, 취소, 대기열 승격, 마감 이후의 최종 상태를 반영해야 한다.
- 트랜잭션 안에서 즉시 SSE를 보내면 DB 반영이 실패했을 때 클라이언트가 롤백된 상태를 먼저 보는 문제가 생길 수 있다.
- 에픽 4 범위에서는 별도 메시지 브로커 없이 애플리케이션 내부에서 구독자 관리와 브로드캐스트를 처리하는 구조가 필요했다.

## 결정사항
- `PartyRealtimeService`가 메모리 기반 `SseEmitter` 레지스트리를 관리하고, 구독 시점에 `connected` 이벤트를 보낸다.
- 파티 상태 변경 서비스는 `PartyRealtimeEventResponse` 스냅샷을 만든 뒤 `TransactionSynchronizationManager`의 `afterCommit` 콜백에서 실제 브로드캐스트를 수행한다.
- 브로드캐스트 대상은 `storeId`, `partyId` 필터와 이벤트 payload의 `storeId`, `id`를 비교해 선별한다.
- 발행 트리거는 `PARTY_CREATED`, `JOIN_CONFIRMED`, `MEMBER_CANCELLED`, `WAITING_PROMOTED`, `PARTY_CLOSED` 다섯 가지로 한정한다.

## 대안
- 트랜잭션 내부에서 즉시 emitter 전송
  - 장점: 구현이 단순하다.
  - 단점: 롤백 시 잘못된 상태를 클라이언트에 먼저 노출할 수 있다.
- outbox를 재사용해 별도 SSE 워커에서 전송
  - 장점: 재시도와 감사 추적이 더 좋아진다.
  - 단점: 에픽 4 범위에서 실시간 지연이 늘고 구조가 과해진다.
- Redis Pub/Sub 기반 브로드캐스트
  - 장점: 다중 인스턴스 확장에 유리하다.
  - 단점: 현재 단일 인스턴스 개발 단계에서는 추가 인프라 비용이 크다.

## 결과
- 긍정적
  - 커밋 이후에만 SSE를 발행하므로 DB 상태와 클라이언트 상태의 정합성이 좋아졌다.
  - 이벤트 스냅샷이 DTO로 분리돼 서비스와 프론트가 같은 payload를 공유하게 됐다.
  - `PartyRealtimeServiceIntegrationTest`로 연결과 필터 브로드캐스트를 검증할 수 있게 됐다.
- 부정적
  - 현재 레지스트리는 메모리 기반이라 멀티 인스턴스 환경에서는 공유되지 않는다.
  - 재시도나 durable delivery가 없어 수신 중이 아니던 클라이언트는 fallback에 의존한다.
- 중립적
  - 이후 Redis나 메시지 브로커를 붙이더라도 발행 시점 정책은 `afterCommit` 원칙을 그대로 유지할 수 있다.
