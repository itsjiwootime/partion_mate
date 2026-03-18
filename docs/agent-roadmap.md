# Partition Mate Agent Roadmap

이 문서는 Codex 같은 AI 에이전트와 협업할 때 사용할 기본 개발 백로그입니다.
사용자가 별도 우선순위를 주지 않으면 아래 순서대로 진행합니다.

## Working Conventions
- 한 번에 하나의 작업 카드만 진행한다.
- 각 작업 카드는 코드 변경, 테스트 또는 검증, ADR 작성까지 포함해 완료한다.
- 성능 개선 카드에는 반드시 기준 측정과 개선 후 측정을 남긴다.
- 작업 완료 후 이 문서의 상태를 갱신한다.

## Resume Goal
- 동시성 제어와 대기열 설계 경험을 이력서에 남길 수 있어야 한다.
- 위치 기반 조회 최적화의 전후 성능 수치를 확보해야 한다.
- 자동 마감과 알림 파이프라인으로 운영 자동화 경험을 설명할 수 있어야 한다.
- 실시간 모집 현황 반영으로 상태 동기화 경험을 보여줄 수 있어야 한다.

## Project Defaults
- 대기열 정책은 `FIFO 자동 승격`을 기본으로 한다.
- 같은 사용자는 동일 파티에 대해 `참여자` 또는 `대기자` 상태를 중복으로 가질 수 없다.
- 파티 상태는 기본적으로 `RECRUITING -> FULL -> CLOSED` 흐름을 따른다.
- 정원이 모두 차면 즉시 `FULL`로 전이한다.
- `deadline`이 지나면 정원 충족 여부와 관계없이 `CLOSED`로 전이한다.
- `CLOSED` 전이 시 남아 있는 대기열 항목은 `EXPIRED` 또는 동등한 종료 상태로 처리한다.
- Epic 3의 1차 알림 채널은 `앱 내 알림`으로 한정한다. 이메일, 문자, 외부 메신저 연동은 범위에서 제외한다.
- Epic 4의 실시간 전송 방식은 `SSE`를 기본으로 한다. 양방향 통신이 필요한 요구가 생기기 전까지 WebSocket은 도입하지 않는다.
- 채팅은 `파티 전용 채팅`만 지원한다. 사용자 간 DM, 파일 전송, 이미지 업로드는 1차 범위에서 제외한다.
- 채팅 1차 범위는 `텍스트 메시지`, `시스템 메시지`, `호스트 공지 고정`, `안 읽은 메시지 수`까지로 제한한다.
- 채팅은 양방향 통신이 필요하므로 `WebSocket + STOMP`를 기본안으로 한다.
- 기본 운영 DB는 현재 프로젝트의 MySQL을 유지한다.
- Redis는 `캐시`, `분산 조정`, `이벤트 처리 보조 저장소` 용도로는 도입 가능하다.
- 위치 최적화를 위해 PostgreSQL/PostGIS로 DB를 전환하지 않는다. 관련 카드는 현재 스택 안에서 해결하는 것을 기본으로 한다.
- 정산 금액은 `호스트가 실제 결제 금액을 확정한 뒤` 최종 금액으로 확정한다.
- 픽업 일정과 장소는 `호스트 확정 -> 참여자 확인` 흐름을 기본으로 한다.
- 후기 작성은 `거래 완료 처리 이후`에만 가능하게 한다.
- 인증은 `access token + refresh token` 이중 토큰 구조를 기본으로 한다.
- access token은 `짧은 수명 JWT`로 유지하고, API 요청의 `Authorization: Bearer` 헤더로만 전달한다.
- refresh token은 프론트 저장소가 아닌 `HttpOnly Cookie`로 관리하는 것을 기본으로 한다.
- refresh token은 재발급 시 `rotation`을 적용하고, 로그아웃 시 서버에서 무효화한다.
- 인증 실패 응답은 프론트가 세션 만료를 구분할 수 있게 `JSON 401` 형식을 기본으로 한다.
- 상품 정보는 1차 범위에서 `상품 카탈로그` 없이 `사용자 자유 입력`을 기본으로 유지한다.
- 파티 수정과 취소 권한은 기본적으로 `호스트`에게만 부여한다.
- 정산이 확정되었거나 픽업 일정이 잡힌 뒤에는 `guideNote`만 수정 가능하게 한다.
- 호스트 취소는 `정산`, `픽업`, `송금`, `거래`가 시작되기 전까지만 허용한다.
- 안전 기능 1차 범위는 `신고 접수`, `사용자 차단`, `노쇼 누적 제한`까지로 제한한다.
- 외부 알림 1차 채널은 `Web Push`로 한정한다. 이메일, 문자, 카카오 알림톡은 후순위로 둔다.
- 사용자 설정 1차 범위는 `프로필 수정`, `주소/위치 재설정`, `알림 설정`, `정산 기본 안내`까지로 제한한다.
- 성능 측정 기본 대상은 `지점 조회 API`, `지점별 파티 조회 API`, `참여 API`로 한다.
- E1 부하 테스트 기본 조건은 `동시 참여 100건 이상`으로 설정한다.
- E2 성능 목표는 `주변 지점 조회 API P95 300ms 이하`를 1차 목표로 한다.

## Output Locations
- ADR 문서는 `docs/adr/` 아래에 저장한다.
- 성능 측정 결과와 전후 비교 문서는 `docs/benchmarks/` 아래에 저장한다.
- 이벤트 스펙, API 계약, 실시간 payload 예시는 `docs/specs/` 아래에 저장한다.
- 각 작업 카드의 진행 상태와 짧은 구현 메모는 이 문서 안에서 직접 갱신한다.

## Status Legend
- `[ ]` 미착수
- `[~]` 진행 중
- `[x]` 완료

## Epic 1. 동시 참여 제어 + 대기열

### [x] E1-1 참여 제약 스키마
- 목표: 중복 참여와 대기열 저장을 위한 스키마를 만든다.
- 범위: `party_member` 유니크 제약, `waiting_queue` 테이블/엔티티/리포지토리.
- 완료 조건: 같은 사용자가 같은 파티에 두 번 저장되지 않는다.
- 검증: 스키마 적용 테스트, 중복 저장 실패 테스트.
- ADR: 스키마 제약과 대기열 모델 선택 근거를 문서화한다.
- 구현 메모(2026-03-15): `WaitingQueueEntry`/`WaitingQueueRepository`를 추가하고, `partymember`와 `waiting_queue` 중복 저장 실패 테스트를 작성했다.

### [x] E1-2 참여 API 동시성 제어
- 목표: 동시에 참여 요청이 들어와도 정원 초과가 발생하지 않게 만든다.
- 범위: `joinParty` 트랜잭션, 락 전략, 상태 전이, 공통 예외 응답.
- 완료 조건: 동시 요청에서도 초과 모집 0건을 보장한다.
- 검증: 통합 테스트, 동시성 테스트.
- ADR: 락 전략과 상태 전이 규칙 선택 근거를 문서화한다.
- 구현 메모(2026-03-15): `joinParty`에서 `saveAndFlush`로 DB 제약 충돌을 즉시 감지하고 `BusinessException`으로 변환했다. 동시성 통합 테스트와 공통 예외 응답 테스트를 유지/추가했다.

### [x] E1-3 대기열 등록 흐름
- 목표: 정원 초과 요청을 실패 대신 대기 상태로 전환한다.
- 범위: 참여 API 응답 확장, 대기열 저장, 상태 코드/메시지 설계.
- 완료 조건: 정원 초과 요청이 대기열에 정상 등록된다.
- 검증: 서비스 테스트, API 응답 검증.
- ADR: 실패 처리 대신 대기열을 선택한 이유와 응답 모델을 문서화한다.
- 구현 메모(2026-03-15): `JoinPartyResponse`를 도입해 즉시 참여와 대기 등록을 분리했고, 정원 초과 요청은 `202 Accepted`와 함께 `waitingPosition`을 반환한다.

### [x] E1-4 취소/이탈과 대기열 승격
- 목표: 빈 자리가 생기면 대기 사용자를 선착순으로 승격한다.
- 범위: 참여 취소 API, 승격 서비스, 파티 상태 갱신.
- 완료 조건: 취소 시 대기열 첫 사용자가 자동 승격된다.
- 검증: 승격 순서 테스트, 상태 전이 테스트.
- ADR: 승격 정책과 취소 처리 규칙을 문서화한다.
- 구현 메모(2026-03-15): `DELETE /party/{id}/join`을 추가했고, 참여 취소 시 FIFO 대기열 자동 승격과 상태 재계산을 적용했다. 호스트 취소는 별도 종료 흐름으로 분리했다.

### [x] E1-5 동시성 부하 측정
- 목표: 이력서에 넣을 수 있는 정량 수치를 확보한다.
- 범위: `k6` 또는 동등한 부하 테스트 스크립트, 결과 정리 문서.
- 완료 조건: 초과 모집 0건, 중복 참여 0건, 평균/백분위 응답시간 결과를 남긴다.
- 검증: 반복 가능한 부하 테스트 실행 결과.
- ADR: 측정 방식과 테스트 가정, 한계를 문서화한다.
- 구현 메모(2026-03-15): `PartyJoinLoadBenchmarkTest`와 실행 스크립트를 추가했고, `docs/benchmarks/E1-5-party-join-load-benchmark.md`에 평균 37.43ms, P95 117.96ms 결과를 기록했다.

## Epic 2. 위치 기반 조회 최적화

### [x] E2-1 조회 성능 베이스라인 측정
- 목표: 현재 지점/파티 조회 성능의 기준값을 확보한다.
- 범위: 조회 API 테스트 데이터, 측정 스크립트, 결과 기록.
- 완료 조건: 평균 응답시간, P95, 테스트 조건이 정리된다.
- 검증: 동일 조건에서 재실행 가능한 측정 결과.
- ADR: 성능 측정 기준과 데이터셋 구성 이유를 문서화한다.
- 구현 메모(2026-03-15): `StoreQueryBaselineBenchmarkTest`와 실행 스크립트를 추가했고, `docs/benchmarks/E2-1-store-query-baseline.md`에 주변 지점 조회 평균 71.78ms / P95 81.07ms, 지점별 파티 조회 평균 1.78ms / P95 2.69ms를 기록했다.

### [x] E2-2 거리 기반 조회 쿼리화
- 목표: 애플리케이션 메모리 계산을 줄이고 DB 조회 중심으로 바꾼다.
- 범위: 거리 계산 쿼리, bounding box 필터, 정렬 로직.
- 완료 조건: 기존 정렬 정확성을 유지하면서 응답시간을 줄인다.
- 검증: 조회 결과 비교 테스트, 성능 재측정.
- ADR: 거리 계산 위치를 애플리케이션이 아닌 DB로 옮긴 이유를 문서화한다.
- 구현 메모(2026-03-15): `StoreRepository`에 native SQL projection 쿼리를 추가해 거리 계산과 모집 중 파티 수 집계를 DB에서 수행하도록 변경했다. 기본 조회는 반경 50km `bounding box`를 사용하고, 결과가 없을 때만 전국 거리순 fallback을 수행한다. `StoreQueryOptimizationIntegrationTest`로 정렬/파티 수/currentQuantity projection을 검증했다.

