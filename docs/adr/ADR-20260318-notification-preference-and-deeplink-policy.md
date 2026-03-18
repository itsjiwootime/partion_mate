# ADR-20260318-notification-preference-and-deeplink-policy

- 날짜: 2026-03-18
- 상태: Accepted
- 관련 작업: `E15-2`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/UserNotificationPreference.java`, `src/main/java/com/project/partition_mate/service/UserNotificationPreferenceService.java`, `src/main/java/com/project/partition_mate/service/NotificationDeepLinkResolver.java`, `src/main/java/com/project/partition_mate/service/WebPushNotificationService.java`, `frontend/src/pages/Profile.jsx`, `frontend/public/push-sw.js`

## 배경
- `E15-1`까지는 브라우저 Web Push 발송 경로만 열려 있었고, 사용자가 어떤 외부 알림을 받을지 선택할 수 없었다.
- 또한 알림 클릭 시 이동 경로가 이벤트 생성 시점마다 흩어져 있어, 어떤 알림은 파티 상세가 맞고 어떤 알림은 채팅방이나 알림 내역이 더 자연스러워도 일관되게 표현하기 어려웠다.
- 프론트도 VAPID 공개 키를 알 수 있는 설정 조회 API와 Service Worker click 처리 규칙이 없어 실제 브라우저 연결 UX를 닫지 못했다.

## 결정사항
- 알림 설정 모델은 사용자별 `user_notification_preference` 테이블로 관리하고, 현재 단계에서는 `notification_type`별 `web_push_enabled`만 저장한다.
- 설정 기본값은 기존 동작을 깨지 않도록 `Web Push 지원 타입은 true`, `미지원 타입은 false`로 둔다.
- Web Push 지원 여부와 표시용 메타데이터는 `UserNotificationType`에 정의하고, 저장 API는 `GET/PUT /api/users/me/notification-preferences`로 제공한다.
- 외부 푸시 구독 전에 프론트가 서버 준비 상태를 확인할 수 있게 `GET /api/push-subscriptions/config`를 추가한다.
- 딥링크는 `NotificationDeepLinkResolver`에서 중앙 관리한다.
  - 참여 확정/대기열 승격: 채팅방
  - 픽업 확정/파티 조건 변경: 파티 상세
  - 파티 종료/대기열 종료: 알림 내역
- Service Worker는 payload의 `url`을 그대로 사용하되, 값이 없으면 `/notifications`로 fallback 한다.

## 대안
- 알림 설정을 프론트 localStorage에만 저장한다.
  - 장점: 서버 테이블과 API를 추가하지 않아도 된다.
  - 단점: 기기 간 동기화가 안 되고, 서버 발송 단계에서 수신 여부를 판단할 수 없다.
- 앱 내 알림 on/off와 Web Push on/off를 한 번에 모두 저장한다.
  - 장점: 사용자가 채널별로 더 세밀하게 제어할 수 있다.
  - 단점: 앱 내 알림 저장 중복 방지, 읽음 처리, 알림 내역 조회 정책까지 같이 바뀌어 현재 카드 범위를 넘긴다.
- 딥링크를 각 이벤트 생성 코드에서 문자열로 직접 만든다.
  - 장점: 구현이 빠르고 단순하다.
  - 단점: 이벤트가 늘수록 라우팅 정책이 분산되고, 채팅/상세/알림내역 기준을 일관되게 유지하기 어렵다.

## 결과
- 긍정적
  - 사용자가 알림 종류별 외부 푸시 on/off를 계정 기준으로 제어할 수 있다.
  - Web Push 지원 범위와 비지원 범위가 API 응답으로 명확히 드러나 프론트가 자연스럽게 UI를 구성할 수 있다.
  - 딥링크 정책을 중앙화해 앱 내 알림과 외부 푸시가 같은 목적지 규칙을 공유한다.
- 부정적
  - 설정은 계정 단위라 여러 브라우저가 같은 Web Push on/off 값을 공유한다.
  - 앱 내 알림 on/off는 아직 지원하지 않아 채널별 완전한 세분화는 불가능하다.
  - Service Worker와 브라우저 권한 상태에 따라 현재 브라우저 연결 UX가 환경 영향을 받는다.
- 중립적
  - 이후 `E15-3`는 현재 설정 모델 위에서 실패 추적과 재시도 정책을 확장하면 된다.
  - 향후 앱 내 알림 채널 제어가 필요해지면 현재 테이블에 채널 필드를 추가하는 방식으로 확장할 수 있다.
