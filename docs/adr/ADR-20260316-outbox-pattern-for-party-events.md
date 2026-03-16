# ADR-20260316-outbox-pattern-for-party-events

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E3-3`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/OutboxEvent.java`, `src/main/java/com/project/partition_mate/service/NotificationOutboxService.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`, `src/main/java/com/project/partition_mate/service/PartyLifecycleService.java`

## 배경
- 파티 참여 완료, 대기열 승격, 시간 만료 종료는 모두 사용자 알림으로 이어져야 하지만, 도메인 트랜잭션 안에서 바로 외부 발송까지 처리하면 실패 전파와 응답 지연이 커진다.
- 반대로 도메인 저장과 알림 이벤트 저장이 분리되면 파티 상태는 바뀌었는데 알림 데이터는 남지 않는 불일치가 발생할 수 있다.
- 현재 단계에서는 Kafka 같은 메시지 브로커 없이도 트랜잭션 정합성을 확보할 수 있는 구조가 필요했다.

## 결정사항
- `outbox_event` 테이블을 도입하고 파티 도메인 변경과 알림 이벤트 저장을 같은 트랜잭션 안에서 처리한다.
- 이벤트 타입은 `PARTY_JOIN_CONFIRMED`, `WAITING_PROMOTED`, `PARTY_CLOSED` 세 가지로 시작한다.
- payload는 JSON 문자열로 저장해 이벤트별 수신자 목록과 링크 대상 파티 정보를 유연하게 담는다.
- `PartyService`와 `PartyLifecycleService`는 상태 변경 직후 `NotificationOutboxService`를 호출해 이벤트를 기록하고, 실제 알림 생성은 별도 워커가 처리한다.

## 대안
- 도메인 서비스에서 곧바로 `UserNotification` 저장
  - 장점: 구조가 단순하고 구현 파일 수가 적다.
  - 단점: 비동기 채널 확장이나 재시도 정책을 붙이기 어렵고, 알림 생성 실패가 도메인 트랜잭션에 직접 영향을 준다.
- Spring 이벤트(`ApplicationEventPublisher`)만 사용
  - 장점: 애플리케이션 내부 비동기 분리가 쉽다.
  - 단점: 프로세스 장애 시 이벤트 유실 가능성이 있어 재시도와 이력 추적이 약하다.
- Kafka/RabbitMQ 도입
  - 장점: 메시징 확장성과 소비자 분리가 좋다.
  - 단점: 개인 프로젝트 현재 단계에서는 인프라 복잡도가 과하고 로컬 재현 비용이 커진다.

## 결과
- 긍정적
  - 도메인 상태 변경과 알림 이벤트 적재를 같은 트랜잭션에 묶어 정합성을 확보했다.
  - 이벤트 테이블 덕분에 실패 재처리, 감사 추적, 향후 외부 채널 연동 확장이 쉬워졌다.
  - `PartyLifecycleIntegrationTest`와 참여 흐름 코드에서 상태 변경 직후 outbox 저장을 확인할 수 있다.
- 부정적
  - 이벤트 스키마와 payload 버전을 직접 관리해야 해서 코드와 문서 동기화 비용이 생긴다.
  - 이벤트 소비가 늦어지면 알림 반영 시점이 도메인 처리와 분리된다.
- 중립적
  - 현재는 DB 기반 outbox만으로 충분하지만, 이후 메시지 브로커를 붙이더라도 producer 계층은 재사용할 수 있다.
