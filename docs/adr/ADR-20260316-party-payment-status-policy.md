# ADR-20260316-party-payment-status-policy

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E6-2`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/PaymentStatus.java`, `src/main/java/com/project/partition_mate/domain/PartyMember.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`

## 배경
- 정산 금액만 계산해도 실제 거래 운영에는 부족하고, 누가 송금을 완료했고 호스트가 확인했는지 추적할 수 있어야 했다.
- 참여자와 호스트가 수행할 수 있는 액션이 다르기 때문에 상태 전이와 권한을 같이 정의하지 않으면 임의 변경이 생길 수 있다.
- 노쇼나 환불처럼 정산 이후 후속 처리도 고려해야 하므로, 단순 boolean 대신 상태 모델이 필요했다.

## 결정사항
- 송금 상태는 `NOT_REQUIRED`, `PENDING`, `PAID`, `CONFIRMED`, `REFUNDED`로 정의한다.
- 호스트는 정산 대상이 아니므로 항상 `NOT_REQUIRED`를 사용한다.
- 참여자는 본인 상태를 `PENDING -> PAID`로만 변경할 수 있고, 호스트는 `PAID -> CONFIRMED` 또는 `PAID/CONFIRMED -> REFUNDED`만 처리할 수 있게 한다.
- 상태 변경 API는 `PUT /party/{partyId}/members/{memberId}/payment` 하나로 통일하되, 서비스에서 요청자와 대상 참여자를 비교해 권한과 전이 규칙을 검증한다.

## 대안
- 참여자와 호스트용 송금 API를 별도로 분리
  - 장점: 각 API 책임이 더 분명하다.
  - 단점: 엔드포인트 수가 늘고, 상태 검증 로직이 중복된다.
- `PAID`와 `CONFIRMED`를 하나로 합침
  - 장점: 상태 수가 줄어 화면이 단순해진다.
  - 단점: 참여자 송금 완료와 호스트 확인 완료를 구분할 수 없어 운영 신뢰성이 떨어진다.
- 환불 상태 없이 거래 상태만으로 처리
  - 장점: 모델이 단순하다.
  - 단점: 금전 흐름과 거래 결과를 분리할 수 없어 노쇼/환불 시나리오를 설명하기 어렵다.

## 결과
- 긍정적
  - 참여자와 호스트의 책임이 분리돼 잘못된 송금 상태 변경을 서비스 레이어에서 막을 수 있게 됐다.
  - 내 파티/상세 화면에서 현재 송금 상태를 명확히 보여줄 수 있게 됐다.
  - 환불 상태가 있어 노쇼나 거래 실패 후 후속 처리를 연결하기 쉬워졌다.
- 부정적
  - 상태 수가 늘어 화면 액션 조건과 테스트 케이스가 증가했다.
  - 현재는 부분 환불이나 분할 송금을 다루지 않으므로 복잡한 정산 시나리오는 범위 밖이다.
- 중립적
  - 향후 외부 결제 연동이 생기면 `PAID` 진입 트리거만 외부 이벤트로 바꾸고 상태 모델은 그대로 재사용할 수 있다.
