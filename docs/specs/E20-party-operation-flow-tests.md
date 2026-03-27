# E20 참여/정산/후기 플로우 테스트 스펙

- 작성일: 2026-03-18
- 대상 작업: `E20-2`
- 관련 화면: `frontend/src/pages/JoinParty.jsx`, `frontend/src/pages/PartyDetail.jsx`
- 관련 테스트: `frontend/src/pages/JoinParty.flow.test.jsx`, `frontend/src/pages/JoinParty.safetyFeedback.test.jsx`, `frontend/src/pages/PartyDetail.operationFlow.test.jsx`, `frontend/src/pages/PartyDetail.favorite.test.jsx`, `frontend/src/pages/PartyDetail.safety.test.jsx`, `frontend/src/pages/PartyDetail.trustSignals.test.jsx`

## 목적
- 참여 이후 실제 운영에 가까운 사용자 흐름을 프론트 테스트로 고정한다.
- 참여 성공, 참여 실패, 호스트 운영, 참여자 후속 액션, 후기 작성이 회귀 없이 유지되게 한다.

## 검증 대상 흐름
- `JoinParty`
  - 즉시 참여 성공 시 파티 상세 화면 복귀
  - 잔여 수량 부족 시 참여 버튼 비활성화와 오류 안내 노출
  - 차단 관계로 참여가 막힐 때 fallback 노출
- `PartyDetail`
  - 호스트가 실구매 총액과 영수증 메모를 저장
  - 호스트가 픽업 장소와 시간을 확정
  - 참여자가 송금 완료를 표시
  - 참여자가 픽업 일정 확인을 완료
  - 참여자가 호스트 후기를 작성

## 테스트 전략
- `JoinParty`는 결과별 네비게이션 차이가 크므로 라우터 이동까지 포함해 검증한다.
- `PartyDetail`은 호스트와 참여자 역할을 분리한 fixture를 사용해 같은 화면에서 역할별 운영 액션을 검증한다.
- 기존 관심 저장, 신고/차단, 신뢰 배지 테스트는 유지하고, 이번 카드에서는 운영 액션 회귀만 추가로 묶는다.

## 검증 명령
- `cd frontend && npm test -- --run src/pages/JoinParty.flow.test.jsx src/pages/JoinParty.safetyFeedback.test.jsx src/pages/PartyDetail.operationFlow.test.jsx src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/PartyDetail.trustSignals.test.jsx`
- `cd frontend && npm run build`

## 제외 사항
- 호스트의 참여자별 송금 확인, 환불 처리, 거래 완료, 노쇼 처리 버튼은 이번 카드 범위에서 제외한다.
- 실시간 이벤트에 따른 재조회와 STOMP/SSE 동기화는 `E20-3`이 아니라 기존 실시간 카드 테스트 범위에 둔다.