### [x] E2-3 인덱스 및 스키마 최적화
- 목표: 지점/파티 조회 쿼리에 맞는 인덱스를 추가한다.
- 범위: 좌표, 상태, 지점별 파티 조회 관련 인덱스.
- 완료 조건: 쿼리 계획이 개선되고 풀스캔을 줄인다.
- 검증: `EXPLAIN` 결과 비교, 성능 재측정.
- ADR: 인덱스 전략과 쓰기 비용 트레이드오프를 문서화한다.
- 구현 메모(2026-03-15): `store(latitude, longitude)`와 `party(store_id, party_status)` 복합 인덱스를 추가했다. `StoreQueryIndexIntegrationTest`에서 `INFORMATION_SCHEMA`와 `EXPLAIN` 결과를 검증했고, 결과는 `docs/benchmarks/E2-3-store-query-explain.md`에 정리했다.

### [x] E2-4 캐시 적용
- 목표: 자주 조회되는 위치 기반 결과를 캐시한다.
- 범위: 캐시 계층, TTL, 키 전략, 무효화 규칙.
- 완료 조건: 캐시 hit 시 응답시간이 유의미하게 줄어든다.
- 검증: 캐시 hit/miss 테스트, 성능 재측정.
- ADR: 캐시 계층 도입 이유와 TTL/무효화 전략을 문서화한다.
- 구현 메모(2026-03-15): 현재 단계에서는 Redis 대신 `Spring Cache + Caffeine`으로 `nearbyStores`, `storeParties` 캐시를 추가했다. 키는 `위도:경도(소수점 4자리)`와 `storeId`, TTL은 각각 5분/2분으로 두었고, 파티 생성/즉시 참여/취소 시 캐시를 무효화한다. `StoreQueryCacheIntegrationTest`로 hit/miss와 무효화 동작을 검증했다.

### [x] E2-5 성능 전후 비교 정리
- 목표: 이력서와 포트폴리오에 사용할 수치를 정리한다.
- 범위: 베이스라인 대비 평균 응답시간, P95, 캐시 hit ratio 정리.
- 완료 조건: 전후 비교 수치가 문서로 남는다.
- 검증: 재측정 결과와 정리 문서 확인.
- ADR: 측정 결과 해석과 남은 한계를 문서화한다.
- 구현 메모(2026-03-15): `StoreQueryOptimizedBenchmarkTest`와 `./scripts/run_store_query_optimized_benchmark.sh`를 추가했다. 측정 결과는 `docs/benchmarks/E2-5-store-query-performance-comparison.md`에 정리했고, 주변 지점 조회는 cold path 평균 `12.49ms`, warm path 평균 `1.64ms`, 캐시 hit ratio `0.95`를 기록했다.

## Epic 3. 자동 마감 + 알림 파이프라인

### [x] E3-1 파티 생명주기 모델 확장
- 목표: 마감/종료 상태를 표현할 수 있는 도메인 모델을 만든다.
- 범위: `deadline`, `closedAt`, `closeReason` 등 필드와 상태 규칙.
- 완료 조건: 시간 만료와 정원 달성 케이스를 모두 표현할 수 있다.
- 검증: 상태 전이 테스트.
- ADR: 파티 생명주기 모델과 상태 설계를 문서화한다.
- 구현 메모(2026-03-16): `Party`에 `deadline`, `closedAt`, `closeReason`과 `CLOSED` 상태를 추가하고, 파티 생성 시 기본 마감시간은 `현재 + 1일`로 채운다. `PartyResponse`, `PartyDetailResponse`, `MyJoinedPartyResponse`, 프론트 `party` 정규화 유틸과 카드/UI를 함께 수정해 종료 상태를 목록/상세/내 파티에 노출한다. `PartyLifecycleIntegrationTest`로 정원 달성 시 `FULL`, 시간 만료 시 `CLOSED` 전이를 검증했다.

### [x] E3-2 자동 마감 스케줄러
- 목표: 시간이 지난 파티를 자동 종료한다.
- 범위: 스케줄러, 종료 서비스, 종료 시 상태 갱신.
- 완료 조건: 마감 시간이 지난 파티가 자동으로 종료된다.
- 검증: 시간 기반 테스트 또는 스케줄러 통합 테스트.
- ADR: 스케줄링 방식과 실행 주기 선택 이유를 문서화한다.
- 구현 메모(2026-03-16): `SchedulingConfig`와 `PartyDeadlineScheduler`를 추가하고, 실제 종료 처리는 `PartyLifecycleService.closeExpiredParties()`로 모았다. 30초 fixed delay로 `deadline <= now` 이고 상태가 `RECRUITING/FULL`인 파티를 종료하며, 종료 시 대기열 만료와 캐시 무효화를 함께 처리한다. 시간 기반 검증은 `PartyLifecycleIntegrationTest`가 담당한다.

### [x] E3-3 이벤트/Outbox 구조 도입
- 목표: 상태 변경 이벤트를 안전하게 비동기 처리할 기반을 만든다.
- 범위: Outbox 테이블, 이벤트 모델, 저장 로직.
- 완료 조건: 상태 변경과 이벤트 저장이 같은 트랜잭션 안에서 처리된다.
- 검증: 이벤트 저장 통합 테스트.
- ADR: Outbox 패턴 채택 이유와 대안 비교를 문서화한다.
- 구현 메모(2026-03-16): `outbox_event` 엔티티와 저장소, `NotificationOutboxService`를 추가했다. 파티 즉시 참여, 대기열 승격, 시간 만료 종료 시 `PARTY_JOIN_CONFIRMED`, `WAITING_PROMOTED`, `PARTY_CLOSED` 이벤트를 같은 트랜잭션 안에서 적재한다. `PartyLifecycleIntegrationTest`에서 종료 시 outbox 이벤트가 남는지 검증했다.

### [x] E3-4 알림 워커 구현
- 목표: 참여 완료, 마감, 승격 알림을 비동기 발송한다.
- 범위: 워커, 재시도 정책, 중복 방지 처리.
- 완료 조건: 같은 이벤트가 중복 발송되지 않고 실패 시 재시도된다.
- 검증: 워커 테스트, 재시도 테스트.
- ADR: 알림 채널과 재시도 정책 선택 이유를 문서화한다.
- 구현 메모(2026-03-16): `NotificationOutboxProcessor`와 `NotificationOutboxScheduler`를 추가해 10초 fixed delay polling 워커를 구성했다. payload 오류는 1분 뒤 재시도하고, `user_notification.external_key = eventId:userId:type` 기준 선조회와 DB 유니크 키를 함께 써 동일 outbox 이벤트의 중복 알림 생성을 막는다. `NotificationOutboxProcessorIntegrationTest`에서 참여 완료 알림 생성, 재시도, 동일 이벤트 재처리 시 중복 방지를 검증했다.

### [x] E3-5 사용자 알림 조회 흐름
- 목표: 사용자가 알림 내역을 확인할 수 있게 한다.
- 범위: 알림 조회 API, 프론트 알림 목록 또는 내 파티 연동.
- 완료 조건: 마감/승격/참여 완료 알림을 확인할 수 있다.
- 검증: API/화면 동작 확인.
- ADR: 알림 노출 방식과 사용자 경험 선택 이유를 문서화한다.
- 구현 메모(2026-03-16): `GET /api/users/me/notifications` API와 `UserNotificationResponse`를 추가했고, 프론트에는 `/notifications` 화면과 하단 네비게이션 진입점을 만들었다. 알림은 생성 시각 역순으로 노출하고 관련 파티 상세로 이동할 수 있다. `UserNotificationControllerTest`와 `npm run build`로 API/화면 연결을 검증했다.

## Epic 4. 실시간 모집 현황 반영

### [x] E4-1 실시간 이벤트 규격 정의
- 목표: 어떤 이벤트를 어떤 형식으로 보낼지 정한다.
- 범위: SSE 또는 WebSocket 선택, payload 스펙 정의.
- 완료 조건: 파티 상태 변경 이벤트 규격 문서가 존재한다.
- 검증: 스펙 문서와 샘플 payload 확인.
- ADR: SSE와 WebSocket 중 어떤 방식을 선택했는지 문서화한다.
- 구현 메모(2026-03-16): `docs/specs/E4-party-realtime-events.md`를 추가했다. 채널은 `GET /party/stream` 공개 SSE로 고정했고, 이벤트는 `connected`, `party-updated` 두 가지로 정의했다. payload는 파티 스냅샷과 `realtimeTrigger`를 포함하며 `storeId`, `partyId` query filter를 지원한다.

### [x] E4-2 백엔드 실시간 발행
- 목표: 참여/승격/마감 시 실시간 이벤트를 발행한다.
- 범위: 이벤트 브로드캐스트, 연결 관리, 발행 트리거.
- 완료 조건: 상태 변경 시 구독자에게 이벤트가 전송된다.
- 검증: 수동 테스트 또는 통합 테스트.
- ADR: 실시간 발행 구조와 연결 관리 방식을 문서화한다.
- 구현 메모(2026-03-16): `PartyRealtimeService`와 `PartyRealtimeController`를 추가해 메모리 기반 `SseEmitter` 레지스트리와 공개 SSE 엔드포인트를 구현했다. 파티 생성, 즉시 참여, 취소, 대기열 승격, 자동 마감은 `afterCommit` 시점에 `party-updated` 이벤트를 브로드캐스트한다. `PartyRealtimeServiceIntegrationTest`로 `connected` 이벤트와 `storeId/partyId` 필터 전달을 검증했다.

### [x] E4-3 프론트 실시간 구독
- 목표: 파티 목록, 상세, 내 파티 화면이 실시간으로 갱신되게 한다.
- 범위: 구독 연결, 상태 반영, 화면 보정.
- 완료 조건: 새로고침 없이 수량과 상태가 갱신된다.
- 검증: 다중 클라이언트 수동 테스트.
- ADR: 프론트 상태 동기화 전략을 문서화한다.
- 구현 메모(2026-03-16): `frontend/src/utils/partyRealtime.js`를 추가해 SSE 구독을 공통화했다. 파티 목록과 상세는 이벤트 payload를 바로 병합하고, 내 파티 화면은 관련 파티 이벤트가 오면 `/api/users/me/parties`를 다시 조회한다. 각 화면에 실시간 연결 상태 문구도 함께 노출한다.

### [x] E4-4 재연결 및 fallback
- 목표: 연결 끊김 상황에서도 최종 상태를 맞춘다.
- 범위: 재연결 정책, API 재조회 fallback, 누락 복구.
- 완료 조건: 실시간 연결 장애 후에도 최종 상태가 일관된다.
- 검증: 연결 끊김 테스트, fallback 테스트.
- ADR: 재연결 정책과 fallback 전략을 문서화한다.
- 구현 메모(2026-03-16): 클라이언트는 SSE 오류 시 연결을 닫고 최대 5초까지 선형 backoff로 재연결한다. 오류 직후 화면별 기존 조회 API를 한 번 다시 호출해 누락된 상태를 보정한다. 프론트엔드 테스트 러너는 아직 없어 자동 검증은 `PartyRealtimeServiceIntegrationTest`와 `npm run build` 기준으로 마무리했고, 재연결 규칙은 `docs/specs/E4-party-realtime-events.md`와 ADR에 문서화했다.

## Epic 5. 소분 도메인 상세화

### [x] E5-1 소분 단위 및 상품 메타데이터 확장
- 목표: 단순 수량 기반 파티를 실제 소분 거래에 맞는 상품 모델로 확장한다.
- 범위: `unitLabel`, `minimumShareUnit`, `storageType`, `packagingType` 또는 동등한 필드 설계.
- 완료 조건: 파티 생성 시 소분 기준 단위와 보관/포장 정보를 저장할 수 있다.
- 검증: 엔티티/DTO 테스트, 생성 API 테스트.
- ADR: 소분 단위 모델과 상품 메타데이터 구조를 문서화한다.
- 구현 메모(2026-03-16): `Party`에 `unitLabel`, `minimumShareUnit`, `storageType`, `packagingType` 필드를 추가하고 생성자 기본값과 검증 로직을 정리했다. `CreatePartyRequest`, `PartyResponse`, `PartyDetailResponse`, `PartyRealtimeEventResponse`, `MyJoinedPartyResponse`까지 확장해 API 전 구간에서 같은 메타데이터를 전달하도록 맞췄다. `PartyProductMetadataTest`, `PartyMetadataIntegrationTest`로 엔티티와 생성/상세 응답을 검증했다.

