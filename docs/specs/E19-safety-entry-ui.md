# E19-1 신고/차단 진입 UI

## 목표
- 파티 상세, 채팅, 프로필에서 신고와 차단 흐름을 쉽게 찾고 바로 실행할 수 있게 한다.

## 화면 범위
- `frontend/src/pages/PartyDetail.jsx`
  - `신뢰·안전` 카드에 `파티 신고`, `호스트 신고`, `호스트 차단` 버튼을 노출한다.
  - 호스트 본인이 자신의 파티를 보는 경우 `호스트 신고`, `호스트 차단`은 노출하지 않는다.
- `frontend/src/pages/Chat.jsx`
  - 채팅 헤더에 `파티 신고` 버튼을 둔다.
  - 다른 사용자의 메시지 아래에 `신고`, `차단` 버튼을 노출한다.
- `frontend/src/pages/Profile.jsx`
  - `신뢰·안전 관리` 카드에서 차단한 사용자 목록과 최근 신고 내역을 보여준다.
  - 차단 해제는 확인 모달을 거친다.

## 상호작용 규칙
- 신고는 공통 모달에서 처리한다.
  - 대상별 사유 목록을 다르게 보여준다.
  - 메모는 선택 입력으로 둔다.
  - 완료 시 성공 토스트를 노출한다.
- 차단은 공통 확인 모달에서 처리한다.
  - 차단 이후 같은 파티/채팅 참여 제한을 안내한다.
  - 완료 시 성공 토스트를 노출한다.
- 비로그인 사용자가 파티 상세에서 신고/차단을 누르면 로그인 화면으로 이동한다.

## API 사용
- `POST /api/reports`
- `GET /api/reports/me`
- `POST /api/blocks`
- `GET /api/blocks`
- `DELETE /api/blocks/{targetUserId}`

## 검증
- `frontend npm test -- --run src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/Chat.safety.test.jsx src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx`
- `frontend npm run build`
