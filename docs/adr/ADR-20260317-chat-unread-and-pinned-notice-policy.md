# ADR-20260317-chat-unread-and-pinned-notice-policy

- 날짜: 2026-03-17
- 상태: Accepted
- 관련 작업: `E8-4`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/ChatReadState.java`, `src/main/java/com/project/partition_mate/service/ChatService.java`, `src/main/java/com/project/partition_mate/controller/ChatController.java`, `frontend/src/pages/PartyList.jsx`, `frontend/src/pages/MyParties.jsx`, `frontend/src/pages/Chat.jsx`

## 배경
- 채팅방이 생기면 사용자는 목록에서도 새 메시지 유입과 중요한 운영 공지를 빠르게 확인할 수 있어야 한다.
- 단순 boolean 읽음 여부로는 시스템 메시지와 일반 메시지가 계속 쌓이는 환경에서 정확한 unread 계산이 어렵다.
- 호스트는 픽업 시간, 정산 안내처럼 반복 질문이 많은 정보를 상단에 고정할 수 있어야 했다.

## 결정사항
- 읽음 상태는 `ChatReadState.lastReadMessageId`로 관리한다. unread count는 `chat_message.id > lastReadMessageId` 개수로 계산한다.
- 채팅방 상세 조회와 명시적 `POST /api/chat/rooms/{partyId}/read` 호출 모두 마지막 메시지 ID까지 읽음 처리한다.
- unread count는 채팅방 목록, 파티 목록, 내 파티 화면에 노출한다.
- 호스트 공지는 `ChatRoom.pinnedNotice`와 `pinnedNoticeUpdatedAt`에 저장하고, 수정 권한은 호스트에게만 부여한다. 공지 변경 시 시스템 메시지도 함께 남긴다.

## 대안
- 메시지마다 읽음 유저 목록 저장
  - 장점: 그룹 채팅에서 정밀한 읽음 표시가 가능하다.
  - 단점: 현재 범위에서는 저장 비용과 계산 복잡도가 과하다.
- 마지막 읽은 시각 기반 unread 계산
  - 장점: 구현이 단순하다.
  - 단점: 같은 시각에 생성된 메시지나 서버 시간 차이에 취약하고 정확한 개수 계산이 어렵다.
- 공지를 별도 메시지 타입으로만 관리
  - 장점: 모델 수가 줄어든다.
  - 단점: 최신 공지 단일 값과 수정 시각을 별도로 추적하기 어렵다.

## 결과
- 긍정적
  - unread 계산이 단순한 정수 비교로 정리돼 목록/상세 모두 같은 방식으로 계산된다.
  - 파티 목록과 내 파티 화면에서 채팅 유입을 바로 확인할 수 있게 됐다.
  - 호스트 공지와 시스템 메시지를 함께 남겨 운영 정보 전달력이 높아졌다.
- 부정적
  - 현재는 개별 메시지별 읽음 사용자 목록을 보여주지 못한다.
  - 공지 수정 이력은 시스템 메시지로만 남고, 별도 버전 관리까지 하지는 않는다.
- 중립적
  - 이후 메시지 페이징이나 멘션 기능이 생겨도 `lastReadMessageId` 기반 unread 계산은 그대로 재사용할 수 있다.
