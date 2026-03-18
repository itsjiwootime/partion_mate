# E15 외부 알림 실패 처리 및 추적 스펙

- 작성일: 2026-03-18
- 대상 작업: `E15-3`
- 관련 API: 없음 (내부 워커/스케줄러 변경)

## 목적
- 외부 알림 발송 실패가 앱 내 알림 저장을 막지 않도록 채널 책임을 분리한다.
- Web Push 발송 실패 내역과 재시도 상태를 DB에서 추적 가능하게 만든다.
- 같은 외부 알림이 outbox 재처리 때문에 중복 생성되지 않도록 delivery key를 강제한다.

## Outbox 처리 규칙
- `outbox_event.status`
  - `PENDING`
  - `PROCESSED`
  - `FAILED`
- outbox 워커는 앱 내 알림 저장과 외부 발송 작업 생성까지만 책임진다.
- outbox 자체 실패는 최대 3번 시도한다.
  - 1차 실패: `+1분`
  - 2차 실패: `+1분`
  - 3차 실패: `FAILED`
- `FAILED` 상태가 되면 더 이상 스케줄러가 집지 않는다.

## 외부 발송 큐 모델
- `external_notification_delivery`
  - `outbox_event_id`
  - `user_notification_id`
  - `recipient_user_id`
  - `channel`
  - `notification_type`
  - `subscription_id`
  - `endpoint`
  - `delivery_key`
  - `payload_json`
  - `status`
  - `attempt_count`
  - `next_attempt_at`
  - `created_at`
  - `last_attempt_at`
  - `delivered_at`
  - `last_status_code`
  - `last_failure_reason`
  - `last_error_message`
- 현재 `channel`은 `WEB_PUSH` 하나만 사용한다.
- `delivery_key`는 `outboxEventId:userNotificationId:WEB_PUSH:subscriptionId` 조합으로 유니크 처리한다.

## 외부 발송 상태
- `PENDING`
  - 아직 보내지 않았거나 재시도 대기 중
- `SENT`
  - 2xx 응답으로 성공한 상태
- `FAILED`
  - 재시도 한도를 넘겼거나 재시도할 수 없는 실패

## 재시도 정책
- 외부 발송은 최대 3번 시도한다.
  - 1차 실패 후 `+1분`
  - 2차 실패 후 `+5분`
  - 3차 실패 시 `FAILED`
- 재시도 대상
  - `429`
  - `5xx`
  - Web Push gateway 예외
- 즉시 실패 처리
  - `404`, `410`
  - `subscription_id`로 찾은 구독이 이미 삭제된 경우
  - 그 외 `4xx`

## 실패 사유 코드
- `SUBSCRIPTION_NOT_FOUND`
- `ENDPOINT_GONE`
- `CLIENT_ERROR`
- `TRANSIENT_ERROR`
- `DELIVERY_EXCEPTION`

## 처리 흐름
1. outbox 워커가 앱 내 알림을 저장하거나 기존 `externalKey`로 재사용한다.
2. Web Push 대상 타입이고 사용자 설정이 켜져 있으면 현재 구독별 `external_notification_delivery`를 생성한다.
3. 별도 스케줄러가 `PENDING` delivery를 읽어 Web Push를 발송한다.
4. 성공 시 `SENT`, 재시도 가능 실패면 `PENDING` 유지 + `next_attempt_at` 갱신, 불가 실패면 `FAILED`
5. `404`, `410`은 구독을 즉시 삭제하고 delivery는 `FAILED`로 남긴다.

## 중복 방지
- 앱 내 알림은 기존 `user_notification.external_key`로 중복 생성되지 않는다.
- 외부 발송은 `delivery_key` 유니크 제약으로 outbox 재처리 시 같은 subscription delivery가 다시 쌓이지 않는다.
- 따라서 outbox가 다시 `PENDING`으로 돌아가도 앱 내 알림 fallback은 유지되고 외부 delivery 큐는 중복되지 않는다.

## 현재 한계
- 실패 내역 조회용 운영 API나 관리자 화면은 아직 없다.
- 외부 발송은 현재 Web Push 하나만 지원하며, 다른 채널 확장은 같은 큐 모델 위에 추가해야 한다.
