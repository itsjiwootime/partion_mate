package com.project.partition_mate.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class PartyProductMetadataTest {

    @Test
    void 소분_메타데이터를_포함해_파티를_생성한다() {
        // given
        Store store = new Store(
                "양재 코스트코",
                "서울시 서초구",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        );

        // when
        Party party = new Party(
                "올리브오일 소분",
                "올리브오일 3L",
                36000,
                store,
                6,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(3),
                "ml",
                500,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.CONTAINER,
                true,
                false,
                "용기 세척 후 소분합니다.",
                null,
                null,
                null,
                null
        );

        // then
        assertThat(party.getUnitLabel()).isEqualTo("ml");
        assertThat(party.getMinimumShareUnit()).isEqualTo(500);
        assertThat(party.getStorageType()).isEqualTo(StorageType.ROOM_TEMPERATURE);
        assertThat(party.getPackagingType()).isEqualTo(PackagingType.CONTAINER);
        assertThat(party.isHostProvidesPackaging()).isTrue();
        assertThat(party.isOnSiteSplit()).isFalse();
        assertThat(party.getGuideNote()).isEqualTo("용기 세척 후 소분합니다.");
        assertThat(party.getExpectedTotalPrice()).isEqualTo(36000);
        assertThat(party.getActualTotalPrice()).isNull();
        assertThat(party.getDisplayTotalPrice()).isEqualTo(36000);
    }

    @Test
    void 실구매가를_확정하면_예상가와_분리해_저장한다() {
        // given
        Store store = new Store(
                "하남 트레이더스",
                "경기도 하남시",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.6,
                127.2,
                "02-1111-1111"
        );
        Party party = new Party(
                "비타민 소분",
                "비타민 C",
                30000,
                store,
                4,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(5),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ZIP_BAG,
                true,
                true,
                "행사 가격 적용 예정",
                null,
                null,
                null,
                null
        );

        // when
        party.updateActualPurchase(26500, "행사 할인가 적용");

        // then
        assertThat(party.getExpectedTotalPrice()).isEqualTo(30000);
        assertThat(party.getActualTotalPrice()).isEqualTo(26500);
        assertThat(party.getDisplayTotalPrice()).isEqualTo(26500);
        assertThat(party.getReceiptNote()).isEqualTo("행사 할인가 적용");
    }
}
