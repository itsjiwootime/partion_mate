# ADR-20260316-notification-worker-retry-policy

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E3-4`
- 관련 파일: `src/main/java/com/project/partition_mate/service/NotificationOutboxProcessor.java`, `src/main/java/com/project/partition_mate/service/NotificationOutboxScheduler.java`, `src/main/java/com/project/partition_mate/domain/UserNotification.java`

## 배경
- outbox 이벤트를 쌓기만 하면 사용자가 실제로 보는 알림 내역이 생성되지 않으므로 별도 소비 워커가 필요했다.
- payload 오류나 일시적 DB 문제로 한 번 실패한 이벤트를 즉시 버리면 알림 누락이 발생하고, 반대로 무제한 재처리를 하면 동일 이벤트 중복 저장 위험이 있다.
- 1차 알림 채널을 앱 내 알림으로 제한한 만큼, 현재 단계에서는 간단하지만 재현 가능한 retry/idempotency 정책이 우선이었다.

## 결정사항
- `NotificationOutboxProcessor`가 `status=PENDING` 이고 `nextAttemptAt <= now` 인 이벤트를 최대 50건씩 polling 방식으로 처리한다.
- 처리 실패 시 1분 뒤 재시도하도록 `retryCount`, `lastError`, `nextAttemptAt`를 갱신한다.
- 사용자 알림은 `eventId:userId:type` 형식의 `externalKey`를 기준으로 먼저 존재 여부를 확인하고, DB 유니크 키를 최종 안전장치로 둬 같은 outbox 이벤트가 재처리돼도 알림은 한 건만 남게 한다.
- 워커 실행은 10초 fixed delay 스케줄러로 돌리고, 실제 발송 채널 대신 `user_notification` 저장을 완료 신호로 삼는다.

## 대안
- 실패 즉시 `FAILED`로 종료
  - 장점: 상태가 단순하고 워커 구현이 쉽다.
  - 단점: 일시적 오류에도 사람이 직접 복구해야 해서 알림 누락 가능성이 크다.
- exponential backoff와 최대 재시도 제한 도입
  - 장점: 장애 상황에서 DB 부하를 줄이고 장기 실패를 제어하기 좋다.
  - 단점: 현재 앱 내 알림 범위에서는 정책이 과하고, 운영 UI 없이 실패 이벤트를 관리하기 어렵다.
- 메시지 큐 consumer acknowledgement 사용
  - 장점: 메시징 플랫폼이 제공하는 재시도/중복 제어를 활용할 수 있다.
  - 단점: 현재 프로젝트에 메시지 브로커가 없고 범위를 넘는 인프라 추가가 필요하다.

## 결과
- 긍정적
  - 간단한 polling 워커만으로 재시도와 중복 방지를 모두 확보했다.
  - `NotificationOutboxProcessorIntegrationTest`에서 payload 오류 재시도와 동일 이벤트 중복 방지를 함께 검증할 수 있다.
  - 이후 이메일이나 푸시 채널을 붙여도 idempotency 기준을 재사용할 수 있다.
- 부정적
  - 고정 1분 재시도는 장애 규모에 따라 너무 짧거나 길 수 있다.
  - 현재는 최대 재시도 제한과 DLQ가 없어 장기 실패 이벤트 관리가 단순하지 않다.
- 중립적
  - 지금 정책은 앱 내 알림 MVP에 맞춘 값이며, 외부 채널이 추가되면 채널별 재시도 정책 분리가 필요하다.
