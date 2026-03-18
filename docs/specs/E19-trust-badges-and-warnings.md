# E19-2 신뢰 배지 및 경고 문구

## 목표
- 참여 전 위험 신호와 신뢰 정보를 파티 상세와 프로필에서 한눈에 이해하게 한다.

## 범위
- `frontend/src/utils/trustSignals.js`
  - `trustLevel`, `reviewCount`, `completionRate`, `noShowCount`를 기반으로 배지, 요약 하이라이트, 경고 문구를 계산한다.
- `frontend/src/pages/PartyDetail.jsx`
  - 호스트 신뢰도 영역에 배지, 후기/완료율/노쇼 하이라이트, 경고 배너를 노출한다.
  - 파티 상단에도 호스트 신뢰 배지를 빠르게 보이게 한다.
- `frontend/src/pages/Profile.jsx`
  - 내 신뢰도 영역에 배지, 하이라이트, 경고 또는 안정 메시지를 노출한다.

## 표시 규칙
- 배지
  - `TOP`: `매우 신뢰`
  - `GOOD`: `신뢰`
  - `NORMAL`: `보통`
  - `NEW`: `신규 거래자`
  - `CAUTION`: `주의 필요`
- 하이라이트
  - `후기`, `완료율`, `노쇼`를 badge 스타일로 보여준다.
  - 수치 상태에 따라 `mint`, `neutral`, `amber`, `rose` 톤을 다르게 쓴다.
- 경고 문구
  - 노쇼 2건 이상: 위험 경고
  - 노쇼 1건: 주의 경고
  - 완료율 70% 미만: 위험 경고
  - 완료율 85% 미만: 주의 경고
  - 별도 위험 신호가 없으면 안정 메시지를 보여준다.

## 검증
- `frontend npm test -- --run src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/PartyDetail.trustSignals.test.jsx src/pages/Chat.safety.test.jsx src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx src/pages/Profile.trustSignals.test.jsx`
- `frontend npm run build`