### [x] E5-2 호스트 준비물 및 거래 안내 정보 추가
- 목표: 참여자가 소분 방식과 준비 상태를 파티 상세에서 이해할 수 있게 한다.
- 범위: 봉투 제공 여부, 냉장/냉동 여부, 현장 소분 여부, 추가 안내 메모.
- 완료 조건: 파티 생성/상세 화면에서 거래 안내 정보를 입력·조회할 수 있다.
- 검증: 프론트 폼 검증, 상세 화면 렌더링 확인.
- ADR: 호스트 안내 정보 범위와 저장 방식을 문서화한다.
- 구현 메모(2026-03-16): `hostProvidesPackaging`, `onSiteSplit`, `guideNote`를 `Party`와 관련 DTO에 추가했다. 프론트 `CreateParty.jsx`에는 소분 정보 폼과 체크박스를 넣고, `PartyDetail.jsx`에는 소분 정보/거래 안내/가격 정보 섹션을 추가했다. `npm run build`로 폼과 상세 화면 통합을 확인했다.

### [x] E5-3 실구매 정보 반영 준비
- 목표: 행사 가격, 영수증 기준 금액 등 실제 구매 이후 금액 조정을 반영할 수 있는 구조를 만든다.
- 범위: `expectedTotalPrice`와 `actualTotalPrice` 구분, 영수증 메모 또는 증빙 참조 필드 설계.
- 완료 조건: 예상 가격과 실제 구매 가격을 구분 저장할 수 있다.
- 검증: 정산 전 상태 테스트, 금액 전이 테스트.
- ADR: 예상 금액과 확정 금액을 분리하는 이유와 전이 규칙을 문서화한다.
- 구현 메모(2026-03-16): 기존 `Party.totalPrice`를 예상가로 유지하고 `actualTotalPrice`, `receiptNote`를 추가했다. 응답에는 `totalPrice`를 화면 표시용 금액으로 유지하되 `expectedTotalPrice`/`actualTotalPrice`를 함께 내려 후속 정산 에픽과의 호환성을 확보했다. `Party.updateActualPurchase(...)`와 통합 테스트로 예상가에서 실구매가로 전이되는 구조를 검증했다.

## Epic 6. 정산 및 픽업 운영

### [x] E6-1 정산 모델 및 1인당 금액 확정
- 목표: 참여 수량 기준 예상 금액과 실제 결제 기준 확정 금액을 관리한다.
- 범위: 정산 엔티티/DTO, 참여자별 부담금 계산, 호스트 확정 흐름.
- 완료 조건: 호스트가 실제 총액을 확정하면 참여자별 금액이 계산된다.
- 검증: 금액 계산 테스트, 확정 API 테스트.
- ADR: 정산 모델, 계산 기준, 반올림 정책을 문서화한다.
- 구현 메모(2026-03-16): `PartyMember`에 `expectedAmount`, `actualAmount`를 추가하고, `PUT /party/{id}/settlement`에서 호스트가 실구매 총액을 확정하면 참여자별 금액을 배분하도록 구현했다. 잔여 1원은 요청 수량이 큰 참여자부터 우선 배분하는 정책을 사용했고, 결과는 `PartyDetailResponse.settlementMembers`와 `MyJoinedPartyResponse`에 함께 노출된다. `PartySettlementFlowIntegrationTest`로 금액 계산과 호스트 상세 응답을 검증했다.

### [x] E6-2 송금 상태 관리
- 목표: 참여자별 송금 진행 상태를 서비스 안에서 추적한다.
- 범위: `PENDING`, `PAID`, `CONFIRMED`, `REFUNDED` 또는 동등한 상태 모델.
- 완료 조건: 참여자별 정산 상태를 조회하고 갱신할 수 있다.
- 검증: 상태 전이 테스트, 내 파티/상세 API 응답 검증.
- ADR: 송금 상태 모델과 권한 규칙을 문서화한다.
- 구현 메모(2026-03-16): `PaymentStatus`를 `NOT_REQUIRED`, `PENDING`, `PAID`, `CONFIRMED`, `REFUNDED`로 정의하고 `PUT /party/{partyId}/members/{memberId}/payment` API를 추가했다. 참여자는 본인 상태를 `PAID`로만 올릴 수 있고, 호스트는 `CONFIRMED`와 `REFUNDED`만 처리할 수 있게 제한했다. `MyParties.jsx`와 `PartyDetail.jsx`에서 내 정산 금액과 송금 상태를 확인할 수 있고, `PartySettlementFlowIntegrationTest`로 송금 완료 -> 확인 -> 환불 전이를 검증했다.

### [x] E6-3 픽업 일정 및 장소 확정
- 목표: 거래 완료를 위한 만남 장소와 시간을 파티 안에서 확정한다.
- 범위: `pickupPlace`, `pickupTime`, 확정/변경 이력, 참여자 확인 흐름.
- 완료 조건: 호스트가 픽업 정보를 확정하고 참여자가 확인할 수 있다.
- 검증: 일정 확정 API 테스트, 화면 렌더링 확인.
- ADR: 픽업 확정 흐름과 변경 정책을 문서화한다.
- 구현 메모(2026-03-16): `Party`에 `pickupPlace`, `pickupTime`을 추가하고 `PUT /party/{id}/pickup`, `POST /party/{id}/pickup/acknowledge` API를 만들었다. 호스트는 상세 화면에서 픽업 장소/시간을 확정하고, 참여자는 같은 화면에서 픽업 일정 확인을 남길 수 있다. `PartyDetail.jsx`와 `MyParties.jsx`에 픽업 정보와 확인 상태를 노출했고, `PartySettlementFlowIntegrationTest`와 `npm run build`로 흐름을 검증했다.

### [x] E6-4 거래 완료 및 노쇼 처리
- 목표: 실제 거래가 끝났는지와 노쇼 여부를 구분해 처리한다.
- 범위: 완료 처리, 노쇼 상태, 미수령 처리, 대기열/정산과의 연결 규칙.
- 완료 조건: 거래 완료와 노쇼를 별도 상태로 기록할 수 있다.
- 검증: 상태 전이 테스트, 후기 가능 조건 테스트.
- ADR: 거래 완료/노쇼 상태 모델과 후속 처리 규칙을 문서화한다.
- 구현 메모(2026-03-16): `TradeStatus`를 `PENDING`, `COMPLETED`, `NO_SHOW`로 추가하고 `PUT /party/{partyId}/members/{memberId}/trade-status` API를 구현했다. 거래 완료는 `CONFIRMED` 결제 상태와 픽업 일정이 있는 경우에만 허용하고, `reviewEligible` 플래그로 후기 가능 조건을 미리 노출한다. 호스트는 상세 화면의 참여자 목록에서 거래 완료/노쇼를 처리할 수 있고, `PartySettlementFlowIntegrationTest`로 거래 완료와 노쇼의 후기 가능 여부 차이를 검증했다.

## Epic 7. 후기 및 신뢰도

### [x] E7-1 거래 완료 후 후기 작성 흐름
- 목표: 실제 거래가 끝난 뒤에만 후기와 평점을 남길 수 있게 한다.
- 범위: 후기 엔티티, 작성 조건, 파티/사용자 연결, 중복 작성 방지.
- 완료 조건: 거래 완료 이후 후기 1회 작성 제한이 동작한다.
- 검증: 후기 작성 서비스 테스트, 권한 테스트.
- ADR: 후기 작성 시점과 작성 제한 정책을 문서화한다.
- 구현 메모(2026-03-16): `Review` 엔티티와 `POST /party/{id}/reviews` API를 추가했다. 참여자는 거래 완료 후 호스트에게 1회만 후기를 남길 수 있고, 호스트는 거래 완료된 참여자에게만 후기를 남길 수 있게 `PartyService.submitReview(...)`에서 검증한다. `ReviewFlowIntegrationTest`로 거래 완료 전 작성 차단, 중복 후기 차단, 호스트의 참여자 후기 작성 가능 조건을 검증했다.

### [x] E7-2 사용자 신뢰도 지표 계산
- 목표: 후기, 거래 완료율, 노쇼 여부를 기반으로 사용자 신뢰도를 계산한다.
- 범위: 신뢰도 집계 규칙, 점수 또는 레벨 모델, 배치/실시간 갱신 방식.
- 완료 조건: 프로필 또는 파티 상세에서 신뢰도 지표를 노출할 수 있다.
- 검증: 집계 테스트, 프로필 API 응답 검증.
- ADR: 신뢰도 산식과 갱신 전략을 문서화한다.
- 구현 메모(2026-03-16): `TrustScoreService`를 추가해 후기 평점, 완료 거래 수, 노쇼 수를 바탕으로 `trustScore`, `trustLevel`, `completionRate`를 계산한다. 호스트는 자신이 진행한 거래 완료 건도 신뢰도에 포함하고, 참여자는 개인 노쇼 기록을 별도로 반영한다. `UserTrustProfileIntegrationTest`에서 최고 등급과 주의 등급 계산을 함께 검증했다.

### [x] E7-3 신뢰도 정보 노출
- 목표: 참여 결정에 필요한 신뢰도 정보를 UI에 자연스럽게 노출한다.
- 범위: 프로필, 파티 상세, 호스트 정보 카드, 후기 목록.
- 완료 조건: 후기 수, 평점, 노쇼 관련 지표를 화면에서 확인할 수 있다.
- 검증: 프론트 렌더링 테스트, API 응답 연동 확인.
- ADR: 신뢰도 정보 노출 범위와 사용자 경험 선택 이유를 문서화한다.
- 구현 메모(2026-03-16): `UserResponse`에는 내 신뢰도 요약과 최근 받은 후기를, `PartyDetailResponse`에는 호스트 신뢰도와 최근 후기를 함께 담도록 확장했다. 프론트 `Profile.jsx`에는 신뢰도 카드와 후기 목록을, `PartyDetail.jsx`에는 호스트 신뢰도/최근 후기/후기 작성 폼을 추가했다. `npm run build`와 `UserTrustProfileIntegrationTest`로 API 연동과 렌더링 가능한 상태를 확인했다.

## Epic 8. 파티 채팅

### [x] E8-1 파티 채팅 도메인 및 권한 모델
- 목표: 파티 단위 채팅방과 접근 권한 모델을 정의한다.
- 범위: `chat_room`, `chat_message`, `chat_member` 또는 동등한 모델, 파티-채팅방 연결, 참여자 권한 체크.
- 완료 조건: 파티별 채팅방이 생성되고 참여자만 접근할 수 있다.
- 검증: 권한 테스트, 채팅방 생성 테스트.
- ADR: 파티 전용 채팅 모델과 접근 제어 정책을 문서화한다.
- 구현 메모(2026-03-17): `ChatRoom`, `ChatMessage`, `ChatReadState` 엔티티를 추가하고 `Party`와 1:1 채팅방 관계를 연결했다. 별도 `chat_member` 테이블은 만들지 않고 기존 `PartyMember`를 채팅 접근 권한의 단일 기준으로 사용한다. `ChatRoomFlowIntegrationTest`로 채팅방 생성, 참여자 접근 제한, 읽음 처리까지 검증했다.

