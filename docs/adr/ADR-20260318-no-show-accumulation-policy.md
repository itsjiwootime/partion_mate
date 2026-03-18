# ADR-20260318-no-show-accumulation-policy

- 날짜: 2026-03-18
- 상태: Accepted
- 관련 작업: `E14-3`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/PartyMember.java`, `src/main/java/com/project/partition_mate/service/NoShowPolicyService.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`, `src/main/java/com/project/partition_mate/dto/JoinPartyResponse.java`

## 배경
- 노쇼 기록은 이미 `TradeStatus.NO_SHOW`로 남기고 있었지만, 반복 노쇼 사용자의 새 파티 재참여를 제어하는 운영 정책은 없었다.
- 총 노쇼 횟수만 세면 오래된 노쇼까지 계속 제한에 반영돼 회복 경로가 불명확해지고, 반대로 최근 연속 노쇼를 보려면 거래 상태가 언제 확정됐는지 판단할 기준이 필요했다.
- 참여 제한은 단순 차단보다 사용자 안내가 중요하므로, 경고 단계와 차단 단계를 함께 정의할 응답 모델도 필요했다.

## 결정사항
- 노쇼 정책은 참여자 기준 최근 확정 거래 이력에서 `COMPLETED`를 만나기 전까지의 연속 `NO_SHOW` 개수를 현재 누적으로 계산한다.
- 최근 연속 노쇼가 `1회`면 참여는 허용하되 `JoinPartyResponse.warning`에 `NO_SHOW_CAUTION` 경고를 담아 반환한다.
- 최근 연속 노쇼가 `2회 이상`이면 `POST /party/{id}/join`에서 참여와 대기열 등록을 모두 차단한다.
- 최근 거래 순서를 안정적으로 계산하기 위해 `PartyMember`에 `tradeStatusUpdatedAt`을 추가하고, `COMPLETED`/`NO_SHOW` 기록 시각을 함께 저장한다.
- 정책 계산은 `NoShowPolicyService`로 분리하고, `PartyService.joinParty()`가 결과에 따라 경고 또는 차단을 적용한다.

## 대안
- 총 누적 노쇼 횟수만으로 `1회 경고`, `2회 차단`을 결정한다.
  - 장점: 구현이 단순하고 추가 필드가 필요 없다.
  - 단점: 예전에 한 번 실수한 사용자도 회복 조건 없이 계속 제한 단계에 묶여 운영 정책이 과도해진다.
- 노쇼가 생기면 관리자 승인 전까지 무조건 차단한다.
  - 장점: 운영자가 직접 해제 여부를 통제할 수 있다.
  - 단점: 현재 관리자 도구가 없어 실제 운영 흐름을 만들기 어렵고, 사용자 경험도 지나치게 무겁다.
- `tradeStatusUpdatedAt` 없이 `PartyMember.id` 또는 `Party.deadline` 순서로 최근 거래를 추정한다.
  - 장점: 스키마 변경 없이 빠르게 붙일 수 있다.
  - 단점: 실제 거래 상태가 확정된 순서를 보장하지 못해 해제 조건 계산이 틀어질 수 있다.

## 결과
- 긍정적
  - 최근 연속 노쇼만 기준으로 삼아 사용자에게 명확한 경고와 회복 경로를 제공할 수 있다.
  - 참여 응답에 경고 메타데이터가 포함돼 프론트가 별도 계산 없이 안내 문구를 노출할 수 있다.
  - `NoShowPolicyService`로 계산 로직이 분리돼 이후 프로필, 관리자 화면, 제재 정책에서 재사용하기 쉬워졌다.
- 부정적
  - `tradeStatusUpdatedAt`이 추가돼 기존 `TradeStatus` 변경 로직과 테스트를 함께 관리해야 한다.
  - 총 노쇼 횟수 기반 신뢰도 지표와 최근 연속 노쇼 기반 참여 제한은 서로 다른 수치를 보여줄 수 있다.
- 중립적
  - 이후 운영 정책이 바뀌면 경고 임계치나 해제 조건을 `NoShowPolicyService`에서 조정하면 된다.
  - 관리자 제재 단계가 추가되면 현재 자동 정책 위에 수동 오버라이드 규칙을 한 번 더 얹어야 한다.
