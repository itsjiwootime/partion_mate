# ADR-20260318-external-notification-delivery-reliability

- 날짜: 2026-03-18
- 상태: Accepted
- 관련 작업: `E15-3`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/OutboxEvent.java`, `src/main/java/com/project/partition_mate/domain/ExternalNotificationDelivery.java`, `src/main/java/com/project/partition_mate/service/NotificationOutboxProcessor.java`, `src/main/java/com/project/partition_mate/service/ExternalNotificationDeliveryService.java`, `src/main/java/com/project/partition_mate/service/WebPushNotificationService.java`

## 배경
- 기존 Web Push 발송은 앱 내 알림을 저장한 직후 같은 트랜잭션 흐름에서 best-effort로 실행돼, 실패해도 로그 외에는 남지 않았다.
- 이 구조에서는 어떤 외부 알림이 실패했는지, 몇 번 재시도했는지, 더 이상 재시도하지 않는 실패인지 운영에서 추적할 수 없었다.
- 또한 outbox 이벤트가 다시 처리되면 앱 내 알림은 `externalKey`로 중복을 막을 수 있어도, 외부 발송은 별도 dedup 키가 없어 추후 재시도 구조를 붙이기 어렵다.

## 결정사항
- outbox 이벤트는 앱 내 알림 저장과 외부 발송 작업 생성까지만 책임지고, 실제 외부 발송은 `external_notification_delivery` 큐로 분리한다.
- `outbox_event.status`에 `FAILED`를 추가하고, payload 파싱이나 큐 생성 자체가 실패한 경우 최대 3번만 재시도한 뒤 종료한다.
- 외부 발송 큐는 `delivery_key = outboxEventId:userNotificationId:WEB_PUSH:subscriptionId` 유니크 규칙으로 중복을 방지한다.
- 외부 발송은 최대 3번 시도하며 `429`, `5xx`, gateway 예외만 재시도한다.
- `404`, `410`, 구독 없음, 기타 `4xx`는 즉시 `FAILED` 처리하고, `404`/`410` 구독은 저장소에서 삭제한다.
- 앱 내 알림은 항상 먼저 저장하거나 기존 `externalKey` 레코드를 재사용해 fallback을 보장한다.

## 대안
- 기존처럼 앱 내 알림 저장 후 같은 흐름에서 외부 발송만 best-effort로 수행한다.
  - 장점: 구현이 단순하다.
  - 단점: 실패 상태를 DB에서 추적할 수 없고 재시도/중복 방지 정책을 붙이기 어렵다.
- 외부 발송도 outbox 이벤트 자체를 사용자/구독 단위로 잘게 쪼개 재사용한다.
  - 장점: 큐 모델이 하나로 통일된다.
  - 단점: 앱 내 알림 생성용 outbox와 외부 발송용 작업 의미가 섞여 aggregate/event 단위 해석이 흐려진다.
- 실패를 DB에 남기지 않고 로그 집계만으로 운영한다.
  - 장점: 데이터 모델 추가가 없다.
  - 단점: 개별 delivery 추적, 중복 방지, 재시도 스케줄 제어를 코드 밖에서 해결해야 한다.

## 결과
- 긍정적
  - 앱 내 알림 fallback과 외부 발송 재시도를 분리해 외부 장애가 사용자 기본 알림 경험을 깨지 않는다.
  - 각 subscription delivery가 DB에 남아 실패 원인과 마지막 상태를 추적할 수 있다.
  - outbox 재처리와 외부 발송 재처리를 각각 다른 책임으로 관리할 수 있다.
- 부정적
  - 테이블, 스케줄러, 상태 모델이 추가되어 구조가 더 복잡해진다.
  - 현재는 운영 조회 API가 없어 DB나 로그 없이 상태를 바로 볼 수는 없다.
  - Web Push 외 채널을 붙일 때 채널별 정책을 delivery 큐에 계속 확장해야 한다.
- 중립적
  - 현재 큐 모델은 `channel` 필드를 포함해 향후 SMS/이메일 같은 외부 채널 확장을 수용할 수 있다.
  - 사용자 설정이 바뀐 직후 이미 큐에 들어간 delivery는 기존 정책대로 발송될 수 있다.
