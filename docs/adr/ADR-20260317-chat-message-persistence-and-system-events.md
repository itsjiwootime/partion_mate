# ADR-20260317-chat-message-persistence-and-system-events

- 날짜: 2026-03-17
- 상태: Accepted
- 관련 작업: `E8-3`
- 관련 파일: `src/main/java/com/project/partition_mate/service/ChatService.java`, `src/main/java/com/project/partition_mate/domain/ChatMessage.java`, `src/main/java/com/project/partition_mate/repository/ChatMessageRepository.java`, `src/main/java/com/project/partition_mate/service/PartyService.java`, `src/main/java/com/project/partition_mate/service/PartyLifecycleService.java`

## 배경
- 채팅은 실시간 전달만으로 끝나지 않고, 사용자가 늦게 들어와도 최근 대화와 운영 이벤트를 확인할 수 있어야 한다.
- 파티 참여, 취소, 대기열 승격, 자동 마감 같은 도메인 이벤트는 별도 알림과 함께 채팅방에도 남아야 사용자 맥락이 끊기지 않는다.
- 메시지 저장과 실시간 브로드캐스트의 시점이 어긋나면 클라이언트에서 본 내용과 DB 상태가 달라질 수 있다.

## 결정사항
- 메시지는 `chat_message` 테이블에 모두 저장하고, 최근 100개를 역순 조회 후 시간순으로 정렬해 채팅방 상세에 내려준다.
- 메시지 타입은 `TEXT`, `SYSTEM` 두 가지로 제한한다. 시스템 메시지는 별도 발신자 없이 `시스템` 이름으로 저장한다.
- 채팅 브로드캐스트는 `ChatService.publishAfterCommit(...)`에서 처리하고, 트랜잭션이 열려 있으면 `afterCommit` 이후 전송한다.
- 시스템 메시지 발생 시점은 채팅방 생성, 파티 참여, 참여 취소, 대기열 승격, 자동 마감, 호스트 공지 변경으로 고정한다.

## 대안
- 메시지를 메모리 큐에만 보관하고 DB 저장 생략
  - 장점: 구현이 단순하고 DB 부하가 적다.
  - 단점: 새로 입장한 사용자가 과거 대화를 볼 수 없고 서버 재시작 시 기록이 사라진다.
- 모든 도메인 이벤트를 알림으로만 처리하고 채팅에는 남기지 않기
  - 장점: 채팅 로그가 더 간결해진다.
  - 단점: 파티 운영 맥락이 채팅에서 단절되고, 사용자가 앱 내 두 위치를 오가야 한다.
- 브로드캐스트를 저장 전에 즉시 전송
  - 장점: 코드가 단순하다.
  - 단점: 저장 실패나 롤백 시 잘못된 메시지가 먼저 노출될 수 있다.

## 결과
- 긍정적
  - 메시지 이력과 실시간 전송이 같은 저장 모델을 공유해 상태 일관성이 좋아졌다.
  - 시스템 메시지가 파티 운영 이벤트를 자연스럽게 설명해 채팅방 자체가 운영 로그 역할도 일부 담당한다.
  - 채팅 상세 API 한 번으로 최근 대화, 공지, unread 초기화까지 함께 처리할 수 있다.
- 부정적
  - 메시지가 계속 누적되므로 장기적으로는 보관 주기나 아카이빙 정책이 필요하다.
  - 최근 100개 기준이라 매우 긴 대화 이력은 별도 페이징이 필요하다.
- 중립적
  - 이후 이미지/파일 메시지를 추가하더라도 현재 `ChatMessageType`과 저장/브로드캐스트 구조를 확장하는 방식으로 대응할 수 있다.
