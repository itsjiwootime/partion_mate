# ADR-20260327-remove-waiting-queue-join-fail-fast

- 날짜: 2026-03-27
- 상태: Accepted
- 관련 작업: `E1-6`
- 관련 파일: `src/main/java/com/project/partition_mate/service/PartyService.java`, `frontend/src/pages/JoinParty.jsx`, `src/test/java/com/project/partition_mate/service/PartyConcurrencyIntegrationTest.java`
- 대체 문서: `ADR-20260315-party-participation-schema.md`, `ADR-20260315-party-waiting-queue-response.md`, `ADR-20260315-party-queue-promotion-policy.md`, `ADR-20260317-waiting-queue-ux-exposure.md`

## 배경
- 파티 참여 정책이 `즉시 참여 + 대기열 등록 + 자동 승격`으로 확장되면서 서비스, 알림, 실시간 이벤트, 프론트, 테스트가 모두 `waiting` 개념에 강하게 결합되었다.
- 현재 프로젝트의 핵심 설명은 동시성 제어로 초과 모집과 중복 참여를 막는 데 있는데, 대기열 승격 정책까지 함께 설명해야 해 면접과 이력서 메시지가 흐려졌다.
- 잔여 수량 부족 시 즉시 실패시키는 정책으로도 모집 정합성 검증과 운영 흐름은 충분히 설명 가능하므로, 복잡도를 줄이고 현재 목표에 더 맞는 모델이 필요했다.

## 결정사항
- `WaitingQueueEntry`, `WaitingQueueRepository`, `WaitingQueueStatus`와 `ParticipationStatus.WAITING`를 제거한다.
- `joinParty`는 비관적 락으로 현재 파티를 잠근 뒤, 즉시 참여 가능한 경우에만 `JOINED`를 반환하고 남은 수량이 부족하면 `BusinessException.insufficientQuantity(...)`로 즉시 실패한다.
- `cancelJoin`은 실제 참여자만 취소할 수 있게 단순화하고, 취소 후에는 잔여 수량과 파티 상태만 재계산한다.
- `WAITING_PROMOTED`, `WAITING_EXPIRED` 같은 알림/실시간/딥링크 타입을 제거하고, 현재 지원하는 이벤트는 `PARTY_JOIN_CONFIRMED`, `PICKUP_UPDATED`, `PARTY_UPDATED`, `PARTY_CLOSED`로 한정한다.
- 프론트는 `FULL` 또는 잔여 수량 부족 상태를 `대기열 등록 가능`이 아니라 `참여 불가`로 표시하고, 관련 CTA와 안내 문구를 제거한다.
- 동시성 검증은 `초과 모집 0건`, `중복 참여 0건`, `잔여 수량 부족 요청의 일관된 실패` 기준으로 다시 작성한다.

## 대안
- 대안 1: 기존 대기열 모델을 유지하고 테스트/문서만 보강한다.
  - 장점: 기존 구현을 최대한 재사용할 수 있다.
  - 단점: 알림, 프론트, 실시간, 자동 승격 정책까지 함께 설명해야 해 현재 프로젝트의 핵심 메시지가 계속 분산된다.
- 대안 2: 대기열 도메인은 남기고 참여 API에서만 숨긴다.
  - 장점: 코드 변경 범위를 줄일 수 있다.
  - 단점: 실제로는 사용하지 않는 엔티티와 알림 타입이 남아 코드베이스가 더 혼란스러워진다.
- 대안 3: 대기열을 유지하되 Redis나 비동기 워커 기반으로 확장한다.
  - 장점: 티켓팅형 트래픽 대응 논의를 이어가기 쉽다.
  - 단점: 현재 단일 DB 구조와 서비스 목적 대비 과도한 복잡도를 도입하고, 이번 피드백의 우선순위인 모집 정합성 설명 단순화와도 맞지 않는다.

## 결과
- 긍정적 트레이드오프
  - 참여 정책이 `즉시 참여 아니면 실패`로 단순해져 동시성 제어 설명과 테스트가 훨씬 명확해진다.
  - 알림, 실시간 이벤트, 프론트 UI, 테스트 전반에서 `waiting` 분기가 사라져 유지보수 복잡도가 줄어든다.
  - 부하 테스트 결과를 `성공/거절` 두 상태로 정리할 수 있어 벤치마크와 이력서 문구의 방어가 쉬워진다.
- 부정적 트레이드오프
  - 빈 자리가 생겼을 때 자동으로 다음 사용자를 승격시키는 사용자 경험은 사라진다.
  - 과거 대기열 관련 설계와 문서를 한 번 더 정리해야 하고, historical ADR과 스펙에 superseded 관계를 남겨야 한다.
- 중립적 트레이드오프 또는 후속 영향 사항
  - 향후 트래픽이 크게 늘어나면 fail-fast 정책 위에 별도 예약/대기 기능을 다시 설계할 수 있지만, 그때는 현재와 다른 문제로 분리해 다루는 것이 적절하다.
  - 이번 결정으로 `동시성 제어`, `정원 초과 즉시 실패`, `정합성 테스트`를 중심으로 설명 축이 재정렬된다.