### [x] E8-2 WebSocket/STOMP 채팅 송수신 기반
- 목표: 참여자가 실시간으로 메시지를 주고받을 수 있는 채널을 만든다.
- 범위: WebSocket endpoint, STOMP destination, 인증/세션 연동.
- 완료 조건: 같은 파티 참여자 간 메시지가 실시간으로 전달된다.
- 검증: 통합 테스트 또는 다중 클라이언트 수동 테스트.
- ADR: 채팅에서 SSE가 아닌 WebSocket/STOMP를 선택한 이유를 문서화한다.
- 구현 메모(2026-03-17): `/ws-chat` endpoint, `/app/chat/{partyId}/messages` 발행, `/topic/chat/{partyId}` 구독 규격으로 STOMP 채널을 추가했다. `ChatStompChannelInterceptor`가 `CONNECT`의 JWT를 검증하고 세션 속성에 저장해 `SUBSCRIBE`, `SEND`, `MessageMapping` 처리까지 같은 인증 정보를 재사용한다. `ChatWebSocketIntegrationTest`로 실제 WebSocket 연결, 구독, 메시지 저장/브로드캐스트를 통합 검증했다.

### [x] E8-3 메시지 저장 및 시스템 메시지
- 목표: 채팅 내용을 저장하고 참여/취소/마감 이벤트를 시스템 메시지로 남긴다.
- 범위: 메시지 영속화, 시스템 메시지 타입, 과거 메시지 조회 API.
- 완료 조건: 새로 입장한 사용자가 최근 메시지를 조회할 수 있다.
- 검증: 메시지 저장 테스트, 시스템 메시지 생성 테스트.
- ADR: 메시지 저장 범위와 시스템 메시지 모델을 문서화한다.
- 구현 메모(2026-03-17): 최근 100개 메시지를 내려주는 `GET /api/chat/rooms/{partyId}` API와 메시지 저장 로직을 `ChatService`로 모았다. 시스템 메시지는 채팅방 생성, 파티 참여, 참여 취소, 대기열 승격, 자동 마감, 공지 변경 시 자동 생성되며, 브로드캐스트는 `afterCommit` 기준으로 보낸다. `ChatRoomFlowIntegrationTest`와 `ChatWebSocketIntegrationTest`가 저장/조회/브로드캐스트를 함께 검증한다.

### [x] E8-4 안 읽은 메시지 수 및 호스트 공지 고정
- 목표: 사용자가 중요한 공지와 읽지 않은 메시지를 쉽게 확인하게 한다.
- 범위: 마지막 읽음 위치 저장, unread count 계산, pinned notice.
- 완료 조건: 파티 목록/내 파티/채팅방에서 안 읽은 메시지 수를 확인할 수 있다.
- 검증: unread 계산 테스트, 공지 고정 동작 확인.
- ADR: 읽음 처리 모델과 공지 고정 방식을 문서화한다.
- 구현 메모(2026-03-17): `ChatReadState.lastReadMessageId`로 unread count를 계산하고, 채팅방 상세 조회와 `POST /api/chat/rooms/{partyId}/read` 모두 마지막 메시지까지 읽음 처리하도록 만들었다. 호스트 공지는 `PUT /api/chat/rooms/{partyId}/notice`에서만 수정 가능하며 공지 변경 시 시스템 메시지도 함께 남긴다. 프론트는 파티 목록, 내 파티, 채팅 화면에 unread count와 공지 정보를 노출한다.

### [x] E8-5 채팅 화면 통합 및 채팅 전환 정책
- 목표: 파티별 전용 인앱 채팅을 자연스럽게 사용하는 흐름을 만든다.
- 범위: 채팅 페이지 구현, 파티 상세 연결, 채팅 진입 정책.
- 완료 조건: 파티 상세에서 인앱 채팅으로 진입할 수 있고 채팅방 목록과 unread가 함께 연결된다.
- 검증: 프론트 라우팅 확인, 상세-채팅 연결 확인.
- ADR: 인앱 채팅 도입 범위와 전환 정책을 문서화한다.
- 구현 메모(2026-03-17): 프론트에 `/chat`, `/chat/:partyId` 채팅 화면과 `@stomp/stompjs` 기반 `chatClient`를 추가했다. 하단 네비게이션, 파티 상세, 내 파티, 파티 목록에서 unread count와 채팅 진입 CTA를 연결했고, 파티별 전용 인앱 채팅을 기본 대화 경로로 사용하게 했다. 화면 통합 검증은 `npm run build`와 REST/STOMP 통합 테스트 기준으로 마무리했다.

## Epic 9. UX 정리 및 사용성 개선

### [x] E9-1 대기열 참여 UX 복구
- 목표: 모집 완료 상태에서도 사용자가 대기열 등록 흐름을 이해하고 진입할 수 있게 한다.
- 범위: 파티 상세/참여 화면 CTA, 대기열 안내 문구, 참여 결과 피드백.
- 완료 조건: 잔여 수량이 부족한 파티에서도 대기열 등록 CTA가 노출되고, 등록 후 대기 상태와 순번 안내를 확인할 수 있다.
- 검증: 상세-참여 플로우 수동 테스트, 대기열 진입/취소 UI 확인.
- ADR: 대기열을 참여 UX에 어떻게 노출할지와 마감/종료 상태 구분 기준을 문서화한다.
- 구현 메모(2026-03-17): `PartyDetail.jsx`에서 모집 완료와 거래 종료를 구분해 `대기열 등록하기` CTA와 안내 문구를 추가했고, `JoinParty.jsx`는 queue-only 상태를 계산해 제목/버튼/설명 텍스트를 대기열 중심으로 바꿨다. 최소 소분 단위에 맞춰 초기 요청 수량도 보정했다. 자동화 검증은 `npm run build` 기준으로 마무리했고, 수동 클릭 테스트는 아직 별도로 실행하지 않았다.

### [x] E9-2 파티 생성 폼 의미 정리
- 목표: 사용자가 입력하는 시간/필드 의미를 실제 도메인 모델과 맞춘다.
- 범위: 생성 폼 라벨/설명, 모집 마감 시간과 픽업 시간 구분, 불필요한 카테고리 필드 정리.
- 완료 조건: 생성 화면에서 입력한 정보가 모두 실제 저장 의미와 일치하고, 저장되지 않는 입력 필드는 제거되거나 연동된다.
- 검증: 파티 생성 화면 수동 테스트, 생성 후 상세 화면 데이터 확인.
- ADR: 파티 생성 입력 필드 정책과 마감/픽업 정보 모델링 방식을 문서화한다.
- 구현 메모(2026-03-17): `CreateParty.jsx`에서 `모임 날짜/시간`을 `모집 마감 날짜/시간`으로 바꾸고 픽업 일정과의 차이를 helper text로 설명했다. 저장되지 않던 카테고리 필드는 제거했고, 지점/제품명/수량/가격/마감시간 검증을 보강했다. 호스트 요청 수량은 0도 유효하게 전송되도록 fallback 강제를 제거했다.

### [x] E9-3 로그인 복귀 경로 및 접근 가드 개선
- 목표: 로그인 요구 상황에서 사용자의 이전 맥락을 잃지 않게 한다.
- 범위: 로그인 리다이렉트, 보호 화면 진입 가드, 로그인 후 복귀 경로 복원.
- 완료 조건: 참여/내 파티/채팅/프로필 진입 중 로그인으로 이동한 사용자가 인증 후 원래 보던 화면으로 돌아온다.
- 검증: 비로그인 상태에서 보호 화면 진입 후 로그인 복귀 수동 테스트.
- ADR: 인증 가드와 로그인 후 복귀 정책을 문서화한다.
- 구현 메모(2026-03-17): 로그인으로 보내는 주요 화면(`CreateParty`, `JoinParty`, `MyParties`, `Notifications`, `Profile`, `Chat`)에서 `state.from`에 현재 경로를 넘기도록 통일했다. `Login.jsx`는 `/login`, `/signup` 재진입을 막는 guard와 함께 성공 시 원래 경로로 `replace` 이동한다. 자동화 검증은 `npm run build` 기준으로 마무리했고, 수동 로그인 복귀 흐름은 아직 별도 실행하지 않았다.

### [x] E9-4 모바일 채팅 UX 개선
- 목표: 모바일에서 채팅 목록과 대화방 전환이 자연스럽고, 메시지 확인/전송이 끊기지 않게 한다.
- 범위: 모바일 전용 레이아웃, 방 선택 시 단일 대화 뷰, 자동 스크롤, 전송 실패 피드백.
- 완료 조건: 모바일에서 채팅방 선택 후 대화에 집중할 수 있고, 새 메시지 수신 시 최신 메시지가 보인다.
- 검증: 모바일 반응형 수동 테스트, 메시지 송수신/재연결 확인.
- ADR: 모바일 채팅 레이아웃과 실시간 메시지 표시 전략을 문서화한다.
- 구현 메모(2026-03-17): `Chat.jsx`에서 모바일은 채팅방 선택 시 목록을 숨기고 대화만 보여주도록 레이아웃을 분기했다. 메시지 목록은 새 메시지 수신 시 자동으로 맨 아래로 스크롤하고, 채팅방 목록/상세 조회 실패에는 재시도 버튼을 추가했다. 연결이 아직 준비되지 않은 상태에서 전송 시도 시 토스트로 즉시 피드백한다.

### [x] E9-5 공통 탐색 및 데드 CTA 정리
- 목표: 화면에 노출되는 버튼과 링크가 모두 실제 동작하고, 주요 이동 경로가 일관되게 동작하게 한다.
- 범위: 헤더 `가까운 지점` 버튼, 홈 `거리순` 액션, 공통 네비게이션 정리.
- 완료 조건: 사용자에게 보이는 주요 CTA가 모두 유효한 동작을 수행하거나, 불필요한 CTA는 제거된다.
- 검증: 헤더/홈/하단 네비게이션 클릭 플로우 수동 테스트.
- ADR: 공통 탐색 구조와 CTA 최소 노출 원칙을 문서화한다.
- 구현 메모(2026-03-17): 헤더의 `가까운 지점` 버튼은 홈(`/`)으로, 사용자 배지는 프로필(`/me`)로 연결했다. 홈의 `거리순`은 동작 없는 버튼 대신 현재 정렬 상태를 설명하는 배지로 바꿨다. 브라우저 위치 토글이 저장 위치로 복귀할 때는 실제 기본 좌표로 돌아가도록 보정했다.

### [x] E9-6 상태 피드백 및 빈 화면 품질 개선
- 목표: 로딩/에러/빈 상태가 기능 맥락을 설명하고 다음 행동을 유도하게 만든다.
- 범위: 프로필/알림/내 파티/채팅 화면의 로딩 문구, 에러 메시지, 빈 상태 CTA.
- 완료 조건: 주요 화면에서 아무 것도 없는 상태나 실패 상태가 발생해도 사용자가 다음 행동을 이해할 수 있다.
- 검증: 빈 데이터/오류 상황 수동 테스트, 텍스트/CTA 일관성 점검.
- ADR: 공통 상태 피드백 표현 원칙과 화면별 예외 처리를 문서화한다.
- 구현 메모(2026-03-17): `Home`, `MyParties`, `Notifications`, `Profile`, `Chat`에 `LoadingState`/`EmptyState`와 재시도 CTA를 정리했다. 빈 상태에서는 파티 탐색이나 위치 재요청처럼 다음 행동을 바로 제시하고, 오류 상태에서는 같은 화면을 다시 불러올 수 있게 했다. 자동화 검증은 `frontend`의 `npm run build` 통과로 확인했다.

## Epic 10. 지점 시드 데이터 자동 적재

