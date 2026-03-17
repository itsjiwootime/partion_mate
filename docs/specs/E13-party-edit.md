# E13 파티 수정 API 스펙

## 목적
- 호스트가 모집 중인 파티의 핵심 정보를 직접 수정할 수 있게 한다.
- 정산 또는 픽업 단계 이후에는 거래 조건 변경을 막고 안내성 정보만 수정하게 한다.

## API

### `PUT /party/{id}`
- 인증 필요
- 파티 호스트만 호출 가능
- 요청 본문
```json
{
  "title": "비타민 대용량 소분",
  "productName": "비타민 C 1000정",
  "totalPrice": 32000,
  "totalQuantity": 4,
  "openChatUrl": "https://open.kakao.com/o/updated-room",
  "deadline": "2026-03-19T10:00:00",
  "unitLabel": "통",
  "minimumShareUnit": 1,
  "storageType": "ROOM_TEMPERATURE",
  "packagingType": "CONTAINER",
  "hostProvidesPackaging": false,
  "onSiteSplit": true,
  "guideNote": "현장 분배 후 전달합니다."
}
```
- 응답
  - `200 OK`
  - `PartyDetailResponse`

## 수정 가능 필드
- `title`
- `productName`
- `totalPrice`
- `totalQuantity`
- `openChatUrl`
- `deadline`
- `unitLabel`
- `minimumShareUnit`
- `storageType`
- `packagingType`
- `hostProvidesPackaging`
- `onSiteSplit`
- `guideNote`

## 수정 불가 필드
- `storeId`
- `hostRequestedQuantity`
- 정산/픽업/거래/후기 관련 필드

## 상태 제한
- `CLOSED` 파티는 수정할 수 없다.
- 마감 시간이 지난 파티는 수정할 수 없다.
- 정산이 확정되었거나 픽업 일정이 잡힌 뒤에는 `guideNote`, `openChatUrl`만 수정할 수 있다.
- `totalQuantity`는 현재 참여 수량보다 작게 줄일 수 없다.

## 후속 동작
- 수정 성공 시 지점 조회 캐시를 무효화한다.
- 수정 후 총 수량 조건이 바뀌면 파티 상태를 다시 계산한다.
  - 예: `FULL` 상태에서 `totalQuantity`를 늘리면 다시 `RECRUITING`으로 복귀할 수 있다.

## 오류 응답 예시
- `403 BUSINESS_ERROR`
  - `파티 수정은 호스트만 처리할 수 있습니다.`
- `409 BUSINESS_ERROR`
  - `이미 종료된 파티입니다.`
  - `마감 시간이 지나 종료된 파티입니다.`
  - `정산 또는 픽업 일정이 진행된 뒤에는 안내 문구와 오픈채팅 링크만 수정할 수 있습니다.`
- `400 VALIDATION_ERROR`
  - DTO 필수값 누락 또는 형식 오류
- `400 INVALID_REQUEST`
  - `총 수량은 현재 참여 수량보다 적을 수 없습니다.`
