package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UserBlockResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.UserBlockRepository;
import com.project.partition_mate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
class UserBlockServiceIntegrationTest {

    @Autowired
    private UserBlockService userBlockService;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userBlockRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 사용자를_차단하면_목록에_저장한다() {
        // given
        User blocker = userRepository.saveAndFlush(createUser("blocker"));
        User blocked = userRepository.saveAndFlush(createUser("blocked"));

        // when
        UserBlockResponse response = userBlockService.blockUser(blocker, blocked.getId());
        List<UserBlockResponse> blockedUsers = userBlockService.getBlockedUsers(blocker);

        // then
        assertThat(response.getTargetUserId()).isEqualTo(blocked.getId());
        assertThat(blockedUsers)
                .hasSize(1)
                .extracting(UserBlockResponse::getTargetUserId)
                .containsExactly(blocked.getId());
    }

    @Test
    void 같은_사용자를_다시_차단하면_예외가_발생한다() {
        // given
        User blocker = userRepository.saveAndFlush(createUser("blocker"));
        User blocked = userRepository.saveAndFlush(createUser("blocked"));
        userBlockService.blockUser(blocker, blocked.getId());

        // when
        Throwable thrown = catchThrowable(() -> userBlockService.blockUser(blocker, blocked.getId()));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 차단한 사용자입니다.");
    }

    @Test
    void 자기_자신을_차단하면_예외가_발생한다() {
        // given
        User blocker = userRepository.saveAndFlush(createUser("blocker"));

        // when
        Throwable thrown = catchThrowable(() -> userBlockService.blockUser(blocker, blocker.getId()));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("자기 자신은 차단할 수 없습니다.");
    }

    @Test
    void 차단을_해제하면_목록에서_사라진다() {
        // given
        User blocker = userRepository.saveAndFlush(createUser("blocker"));
        User blocked = userRepository.saveAndFlush(createUser("blocked"));
        userBlockService.blockUser(blocker, blocked.getId());

        // when
        userBlockService.unblockUser(blocker, blocked.getId());

        // then
        assertThat(userBlockService.getBlockedUsers(blocker)).isEmpty();
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }
}