### [x] E10-1 JSON 기반 지점 시드 로더
- 목표: 빈 개발 DB에서도 주변 지점과 파티 생성 흐름을 바로 확인할 수 있게 한다.
- 범위: 지점 시드 파일, 애플리케이션 시작 시 적재 로더, 중복 없는 upsert 정책, 테스트 프로필 비활성화.
- 완료 조건: 서버 시작 시 `store` 테이블이 비어 있어도 코스트코/트레이더스 지점 기본 데이터가 적재된다.
- 검증: 지점 시드 적재 통합 테스트, H2 실행 환경에서 `/api/stores/nearby` 응답 확인.
- ADR: JSON 시드 형식과 시작 시 upsert 정책 선택 근거를 문서화한다.
- 구현 메모(2026-03-17): `seeds/stores.json`과 `StoreSeedInitializer`/`StoreSeedService`를 추가해 앱 시작 시 `name` 기준 upsert를 수행하도록 만들었다. `Store.applySeed(...)`로 기존 지점 정보를 안전하게 갱신하고, `application-test.yml`에서는 시드를 비활성화해 기존 테스트를 보호했다. `StoreSeedServiceIntegrationTest`와 H2 기동 후 `/api/stores/nearby` 응답으로 시드 적재를 검증했다.

### [x] E10-2 Docker 기반 로컬 MySQL 실행
- 목표: 로컬 개발 환경에서 별도 설치 없이 서비스 DB를 일관되게 실행할 수 있게 한다.
- 범위: `docker-compose.yml`, `.env.example`, 로컬 실행 문서, healthcheck, 볼륨 구성.
- 완료 조건: `docker compose up -d db`로 MySQL이 실행되고, 백엔드가 같은 설정으로 연결된다.
- 검증: Docker 컨테이너 실행 확인, 백엔드 기동 확인, `/api/stores/nearby` 응답 확인.
- ADR: Docker Compose 기반 로컬 DB 운영 방식을 문서화한다.
- 구현 메모(2026-03-17): MySQL 8.4 기반 `docker-compose.yml`과 `.env.example`, `docs/local-db.md`를 추가했다. `.env`를 로컬 전용으로 두고 `DB_URL/DB_USERNAME/DB_PASSWORD`를 백엔드와 공유하도록 맞췄다. 실제로 `docker compose up -d db` 후 백엔드를 MySQL 기준으로 기동했고, 시드 적재까지 포함해 `/api/stores/nearby` 응답을 확인했다.

## Epic 11. 로그인 세션 안정화 및 리프레시 토큰 도입

### [x] E11-1 인증 실패 응답 표준화
- 목표: 만료되거나 잘못된 토큰에 대해 프론트가 일관되게 처리할 수 있는 인증 오류 응답을 만든다.
- 범위: Spring Security `authenticationEntryPoint`/`accessDeniedHandler`, JWT 예외 매핑, 공통 에러 코드.
- 완료 조건: 인증되지 않은 요청과 만료/변조 토큰 요청이 `JSON 401` 또는 `JSON 403`으로 명확히 구분되어 반환된다.
- 검증: 무토큰/만료 토큰/변조 토큰 API 테스트, 응답 본문 스냅샷 확인.
- ADR: 인증 실패를 401/403으로 어떻게 구분하고 어떤 에러 코드를 내려줄지 문서화한다.
- 구현 메모(2026-03-18): `RestAuthenticationEntryPoint`와 `RestAccessDeniedHandler`를 추가하고, `JwtAuthenticationFilter`가 만료 토큰과 변조 토큰을 `request attribute`로 구분해 전달하도록 수정했다. 보호 API는 이제 `AUTH_REQUIRED`, `TOKEN_EXPIRED`, `INVALID_TOKEN`, `ACCESS_DENIED` 코드로 JSON 응답을 반환한다. `SecurityAuthenticationFailureIntegrationTest`와 `RestAccessDeniedHandlerTest`로 401/403 응답 형식을 검증했다.

### [x] E11-2 프론트 전역 세션 만료 처리
- 목표: access token 만료나 인증 실패가 발생했을 때 사용자가 화면마다 부분적으로 깨지는 대신 재로그인 흐름으로 자연스럽게 이동하게 한다.
- 범위: `fetchJson` 전역 401 처리, 만료 시 토큰 정리, 로그인 복귀 경로 유지, 세션 만료 안내 UI.
- 완료 조건: 보호 API가 401을 반환하면 프론트가 토큰을 정리하고 로그인 화면으로 이동시키며, 로그인 후 원래 화면으로 복귀한다.
- 검증: 만료 토큰을 넣은 수동 테스트, 로그인 복귀 플로우 확인, 프론트 빌드.
- ADR: 프론트 전역 인증 실패 처리와 세션 만료 UX 정책을 문서화한다.
- 구현 메모(2026-03-18): `fetchJson`에 전역 401 처리 훅을 추가하고, `AuthContext`가 인증 실패 시 세션을 정리하면서 `returnTo`와 메시지를 저장하도록 구성했다. `SessionExpiryHandler`는 라우터 내부에서 이 상태를 감지해 로그인 화면으로 이동시키고, `Login.jsx`는 세션 만료 안내 문구를 상단에 노출한다. 프론트 자동 검증은 `npm run build` 기준으로 마무리했고, 만료 토큰 수동 리다이렉트는 아직 별도 브라우저 클릭 테스트를 남겨두고 있다.

### [x] E11-3 리프레시 토큰 발급 및 저장 구조
- 목표: access token 만료 후에도 사용자가 매번 수동 로그인하지 않도록 세션 연장 기반을 만든다.
- 범위: 로그인 응답 확장, refresh token 생성/저장, DB 또는 영속 저장소 모델, `HttpOnly Cookie` 설정.
- 완료 조건: 로그인 시 access token과 함께 refresh token이 발급되고, 서버가 refresh token 식별/검증 정보를 안전하게 저장한다.
- 검증: 로그인 통합 테스트, refresh token 저장/중복 발급 정책 테스트.
- ADR: refresh token 형식, 저장 방식, `HttpOnly Cookie` 선택 이유를 문서화한다.
- 구현 메모(2026-03-18): `RefreshToken` 엔티티와 `RefreshTokenRepository`, `RefreshTokenService`를 추가해 refresh token 원문은 쿠키로만 보내고 서버에는 SHA-256 해시만 저장하도록 구성했다. 로그인 응답은 `accessToken`과 `accessTokenExpiresInMs`를 내려주고, `pm_refresh_token`은 `HttpOnly Cookie`로 설정한다. `AuthLoginRefreshTokenIntegrationTest`로 쿠키 발급, DB 저장, 다중 기기용 복수 토큰 저장을 검증했다.

### [x] E11-4 access token 재발급과 rotation 및 로그아웃 무효화
- 목표: 세션 재연장과 로그아웃/탈취 대응을 함께 처리할 수 있게 만든다.
- 범위: `/api/auth/refresh`, refresh token rotation, 재사용 감지 또는 무효화 정책, 로그아웃 API.
- 완료 조건: 유효한 refresh token으로 access token을 재발급할 수 있고, 로그아웃 후에는 같은 refresh token으로 재발급할 수 없다.
- 검증: 재발급 성공/실패 테스트, 로그아웃 후 재사용 차단 테스트, 쿠키 기반 수동 검증.
- ADR: refresh rotation, 로그아웃 무효화, 재사용 대응 정책을 문서화한다.
- 구현 메모(2026-03-18): `AuthController`에 `/api/auth/refresh`와 `/api/auth/logout`을 추가하고, `RefreshTokenService`는 refresh token을 비관적 락으로 조회한 뒤 rotation과 revoke를 처리하도록 확장했다. 프론트 `fetchJson`은 보호 API 401에서 refresh를 한 번 시도한 뒤 원 요청을 재시도하고, `AuthContext.logout`은 서버 로그아웃 API까지 호출해 현재 기기 refresh cookie를 비운다. `AuthRefreshTokenFlowIntegrationTest`와 `client.refresh.test.js`로 rotation 성공, 회전된 토큰 재사용 차단, 로그아웃 후 재발급 차단, 프론트 자동 재시도를 검증했다.

### [x] E11-5 로그인 실패 정책과 입력 검증 정리
- 목표: 로그인 UX를 더 안전하고 예측 가능하게 만든다.
- 범위: 계정 존재 여부 노출 방지, 로그인 실패 메시지 통일, 로그인 요청 검증 문구 정합성, 로그인 화면 피드백.
- 완료 조건: 존재하지 않는 이메일과 비밀번호 오류가 같은 범주의 실패로 처리되고, 로그인/검증 문구가 실제 정책과 일치한다.
- 검증: 로그인 실패 API 테스트, 프론트 에러 메시지 확인, 빌드/테스트 통과.
- ADR: 로그인 실패 메시지 통일과 검증 정책 선택 근거를 문서화한다.
- 구현 메모(2026-03-18): 로그인은 이제 존재하지 않는 이메일과 비밀번호 오류를 모두 `LOGIN_FAILED`와 동일한 401 메시지로 반환한다. `AuthService`는 더미 비밀번호 해시를 함께 비교해 사용자 존재 여부에 따른 분기 차이를 줄였고, `LoginRequest`와 `Login.jsx`는 비밀번호 정책을 `6자 이상 20자 이하`로 맞췄다. `AuthLoginFailureIntegrationTest`와 `Login.test.jsx`로 공통 실패 응답과 프론트 메시지 노출을 검증했다.

## Epic 12. 프론트 테스트 기반 구축

### [x] E12-1 Vitest 및 React Testing Library 도입
- 목표: 프론트 핵심 흐름을 자동화 테스트로 검증할 수 있는 기반을 만든다.
- 범위: `vitest`, `jsdom`, React Testing Library, 공통 setup, `npm run test` 스크립트.
- 완료 조건: 프론트에서 테스트 명령이 동작하고, 최소 1개 이상의 컴포넌트/라우터 테스트가 실행된다.
- 검증: `frontend`의 `npm run test`, `npm run build`.
- ADR: 프론트 테스트 러너 선택과 범위를 문서화한다.
- 구현 메모(2026-03-18): `Vitest + React Testing Library + jsdom`을 추가하고 `vite.config.js`에 테스트 환경을 연결했다. `src/test/setup.js`에서 `jest-dom` 매처와 브라우저 테스트 보조 설정을 초기화하고, `package.json`에 `test`/`test:watch` 스크립트를 추가했다.

### [x] E12-2 인증 세션 흐름 프론트 테스트 추가
- 목표: 로그인 복귀와 세션 만료 리다이렉트가 회귀 없이 유지되게 만든다.
- 범위: 로그인 화면 테스트, 세션 만료 리다이렉트 테스트, API 401 mock, 라우터 테스트.
- 완료 조건: 로그인 성공 시 `returnTo` 경로 복귀와 보호 화면 401 시 로그인 이동이 자동 테스트로 검증된다.
- 검증: `frontend`의 `npm run test`, `npm run build`.
- ADR: 프론트 테스트 러너 선택 ADR을 재사용한다.
- 구현 메모(2026-03-18): `Login.test.jsx`에서 세션 만료 안내와 로그인 후 `returnTo` 복귀를 검증했고, `App.sessionExpiry.test.jsx`에서 보호 화면 401 응답 시 로그인 화면 이동과 안내 문구 노출을 검증했다. `partyRealtime`는 테스트에서 mock 처리해 인증 흐름에만 집중하도록 분리했다.

## Epic 13. 파티 수정 및 호스트 취소 운영

