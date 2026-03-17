package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserBlock;
import com.project.partition_mate.dto.UserBlockResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.UserBlockRepository;
import com.project.partition_mate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public UserBlockResponse blockUser(User blocker, Long targetUserId) {
        if (blocker.getId().equals(targetUserId)) {
            throw BusinessException.blockSelfNotAllowed();
        }

        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("차단 대상 사용자가 존재하지 않습니다."));

        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw BusinessException.duplicateUserBlock();
        }

        UserBlock userBlock = userBlockRepository.save(UserBlock.create(
                blocker,
                blocked,
                LocalDateTime.now(clock)
        ));
        return UserBlockResponse.from(userBlock);
    }

    @Transactional
    public void unblockUser(User blocker, Long targetUserId) {
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("차단 대상 사용자가 존재하지 않습니다."));

        UserBlock userBlock = userBlockRepository.findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(BusinessException::userBlockNotFound);
        userBlockRepository.delete(userBlock);
    }

    public List<UserBlockResponse> getBlockedUsers(User blocker) {
        return userBlockRepository.findAllByBlockerOrderByCreatedAtDesc(blocker).stream()
                .map(UserBlockResponse::from)
                .toList();
    }
}
