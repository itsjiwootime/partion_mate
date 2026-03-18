# E16 관심 파티 저장 스펙

- 작성일: 2026-03-18
- 대상 작업: `E16-3`
- 관련 백엔드: `src/main/java/com/project/partition_mate/controller/UserController.java`, `src/main/java/com/project/partition_mate/service/FavoritePartyService.java`
- 관련 프론트: `frontend/src/pages/PartyList.jsx`, `frontend/src/pages/PartyDetail.jsx`, `frontend/src/pages/FavoriteParties.jsx`

## 목적
- 사용자가 나중에 다시 보고 싶은 파티를 목록과 상세에서 바로 저장할 수 있게 한다.
- 저장한 파티를 한 곳에서 다시 확인하고, 저장 해제까지 같은 흐름에서 처리하게 한다.
- 기존 공개 조회 API 구조를 유지하면서 로그인 사용자의 관심 상태만 응답에 덧붙인다.

## 데이터 모델
- 관심 파티는 `favorite_party` 엔티티로 저장한다.
- 키 구성
  - `id`
  - `user_id`
  - `party_id`
  - `created_at`
- 제약 조건
  - `(user_id, party_id)` 유니크
  - 사용자 기준 최신 저장 순 목록 조회를 위한 인덱스

## API
- `GET /api/users/me/favorite-parties`
  - 현재 사용자가 저장한 관심 파티 목록을 최신 저장 순으로 반환한다.
- `PUT /api/users/me/favorite-parties/{partyId}`
  - 관심 파티 저장.
  - 이미 저장된 경우 성공으로 처리한다.
- `DELETE /api/users/me/favorite-parties/{partyId}`
  - 관심 파티 해제.
  - 저장 이력이 없어도 성공으로 처리한다.
- `GET /party/all`
- `GET /party/{id}`
- `GET /api/stores/{storeId}/parties`
  - 인증 사용자가 호출하면 응답에 `favorite` boolean을 함께 포함한다.
  - 비로그인 사용자는 항상 `favorite=false`로 본다.

## 프론트 동작
- 파티 목록 카드
  - `저장/저장됨` 버튼으로 관심 파티를 토글한다.
  - 비로그인 사용자가 누르면 로그인 화면으로 이동한다.
- 파티 상세
  - 제목 영역에서 관심 파티 저장/해제를 처리한다.
  - 저장 후 현재 화면 상태를 즉시 갱신한다.
- 내 관심 파티
  - 경로는 `/favorite-parties`
  - 프로필 화면에서 진입한다.
  - 저장한 파티만 모아 보여주고, 여기서 바로 저장 해제가 가능하다.

## 상태 동기화
- 관심 파티 저장/해제는 프론트에서 낙관적으로 반영한 뒤 실패 시 원복한다.
- `FavoriteParties` 화면은 실시간 파티 스트림을 구독해 모집 상태와 읽지 않은 채팅 수를 다시 반영한다.
- 관심 여부 자체는 SSE 이벤트 대상이 아니므로 저장/해제는 현재 화면 기준으로만 즉시 반영한다.

## 현재 한계
- 관심 파티 수량 제한, 메모, 폴더 분류는 아직 없다.
- 홈 화면 카드에는 아직 관심 파티 토글을 두지 않았다.
- 관심 상태는 공개 조회 응답에 붙지만, 서버 전용 관심 파티 추천/정렬에는 아직 사용하지 않는다.
