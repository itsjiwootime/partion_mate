package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.dto.PartyRealtimeTrigger;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PartyRealtimeServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartyRealtimeService partyRealtimeService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @AfterEach
    void tearDown() {
        partyRepository.deleteAll();
        storeRepository.deleteAll();
    }

    @Test
    void SSE_구독을_시작하면_connected_이벤트를_받는다() throws Exception {
        // given

        // when
        MvcResult result = mockMvc.perform(get("/party/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();
        String responseBody = awaitResponseContaining(result, "event:connected");

        // then
        assertThat(responseBody)
                .contains("event:connected")
                .contains("\"reconnectDelayMs\":3000");
    }

    @Test
    void partyId_필터와_일치하는_파티_변경만_전달한다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(createStore("파티 필터 지점", 37.50, 127.00));
        Party targetParty = partyRepository.saveAndFlush(createParty(store, "대상 파티"));
        Party otherParty = partyRepository.saveAndFlush(createParty(store, "다른 파티"));

        MvcResult result = mockMvc.perform(get("/party/stream")
                        .param("partyId", String.valueOf(targetParty.getId())))
                .andExpect(request().asyncStarted())
                .andReturn();
        awaitResponseContaining(result, "event:connected");

        // when
        partyRealtimeService.publishPartyUpdatedAfterCommit(otherParty, PartyRealtimeTrigger.PARTY_CREATED);
        Thread.sleep(150);
        partyRealtimeService.publishPartyUpdatedAfterCommit(targetParty, PartyRealtimeTrigger.JOIN_CONFIRMED);
        String responseBody = awaitResponseContaining(result, "\"id\":" + targetParty.getId());

        // then
        assertThat(responseBody)
                .contains("event:party-updated")
                .contains("\"id\":" + targetParty.getId())
                .contains("\"realtimeTrigger\":\"JOIN_CONFIRMED\"")
                .doesNotContain("\"id\":" + otherParty.getId());
    }

    @Test
    void storeId_필터와_일치하는_지점의_파티_변경만_전달한다() throws Exception {
        // given
        Store targetStore = storeRepository.saveAndFlush(createStore("대상 지점", 37.50, 127.00));
        Store otherStore = storeRepository.saveAndFlush(createStore("다른 지점", 37.60, 127.10));
        Party targetParty = partyRepository.saveAndFlush(createParty(targetStore, "대상 지점 파티"));
        Party otherParty = partyRepository.saveAndFlush(createParty(otherStore, "다른 지점 파티"));

        MvcResult result = mockMvc.perform(get("/party/stream")
                        .param("storeId", String.valueOf(targetStore.getId())))
                .andExpect(request().asyncStarted())
                .andReturn();
        awaitResponseContaining(result, "event:connected");

        // when
        partyRealtimeService.publishPartyUpdatedAfterCommit(otherParty, PartyRealtimeTrigger.PARTY_CREATED);
        Thread.sleep(150);
        partyRealtimeService.publishPartyUpdatedAfterCommit(targetParty, PartyRealtimeTrigger.PARTY_CLOSED);
        String responseBody = awaitResponseContaining(result, "\"storeId\":" + targetStore.getId());

        // then
        assertThat(responseBody)
                .contains("\"storeId\":" + targetStore.getId())
                .contains("\"realtimeTrigger\":\"PARTY_CLOSED\"")
                .doesNotContain("\"storeId\":" + otherStore.getId());
    }

    @Test
    void 파티_수정_이벤트는_PARTY_UPDATED_trigger로_전달한다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(createStore("수정 지점", 37.50, 127.00));
        Party party = partyRepository.saveAndFlush(createParty(store, "수정 대상 파티"));

        MvcResult result = mockMvc.perform(get("/party/stream")
                        .param("partyId", String.valueOf(party.getId())))
                .andExpect(request().asyncStarted())
                .andReturn();
        awaitResponseContaining(result, "event:connected");

        // when
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.PARTY_UPDATED);
        String responseBody = awaitResponseContaining(result, "\"realtimeTrigger\":\"PARTY_UPDATED\"");

        // then
        assertThat(responseBody)
                .contains("\"id\":" + party.getId())
                .contains("\"realtimeTrigger\":\"PARTY_UPDATED\"");
    }

    private String awaitResponseContaining(MvcResult result, String expectedToken) throws Exception {
        long deadline = System.currentTimeMillis() + 1_500L;

        while (System.currentTimeMillis() < deadline) {
            String responseBody = result.getResponse().getContentAsString();
            if (responseBody.contains(expectedToken)) {
                return responseBody;
            }
            Thread.sleep(50);
        }

        return result.getResponse().getContentAsString();
    }

    private Store createStore(String name, double latitude, double longitude) {
        return new Store(
                name,
                "서울시 테스트로",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                latitude,
                longitude,
                "02-0000-0000"
        );
    }

    private Party createParty(Store store, String title) {
        return new Party(
                title,
                "테스트 상품",
                10000,
                store,
                4,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(3)
        );
    }
}
