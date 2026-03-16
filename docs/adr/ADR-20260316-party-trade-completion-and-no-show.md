# ADR-20260316-party-trade-completion-and-no-show

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E6-4`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/TradeStatus.java`, `src/main/java/com/project/partition_mate/domain/PartyMember.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`

## 배경
- 송금과 픽업 일정이 확정돼도 실제로 거래가 완료됐는지, 참여자가 나타나지 않았는지는 별도 상태로 관리해야 한다.
- 에픽 7에서 후기 작성 가능 조건을 거래 완료 여부에 연결할 예정이므로, 지금 단계에서 완료/노쇼 상태를 명시적으로 남길 필요가 있었다.
- 노쇼는 결제 상태와 동일하지 않기 때문에, 거래 결과와 금전 상태를 분리한 모델이 필요했다.

## 결정사항
- 거래 상태는 `PENDING`, `COMPLETED`, `NO_SHOW`로 정의하고 `PartyMember.tradeStatus`에 저장한다.
- 호스트만 `PUT /party/{partyId}/members/{memberId}/trade-status`로 참여자 상태를 변경할 수 있게 한다.
- `COMPLETED`는 픽업 일정이 존재하고 결제 상태가 `CONFIRMED`인 경우에만 허용한다. `NO_SHOW`는 픽업 일정만 있으면 기록할 수 있게 한다.
- 참여자의 후기 가능 여부는 별도 테이블이 아닌 `reviewEligible = tradeStatus == COMPLETED` 파생 값으로 응답에 노출한다.

## 대안
- 거래 완료를 파티 전체 상태로만 관리
  - 장점: 모델이 단순하다.
  - 단점: 참여자마다 거래 결과가 다를 수 있는 소분 공동구매 특성을 반영하지 못한다.
- 노쇼를 송금 상태의 일부로 합침
  - 장점: 상태 수가 줄어든다.
  - 단점: 거래 실패와 환불/결제 흐름이 섞여 의미가 불명확해진다.
- 후기 가능 여부를 에픽 7에서만 계산
  - 장점: 현재 에픽 범위가 줄어든다.
  - 단점: 거래 완료 모델이 후기 조건과 분리돼 나중에 상태 정의를 다시 맞춰야 한다.

## 결과
- 긍정적
  - 참여자별 거래 결과를 명확히 기록해 후기, 신뢰도, 알림으로 자연스럽게 이어질 수 있게 됐다.
  - 결제 확인이 된 건만 거래 완료로 처리해 운영 규칙이 일관된다.
  - 노쇼를 별도 상태로 남겨 추후 신뢰도 계산에 바로 사용할 수 있다.
- 부정적
  - 호스트용 관리 버튼과 테스트 케이스가 늘어나 구현량이 증가했다.
  - 현재는 노쇼 사유나 이력 메모를 따로 저장하지 않아 운영 기록이 제한적이다.
- 중립적
  - 이후 에픽 7에서는 `reviewEligible` 파생값을 바탕으로 실제 후기 엔티티 작성 제한을 구현하면 된다.
