# ADR-20260318-refresh-rotation-and-logout-invalidation

- 날짜: 2026-03-18
- 상태: Accepted
- 관련 작업: `E11-4`
- 관련 파일: `src/main/java/com/project/partition_mate/service/AuthService.java`, `src/main/java/com/project/partition_mate/security/RefreshTokenService.java`, `src/main/java/com/project/partition_mate/controller/AuthController.java`, `frontend/src/api/client.js`, `frontend/src/context/AuthContext.jsx`

## 배경
- access token만 짧게 쓰고 refresh token을 저장만 해둔 상태에서는 실제 세션 재연장과 로그아웃 무효화가 되지 않는다.
- refresh token을 rotation 없이 재사용하면 탈취된 토큰이 장기간 유효할 수 있고, 로그아웃 후에도 같은 쿠키로 세션 재발급을 시도할 수 있다.
- 프론트는 401을 받으면 바로 로그인 화면으로 보내는 구조라, refresh token이 준비돼 있어도 사용자 경험상 자동 복구가 되지 않는다.

## 결정사항
- `/api/auth/refresh`를 추가해 `HttpOnly Cookie`의 refresh token으로 새 access token과 새 refresh token을 함께 발급한다.
- refresh token은 DB에서 `PESSIMISTIC_WRITE`로 조회한 뒤 검증하고, 유효하면 현재 토큰을 revoke한 다음 새 토큰을 발급하는 rotation 정책을 사용한다.
- `/api/auth/logout`은 현재 기기의 refresh token만 revoke하고, 응답에서 같은 쿠키를 `Max-Age=0`으로 비워 브라우저에서 제거한다.
- 프론트 `fetchJson`은 보호 API가 401을 반환하면 refresh를 한 번 시도하고, 성공하면 원 요청을 한 번만 재시도한다. refresh까지 실패하면 그때 세션 만료 처리와 로그인 이동을 수행한다.
- 여러 기기 동시 로그인 허용 정책은 유지하고, rotation/로그아웃은 현재 기기의 refresh token에만 적용한다.

## 대안
- refresh token을 rotation 없이 고정 토큰으로 유지한다.
  - 장점: 구현이 단순하고 재발급 로직이 짧다.
  - 단점: 탈취된 토큰의 재사용 창이 길고, 로그아웃 후에도 동일 토큰 통제가 약하다.
- refresh token을 Redis 세션 저장소로 옮기고 기기별 세션을 별도 관리한다.
  - 장점: 만료/무효화와 다중 기기 세션 조회가 빠르고 운영 기능 확장이 쉽다.
  - 단점: 현재 프로젝트 스택에 Redis가 추가되고 배포/운영 복잡도가 커진다.
- refresh token 재사용을 감지하면 사용자 전체 세션을 모두 폐기하는 가족(family) 추적 모델을 도입한다.
  - 장점: 탈취 의심 상황에 더 강하게 대응할 수 있다.
  - 단점: 토큰 계보 모델과 추가 메타데이터가 필요하고, 개인 프로젝트 현재 범위를 넘는 복잡도가 생긴다.

## 결과
- 긍정적 트레이드오프: access token 만료 시 대부분의 보호 API는 자동으로 세션을 복구하고, 로그아웃 후 같은 refresh token 재사용은 차단된다.
- 부정적 트레이드오프: refresh token 가족 추적이나 전체 기기 강제 로그아웃은 아직 없어서, 기기 단위 이상의 보안 대응은 후속 작업으로 남는다.
- 중립적 트레이드오프: 프론트는 401마다 최대 한 번의 refresh 요청을 더 보내므로 요청 흐름이 조금 복잡해지지만, 단일 `refreshPromise`로 중복 재발급은 줄였다.
