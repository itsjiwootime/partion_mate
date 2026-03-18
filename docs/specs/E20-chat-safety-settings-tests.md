# E20 채팅 및 안전 기능 테스트 스펙

- 작성일: 2026-03-18
- 대상 작업: `E20-3`
- 관련 화면: `frontend/src/pages/Chat.jsx`, `frontend/src/pages/Profile.jsx`, `frontend/src/pages/Notifications.jsx`
- 관련 테스트: `frontend/src/pages/Chat.flow.test.jsx`, `frontend/src/pages/Chat.safety.test.jsx`, `frontend/src/pages/Profile.notificationSettings.test.jsx`, `frontend/src/pages/Profile.safetyCenter.test.jsx`, `frontend/src/pages/Notifications.deepLink.test.jsx`, `frontend/src/pages/Notifications.state.test.jsx`

## 목적
- 채팅, 신고/차단, 브라우저 푸시 설정, 알림 화면 상태 처리가 회귀 없이 유지되게 한다.
- 안전 기능은 단순 렌더링이 아니라, 실제 액션 이후 상태 전환과 후속 진입점까지 검증한다.

## 검증 대상 흐름
- `Chat`
  - 채팅방 목록에서 방 선택 후 상세 로드와 읽음 처리
  - 호스트 공지 저장 성공
  - 메시지 단위 차단 후 fallback 노출
- `Profile`
  - 타입별 브라우저 푸시 토글 저장
  - 브라우저 푸시 전체 토글 저장
  - 현재 브라우저의 Web Push 연결
  - 현재 브라우저의 Web Push 연결 해제
  - 차단 목록 확인과 차단 해제
  - 안전 센터 focus state와 안내 배너 노출
- `Notifications`
  - 채팅 딥링크 알림 이동
  - 비로그인 사용자의 로그인 가드
  - 조회 실패 후 다시 불러오기 복구

## 테스트 전략
- `Chat`은 목록과 상세가 같은 화면에 공존하므로 `MemoryRouter` 기반 테스트로 방 선택과 상세 액션을 함께 본다.
- `Profile`은 계정 설정과 브라우저 환경 의존성이 크므로 Web Push 유틸을 전부 mock하고 계정 기준 설정 저장 흐름에 집중한다.
- `Notifications`는 링크 이동, 로그인 가드, 오류 복구를 나눠 화면 상태 전환을 검증한다.

## 검증 명령
- `cd frontend && npm test -- --run src/pages/Chat.flow.test.jsx src/pages/Chat.safety.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx src/pages/Notifications.deepLink.test.jsx src/pages/Notifications.state.test.jsx`
- `cd frontend && npm run build`

## 제외 사항
- STOMP 실시간 메시지 수신과 SSE fallback 자체는 기존 실시간 카드의 책임으로 두고, 이번 카드에서는 화면 상호작용과 결과 상태만 검증한다.
- 브라우저 실제 권한 팝업이나 서비스워커 등록 성공 여부는 mock 기반 범위 밖으로 둔다.