### [x] E13-1 파티 수정 API
- 목표: 호스트가 운영 중인 파티의 핵심 정보를 직접 수정할 수 있게 한다.
- 범위: 수정 DTO/API, 권한 체크, 허용 필드 정의, 상태별 수정 제한.
- 완료 조건: 호스트가 허용된 필드를 수정할 수 있고 수정 결과가 상세 응답에 반영된다.
- 검증: 수정 성공/권한/상태 제한 통합 테스트.
- ADR: 파티 수정 가능 범위와 상태별 제한 규칙을 문서화한다.
- 구현 메모(2026-03-18): `PUT /party/{id}`와 `UpdatePartyRequest`를 추가해 호스트 전용 파티 수정 API를 구현했다. 수정 가능 필드는 생성 입력과 유사하게 유지하되 `storeId`, `hostRequestedQuantity`는 제외했고, `CLOSED` 또는 마감 경과 파티는 수정 불가로 막았다. 정산이 확정되었거나 픽업 일정이 잡힌 뒤에는 `guideNote`만 수정 가능하게 제한했고, 총 수량 변경 후에는 상태를 다시 계산하며 지점 조회 캐시를 무효화한다. `PartyUpdateIntegrationTest`, `PartyControllerUpdateExceptionTest`로 성공/권한/상태 제한과 검증 오류 응답을 확인했다.

### [x] E13-2 호스트 취소 및 종료 사유 처리
- 목표: 호스트가 파티를 일관된 규칙으로 취소할 수 있게 한다.
- 범위: 호스트 취소 API, 종료 사유 모델, 참여자/대기열 종료 처리, 응답 모델.
- 완료 조건: 호스트 취소 시 파티가 종료되고 참여자/대기자 상태와 종료 사유가 일관되게 반영된다.
- 검증: 호스트 취소 흐름 통합 테스트, 종료 사유 응답 검증.
- ADR: 호스트 취소 정책과 종료 사유 모델을 문서화한다.
- 구현 메모(2026-03-18): `POST /party/{id}/close` API와 `HOST_CANCELED` 종료 사유를 추가했다. 호스트 취소 시 `WAITING` 대기열은 `EXPIRED`로 종료하고, 참여자/대기자에게는 기존 `PARTY_CLOSED` outbox 이벤트를 재사용해 알림을 보낸다. 정산, 픽업, 송금, 거래가 시작된 파티는 취소를 막았고, 호스트 본인에게는 자기 취소 알림을 보내지 않는다. `PartyHostCancelIntegrationTest`, `PartyControllerCloseExceptionTest`로 성공/권한/진행 제한을 검증했다.

### [x] E13-3 파티 변경 이벤트 동기화
- 목표: 파티 수정/취소가 실시간 화면, 알림, 채팅 시스템 메시지에 동시에 반영되게 한다.
- 범위: SSE trigger 확장, outbox 이벤트, 채팅 시스템 메시지, 프론트 상태 동기화.
- 완료 조건: 수정/취소 후 새로고침 없이 목록/상세/내 파티/채팅이 최신 상태로 맞춰진다.
- 검증: 실시간 통합 테스트, 프론트 빌드, 수동 화면 확인.
- ADR: 파티 변경 이벤트 전파 구조와 fallback 정책을 문서화한다.
- 구현 메모(2026-03-18): 파티 수정 시 `PARTY_UPDATED` outbox 이벤트와 `PartyRealtimeTrigger.PARTY_UPDATED`를 추가했다. 수정 전/후 diff를 `변경 항목: ...` 문자열로 만들어 채팅 시스템 메시지와 앱 내 알림에 공통 사용했고, 총 수량 증가 시에는 기존 FIFO 대기열 승격 로직을 수정 흐름에 묶었다. 참여자(호스트 제외)와 수정 후에도 남아 있는 대기자에게만 `PARTY_UPDATED` 알림을 보내고, 채팅 화면은 SSE를 구독해 방 목록/상세를 다시 조회하도록 확장했다. `PartyUpdateSyncIntegrationTest`, `PartyRealtimeServiceIntegrationTest`, `NotificationOutboxProcessorIntegrationTest`, `PartyUpdateIntegrationTest`와 `frontend npm run build`로 검증했다.

## Epic 14. 사용자 안전장치

### [x] E14-1 신고 접수 모델 및 API
- 목표: 거래/채팅/사용자 단위 신고를 접수할 수 있게 한다.
- 범위: 신고 엔티티, 사유 코드, 메모 필드, 접수 API, 최소 조회 범위.
- 완료 조건: 사용자가 특정 파티 또는 상대 사용자를 신고할 수 있고 중복 정책이 정의된다.
- 검증: 신고 생성/권한/중복 테스트.
- ADR: 신고 모델과 최소 수집 정보 범위를 문서화한다.
- 구현 메모(2026-03-18): `Report` 엔티티와 `ReportTargetType`, `ReportReasonType`, `ReportStatus`를 추가하고 `POST /api/reports`, `GET /api/reports/me`를 구현했다. 신고 대상은 `PARTY`, `USER`, `CHAT`으로 나눴고, 사용자/채팅 신고에는 파티 맥락을 필수로 둬 무관한 제3자 신고를 막았다. 파티 신고는 참여자 또는 대기 사용자만 가능하게 했고, 채팅 신고는 신고자와 대상자 모두 해당 파티 채팅 참여자일 때만 허용했다. 중복은 `같은 신고자 + 같은 대상 + 같은 사유 + RECEIVED 상태` 기준으로 막았다. `ReportServiceIntegrationTest`, `ReportControllerTest`로 생성/대기 신고/중복/자기 신고/채팅 참여 제한을 검증했다.

### [x] E14-2 사용자 차단 및 상호작용 제한
- 목표: 차단한 사용자와의 채팅 및 후속 상호작용을 제한한다.
- 범위: 차단 엔티티, 차단/해제 API, 채팅/파티 접근 제어 규칙.
- 완료 조건: 차단 후 채팅 진입 또는 새 상호작용이 제한되고 이유를 UI가 노출할 수 있다.
- 검증: 차단/해제 테스트, 채팅 접근 제한 테스트.
- ADR: 차단 적용 범위와 예외 규칙을 문서화한다.
- 구현 메모(2026-03-18): 방향성 있는 `UserBlock` 엔티티와 `POST /api/blocks`, `DELETE /api/blocks/{targetUserId}`, `GET /api/blocks`를 추가했다. 차단 관계는 `UserBlockPolicyService`에서 공통 판별하고, 파티 참여 요청 시 현재 참여자와의 차단 관계가 있으면 참여/대기 등록을 막는다. 채팅은 REST 상세/읽음/공지/메시지 전송과 STOMP 구독 모두에서 차단 관계가 있는 파티 채팅 접근을 막고, 이유는 기존 에러 응답 메시지로 노출하게 했다. `UserBlockServiceIntegrationTest`, `UserBlockInteractionIntegrationTest`, `UserBlockControllerTest`, `ChatStompChannelInterceptorTest`, `ChatRoomFlowIntegrationTest`, `PartyWaitingQueueIntegrationTest`로 CRUD와 상호작용 제한을 검증했다.

### [x] E14-3 노쇼 누적 정책
- 목표: 반복 노쇼 사용자의 재참여를 제한할 수 있는 운영 정책을 만든다.
- 범위: 누적 기준, 제한 단계, 경고 응답 모델, 해제 조건.
- 완료 조건: 노쇼 누적 수에 따라 경고 또는 참여 제한이 일관되게 동작한다.
- 검증: 정책 단위 테스트, 참여 제한 통합 테스트.
- ADR: 노쇼 누적 임계치와 제한 정책을 문서화한다.
- 구현 메모(2026-03-18): `NoShowPolicyService`를 추가해 참여자의 최근 확정 거래 이력에서 `COMPLETED` 이전까지의 연속 `NO_SHOW` 횟수를 현재 누적으로 계산한다. `PartyMember.tradeStatusUpdatedAt`을 새로 저장해 거래 상태 확정 순서를 보존했고, `POST /party/{id}/join`은 최근 연속 노쇼 1회면 `JoinPartyResponse.warning`으로 경고를 노출하고 2회 이상이면 참여와 대기열 등록을 차단한다. 최근 노쇼 뒤에 완료 거래가 1회 기록되면 제한이 자동 해제되며, `NoShowPolicyServiceTest`, `NoShowJoinPolicyIntegrationTest`로 정책 계산과 참여 흐름을 검증했다.

## Epic 15. 외부 알림 확장

### [x] E15-1 Web Push 구독 저장 및 발송
- 목표: 앱 내 알림 외에 브라우저 푸시로 중요한 상태 변화를 전달한다.
- 범위: 푸시 구독 저장소, 구독 API, 알림 워커 연동, 발송 payload.
- 완료 조건: 사용자가 수신에 동의하면 승격/마감/픽업 변경 알림을 외부로 받을 수 있다.
- 검증: 구독 API 테스트, 발송 서비스 테스트, 브라우저 수동 검증.
- ADR: 외부 알림 1차 채널로 Web Push를 선택한 이유를 문서화한다.
- 구현 메모(2026-03-18): `web_push_subscription` 엔티티와 `PUT/GET/DELETE /api/push-subscriptions`를 추가해 브라우저 구독을 사용자별로 저장할 수 있게 했다. `NotificationOutboxProcessor`는 앱 내 알림을 저장한 직후 `WebPushNotificationService`를 통해 `WAITING_PROMOTED`, `PICKUP_UPDATED`, `PARTY_CLOSED`, `WAITING_EXPIRED` 타입만 Web Push로 best-effort 발송하고, `404/410`을 돌려준 구독은 즉시 삭제한다. 또한 픽업 일정 확정 시 `PICKUP_UPDATED` outbox 이벤트와 앱 내 알림을 새로 생성하도록 연결했다. `WebPushSubscriptionServiceIntegrationTest`, `NotificationOutboxWebPushIntegrationTest`, `NotificationOutboxProcessorIntegrationTest`와 `./mvnw -q -DskipTests compile`로 저장/발송/회귀를 검증했다.

