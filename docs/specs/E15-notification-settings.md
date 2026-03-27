# E15 알림 설정 및 딥링크 스펙

- 작성일: 2026-03-18
- 대상 작업: `E15-2`
- 관련 API: `GET /api/users/me/notification-preferences`, `PUT /api/users/me/notification-preferences`, `GET /api/push-subscriptions/config`

## 목적
- 사용자가 알림 종류별 브라우저 푸시 수신 여부를 직접 고를 수 있게 한다.
- 외부 푸시를 눌렀을 때 현재 상태에 맞는 화면으로 복귀하도록 딥링크 규칙을 명확히 한다.

## 설정 모델
- `user_notification_preference`
  - `user_id`
  - `notification_type`
  - `web_push_enabled`
  - `updated_at`
- 같은 사용자와 알림 타입 조합은 하나만 저장한다.
- 설정 행이 없으면 타입별 기본값을 사용한다.
  - Web Push 지원 타입: 기본값 `true`
  - Web Push 미지원 타입: 기본값 `false`

## Web Push 지원 범위
- Web Push 지원
  - `PICKUP_UPDATED`
  - `PARTY_CLOSED`
- 앱 내 알림만 지원
  - `PARTY_JOIN_CONFIRMED`
  - `PARTY_UPDATED`

## API

### `GET /api/users/me/notification-preferences`
- 로그인한 사용자의 전체 알림 타입 설정을 반환한다.
- 응답에는 표시용 라벨, 설명, 딥링크 목적지 라벨, Web Push 지원 여부가 함께 포함된다.

### `200 OK`
```json
[
  {
    "type": "PICKUP_UPDATED",
    "label": "픽업 일정 확정",
    "description": "픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.",
    "deepLinkTargetLabel": "파티 상세",
    "webPushSupported": true,
    "webPushEnabled": true
  },
  {
    "type": "PARTY_UPDATED",
    "label": "파티 조건 변경",
    "description": "모집 수량, 마감 시간, 안내 문구 등 파티 조건이 바뀌면 알려줍니다.",
    "deepLinkTargetLabel": "파티 상세",
    "webPushSupported": false,
    "webPushEnabled": false
  }
]
```

### `PUT /api/users/me/notification-preferences`
```json
{
  "preferences": [
    {
      "type": "PICKUP_UPDATED",
      "webPushEnabled": false
    },
    {
      "type": "PARTY_CLOSED",
      "webPushEnabled": true
    }
  ]
}
```

- 같은 `type`을 한 요청에 두 번 보낼 수 없다.
- Web Push 미지원 타입을 `webPushEnabled=true`로 저장하려 하면 `400 Bad Request`

### `GET /api/push-subscriptions/config`
```json
{
  "enabled": true,
  "publicKey": "BOr...."
}
```

- 프론트가 Service Worker 구독과 권한 요청을 시작하기 전에 사용할 설정 조회 API다.
- 서버에 VAPID 키가 준비되지 않았으면 `enabled=false`, `publicKey=""`

## 딥링크 규칙
- `PARTY_JOIN_CONFIRMED`
  - `/chat/{partyId}`
- `PICKUP_UPDATED`, `PARTY_UPDATED`
  - `/parties/{partyId}`
- `PARTY_CLOSED`
  - `/notifications`
- `partyId`를 만들 수 없는 예외 상황은 `/notifications`로 fallback 한다.

## 프론트 동작
- 앱 시작 시 `/push-sw.js` Service Worker를 등록한다.
- 프로필 화면에서 현재 브라우저 Web Push 연결 상태와 연결된 브라우저 수를 노출한다.
- 현재 브라우저 연결 버튼은 브라우저 권한 허용 후 Push Subscription을 생성하고 `PUT /api/push-subscriptions`로 저장한다.
- 연결 해제 시 현재 브라우저 subscription을 해지하고 서버 저장소에서도 같은 endpoint를 삭제한다.
- 알림 내역 화면은 `linkUrl` 패턴에 따라 `관련 채팅 보기`, `관련 파티 보기`, `알림 내역 보기` 라벨을 분기한다.

## 현재 한계
- 알림 설정은 계정 단위로 저장되므로, 같은 계정에 연결된 여러 브라우저가 동일한 Web Push on/off 정책을 공유한다.
- Web Push 실패 재시도, 발송 추적, 중복 방지 확장은 `E15-3`에서 다룬다.
