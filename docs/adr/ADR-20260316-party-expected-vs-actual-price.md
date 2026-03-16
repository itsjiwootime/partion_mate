# ADR-20260316-party-expected-vs-actual-price

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E5-3`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/Party.java`, `src/main/java/com/project/partition_mate/dto/PartyDetailResponse.java`, `src/main/java/com/project/partition_mate/dto/PartyResponse.java`, `src/test/java/com/project/partition_mate/service/PartyMetadataIntegrationTest.java`

## 배경
- 트레이더스/코스트코 상품은 행사 할인, 현장 가격 차이, 카드 할인 등으로 인해 파티 생성 시점의 예상 총액과 실제 결제 총액이 달라질 수 있다.
- 기존에는 `totalPrice` 하나만 있어 모집 시 안내용 금액과 정산 기준 금액을 구분할 수 없었고, 이후 정산 에픽에서 금액 전이 규칙을 설계하기 어려웠다.
- 하지만 지금 단계에서 정산 도메인 전체를 미리 도입하면 범위가 커지므로, 우선 가격 표현 구조만 열어두는 접근이 필요했다.

## 결정사항
- 기존 `Party.totalPrice`는 예상가 역할로 유지하고, `actualTotalPrice`와 `receiptNote`를 추가해 실제 구매 이후 금액과 영수증 메모를 별도로 저장한다.
- 응답 DTO에서는 기존 `totalPrice` 필드를 화면 표시용 총액으로 유지하되, `expectedTotalPrice`와 `actualTotalPrice`를 함께 내려 하위 호환성과 후속 정산 확장성을 동시에 확보한다.
- 도메인에는 `updateActualPurchase(...)`, `getExpectedTotalPrice()`, `getDisplayTotalPrice()`를 추가해 가격 전이와 화면 표시 규칙을 명시적으로 분리한다.
- 실구매가 수정용 전용 API는 아직 만들지 않고, 에픽 6의 정산 흐름에서 호스트 확정 API와 함께 노출할 수 있도록 구조만 준비한다.

## 대안
- `totalPrice` 하나만 계속 사용하고 실제 구매 후 값을 덮어쓴다.
  - 장점: 스키마 변경이 가장 적다.
  - 단점: 모집 시 안내 가격과 정산 기준 금액 이력이 사라지고, 가격 변경 사유를 설명하기 어렵다.
- 예상가/실구매가를 별도 정산 엔티티로 바로 분리한다.
  - 장점: 정산 모델과 금액 이력이 더 명확해진다.
  - 단점: 현재 에픽 범위를 넘어가며, 송금 상태/참여자별 정산까지 함께 설계해야 해 변경량이 급격히 커진다.
- 영수증 URL이나 파일 업로드까지 함께 도입한다.
  - 장점: 실제 증빙을 바로 연결할 수 있다.
  - 단점: 스토리지, 업로드 정책, 보안 범위가 추가돼 현재 개인 프로젝트 단계에서 과하다.

## 결과
- 긍정적
  - 모집 단계 가격과 실제 구매 후 가격을 모두 보존해 후속 정산 기능으로 자연스럽게 이어질 수 있다.
  - 기존 화면은 `display total` 규칙으로 크게 깨지지 않으면서도, 상세에서는 예상가/실구매가를 함께 보여줄 수 있게 됐다.
  - `PartyMetadataIntegrationTest`로 가격 전이와 응답 노출 규칙을 회귀 검증할 수 있게 됐다.
- 부정적
  - DTO 필드 수가 늘어 목록/상세/실시간 응답 매핑이 더 복잡해졌다.
  - `totalPrice`가 더 이상 순수한 저장값 의미만 갖지 않고 화면 표시용 의미를 같이 갖게 되어, 팀 내 규칙을 문서로 유지해야 한다.
- 중립적
  - 실제 정산 확정 권한과 참여자별 금액 계산은 아직 에픽 6 범위로 남아 있으며, 현재 결정은 그 확장을 위한 사전 구조화 단계다.
