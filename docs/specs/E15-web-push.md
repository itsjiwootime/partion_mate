# E15 Web Push 구독 저장 및 발송 스펙

- 작성일: 2026-03-18
- 대상 작업: `E15-1`
- 관련 API: `PUT /api/push-subscriptions`, `GET /api/push-subscriptions`, `DELETE /api/push-subscriptions/{subscriptionId}`

## 목적
- 앱 내 알림 외에 브라우저 Web Push를 통해 중요한 상태 변화를 즉시 전달한다.
- 현재 단계에서는 사용자별 구독 저장과 기본 발송 경로만 만든다.

## 구독 모델
- `web_push_subscription`
  - `user_id`
  - `endpoint`
  - `p256dh`
  - `auth`
  - `userAgent`
  - `createdAt`
  - `updatedAt`
- `endpoint`는 전역 유니크로 관리한다.
- 같은 `endpoint`로 다시 등록하면 새 행을 만들지 않고 현재 사용자 기준으로 키와 `updatedAt`만 갱신한다.

## API

### `PUT /api/push-subscriptions`
```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/...",
  "keys": {
    "p256dh": "BOr....",
    "auth": "abc123=="
  },
  "userAgent": "Chrome/134.0"
}
```

### `200 OK`
```json
{
  "id": 5,
  "endpoint": "https://fcm.googleapis.com/fcm/send/...",
  "createdAt": "2026-03-18T14:10:00",
  "createdAtLabel": "2026.03.18 14:10",
  "updatedAt": "2026-03-18T14:10:00",
  "updatedAtLabel": "2026.03.18 14:10"
}
```

### `GET /api/push-subscriptions`
- 로그인한 사용자의 현재 Web Push 구독 목록을 최신 갱신순으로 반환한다.

### `DELETE /api/push-subscriptions/{subscriptionId}`
- 본인 구독만 삭제할 수 있다.
- 성공 시 `204 No Content`
- 없는 구독이면 `404 Not Found`

## 발송 규칙
- 기존 outbox 워커가 앱 내 알림을 저장한 직후 Web Push를 best-effort로 보낸다.
- 외부 푸시 대상 알림 타입
  - `WAITING_PROMOTED`
  - `PICKUP_UPDATED`
  - `PARTY_CLOSED`
  - `WAITING_EXPIRED`
- 앱 내 알림만 유지하는 타입
  - `PARTY_JOIN_CONFIRMED`
  - `PARTY_UPDATED`

## Pickup 변경
- 호스트가 픽업 일정을 확정하면 `PICKUP_UPDATED` outbox 이벤트를 생성한다.
- 수신 대상은 호스트를 제외한 현재 참여자다.
- 메시지는 `픽업 장소 + 픽업 시각` 요약을 포함한다.

## Web Push payload
```json
{
  "type": "WAITING_PROMOTED",
  "title": "대기열에서 승격되었습니다",
  "body": "휴지 공동구매 파티에서 1개 요청이 참여로 승격되었습니다.",
  "url": "/parties/12",
  "tag": "WAITING_PROMOTED"
}
```

## 현재 한계
- 외부 푸시 실패는 로그만 남기고 outbox 재시도 사유로 승격하지 않는다.
- `404`, `410` 응답을 준 구독은 즉시 삭제하지만, 그 외 실패 추적과 재시도 정책은 `E15-3`에서 다룬다.
- 프론트의 실제 브라우저 권한 요청과 Service Worker 연결은 후속 카드에서 붙인다.
