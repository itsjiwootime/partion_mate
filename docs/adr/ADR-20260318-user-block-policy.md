# ADR-20260318-user-block-policy

- 날짜: 2026-03-18
- 상태: Accepted
- 관련 작업: `E14-2`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/UserBlock.java`, `src/main/java/com/project/partition_mate/service/UserBlockService.java`, `src/main/java/com/project/partition_mate/service/UserBlockPolicyService.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`, `src/main/java/com/project/partition_mate/service/ChatService.java`, `src/main/java/com/project/partition_mate/security/ChatStompChannelInterceptor.java`

## 배경
- 신고 접수 모델이 추가됐지만, 사용자가 즉시 상대와의 상호작용을 끊을 수 있는 자기 방어 수단은 아직 없었다.
- 현재 서비스의 상호작용 핵심은 파티 참여와 파티 채팅이며, 이 둘을 막지 않으면 차단 API만 있어도 실제 체감 효과가 거의 없다.
- 반대로 초기에 강퇴, 후기 제한, 정산 제한까지 한 번에 연결하면 정책 범위가 급격히 넓어져 운영 규칙과 예외 처리가 복잡해진다.

## 결정사항
- 차단은 방향성 있는 `UserBlock(blocker, blocked)` 모델로 저장한다.
- 사용자는 `POST /api/blocks`, `DELETE /api/blocks/{targetUserId}`, `GET /api/blocks`로 차단/해제/목록 조회를 수행한다.
- 파티 참여 요청 시 요청자와 현재 파티 참여자 사이에 차단 관계가 있으면 참여와 대기열 등록을 모두 막는다.
- 채팅은 REST 진입(`상세`, `읽음`, `공지 수정`)과 메시지 전송, STOMP 구독 모두 같은 차단 규칙을 적용한다.
- 차단 여부 판단은 `UserBlockPolicyService`로 분리해 `PartyService`, `ChatService`, `ChatStompChannelInterceptor`가 같은 정책을 재사용하게 한다.

## 대안
- 차단은 저장만 하고 실제 상호작용 제한은 나중에 붙인다.
  - 장점: 구현이 가장 단순하다.
  - 단점: 사용자가 차단 직후에도 동일한 파티 참여나 채팅 진입을 계속 겪게 되어 기능 체감이 거의 없다.
- 차단 즉시 상대를 파티와 채팅방에서 자동 강퇴한다.
  - 장점: 차단 효과가 즉시 강하게 나타난다.
  - 단점: 현재 파티 운영 모델에는 강퇴/환불/대기열 재정렬 정책이 없어 후속 영향이 너무 크다.
- 채팅만 막고 파티 참여는 허용한다.
  - 장점: 채팅 악용만 우선 차단할 수 있다.
  - 단점: 차단한 상대가 같은 파티에 새로 들어오는 상황을 막지 못해 사용자 기대와 어긋난다.

## 결과
- 긍정적
  - 사용자가 차단한 상대와의 새 파티 참여 및 채팅 상호작용을 즉시 제한할 수 있다.
  - 차단 정책이 `UserBlockPolicyService`로 모여 REST와 STOMP에 같은 규칙이 적용된다.
  - 프론트는 별도 정책 계산 없이 기존 에러 메시지와 차단 목록 API만으로 제한 이유를 노출할 수 있다.
- 부정적
  - 이미 참여 중인 파티의 관계를 자동 정리하지 않으므로 차단 후에도 파티 자체는 그대로 유지된다.
  - 채팅방 목록은 아직 숨기지 않아서 사용자가 진입 시점에야 차단 메시지를 보게 된다.
- 중립적
  - 이후 신고/제재와 연결하려면 차단 사유, 메모, 상호 차단 통계 같은 운영 필드가 필요해질 수 있다.
  - 강퇴나 후기 제한을 붙일 시점에는 정산/거래 상태와 함께 다시 정책을 재정리해야 한다.
