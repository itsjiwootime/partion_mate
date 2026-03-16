# ADR-20260317-websocket-stomp-chat-transport

- 날짜: 2026-03-17
- 상태: Accepted
- 관련 작업: `E8-2`
- 관련 파일: `src/main/java/com/project/partition_mate/config/WebSocketConfig.java`, `src/main/java/com/project/partition_mate/security/ChatStompChannelInterceptor.java`, `src/main/java/com/project/partition_mate/controller/ChatMessageController.java`, `src/test/java/com/project/partition_mate/service/ChatWebSocketIntegrationTest.java`, `docs/specs/E8-party-chat.md`

## 배경
- 채팅은 파티 실시간 상태 반영과 달리 양방향 메시지 송수신이 필요하므로 SSE보다 WebSocket 계열이 적합하다.
- 현재 인증 구조는 JWT Bearer 헤더 기반이며, 브라우저 인앱 채팅 화면에서는 별도 폴링 없이 자연스럽게 재연결되는 클라이언트 구성이 필요했다.
- 메시지 발송과 구독 모두 파티 참여 권한과 연결된 세션 인증이 필요했다.

## 결정사항
- 채팅 전송 채널은 `WebSocket + STOMP`로 구현한다.
- 엔드포인트는 `/ws-chat`, 발행 경로는 `/app/chat/{partyId}/messages`, 구독 경로는 `/topic/chat/{partyId}`로 고정한다.
- 인증은 `CONNECT` 프레임의 `Authorization: Bearer <token>` 헤더를 기준으로 처리하고, `ChatStompChannelInterceptor`가 세션 속성에 토큰을 저장해 `SUBSCRIBE`, `SEND`, `MessageMapping` 처리 시 재사용한다.
- 브라우저 클라이언트는 `@stomp/stompjs`를 사용하고, 재연결은 라이브러리 기본 `reconnectDelay` 정책으로 처리한다.

## 대안
- SSE 사용
  - 장점: 구현이 단순하고 E4와 같은 방식으로 운영할 수 있다.
  - 단점: 채팅은 양방향 입력이 핵심이라 별도 POST API와 재조회가 필요해져 UX가 떨어진다.
- 순수 WebSocket JSON 프로토콜 사용
  - 장점: 프레임 오버헤드가 적고 제어가 단순할 수 있다.
  - 단점: 구독/브로커/재연결 관리를 직접 구현해야 해 현재 범위 대비 비용이 크다.
- SockJS fallback 포함
  - 장점: 구형 브라우저 대응 범위가 넓어진다.
  - 단점: 현재 타깃 환경에서는 필요성이 낮고 클라이언트/서버 설정이 늘어난다.

## 결과
- 긍정적
  - REST 조회와 분리된 실시간 양방향 채널을 구성해 메시지 전송 UX가 자연스러워졌다.
  - `ChatWebSocketIntegrationTest`로 실제 WebSocket 세션, 인증, 메시지 왕복을 통합 검증할 수 있게 됐다.
  - 세션 속성 토큰 복원으로 STOMP 세션과 `MessageMapping` 사이의 인증 누락 문제를 해소했다.
- 부정적
  - 현재는 단일 애플리케이션 인스턴스 기준이며, 다중 인스턴스 확장 시 별도 브로커가 필요하다.
  - 연결 기반 프로토콜이라 SSE보다 서버 리소스 사용량과 운영 복잡도가 높다.
- 중립적
  - 이후 Redis Pub/Sub나 외부 브로커를 붙이더라도 STOMP destination 계약은 그대로 유지할 수 있다.
