package com.project.partition_mate.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StoreQueryIndexIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 위치_조회와_지점별_파티_조회에_필요한_인덱스를_생성한다() {
        // given
        List<String> indexNames = jdbcTemplate.queryForList(
                """
                select index_name
                from information_schema.indexes
                where table_name in ('STORE', 'PARTY')
                """,
                String.class
        );

        // when
        String storeExplain = jdbcTemplate.queryForObject(
                "EXPLAIN SELECT * FROM store WHERE latitude BETWEEN 37.4 AND 37.7 AND longitude BETWEEN 127.0 AND 127.3",
                String.class
        );
        String partyExplain = jdbcTemplate.queryForObject(
                "EXPLAIN SELECT * FROM party WHERE store_id = 1 AND party_status = 'RECRUITING'",
                String.class
        );

        // then
        assertThat(indexNames)
                .contains("IDX_STORE_LATITUDE_LONGITUDE", "IDX_PARTY_STORE_STATUS");
        assertThat(storeExplain).containsIgnoringCase("IDX_STORE_LATITUDE_LONGITUDE");
        assertThat(partyExplain).containsIgnoringCase("IDX_PARTY_STORE_STATUS");

        System.out.printf(
                "STORE_QUERY_EXPLAIN storePlan=%s%nSTORE_QUERY_EXPLAIN partyPlan=%s%n",
                storeExplain,
                partyExplain
        );
    }
}
