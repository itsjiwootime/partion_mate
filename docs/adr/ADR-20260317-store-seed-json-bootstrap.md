# ADR-20260317-store-seed-json-bootstrap

- 날짜: 2026-03-17
- 상태: Accepted
- 관련 작업: `E10-1`
- 관련 파일: `src/main/resources/seeds/stores.json`, `src/main/java/com/project/partition_mate/config/StoreSeedInitializer.java`, `src/main/java/com/project/partition_mate/service/StoreSeedService.java`, `src/main/java/com/project/partition_mate/domain/Store.java`

## 배경
- 현재 지점 데이터는 `store` 테이블에 미리 있어야만 동작하고, 런타임에 자동으로 채워주는 로직이 없었다.
- H2 같은 빈 개발 DB로 서버를 띄우면 `/api/stores/nearby`가 빈 배열을 반환해 홈 화면과 파티 생성 흐름을 바로 확인하기 어려웠다.
- 테스트에서는 매번 `storeRepository.save(...)`로 지점을 넣고 있었지만, 실제 애플리케이션 실행 시에는 같은 편의가 없었다.

## 결정사항
- 지점 시드는 `src/main/resources/seeds/stores.json`에서 관리한다.
- 애플리케이션 시작 시 `StoreSeedInitializer`가 JSON을 읽어 `StoreSeedService`를 통해 `name` 기준 upsert를 수행한다.
- 기존 지점이 있으면 `Store.applySeed(...)`로 주소, 영업시간, 전화번호, 좌표를 동기화하고, 없으면 새로 생성한다.
- 시드 기능은 `app.seed.stores.enabled`와 `app.seed.stores.resource` 설정으로 제어하고, 테스트 프로필에서는 기본적으로 비활성화한다.

## 대안
- `data.sql`로 고정 INSERT 수행
  - 장점: 구현이 단순하다.
  - 단점: 중복 실행 시 충돌 처리와 업데이트 반영이 불편하고, JSON보다 관리성이 떨어진다.
- 관리자용 지점 등록 API를 먼저 추가
  - 장점: 운영 데이터 변경이 유연하다.
  - 단점: 현재 문제는 "빈 DB에서도 바로 실행 가능해야 한다"는 점이라 초기 비용이 과하다.
- CSV 파일을 직접 읽어 적재
  - 장점: 엑셀 기반 관리와 연동하기 쉽다.
  - 단점: 현재 프로젝트에서는 JSON이 구조 검증과 코드 매핑이 더 단순하다.

## 결과
- 긍정적
  - 빈 개발 DB에서도 서버 기동 직후 지점 조회와 파티 생성 흐름을 확인할 수 있게 됐다.
  - 지점명이 같으면 중복 생성 대신 업데이트해 시드 재실행이 안전해졌다.
  - 테스트 프로필에서는 기능을 끌 수 있어 기존 테스트 데이터 가정을 유지할 수 있다.
- 부정적
  - 지점명이 사실상 자연키 역할을 하므로 이름 변경 정책에 민감해졌다.
  - JSON 파일에 주소/좌표를 계속 직접 관리해야 한다.
- 중립적
  - 이후 관리자 기능이 생기면 JSON 시드는 초기 부트스트랩 용도로 축소하고, 운영 변경은 관리자 경로로 넘기는 구조가 자연스럽다.
