package com.project.partition_mate.service;

import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.dto.NotificationPreferenceResponse;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.UpdateUserProfileRequest;
import com.project.partition_mate.dto.UpdateNotificationPreferencesRequest;
import com.project.partition_mate.dto.UserNotificationResponse;
import com.project.partition_mate.dto.MyJoinedPartyResponse;
import com.project.partition_mate.dto.ReviewResponse;
import com.project.partition_mate.dto.TrustSummaryResponse;
import com.project.partition_mate.dto.UserResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final TrustScoreService trustScoreService;
    private final UserNotificationPreferenceService userNotificationPreferenceService;
    private final FavoritePartyService favoritePartyService;

    public User getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를  찾을 수 없습니다."));

        return user;
    }

    public UserResponse getProfile(User user) {
        return UserResponse.from(
                user,
                trustScoreService.getTrustSummary(user),
                trustScoreService.getRecentReviews(user, 5)
        );
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateUserProfileRequest request) {
        User persistedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        String nextName = request.getName().trim();
        String nextAddress = request.getAddress().trim();

        userRepository.findByUsername(nextName)
                .filter(foundUser -> !foundUser.getId().equals(persistedUser.getId()))
                .ifPresent(foundUser -> {
                    throw BusinessException.usernameAlreadyExists();
                });

        boolean addressChanged = !Objects.equals(persistedUser.getAddress(), nextAddress);

        persistedUser.updateProfile(nextName, nextAddress);

        if (request.getLatitude() != null && request.getLongitude() != null) {
            persistedUser.updateCoordinate(request.getLatitude(), request.getLongitude());
        } else if (addressChanged) {
            persistedUser.clearCoordinate();
        }

        return getProfile(persistedUser);
    }

    public List<MyJoinedPartyResponse> getMyParties(User user) {
        List<MyJoinedPartyResponse> responses = new ArrayList<>();

        responses.addAll(partyMemberRepository.findByUser(user).stream()
                .map(pm -> MyJoinedPartyResponse.joined(
                        pm.getParty().getId(),
                        pm.getParty().getTitle(),
                        pm.getParty().getProductName(),
                        pm.getParty().getStore() != null ? pm.getParty().getStore().getName() : null,
                        pm.getParty().getPartyStatus(),
                        pm.getParty().getTotalQuantity(),
                        pm.getParty().getRequestedQuantity(),
                        pm.getId(),
                        pm.getRole(),
                        pm.getParty().getDisplayTotalPrice(),
                        pm.getParty().getExpectedTotalPrice(),
                        pm.getParty().getActualTotalPrice(),
                        pm.getParty().getOpenChatUrl(),
                        pm.getRequestedQuantity(),
                        resolveExpectedAmount(pm),
                        pm.getActualAmount(),
                        pm.getPaymentStatus(),
                        pm.getTradeStatus(),
                        pm.getParty().getUnitLabel(),
                        pm.getParty().getMinimumShareUnit(),
                        pm.getParty().getStorageType(),
                        pm.getParty().getPackagingType(),
                        pm.getParty().isHostProvidesPackaging(),
                        pm.getParty().isOnSiteSplit(),
                        pm.getParty().getGuideNote(),
                        pm.getParty().getReceiptNote(),
                        pm.getParty().getPickupPlace(),
                        pm.getParty().getPickupTime(),
                        pm.isPickupAcknowledged(),
                        pm.isReviewEligible(),
                        pm.getParty().getDeadline(),
                        pm.getParty().getClosedAt(),
                        pm.getParty().getCloseReason()
                ))
                .toList());

        responses.addAll(waitingQueueRepository.findAllByUserAndStatusOrderByQueuedAtDesc(user, WaitingQueueStatus.WAITING).stream()
                .map(this::toWaitingResponse)
                .toList());

        return responses;
    }

    private MyJoinedPartyResponse toWaitingResponse(WaitingQueueEntry waitingQueueEntry) {
        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                waitingQueueEntry.getParty(),
                WaitingQueueStatus.WAITING
        );

        int waitingPosition = 1;
        for (WaitingQueueEntry currentEntry : waitingEntries) {
            if (currentEntry.getId().equals(waitingQueueEntry.getId())) {
                break;
            }
            waitingPosition++;
        }

        return MyJoinedPartyResponse.waiting(
                waitingQueueEntry.getParty().getId(),
                waitingQueueEntry.getParty().getTitle(),
                waitingQueueEntry.getParty().getProductName(),
                waitingQueueEntry.getParty().getStore() != null ? waitingQueueEntry.getParty().getStore().getName() : null,
                waitingQueueEntry.getParty().getPartyStatus(),
                waitingQueueEntry.getParty().getTotalQuantity(),
                waitingQueueEntry.getParty().getRequestedQuantity(),
                waitingQueueEntry.getParty().getDisplayTotalPrice(),
                waitingQueueEntry.getParty().getExpectedTotalPrice(),
                waitingQueueEntry.getParty().getActualTotalPrice(),
                waitingQueueEntry.getParty().getOpenChatUrl(),
                waitingPosition,
                waitingQueueEntry.getRequestedQuantity(),
                waitingQueueEntry.getParty().getUnitLabel(),
                waitingQueueEntry.getParty().getMinimumShareUnit(),
                waitingQueueEntry.getParty().getStorageType(),
                waitingQueueEntry.getParty().getPackagingType(),
                waitingQueueEntry.getParty().isHostProvidesPackaging(),
                waitingQueueEntry.getParty().isOnSiteSplit(),
                waitingQueueEntry.getParty().getGuideNote(),
                waitingQueueEntry.getParty().getReceiptNote(),
                waitingQueueEntry.getParty().getPickupPlace(),
                waitingQueueEntry.getParty().getPickupTime(),
                waitingQueueEntry.getParty().getDeadline(),
                waitingQueueEntry.getParty().getClosedAt(),
                waitingQueueEntry.getParty().getCloseReason()
        );
    }

    private Integer resolveExpectedAmount(PartyMember partyMember) {
        if (partyMember.getExpectedAmount() != null) {
            return partyMember.getExpectedAmount();
        }

        List<PartyMember> joinedMembers = partyMember.getParty().getMembers().stream()
                .filter(member -> member.getRequestedQuantity() > 0)
                .sorted(Comparator
                        .comparing(PartyMember::getRequestedQuantity, Comparator.reverseOrder())
                        .thenComparing(PartyMember::getId))
                .toList();

        int totalRequestedQuantity = joinedMembers.stream()
                .mapToInt(PartyMember::getRequestedQuantity)
                .sum();

        if (totalRequestedQuantity <= 0) {
            return null;
        }

        int baseUnitAmount = partyMember.getParty().getExpectedTotalPrice() / totalRequestedQuantity;
        int remainder = partyMember.getParty().getExpectedTotalPrice() % totalRequestedQuantity;

        Map<Long, Integer> allocations = new HashMap<>();
        for (PartyMember member : joinedMembers) {
            int bonus = Math.min(remainder, member.getRequestedQuantity());
            allocations.put(member.getId(), baseUnitAmount * member.getRequestedQuantity() + bonus);
            remainder -= bonus;
        }

        return allocations.get(partyMember.getId());
    }

    public List<UserNotificationResponse> getNotifications(User user) {
        return userNotificationRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(UserNotificationResponse::from)
                .toList();
    }

    public List<NotificationPreferenceResponse> getNotificationPreferences(User user) {
        return userNotificationPreferenceService.getPreferences(user);
    }

    public List<NotificationPreferenceResponse> updateNotificationPreferences(User user,
                                                                              UpdateNotificationPreferencesRequest request) {
        return userNotificationPreferenceService.updatePreferences(user, request);
    }

    public TrustSummaryResponse getTrustSummary(User user) {
        return trustScoreService.getTrustSummary(user);
    }

    public List<ReviewResponse> getRecentReviews(User user, int limit) {
        return trustScoreService.getRecentReviews(user, limit);
    }

    public List<PartyResponse> getFavoriteParties(User user) {
        return favoritePartyService.getFavoriteParties(user);
    }

    public void saveFavoriteParty(User user, Long partyId) {
        favoritePartyService.save(user, partyId);
    }

    public void removeFavoriteParty(User user, Long partyId) {
        favoritePartyService.remove(user, partyId);
    }
}
