# E16 파티 검색 및 필터 스펙

- 작성일: 2026-03-18
- 대상 작업: `E16-1`
- 관련 화면: `frontend/src/pages/Home.jsx`, `frontend/src/pages/PartyList.jsx`
- 관련 API: `GET /party/all`, `GET /api/stores/{storeId}/parties`

## 목적
- 홈과 파티 목록에서 원하는 파티를 빠르게 좁혀볼 수 있게 한다.
- 별도 백엔드 검색 API 없이 현재 파티 요약 응답만으로 1차 탐색 경험을 제공한다.
- 필터 상태를 URL 쿼리로 유지해 새로고침, 링크 공유, 홈 진입점 이동 시 같은 조건을 재현할 수 있게 한다.

## 적용 화면
- 홈
  - 빠른 검색 입력
  - 대표 필터 카드
- 전체 파티 목록 `/parties`
- 지점별 파티 목록 `/branch/:id`

## 필터 모델
- `q`
  - 자유 검색어
  - 파티 제목, 상품명, 지점명에 대해 부분 일치 검색
- `status`
  - `all`
  - `active`
  - `full`
  - `closed`
- `storage`
  - `all`
  - `ROOM_TEMPERATURE`
  - `REFRIGERATED`
  - `FROZEN`
- `unit`
  - `all`
  - `ONE`
  - `TWO_PLUS`
  - `FIVE_PLUS`

## 기본값
- 검색어는 빈 문자열
- `status`, `storage`, `unit`은 모두 `all`
- 유효하지 않은 쿼리 값은 기본값으로 정규화한다.

## 필터 적용 규칙
- 검색어는 정규화된 파티 요약의 `title`, `productName`, `storeName`를 하나의 문자열로 합쳐 소문자 부분 일치로 비교한다.
- 상태 필터는 정규화된 `status` 값과 일치 비교한다.
- 보관 방식 필터는 `storageType`과 일치 비교한다.
- 소분 단위 필터는 `minimumShareUnit` 기준으로 계산한다.
  - `ONE`: 1개
  - `TWO_PLUS`: 2개 이상
  - `FIVE_PLUS`: 5개 이상

## 홈 빠른 탐색
- 검색 입력 후 `검색` 버튼을 누르면 `/parties?q=...` 로 이동한다.
- 대표 필터 카드는 다음 쿼리로 이동한다.
  - 모집 중만: `status=active`
  - 냉장 식품: `storage=REFRIGERATED`
  - 1개 소분: `unit=ONE`

## 파티 목록 UX
- 필터 UI는 검색 input, 상태 pill, 보관 방식 select, 소분 단위 select로 구성한다.
- 활성 필터가 있으면 `필터 초기화` 버튼을 노출한다.
- 현재 표시 건수를 `전체 n개 중 m개 표시`로 보여준다.
- 활성 조건은 badge 요약으로 노출한다.
- 전체 데이터가 비어 있으면 기존 빈 상태를 유지한다.
- 전체 데이터는 있지만 필터 결과가 0건이면 별도 빈 상태와 초기화 버튼을 보여준다.

## 현재 한계
- 검색/필터는 현재 목록에 로드된 데이터에 대해서만 프론트에서 수행한다.
- 서버 정렬, 페이지네이션, 인기/마감임박 등 발견형 계산은 다음 작업 `E16-2`에서 다룬다.
- 검색 대상 필드는 현재 파티 요약 응답에 포함된 값으로 제한된다.
