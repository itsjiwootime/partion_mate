# E4 Party Realtime Event Spec

- 작성일: 2026-03-16
- 대상 작업: `E4-1`, `E4-2`, `E4-3`, `E4-4`
- 엔드포인트: `GET /party/stream`
- 전송 방식: `text/event-stream`

## 목적
- 파티 목록, 파티 상세, 내 파티 화면이 새로고침 없이 모집 현황 변화를 반영하도록 한다.
- 현재 인증 구조를 유지하기 위해 스트림은 공개 SSE로 제공하고, 보호된 화면은 필요한 경우 기존 인증 API를 fallback으로 다시 조회한다.

## 구독 파라미터
- `storeId` 선택
  - 해당 지점의 파티 이벤트만 수신한다.
- `partyId` 선택
  - 특정 파티 이벤트만 수신한다.
- 둘 다 없으면
  - 전체 공개 파티 이벤트를 수신한다.

## 이벤트 종류

### `connected`
- 의미: 스트림 연결 또는 재연결이 성립했다.
- payload

```json
{
  "streamId": "9a4c19f9-4d39-41c8-9a3e-0b822eb7bc0b",
  "connectedAt": "2026-03-16T14:20:30",
  "reconnectDelayMs": 3000
}
```

### `party-updated`
- 의미: 파티 생성, 참여 확정, 취소, 대기열 승격, 마감으로 공개 상태가 바뀌었다.
- payload

```json
{
  "eventId": "18faaf28-8232-4bca-9096-cf36d4706cc7",
  "id": 12,
  "title": "코스트코 비타민 소분",
  "productName": "비타민",
  "totalPrice": 32000,
  "totalQuantity": 4,
  "status": "FULL",
  "storeId": 3,
  "storeName": "양재 코스트코",
  "currentQuantity": 4,
  "openChatUrl": "https://open.kakao.com/o/example",
  "deadline": "2026-03-16T20:00:00",
  "deadlineLabel": "2026.03.16 20:00",
  "closedAt": null,
  "closeReason": null,
  "remainingQuantity": 0,
  "realtimeTrigger": "JOIN_CONFIRMED",
  "changedAt": "2026-03-16T14:20:31"
}
```

## `realtimeTrigger` 정의
- `PARTY_CREATED`
  - 새 파티가 생성됐다.
- `JOIN_CONFIRMED`
  - 즉시 참여로 현재 수량이 증가했다.
- `MEMBER_CANCELLED`
  - 기존 참여자가 취소해 현재 수량이 감소했다.
- `WAITING_PROMOTED`
  - 대기열 승격으로 공개 상태가 갱신됐다.
- `PARTY_CLOSED`
  - 마감 시간 도달로 파티가 종료됐다.

## 프론트 반영 규칙
- 파티 목록
  - `id` 기준으로 기존 항목을 교체한다.
  - 항목이 없고 `realtimeTrigger=PARTY_CREATED`면 목록 앞에 추가한다.
  - `storeId` 필터가 있는 경우 다른 지점 이벤트는 무시한다.
- 파티 상세
  - `partyId`가 일치하는 이벤트만 현재 화면 상태에 병합한다.
- 내 파티
  - 현재 목록에 포함된 파티 ID와 이벤트 `id`가 일치하면 `/api/users/me/parties`를 다시 조회한다.

## 재연결 및 fallback 규칙
- 클라이언트는 `EventSource` 오류 시 연결을 닫고 최대 5초까지 선형 backoff로 재연결한다.
- 오류가 발생한 즉시 화면별 기존 조회 API를 한 번 다시 호출해 누락 가능성을 보정한다.
- 재연결이 완료되면 `connected` 이벤트로 상태를 `live`로 되돌린다.

## 현재 한계
- `Last-Event-ID` 기반 재전송은 아직 지원하지 않는다.
- 읽음 처리나 사용자별 비공개 이벤트는 스트림에 포함하지 않는다.
