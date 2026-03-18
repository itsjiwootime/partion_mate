# ADR-20260318-web-push-first-channel

- 날짜: 2026-03-18
- 상태: Accepted
- 관련 작업: `E15-1`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/WebPushSubscription.java`, `src/main/java/com/project/partition_mate/service/WebPushSubscriptionService.java`, `src/main/java/com/project/partition_mate/service/WebPushNotificationService.java`, `src/main/java/com/project/partition_mate/service/LibraryWebPushGateway.java`, `src/main/java/com/project/partition_mate/service/NotificationOutboxProcessor.java`

## 배경
- 현재 알림 시스템은 outbox 기반 앱 내 알림까지만 지원해, 사용자가 브라우저를 보고 있지 않으면 승격이나 종료 같은 중요한 변화를 놓치기 쉽다.
- 외부 알림 1차 채널은 로드맵에서 Web Push로 고정돼 있었지만, 실제로는 구독 저장소와 발송 서비스가 없어 앱 내 알림 이상으로 확장할 수 없었다.
- 동시에 외부 발송 실패를 outbox 재시도와 강하게 묶으면 중복 발송, 장애 전파, 실패 추적 모델까지 한 번에 커져 초기 범위를 넘기게 된다.

## 결정사항
- 외부 알림 1차 채널은 VAPID 기반 Web Push로 구현하고, 사용자별 `web_push_subscription` 엔티티에 `endpoint`, `p256dh`, `auth`, `userAgent`를 저장한다.
- 구독 API는 `PUT /api/push-subscriptions`, `GET /api/push-subscriptions`, `DELETE /api/push-subscriptions/{subscriptionId}`로 제공한다.
- 기존 outbox 워커는 앱 내 알림을 먼저 저장한 뒤, 새로 생성된 알림에 한해 Web Push를 best-effort로 발송한다.
- 외부 푸시 대상은 `WAITING_PROMOTED`, `PICKUP_UPDATED`, `PARTY_CLOSED`, `WAITING_EXPIRED`로 제한한다.
- 호스트가 픽업 일정을 확정하면 새 `PICKUP_UPDATED` outbox 이벤트를 적재해 참여자에게 앱 내 알림과 Web Push를 같이 보낸다.
- Web Push 실패는 현재 단계에서 워커 재시도 사유로 승격하지 않고 로그만 남긴다. 단, `404`/`410` 응답을 준 구독은 만료된 것으로 보고 즉시 삭제한다.

## 대안
- 이메일이나 SMS를 1차 외부 채널로 도입한다.
  - 장점: 브라우저 상태와 무관하게 도달성이 높다.
  - 단점: 비용, 개인정보 처리, 공급자 연동 범위가 커져 현재 단계에 과하다.
- 외부 발송도 outbox 실패로 간주해 즉시 재시도 체계를 완성한다.
  - 장점: 장애 복구가 더 강해진다.
  - 단점: 수신자별 중복 방지, 부분 성공 처리, 실패 로그 모델까지 필요해 `E15-3` 범위를 선행하게 된다.
- 구독 저장 없이 Service Worker에서 직접 로컬 알림만 띄운다.
  - 장점: 서버 구현이 단순하다.
  - 단점: 브라우저가 닫힌 상태에서는 도달하지 못해 외부 알림 채널 목적을 달성하지 못한다.

## 결과
- 긍정적
  - 앱 내 알림 파이프라인을 재사용해 큰 구조 변경 없이 Web Push 채널을 추가할 수 있다.
  - 구독 저장과 invalid endpoint 정리까지 포함돼 브라우저별 외부 알림 기본 흐름이 닫힌다.
  - `PICKUP_UPDATED`를 별도 outbox 이벤트로 분리해 픽업 변경도 앱 내/외부 알림에서 같은 의미로 전달할 수 있다.
- 부정적
  - 외부 발송 실패는 아직 재시도나 운영 추적이 없어 일부 푸시가 조용히 누락될 수 있다.
  - 브라우저 권한, Service Worker 등록, 실제 클릭 딥링크 처리는 후속 프론트 작업이 필요하다.
- 중립적
  - 이후 `E15-2`, `E15-3`에서는 현재 구독 모델 위에 알림 설정과 실패 추적을 확장하게 된다.
  - 같은 outbox 이벤트라도 앱 내 알림은 유지되고 외부 푸시만 실패할 수 있는 구조를 기준선으로 삼는다.
