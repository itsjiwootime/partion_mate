# ADR-20260316-deadline-closing-scheduler

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E3-2`
- 관련 파일: `src/main/java/com/project/partition_mate/config/SchedulingConfig.java`, `src/main/java/com/project/partition_mate/service/PartyLifecycleService.java`, `src/main/java/com/project/partition_mate/service/PartyDeadlineScheduler.java`

## 배경
- 마감 시간이 지난 파티를 사용자가 직접 열어보기 전까지 그대로 두면 모집 상태와 실제 비즈니스 상태가 어긋나게 된다.
- 종료 처리에는 대기열 만료, 종료 상태 전이, 알림 이벤트 저장, 캐시 무효화가 함께 묶여야 해서 단순한 조회 시 지연 계산보다 명시적인 종료 작업이 필요했다.
- 개인 프로젝트 단계에서는 Quartz 같은 별도 인프라 없이도 로컬과 테스트 환경에서 재현 가능한 스케줄링 방식이 적합했다.

## 결정사항
- `@EnableScheduling`과 `@Scheduled`를 사용해 30초 간격 fixed delay 스케줄러를 둔다.
- 실제 종료 로직은 `PartyLifecycleService`에 모아두고, 스케줄러는 `closeExpiredParties()`를 호출하는 얇은 트리거로만 유지한다.
- 종료 판단은 `deadline <= now` 이면서 상태가 `RECRUITING` 또는 `FULL`인 파티를 대상으로 한다.
- 시간 의존성은 `Clock` 빈으로 주입해서 테스트에서 종료 기준 시각을 제어할 수 있게 한다.

## 대안
- Quartz 도입
  - 장점: 작업 영속성, 클러스터링, 복잡한 스케줄 표현에 강하다.
  - 단점: 현재 범위에서는 설정과 운영 비용이 크고 단일 서비스 스케줄 요구를 넘는다.
- 사용자 조회 시점에만 만료 여부를 계산
  - 장점: 백그라운드 작업이 필요 없다.
  - 단점: 대기열 만료와 알림 발송이 늦어지고, 조회가 없으면 상태가 영구히 stale해질 수 있다.
- DB 이벤트 스케줄러 사용
  - 장점: 애플리케이션 외부에서 주기 실행이 가능하다.
  - 단점: H2 테스트 환경과의 일관성이 떨어지고 애플리케이션 코드에서 정책을 추적하기 어렵다.

## 결과
- 긍정적
  - 종료 로직을 서비스에 집중시켜 테스트와 운영 경로가 같은 코드를 타게 됐다.
  - 별도 인프라 없이도 자동 종료, 대기열 만료, 알림 이벤트 기록을 주기적으로 처리할 수 있다.
  - `PartyLifecycleIntegrationTest`로 시간 기반 종료 동작을 검증할 수 있다.
- 부정적
  - fixed delay 방식이라 최대 30초 정도 종료 반영 지연이 생길 수 있다.
  - 현재는 단일 인스턴스 전제를 두고 있어 다중 인스턴스 환경에서는 분산 스케줄 조정이 필요하다.
- 중립적
  - 실행 주기는 설정값으로 열어두었기 때문에 트래픽이나 운영 환경에 맞춰 더 짧거나 긴 주기로 조정할 수 있다.
