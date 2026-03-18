# E19-3 차단/신고 후 상태 피드백

## 목표
- 신고 또는 차단 이후 사용자가 다음에 무엇이 바뀌는지 바로 이해하게 한다.

## 범위
- `frontend/src/components/SafetyFeedback.jsx`
  - 신고 완료 상태 배너와 차단 후 fallback 카드를 공통 컴포넌트로 제공한다.
- `frontend/src/pages/PartyDetail.jsx`
  - 신고 후 `내 신고 내역 보기` 배너를 보여준다.
  - 호스트 차단 후 참여/채팅 제한을 설명하는 fallback을 참여 카드에 노출한다.
- `frontend/src/pages/Chat.jsx`
  - 신고 후 `내 신고 내역 보기` 배너를 보여준다.
  - 차단 후 또는 서버가 차단 접근을 반환한 경우 채팅 상세 대신 fallback 화면을 보여준다.
- `frontend/src/pages/JoinParty.jsx`
  - 차단 관계로 참여가 거절되면 일반 오류 대신 안전 fallback을 보여준다.
- `frontend/src/pages/Profile.jsx`
  - 다른 화면에서 넘어온 `focusSafetyCenter`, `safetyNotice` state를 읽어 `신뢰·안전 관리` 카드에 안내 배너를 노출한다.

## 상태 규칙
- 신고 성공
  - 즉시 성공 토스트를 보여준다.
  - 같은 화면에 `내 신고 내역 보기` 배너를 추가한다.
  - 버튼을 누르면 프로필의 `신뢰·안전 관리` 카드로 이동한다.
- 차단 성공
  - 즉시 성공 토스트를 보여준다.
  - 현재 화면에서 접근 제한 fallback을 보여준다.
  - fallback에는 `차단 관리 열기`와 다른 화면으로 이동하는 CTA를 둔다.
- 차단된 채팅/파티 접근
  - 서버 메시지가 차단 제한일 때 일반 오류 대신 fallback 화면을 보여준다.

## 검증
- `frontend npm test -- --run src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/PartyDetail.trustSignals.test.jsx src/pages/Chat.safety.test.jsx src/pages/JoinParty.safetyFeedback.test.jsx src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx src/pages/Profile.trustSignals.test.jsx`
- `frontend npm run build`
