# E14 신고 접수 API 스펙

- 작성일: 2026-03-18
- 대상 작업: `E14-1`
- 관련 API: `POST /api/reports`, `GET /api/reports/me`

## 목적
- 사용자가 파티 운영, 거래 상대, 채팅 문제를 앱 안에서 바로 신고 접수할 수 있게 한다.
- 현재 단계에서는 관리자 처리 화면 없이도 사용자가 자신의 신고 접수 결과를 다시 확인할 수 있게 한다.

## 신고 대상 타입
- `PARTY`
  - 특정 파티 자체를 신고한다.
  - `partyId`는 필수, `targetUserId`는 비워야 한다.
- `USER`
  - 특정 파티 안의 상대 사용자를 신고한다.
  - `partyId`, `targetUserId` 모두 필수다.
- `CHAT`
  - 특정 파티 채팅에서 상대 사용자를 신고한다.
  - `partyId`, `targetUserId` 모두 필수다.
  - 신고자와 대상자 모두 해당 파티 채팅 참여자여야 한다.

## 신고 사유 코드
- `NO_SHOW`
- `PAYMENT_ISSUE`
- `INAPPROPRIATE_CHAT`
- `FRAUD_SUSPECTED`
- `SPAM`
- `OTHER`

## 생성 API

### `POST /api/reports`

```json
{
  "targetType": "USER",
  "partyId": 12,
  "targetUserId": 3,
  "reasonType": "PAYMENT_ISSUE",
  "memo": "정산 금액 안내와 실제 요청 내용이 다릅니다."
}
```

### 성공 응답 `201 Created`

```json
{
  "id": 7,
  "targetType": "USER",
  "targetTypeLabel": "상대 사용자",
  "partyId": 12,
  "partyTitle": "코스트코 비타민 소분",
  "targetUserId": 3,
  "targetUsername": "host-user",
  "reasonType": "PAYMENT_ISSUE",
  "reasonTypeLabel": "정산 문제",
  "status": "RECEIVED",
  "statusLabel": "접수됨",
  "memo": "정산 금액 안내와 실제 요청 내용이 다릅니다.",
  "createdAt": "2026-03-18T02:20:00",
  "createdAtLabel": "2026.03.18 02:20"
}
```

## 내 신고 조회 API

### `GET /api/reports/me`
- 로그인한 사용자가 직접 접수한 신고만 최신순으로 조회한다.
- 현재 단계에서는 관리자 처리 상태 상세 이력 없이 `RECEIVED` 상태만 노출한다.

## 검증 및 제한 규칙
- 파티 신고는 해당 파티의 참여자 또는 대기 사용자만 접수할 수 있다.
- 사용자 신고와 채팅 신고는 대상 사용자가 해당 파티 참여자여야 한다.
- 자기 자신은 신고할 수 없다.
- 채팅 신고는 신고자 본인도 해당 파티 채팅 참여자여야 한다.
- 같은 신고자, 같은 대상 타입, 같은 파티, 같은 대상 사용자, 같은 사유 조합으로 `RECEIVED` 상태 신고가 이미 있으면 중복 접수를 막는다.

## 현재 한계
- 관리자 검토, 상태 변경, 제재 연결은 아직 없다.
- 채팅 신고는 개별 메시지 ID가 아니라 파티 채팅과 대상 사용자 단위로만 접수한다.
