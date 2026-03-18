# E16 파티 정렬 및 발견 섹션 스펙

- 작성일: 2026-03-18
- 대상 작업: `E16-2`
- 관련 화면: `frontend/src/pages/PartyList.jsx`, `frontend/src/pages/Home.jsx`
- 관련 API: `GET /party/all`, `GET /api/stores/{storeId}/parties`, `GET /api/stores/nearby`

## 목적
- 필터 이후 어떤 파티를 먼저 보여줄지 명확한 기준을 제공한다.
- 홈과 파티 목록에서 마감 임박, 인기, 신규 관점의 발견 진입점을 제공한다.
- 별도 백엔드 정렬 API 없이 현재 파티 요약 응답으로 정렬과 발견 카드를 계산한다.

## 정렬 모델
- 쿼리 파라미터: `sort`
- 지원 값
  - `recommended`
  - `deadline`
  - `popular`
  - `newest`
- 기본값은 `recommended`
- 유효하지 않은 값은 `recommended`로 정규화한다.

## 정렬 규칙
- 공통 우선순위
  - `active`
  - `full`
  - `closed`
- `recommended`
  - 상태 우선순위 후 `마감 임박 -> 인기 -> 최신` 순으로 tie-break 한다.
- `deadline`
  - `deadline`이 빠른 순
  - tie-break는 `popular -> newest`
- `popular`
  - 달성률(`currentQuantity / targetQuantity`) 높은 순
  - tie-break는 남은 수량 적은 순, 이후 `deadline -> newest`
- `newest`
  - 현재 API에 `createdAt`이 없으므로 `partyId` 내림차순으로 최신을 추정한다.
  - tie-break는 `popular -> deadline`

## 발견 섹션
- 파티 목록
  - `마감 임박`, `인기 파티`, `신규 파티` 카드 3개를 노출한다.
  - 각 카드는 현재 필터 결과 안에서 해당 기준의 대표 파티 1개를 보여준다.
  - 카드를 누르면 같은 필터를 유지한 채 `sort`만 바꾼다.
- 홈
  - `지금 발견하기` 섹션에서 같은 3개 카드를 노출한다.
  - 카드를 누르면 `/parties?sort=...` 로 이동한다.
- 거리순
  - 홈의 `내 주변 지점` 섹션은 기존처럼 거리순으로 유지한다.

## 카드 강조 규칙
- 파티 목록의 현재 정렬 결과 상위 3개 카드에 강조 badge를 붙인다.
- badge 라벨
  - `recommended`: `추천`
  - `deadline`: `마감 임박`
  - `popular`: `인기 파티`
  - `newest`: `신규`
- 강조 색상은 정렬 기준별 tone을 사용한다.

## 현재 한계
- `recommended`와 발견 섹션은 모두 프론트 계산이며 서버 기준 정렬 보장은 없다.
- `newest`는 `partyId` 역순 추정이라 실제 생성 시각과 다를 수 있다.
- 발견 섹션은 현재 로드된 목록에서만 계산되므로, 추후 페이지네이션이나 서버 검색이 도입되면 정책을 다시 맞춰야 한다.