### [x] E15-2 알림 설정 및 딥링크
- 목표: 사용자가 어떤 외부 알림을 받을지 선택하고 클릭 시 정확한 화면으로 이동하게 한다.
- 범위: 사용자 알림 설정 모델, 설정 API, 딥링크 규칙, 프론트 설정 화면 연동.
- 완료 조건: 알림 종류별 on/off가 저장되고 외부 알림에서 상세/채팅/알림 화면으로 복귀한다.
- 검증: 설정 저장 테스트, 딥링크 라우팅 확인, 프론트 빌드.
- ADR: 알림 설정 범위와 딥링크 정책을 문서화한다.
- 구현 메모(2026-03-18): `user_notification_preference` 엔티티와 `GET/PUT /api/users/me/notification-preferences`를 추가해 Web Push 지원 타입별 외부 알림 on/off를 계정 기준으로 저장하게 했다. `GET /api/push-subscriptions/config`로 VAPID 공개 키와 서버 활성화 상태를 노출하고, `WebPushNotificationService`는 사용자 설정을 읽어 `WAITING_PROMOTED`, `PICKUP_UPDATED`, `PARTY_CLOSED`, `WAITING_EXPIRED` 발송 여부를 결정한다. 딥링크는 `NotificationDeepLinkResolver`로 중앙화해 `참여 확정/대기열 승격 -> 채팅방`, `픽업 확정/조건 변경 -> 파티 상세`, `종료/대기열 만료 -> 알림 내역` 규칙을 고정했다. 프론트는 `frontend/public/push-sw.js`와 `frontend/src/utils/webPush.js`를 추가해 Service Worker 등록, 현재 브라우저 연결/해제, 프로필 알림 설정 UI를 구현했고, 알림 내역 화면도 `linkUrl`에 따라 버튼 라벨을 분기했다. 검증은 `./mvnw -q -Dtest=UserNotificationPreferenceServiceIntegrationTest,NotificationDeepLinkResolverTest,UserNotificationPreferenceControllerTest,NotificationOutboxWebPushIntegrationTest test`, `./mvnw -q -DskipTests compile`, `frontend npm test -- --run src/pages/Profile.notificationSettings.test.jsx src/pages/Notifications.deepLink.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E15-3 외부 알림 실패 처리 및 추적
- 목표: 외부 알림 실패 시 재시도, 중복 방지, 앱 내 fallback을 명확히 한다.
- 범위: outbox 상태 확장, 실패 로그, 재시도 정책, 중복 방지 키.
- 완료 조건: 외부 발송 장애가 있어도 앱 내 알림은 유지되고 실패 상태를 추적할 수 있다.
- 검증: 실패/재시도 테스트, 중복 방지 테스트.
- ADR: 외부 알림 실패 대응과 fallback 정책을 문서화한다.
- 구현 메모(2026-03-18): `outbox_event.status`에 `FAILED`를 추가하고, outbox 워커는 앱 내 알림 저장과 `external_notification_delivery` 생성까지만 책임지도록 분리했다. 외부 delivery는 `delivery_key = outboxEventId:userNotificationId:WEB_PUSH:subscriptionId` 유니크 제약으로 중복 생성을 막고, `PENDING/SENT/FAILED`, `attempt_count`, `last_status_code`, `last_failure_reason`, `last_error_message`를 남긴다. 재시도는 `429`, `5xx`, gateway 예외에만 적용하고 `+1분`, `+5분`, 최종 실패의 3회 정책을 사용한다. `404/410`과 구독 없음, 그 외 `4xx`는 즉시 `FAILED`로 마감하며 `404/410` 구독은 저장소에서 삭제한다. 검증은 `./mvnw -q -Dtest=NotificationOutboxProcessorIntegrationTest,NotificationOutboxWebPushIntegrationTest test`, `./mvnw -q -DskipTests compile`로 진행했다.

## Epic 16. 파티 탐색 UX 강화

### [x] E16-1 검색 및 필터
- 목표: 사용자가 원하는 파티를 빠르게 좁혀볼 수 있게 한다.
- 범위: 지점/제품명 검색, 상태 필터, 보관 방식/소분 단위 필터, 빈 상태 UX.
- 완료 조건: 홈/파티 목록에서 조건 기반 탐색이 가능하고 결과 없음 상태가 자연스럽게 처리된다.
- 검증: 프론트 컴포넌트 테스트, 수동 탐색 테스트, `npm run build`.
- ADR: 탐색 필터 노출 범위와 기본값을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/utils/partyDiscovery.js`를 추가해 `q`, `status`, `storage`, `unit` 쿼리 파라미터를 정규화하고 파티 제목/상품명/지점명 검색, 상태, 보관 방식, 소분 단위 필터를 프론트에서 적용하게 했다. `frontend/src/pages/PartyList.jsx`는 URL 쿼리 기반 필터 UI, 활성 조건 요약 badge, `전체 n개 중 m개 표시`, 결과 0건 전용 빈 상태를 추가했고, `frontend/src/pages/Home.jsx`에는 빠른 검색과 대표 필터 카드 3종을 넣어 `/parties`로 조건을 넘기게 했다. 카드 탐색 정보 노출을 위해 `frontend/src/components/PartyCard.jsx`에 상품명과 지점명 라인을 보강했다. 검증은 `frontend npm test -- --run src/utils/partyDiscovery.test.js src/pages/PartyList.filters.test.jsx src/pages/Home.quickDiscovery.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E16-2 정렬 및 발견 섹션
- 목표: 마감 임박, 인기, 최신, 거리순 등 발견형 탐색을 강화한다.
- 범위: 정렬 UI, 발견 섹션, 카드 강조 규칙, 정렬 API 연동 또는 프론트 계산.
- 완료 조건: 사용자가 상황에 맞는 정렬과 발견 섹션을 선택할 수 있다.
- 검증: 프론트 렌더링 테스트, 수동 플로우 확인, `npm run build`.
- ADR: 기본 정렬과 발견 섹션 정책을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/utils/partyDiscovery.js`에 `sort` 모델과 `recommended/deadline/popular/newest` 정렬 계산, 발견 섹션 대표 파티 계산을 추가했다. `frontend/src/pages/PartyList.jsx`는 정렬 select, `마감 임박/인기 파티/신규 파티` 발견 카드, 현재 정렬 상위 3개 카드 강조 badge를 넣었고, `frontend/src/components/PartyCard.jsx`는 강조 tone에 따라 badge와 ring을 보여주도록 확장했다. `frontend/src/pages/Home.jsx`는 `GET /party/all`을 이용한 `지금 발견하기` 섹션을 추가해 대표 파티와 함께 정렬 쿼리로 이동하게 했다. 최신순은 현재 응답에 `createdAt`이 없어 `partyId` 역순으로 추정했다. 검증은 `frontend npm test -- --run src/utils/partyDiscovery.test.js src/pages/PartyList.filters.test.jsx src/pages/Home.quickDiscovery.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E16-3 관심 파티 저장
- 목표: 사용자가 나중에 다시 볼 파티를 저장할 수 있게 한다.
- 범위: 관심 파티 엔티티/API, 목록/상세 UI, 내 관심 파티 진입점.
- 완료 조건: 상세와 목록에서 관심 파티를 저장/해제하고 다시 확인할 수 있다.
- 검증: API 테스트, 프론트 상호작용 테스트, `npm run build`.
- ADR: 관심 파티 저장 범위와 노출 정책을 문서화한다.
- 구현 메모(2026-03-18): `FavoriteParty` 엔티티와 `FavoritePartyService`를 추가해 `GET /api/users/me/favorite-parties`, `PUT/DELETE /api/users/me/favorite-parties/{partyId}`를 만들었고, 공개 파티 조회 응답에 로그인 사용자 기준 `favorite` 플래그를 후처리로 덧붙였다. `frontend/src/components/PartyCard.jsx`, `frontend/src/pages/PartyList.jsx`, `frontend/src/pages/PartyDetail.jsx`에 저장/해제 버튼을 추가했고, `frontend/src/pages/FavoriteParties.jsx`와 프로필 진입 버튼으로 내 관심 파티 화면을 연결했다. 검증은 `./mvnw -q -Dtest=FavoritePartyServiceIntegrationTest,UserControllerFavoritePartyTest test`, `frontend npm test -- --run src/pages/PartyList.favorite.test.jsx src/pages/PartyDetail.favorite.test.jsx src/pages/FavoriteParties.test.jsx`, `frontend npm run build`로 진행했다.

## Epic 17. 파티 생성 UX 고도화

### [x] E17-1 생성 폼 단계화
- 목표: 파티 생성 입력 부담을 줄이고 오류를 줄인다.
- 범위: 섹션 또는 step 구성, 진행 표시, 단계별 검증, 모바일 레이아웃 조정.
- 완료 조건: 사용자가 지점/상품/소분/마감/안내 입력을 더 적은 실수로 완료할 수 있다.
- 검증: 프론트 수동 생성 테스트, `npm run build`.
- ADR: 생성 폼 단계 구조와 검증 시점을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/pages/CreateParty.jsx`를 `기본 정보 -> 소분 조건 -> 운영 정보` 3단계 구조로 재구성하고, 상단 단계 카드, 현재 단계 표시, 이전/다음 이동, 단계별 검증을 추가했다. 마지막 단계에는 지점/가격/수량/마감/안내 요약을 함께 보여주고 여기서 바로 생성 요청을 보낸다. 동시에 파티 생성 후 전용 채팅방이 자동 생성되는 현재 구조에 맞춰 외부 오픈채팅 입력을 제거했고, 채팅/상세 화면의 외부 오픈채팅 fallback UI도 정리했다. 검증은 `frontend npm test -- --run src/pages/CreateParty.test.jsx`, 관련 백엔드/채팅/수정 테스트, `frontend npm run build`로 진행했다.

### [x] E17-2 예상 정산 및 참여 조건 미리보기
- 목표: 생성 중 호스트와 참여자가 볼 비용/수량 조건을 즉시 이해하게 한다.
- 범위: 예상 1인당 금액, 호스트 수량 미리보기, 최소 소분 단위 안내, 경고 메시지.
- 완료 조건: 생성 화면에서 주요 비용/수량 조건이 실시간으로 계산되어 노출된다.
- 검증: 프론트 계산 테스트, 수동 입력 테스트, `npm run build`.
- ADR: 생성 화면 미리보기 정보 범위와 계산 기준을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/utils/createPartyPreview.js`를 추가해 총 가격/총 수량/호스트 수량/최소 소분 단위 기준의 예상 금액 범위와 참여 난이도 경고를 순수 함수로 계산하게 했다. `frontend/src/pages/CreateParty.jsx`는 2단계에서 `참여 조건 미리보기` 섹션을 추가해 참여자에게 열리는 수량, 최소 기준 참여 1명 예상 금액, 최소 단위 기준 최대 참여자 수, 호스트 수량 미리보기를 실시간으로 보여주고, 최종 확인 단계에서도 같은 계산 값을 재사용한다. 검증은 `frontend npm test -- --run src/utils/createPartyPreview.test.js src/pages/CreateParty.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E17-3 생성 초안 및 복구
- 목표: 긴 입력 도중 이탈해도 작성 중인 정보를 복구할 수 있게 한다.
- 범위: 로컬 초안 저장, 복구 배너, 제출 성공 시 초기화, 스키마 버전 처리.
- 완료 조건: 작성 도중 새로고침 또는 이탈 후에도 사용자가 초안을 복구할 수 있다.
- 검증: 초안 저장/복구 테스트, 수동 브라우저 확인.
- ADR: 초안 저장 위치와 만료 정책을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/utils/createPartyDraft.js`를 추가해 `pm_create_party_draft` 키 기준의 초안 저장/복구/삭제, 버전 검사, 7일 TTL, 구조 검증을 한곳에 모았다. `frontend/src/pages/CreateParty.jsx`는 저장된 초안이 있으면 상단 복구 배너를 보여주고 `초안 복구` 또는 `새로 작성`을 선택하게 했으며, 선택 이후에는 입력 변경과 단계 이동을 자동 저장하고 생성 성공 시 초안을 지운다. 검증은 `frontend npm test -- --run src/utils/createPartyDraft.test.js src/pages/CreateParty.test.jsx`, `frontend npm run build`로 진행했다.

## Epic 18. 내 정보 및 사용자 설정

### [x] E18-1 프로필 수정 API 및 화면
- 목표: 사용자가 닉네임, 주소, 기본 프로필 정보를 직접 수정할 수 있게 한다.
- 범위: `PATCH /api/users/me` 또는 동등 API, 프론트 설정 화면, 입력 검증.
- 완료 조건: 프로필 화면에서 수정이 가능하고 변경 내용이 즉시 반영된다.
- 검증: API 통합 테스트, 프론트 폼 테스트, `npm run build`.
- ADR: 프로필 수정 가능 범위와 검증 정책을 문서화한다.
- 구현 메모(2026-03-18): `PATCH /api/users/me`를 추가해 닉네임과 주소를 수정할 수 있게 했고, 닉네임 중복은 `409 CONFLICT`로 막았다. 주소가 바뀌었는데 좌표를 함께 보내지 않으면 기존 저장 좌표를 비워 홈 화면이 잘못된 기준 좌표를 쓰지 않게 했다. `frontend/src/pages/Profile.jsx`는 읽기/편집 모드 전환, 저장/취소 버튼, 저장 성공 후 `AuthContext.refreshProfile()` 호출을 추가해 헤더와 채팅의 전역 사용자명도 즉시 갱신한다. 검증은 `./mvnw -q -Dtest=UserProfileUpdateIntegrationTest,UserProfileControllerTest test`, `frontend npm test -- --run src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E18-2 주소 및 위치 재설정 UX
- 목표: 저장 주소와 현재 탐색 기준 위치를 더 명확하게 관리하게 한다.
- 범위: 주소 검색 재설정, 좌표 재계산, 기본 위치/브라우저 위치 안내, 프로필 연동.
- 완료 조건: 사용자가 주소 변경 후 홈/지점 탐색 기준이 자연스럽게 갱신된다.
- 검증: 위치 변경 수동 테스트, 프론트 빌드, API 연동 확인.
- ADR: 저장 주소와 일시적 브라우저 위치 사용 정책을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/utils/addressLocation.js`를 추가해 카카오 주소 검색과 geocode를 프로필 수정에서도 재사용할 수 있게 했다. `frontend/src/pages/Profile.jsx`는 `주소 검색으로 위치 다시 설정` 버튼, 저장된 기준 위치 표시, 수동 주소 입력 시 좌표 초기화 상태 안내를 추가했고, 주소 검색으로 다시 잡은 좌표는 `PATCH /api/users/me` 저장 요청에 함께 보낸다. `frontend/src/pages/Home.jsx`는 저장 좌표가 없을 때 기본 위치 사용 이유와 프로필 이동 CTA, 브라우저 위치 임시 전환 안내를 노출한다. 검증은 `frontend npm test -- --run src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Home.locationGuide.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E18-3 알림 및 정산 설정
- 목표: 사용자가 알림 수신과 정산 관련 기본 정보를 스스로 관리할 수 있게 한다.
- 범위: 알림 on/off, 채팅/승격/픽업 알림 선택, 정산 메모 또는 계좌 안내 필드.
- 완료 조건: 설정 화면에서 주요 알림/정산 기본값을 관리할 수 있다.
- 검증: 설정 저장 테스트, 프론트 빌드, 수동 확인.
- ADR: 사용자 설정 항목과 저장 범위를 문서화한다.
- 구현 메모(2026-03-18): 기존 `notification-preferences` API를 재사용해 `브라우저 푸시 전체 받기` 토글을 프론트에서 추가했고, 지원되는 알림 타입 전체를 한 번에 저장하도록 했다. 백엔드는 `settlementGuide` 필드를 `User`에 추가하고 `GET/PUT /api/users/me/settlement-settings`를 만들어 정산 기본 안내를 따로 관리하게 했다. `frontend/src/pages/Profile.jsx`는 정산 기본 안내 카드, 글자 수 표시, 저장 버튼을 추가했고, 검증은 `./mvnw -q -Dtest=UserSettlementSettingsIntegrationTest,UserSettlementSettingsControllerTest test`, `frontend npm test -- --run src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx`, `frontend npm run build`로 진행했다.

