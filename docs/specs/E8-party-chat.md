# E8 파티 채팅 스펙

## 목적
- 파티 참여자 전용 인앱 채팅방을 제공한다.
- 파티 운영 이벤트를 시스템 메시지로 기록하고, unread count와 호스트 공지를 같은 모델로 노출한다.

## REST API

### `GET /api/chat/rooms`
- 인증 필요
- 현재 사용자가 참여 중인 파티 채팅방 목록을 반환한다.
- 주요 필드
  - `roomId`
  - `partyId`
  - `partyTitle`
  - `storeName`
  - `pinnedNotice`
  - `lastMessagePreview`
  - `lastMessageType`
  - `lastMessageCreatedAt`
  - `lastMessageCreatedAtLabel`
  - `unreadCount`

### `GET /api/chat/rooms/{partyId}`
- 인증 필요
- 파티 참여자만 조회 가능
- 최근 100개 메시지를 시간순으로 반환하고, 마지막 메시지까지 읽음 처리한다.
- 주요 필드
  - `roomId`
  - `partyId`
  - `partyTitle`
  - `storeName`
  - `host`
  - `pinnedNotice`
  - `pinnedNoticeUpdatedAt`
  - `pinnedNoticeUpdatedAtLabel`
  - `unreadCount`
  - `messages[]`

### `POST /api/chat/rooms/{partyId}/read`
- 인증 필요
- 파티 참여자만 호출 가능
- 마지막 메시지까지 읽음 처리하고 `unreadCount=0`을 반환한다.

### `PUT /api/chat/rooms/{partyId}/notice`
- 인증 필요
- 파티 호스트만 호출 가능
- body
```json
{
  "pinnedNotice": "픽업 10분 전 도착 부탁드립니다."
}
```
- `pinnedNotice`가 빈 값이면 공지를 해제한다.

## WebSocket / STOMP

### 연결
- endpoint: `/ws-chat`
- 인증: `CONNECT` 프레임에 `Authorization: Bearer <jwt>` 헤더 필요

### 메시지 전송
- destination: `/app/chat/{partyId}/messages`
- payload
```json
{
  "content": "안녕하세요. 채팅 연결 테스트입니다."
}
```

### 메시지 구독
- destination: `/topic/chat/{partyId}`
- payload
```json
{
  "messageId": 10,
  "partyId": 3,
  "type": "TEXT",
  "senderId": 5,
  "senderName": "member",
  "content": "안녕하세요. 채팅 연결 테스트입니다.",
  "mine": false,
  "createdAt": "2026-03-17T01:52:20",
  "createdAtLabel": "방금 전",
  "pinnedNotice": null
}
```

## 메시지 타입
- `TEXT`
  - 일반 사용자 메시지
- `SYSTEM`
  - 채팅방 생성
  - 파티 참여
  - 참여 취소
  - 대기열 승격
  - 자동 마감
  - 호스트 공지 변경

## 읽음 처리 정책
- `ChatReadState.lastReadMessageId` 기준으로 unread count를 계산한다.
- 채팅방 상세 조회와 명시적 읽음 API 호출 모두 마지막 메시지까지 읽음 처리한다.
- 내가 보낸 일반 메시지는 저장 직후 내 읽음 위치도 함께 갱신한다.

## 호스트 공지 정책
- 호스트만 공지를 저장/해제할 수 있다.
- 공지 값은 `ChatRoom.pinnedNotice`에 저장한다.
- 공지 변경 시 채팅방에도 시스템 메시지를 추가하고, WebSocket payload의 `pinnedNotice` 스냅샷에 반영한다.

## 프론트 연결 정책
- 라우트
  - `/chat`
  - `/chat/:partyId`
- 목록/내 파티/파티 상세에서 채팅 unread count와 진입 CTA를 제공한다.
- 파티 생성 후 전용 인앱 채팅방이 자동으로 열리며, 채팅 화면과 파티 상세 모두 인앱 채팅을 기본 경로로 사용한다.
