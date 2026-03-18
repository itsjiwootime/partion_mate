package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UpdateUserProfileRequest;
import com.project.partition_mate.dto.UserResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserProfileUpdateIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void 내_프로필_정보를_수정한다() {
        // given
        User user = userRepository.saveAndFlush(new User("tester", "tester@test.com", "pw", "서울 서초구", 37.5, 127.0));
        UpdateUserProfileRequest request = updateRequest("새닉네임", "서울 송파구", null, null);

        // when
        UserResponse response = userService.updateProfile(user, request);

        // then
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(response.getName()).isEqualTo("새닉네임");
        assertThat(response.getAddress()).isEqualTo("서울 송파구");
        assertThat(response.getLatitude()).isNull();
        assertThat(response.getLongitude()).isNull();
        assertThat(persistedUser.getUsername()).isEqualTo("새닉네임");
        assertThat(persistedUser.getAddress()).isEqualTo("서울 송파구");
        assertThat(persistedUser.getLatitude()).isNull();
        assertThat(persistedUser.getLongitude()).isNull();
    }

    @Test
    void 이미_사용중인_닉네임으로_수정하면_예외가_발생한다() {
        // given
        User user = userRepository.saveAndFlush(new User("tester", "tester@test.com", "pw", "서울 서초구", 37.5, 127.0));
        userRepository.saveAndFlush(new User("dup", "dup@test.com", "pw", "서울 강남구", 37.6, 127.1));
        UpdateUserProfileRequest request = updateRequest("dup", "서울 서초구", null, null);

        // when
        // then
        assertThatThrownBy(() -> userService.updateProfile(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    private UpdateUserProfileRequest updateRequest(String name, String address, Double latitude, Double longitude) {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "address", address);
        ReflectionTestUtils.setField(request, "latitude", latitude);
        ReflectionTestUtils.setField(request, "longitude", longitude);
        return request;
    }
}
