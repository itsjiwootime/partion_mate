# ADR-20260317-party-chat-domain-and-access-policy

- 날짜: 2026-03-17
- 상태: Accepted
- 관련 작업: `E8-1`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/ChatRoom.java`, `src/main/java/com/project/partition_mate/domain/ChatMessage.java`, `src/main/java/com/project/partition_mate/domain/ChatReadState.java`, `src/main/java/com/project/partition_mate/service/ChatService.java`, `src/main/java/com/project/partition_mate/repository/PartyMemberRepository.java`

## 배경
- 에픽 8의 목표는 파티별 인앱 채팅을 추가하되, 현재 공동구매 도메인과 자연스럽게 연결되는 최소 모델을 만드는 것이다.
- 개인 채팅이나 범용 메신저 기능까지 확장하면 현재 서비스의 핵심인 파티 운영 흐름보다 채팅 자체가 더 큰 복잡도로 커질 수 있다.
- 파티 참여자만 채팅을 사용할 수 있어야 하고, 나중에 읽음 처리와 공지 고정까지 확장할 수 있는 구조가 필요했다.

## 결정사항
- 채팅 모델은 `Party` 기준 1:1 `ChatRoom` 구조로 고정한다. 파티가 생성되면 채팅방도 함께 생성되고, 별도 방 목록 생성 API는 두지 않는다.
- 메시지는 `ChatMessage`, 읽음 위치는 `ChatReadState`로 분리한다. 별도의 `chat_member` 테이블은 만들지 않고 기존 `PartyMember`를 채팅 참여 권한의 단일 기준으로 사용한다.
- 접근 권한은 `PartyMemberRepository.findByPartyAndUser(...)`, `existsByPartyIdAndUserId(...)`로 검증한다. REST 조회와 메시지 발송 모두 현재 파티 참여자인지 확인한 뒤 처리한다.
- 채팅방 목록은 `partyMemberRepository.findByUser(user)`를 기준으로 구성하고, 파티 제목/지점명/오픈채팅 링크를 함께 내려준다.

## 대안
- `chat_member` 별도 테이블 도입
  - 장점: 음소거, 강퇴, 역할 분리 같은 채팅 전용 상태를 더 세밀하게 다룰 수 있다.
  - 단점: 현재는 파티 참여 여부와 채팅 참여 여부가 같아 중복 모델이 된다.
- 파티당 여러 채팅방 허용
  - 장점: 호스트 공지방, 실시간 협의방 같은 확장이 가능하다.
  - 단점: MVP 범위에서 UI와 권한 모델이 과도하게 복잡해진다.
- 파티 상세에 메시지를 직접 임베드하고 독립 채팅방 엔티티를 두지 않기
  - 장점: 모델 수가 줄어든다.
  - 단점: 읽음 상태, 최근 메시지, 고정 공지 같은 기능을 확장하기 어렵다.

## 결과
- 긍정적
  - 기존 `PartyMember`를 그대로 재사용해 권한 기준이 단일화됐다.
  - 파티 생성, 참여, 취소, 마감 이벤트와 채팅 시스템 메시지를 자연스럽게 연결할 수 있게 됐다.
  - `ChatRoomFlowIntegrationTest`로 채팅방 생성, 권한, 읽음 처리 흐름을 검증할 수 있다.
- 부정적
  - 현재 구조는 파티 외부 사용자를 초대하거나, 파티 내부 역할을 별도로 나누는 요구에 약하다.
  - 채팅방이 파티와 1:1이라 향후 다중 채널 요구가 생기면 마이그레이션이 필요하다.
- 중립적
  - 추후 음소거/강퇴/공지 작성 권한 세분화가 필요해지면 `ChatReadState`와 별도 `chat_member` 모델을 추가하는 방향으로 확장할 수 있다.
