package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.SettlementSettingsResponse;
import com.project.partition_mate.dto.UpdateSettlementSettingsRequest;
import com.project.partition_mate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserSettlementSettingsIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void 정산_기본_안내를_저장한다() {
        // given
        User user = userRepository.saveAndFlush(new User("tester", "tester@test.com", "pw", "서울 서초구", 37.5, 127.0));
        UpdateSettlementSettingsRequest request = updateRequest("입금 후 채팅에 마지막 4자리를 남겨주세요.");

        // when
        SettlementSettingsResponse response = userService.updateSettlementSettings(user, request);

        // then
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(response.getSettlementGuide()).isEqualTo("입금 후 채팅에 마지막 4자리를 남겨주세요.");
        assertThat(persistedUser.getSettlementGuide()).isEqualTo("입금 후 채팅에 마지막 4자리를 남겨주세요.");
    }

    @Test
    void 공백_정산_기본_안내는_비운다() {
        // given
        User user = userRepository.saveAndFlush(new User("tester", "tester@test.com", "pw", "서울 서초구", 37.5, 127.0));
        user.updateSettlementGuide("기존 안내");
        userRepository.saveAndFlush(user);
        UpdateSettlementSettingsRequest request = updateRequest("   ");

        // when
        SettlementSettingsResponse response = userService.updateSettlementSettings(user, request);

        // then
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(response.getSettlementGuide()).isNull();
        assertThat(persistedUser.getSettlementGuide()).isNull();
    }

    private UpdateSettlementSettingsRequest updateRequest(String settlementGuide) {
        UpdateSettlementSettingsRequest request = new UpdateSettlementSettingsRequest();
        ReflectionTestUtils.setField(request, "settlementGuide", settlementGuide);
        return request;
    }
}