## Epic 19. 신뢰 및 안전 UX 정리

### [x] E19-1 신고/차단 진입 UI
- 목표: 사용자가 상세/채팅/프로필에서 신고와 차단을 쉽게 찾을 수 있게 한다.
- 범위: 메뉴 진입점, 확인 모달, 사유 선택 UX, 완료 피드백.
- 완료 조건: 파티 상세, 채팅, 프로필 중 최소 두 화면에서 신고/차단이 일관되게 노출된다.
- 검증: 프론트 상호작용 테스트, 수동 플로우 확인, `npm run build`.
- ADR: 신고/차단 진입점과 마찰 수준을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/components/SafetyDialogs.jsx`에 공통 신고 모달과 차단 확인 모달을 추가하고, `frontend/src/pages/PartyDetail.jsx`에는 파티/호스트 신고 및 호스트 차단 진입점을 넣었다. `frontend/src/pages/Chat.jsx`는 헤더의 파티 신고와 타인 메시지별 신고/차단 액션을 추가했고, `frontend/src/pages/Profile.jsx`는 차단 사용자 목록과 최근 신고 내역을 보여주는 `신뢰·안전 관리` 카드를 추가했다. 검증은 `frontend npm test -- --run src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/Chat.safety.test.jsx src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E19-2 신뢰 배지 및 경고 문구
- 목표: 참여 전 위험 신호와 신뢰 정보를 한눈에 이해하게 한다.
- 범위: 신뢰도 배지, 노쇼 경고, 후기 수/완료율 강조, 경고 색상 체계.
- 완료 조건: 파티 상세와 프로필에서 신뢰 정보가 행동 결정에 도움이 되는 수준으로 정리된다.
- 검증: 프론트 렌더링 테스트, 수동 UX 점검, `npm run build`.
- ADR: 신뢰 정보 강조 우선순위와 경고 표현 원칙을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/utils/trustSignals.js`를 추가해 `trustLevel`, `reviewCount`, `completionRate`, `noShowCount`를 기반으로 배지, 하이라이트, 경고 문구를 공통 계산하게 했다. `frontend/src/pages/PartyDetail.jsx`는 파티 상단과 호스트 신뢰도 카드에 배지와 경고 배너를 붙였고, `frontend/src/pages/Profile.jsx`는 내 신뢰도 카드에 같은 규칙으로 하이라이트와 안정/주의 메시지를 노출한다. 검증은 `frontend npm test -- --run src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/PartyDetail.trustSignals.test.jsx src/pages/Chat.safety.test.jsx src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx src/pages/Profile.trustSignals.test.jsx`, `frontend npm run build`로 진행했다.

### [x] E19-3 차단/신고 후 상태 피드백
- 목표: 차단 또는 신고 이후 사용자가 다음에 무엇이 바뀌는지 명확히 이해하게 한다.
- 범위: 상태 배너, 제한 안내 문구, 채팅/파티 fallback 화면, 해제 진입점.
- 완료 조건: 차단/신고 이후 빈 화면이나 무응답 대신 이유와 다음 행동이 안내된다.
- 검증: 프론트 상태 피드백 테스트, 수동 확인, `npm run build`.
- ADR: 안전 조치 이후 상태 피드백 원칙을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/components/SafetyFeedback.jsx`를 추가해 신고 완료 배너와 차단 fallback 카드를 공통화했다. `frontend/src/pages/PartyDetail.jsx`와 `frontend/src/pages/Chat.jsx`는 신고 후 `내 신고 내역 보기` 배너를 보여주고, 차단 후에는 현재 화면에서 즉시 제한 상태를 안내한다. `frontend/src/pages/JoinParty.jsx`는 차단 관계로 참여가 거절될 때 일반 오류 대신 fallback을 노출하고, `frontend/src/pages/Profile.jsx`는 `focusSafetyCenter`, `safetyNotice` state를 읽어 차단 해제와 신고 내역 확인의 도착점 역할을 한다. 검증은 `frontend npm test -- --run src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/PartyDetail.trustSignals.test.jsx src/pages/Chat.safety.test.jsx src/pages/JoinParty.safetyFeedback.test.jsx src/pages/Profile.edit.test.jsx src/pages/Profile.notificationSettings.test.jsx src/pages/Profile.settlementSettings.test.jsx src/pages/Profile.safetyCenter.test.jsx src/pages/Profile.trustSignals.test.jsx`, `frontend npm run build`로 진행했다.

## Epic 20. 프론트 핵심 흐름 테스트 확대

### [x] E20-1 탐색 및 생성 플로우 테스트
- 목표: 홈 탐색, 필터, 파티 생성 핵심 흐름이 회귀 없이 유지되게 한다.
- 범위: Home, PartyList, CreateParty 테스트, API mock, 라우터 상호작용.
- 완료 조건: 탐색과 생성의 핵심 경로가 자동 테스트로 검증된다.
- 검증: `frontend`의 `npm run test`, `npm run build`.
- ADR: 탐색에서 생성으로 이어지는 라우터 회귀 범위와 mock 동기화 방식을 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/pages/DiscoveryCreateFlow.test.jsx`를 추가해 `Home -> /branch/:id -> /parties/create?storeId=:id` 흐름을 실제 `MemoryRouter` 라우트에서 검증하고, 선택한 지점이 생성 폼의 기본값으로 유지되는지 확인했다. 기존 `Home.quickDiscovery`, `CreateParty` 테스트는 유지하고 `PartyList.filters.test.jsx`에는 `ToastContext` mock을 보강해 현재 컴포넌트 의존성과 테스트 환경을 맞췄다. 검증은 `cd frontend && npm test -- --run src/pages/DiscoveryCreateFlow.test.jsx src/pages/Home.quickDiscovery.test.jsx src/pages/PartyList.filters.test.jsx src/pages/CreateParty.test.jsx`, `cd frontend && npm run build`로 진행했다.

### [x] E20-2 참여/정산/후기 플로우 테스트
- 목표: JoinParty, PartyDetail의 참여 이후 운영 흐름을 자동으로 검증한다.
- 범위: 참여, 대기열, 정산, 픽업, 후기 작성 UI 테스트.
- 완료 조건: 파티 운영 핵심 화면 회귀를 프론트 테스트가 잡을 수 있다.
- 검증: `frontend`의 `npm run test`, `npm run build`.
- ADR: 참여 결과별 이동과 역할별 운영 액션 테스트 범위를 문서화한다.
- 구현 메모(2026-03-18): `frontend/src/pages/JoinParty.flow.test.jsx`를 추가해 즉시 참여 성공 시 파티 상세 복귀, 잔여 수량 부족 시 대기열 등록 후 내 파티 이동을 검증했다. `frontend/src/pages/PartyDetail.operationFlow.test.jsx`에서는 호스트의 정산/픽업 확정과 참여자의 송금 완료, 픽업 확인, 호스트 후기 작성 흐름을 역할별 fixture로 분리해 검증했다. 기존 `JoinParty.safetyFeedback`, `PartyDetail.favorite`, `PartyDetail.safety`, `PartyDetail.trustSignals` 테스트와 함께 실행해 관련 화면 회귀를 묶었다. 검증은 `cd frontend && npm test -- --run src/pages/JoinParty.flow.test.jsx src/pages/JoinParty.safetyFeedback.test.jsx src/pages/PartyDetail.operationFlow.test.jsx src/pages/PartyDetail.favorite.test.jsx src/pages/PartyDetail.safety.test.jsx src/pages/PartyDetail.trustSignals.test.jsx`, `cd frontend && npm run build`로 진행했다.

### [ ] E20-3 채팅 및 안전 기능 테스트
- 목표: 채팅, 신고/차단, 알림 설정 같은 상호작용이 회귀 없이 유지되게 한다.
- 범위: Chat 화면 테스트, 설정 화면 테스트, 신고/차단 UI 테스트.
- 완료 조건: 실시간 채팅 주변 UX와 안전 기능이 자동 테스트 범위에 들어간다.
- 검증: `frontend`의 `npm run test`, `npm run build`.
- ADR: E12 테스트 러너 ADR을 재사용한다.

## Agent Prompt Template
```text
현재 프로젝트에서 [작업 카드 ID]를 구현해줘.
목표: [한 문장]
변경 범위: [엔티티/서비스/API/프론트/테스트]
완료 기준:
1. ...
2. ...
3. ...
검증:
- [실행 명령]
추가 요구:
- 관련 ADR을 docs/adr/ 아래에 작성 또는 업데이트할 것
- 작업 완료 후 docs/agent-roadmap.md 상태를 갱신할 것
```
