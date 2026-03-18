# E20 탐색 및 생성 플로우 테스트 스펙

- 작성일: 2026-03-18
- 대상 작업: `E20-1`
- 관련 화면: `frontend/src/pages/Home.jsx`, `frontend/src/pages/PartyList.jsx`, `frontend/src/pages/CreateParty.jsx`
- 관련 테스트: `frontend/src/pages/Home.quickDiscovery.test.jsx`, `frontend/src/pages/PartyList.filters.test.jsx`, `frontend/src/pages/CreateParty.test.jsx`, `frontend/src/pages/DiscoveryCreateFlow.test.jsx`

## 목적
- 홈 탐색, 지점 파티 목록, 파티 생성 화면 사이의 핵심 라우팅 연결이 회귀 없이 유지되게 한다.
- 검색/필터와 생성 폼 자체 테스트는 유지하되, 페이지 간 handoff가 끊기는 문제를 별도 테스트로 잡는다.

## 검증 대상 흐름
- 홈 검색어 입력 후 `/parties` 쿼리 전달
- 홈 빠른 필터 및 발견 섹션에서 파티 목록 정렬/필터 쿼리 전달
- 홈의 `내 주변 지점` 카드 클릭 후 `/branch/:id` 이동
- 지점 파티 목록의 `파티 만들기` 버튼 클릭 후 `/parties/create?storeId=:id` 이동
- 생성 화면에서 `storeId` 쿼리로 전달된 지점이 기본 선택값으로 유지됨
- 생성 화면의 3단계 입력, 미리보기, 초안 복구 흐름 유지

## 테스트 전략
- 라우터 handoff는 `MemoryRouter` 위에 `Home`, `PartyList`, `CreateParty`를 실제 라우트로 올린 통합 성격 테스트로 검증한다.
- 화면 내부 동작은 기존 페이지별 테스트 파일에서 유지한다.
- `PartyList`는 실제 런타임처럼 `ToastContext` 의존성을 mock에 포함해 테스트 환경과 컴포넌트 의존성을 맞춘다.

## 검증 명령
- `cd frontend && npm test -- --run src/pages/DiscoveryCreateFlow.test.jsx src/pages/Home.quickDiscovery.test.jsx src/pages/PartyList.filters.test.jsx src/pages/CreateParty.test.jsx`
- `cd frontend && npm run build`

## 제외 사항
- 실제 브라우저 지오로케이션 권한 요청과 주소 검색 팝업 상호작용은 이번 카드 범위에 포함하지 않는다.
- 서버 API 계약 검증이나 네트워크 장애 시나리오는 프론트 라우터 흐름 테스트 범위에서 제외한다.
