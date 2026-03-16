# ADR-20260316-party-share-unit-metadata-model

- 날짜: 2026-03-16
- 상태: Accepted
- 관련 작업: `E5-1`
- 관련 파일: `src/main/java/com/project/partition_mate/domain/Party.java`, `src/main/java/com/project/partition_mate/dto/CreatePartyRequest.java`, `src/main/java/com/project/partition_mate/dto/PartyResponse.java`, `src/main/java/com/project/partition_mate/dto/PartyDetailResponse.java`

## 배경
- 기존 파티 모델은 `총수량`과 `상품명`만 저장해서 실제 소분 거래에서 중요한 최소 소분 단위, 보관 방식, 포장 방식을 표현할 수 없었다.
- 트레이더스/코스트코 상품은 냉장/냉동 여부나 원포장 유지 여부에 따라 참여 가능 여부가 달라지므로, 단순 게시글 메모로만 남기면 목록/상세/API 전반에서 일관되게 재사용하기 어렵다.
- 이후 정산, 픽업, 후기 단계에서도 어떤 상품을 어떤 방식으로 나누는지 같은 메타데이터를 공통 기준으로 써야 한다.

## 결정사항
- `Party`에 `unitLabel`, `minimumShareUnit`, `storageType`, `packagingType` 필드를 추가해 소분 기준을 도메인 필드로 승격한다.
- 기본값은 `개`, `1`, `ROOM_TEMPERATURE`, `ORIGINAL_PACKAGE`로 두어 기존 테스트/생성 경로를 깨지 않으면서 새 메타데이터를 점진적으로 도입한다.
- 생성 요청 DTO와 목록/상세/내 파티/실시간 응답 DTO를 모두 확장해, 소분 메타데이터가 어떤 조회 경로로 들어와도 동일하게 전달되도록 맞춘다.
- 보관 방식과 포장 방식은 자유 텍스트 대신 enum으로 제한하고, 화면에는 별도 라벨 필드를 함께 내려 사용자 친화적으로 노출한다.

## 대안
- 안내 메모 하나에 소분 정보를 모두 자유 텍스트로 저장
  - 장점: DB 스키마 변경이 가장 작고 화면 구현이 빠르다.
  - 단점: 필터링, 검증, 일관된 표시가 어렵고 실시간/목록/정산 단계에서 재사용 가치가 낮다.
- 상품 메타데이터를 `PartyProduct` 같은 별도 엔티티로 완전히 분리
  - 장점: 상품 카탈로그 재사용이나 이미지, 바코드 같은 추가 메타데이터 확장에 유리하다.
  - 단점: 현재 프로젝트는 파티 중심 구조라 조회 경로와 생성 흐름이 불필요하게 복잡해진다.
- `minimumShareUnit`만 추가하고 나머지는 enum 없이 문자열로 저장
  - 장점: 필드 수가 적고 데이터 입력 자유도가 높다.
  - 단점: 오탈자와 표현 불일치가 늘고, 프론트가 가능한 값을 별도로 알아야 한다.

## 결과
- 긍정적
  - 파티가 단순 모집 글이 아니라 실제 소분 거래 단위를 설명하는 도메인 모델로 확장됐다.
  - 목록 카드, 상세, 실시간 이벤트, 내 파티 화면이 같은 메타데이터 집합을 공유하게 되어 이후 기능 추가 시 중복 매핑이 줄었다.
  - `PartyProductMetadataTest`, `PartyMetadataIntegrationTest`로 엔티티 검증과 API 응답을 함께 회귀 검증할 수 있게 됐다.
- 부정적
  - DTO와 projection 쿼리, 프론트 정규화 유틸까지 수정 범위가 넓어져 초기 변경량이 커졌다.
  - enum 값을 추가하거나 변경할 때 프론트 옵션과 라벨 노출도 같이 관리해야 한다.
- 중립적
  - 현재는 파티별 메타데이터만 저장하지만, 향후 상품 카탈로그가 필요해지면 이 필드들을 별도 상품 모델의 스냅샷으로 옮길 수 있다.
