# ADR-20260316-party-realtime-sse-choice

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E4-1`
- 관련 파일: `src/main/java/com/project/partition_mate/controller/PartyRealtimeController.java`, `src/main/java/com/project/partition_mate/service/PartyRealtimeService.java`, `docs/specs/E4-party-realtime-events.md`

## 배경
- 에픽 4의 목표는 파티 목록, 상세, 내 파티 화면이 모집 상태를 새로고침 없이 따라오게 만드는 것이다.
- 프로젝트 기본 정책은 SSE 우선이지만, 현재 인증 구조는 JWT Bearer 헤더 기반이라 브라우저 기본 `EventSource`에 커스텀 인증 헤더를 붙이기 어렵다.
- 공개 파티 정보만 실시간 반영하면 대부분의 UX를 해결할 수 있기 때문에, 복잡한 양방향 연결보다 단방향 상태 전파가 더 적합했다.

## 결정사항
- 실시간 채널은 `GET /party/stream` 공개 SSE 엔드포인트로 제공한다.
- 스트림은 `connected`, `party-updated` 두 이벤트만 정의하고, payload는 현재 파티 스냅샷과 `realtimeTrigger`를 함께 보낸다.
- 구독 범위는 `storeId`, `partyId` query parameter 필터로 좁힐 수 있게 한다.
- 사용자별 비공개 데이터는 SSE에 넣지 않고, 보호된 화면은 기존 인증 API 재조회로 보완한다.

## 대안
- WebSocket/STOMP 사용
  - 장점: 양방향 통신과 복잡한 채널 제어에 유리하다.
  - 단점: 현재 범위는 서버 푸시가 핵심이라 오버헤드가 크고 인증 처리도 더 복잡해진다.
- 폴링만 사용
  - 장점: 구현이 단순하고 기존 API만으로 가능하다.
  - 단점: 반영 지연이 크고 불필요한 재조회가 많아진다.
- JWT를 query parameter에 실어 사용자별 SSE를 구성
  - 장점: `EventSource` 제한을 우회해 인증 스트림을 만들 수 있다.
  - 단점: URL 노출 위험이 있고 현재 보안 모델과 맞지 않는다.

## 결과
- 긍정적
  - 브라우저 기본 API만으로 파티 상태를 실시간 반영할 수 있게 됐다.
  - 공개 파티 기준으로 목록/상세/내 파티가 같은 이벤트 스키마를 재사용한다.
  - `storeId`, `partyId` 필터로 구독 범위를 줄여 불필요한 이벤트를 낮췄다.
- 부정적
  - 사용자별 비공개 이벤트는 스트림에 직접 실을 수 없어 fallback 조회가 필요하다.
  - 현재는 서버 재시작 시 스트림이 끊기며, 이벤트 재전송 기능도 없다.
- 중립적
  - 이후 채팅이나 사용자별 푸시가 복잡해지면 WebSocket을 별도 채널로 병행할 여지는 남겨두었다.
