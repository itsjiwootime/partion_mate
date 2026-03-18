package com.project.partition_mate.service;

import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PaymentStatus;
import com.project.partition_mate.domain.Review;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.dto.ConfirmSettlementRequest;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.CreateReviewRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.PartySettlementMemberResponse;
import com.project.partition_mate.dto.PartyRealtimeTrigger;
import com.project.partition_mate.dto.ReviewResponse;
import com.project.partition_mate.dto.TrustSummaryResponse;
import com.project.partition_mate.dto.UpdatePartyRequest;
import com.project.partition_mate.dto.UpdatePaymentStatusRequest;
import com.project.partition_mate.dto.UpdateTradeStatusRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.ReviewRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
import com.project.partition_mate.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;
    private final StoreQueryCacheSupport storeQueryCacheSupport;
    private final NotificationOutboxService notificationOutboxService;
    private final PartyRealtimeService partyRealtimeService;
    private final ChatService chatService;
    private final PartyLifecycleService partyLifecycleService;
    private final UserBlockPolicyService userBlockPolicyService;
    private final NoShowPolicyService noShowPolicyService;
    private final TrustScoreService trustScoreService;
    private final FavoritePartyService favoritePartyService;
    private final Clock clock;

    @Transactional
    public Party createParty(CreatePartyRequest request) {

        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User host = customUserDetails.getUser();

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 매장 ID입니다."));

        Party party = new Party(
                request.getTitle(),
                request.getProductName(),
                request.getTotalPrice(),
                store,
                request.getTotalQuantity(),
                null,
                resolveDeadline(request.getDeadline()),
                request.getUnitLabel(),
                request.getMinimumShareUnit(),
                request.getStorageType(),
                request.getPackagingType(),
                Boolean.TRUE.equals(request.getHostProvidesPackaging()),
                Boolean.TRUE.equals(request.getOnSiteSplit()),
                request.getGuideNote(),
                null,
                null,
                null,
                null
        );

        PartyMember hostMember = PartyMember.joinAsHost(
                party,
                host,
                request.getHostRequestedQuantity()
        );

        party.acceptMember(hostMember);

        Party savedParty = partyRepository.save(party);
        chatService.initializePartyChatRoom(savedParty, host);
        storeQueryCacheSupport.evictStoreQueries(store.getId());
        partyRealtimeService.publishPartyUpdatedAfterCommit(savedParty, PartyRealtimeTrigger.PARTY_CREATED);
        return savedParty;
    }

    @Transactional
    public JoinPartyResponse joinParty(Long partyId, JoinPartyRequest request) {
        User member = getCurrentUser();
        LocalDateTime now = LocalDateTime.now(clock);

        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        if (userBlockPolicyService.hasBlockedParticipantInParty(party, member.getId())) {
            throw BusinessException.blockedPartyInteractionNotAllowed();
        }

        validatePartyOpenForJoin(party, now);
        validateNotAlreadyJoinedOrWaiting(party, member);
        NoShowPolicyService.Decision noShowDecision = noShowPolicyService.evaluate(member);
        if (noShowDecision.shouldBlock()) {
            throw BusinessException.noShowParticipationRestricted();
        }

        if (canJoinImmediately(party, request.getMemberRequestQuantity())) {
            return joinImmediately(party, member, request.getMemberRequestQuantity(), noShowDecision);
        }

        return joinWaitingQueue(party, member, request.getMemberRequestQuantity(), noShowDecision);
    }

    @Transactional
    public void cancelJoin(Long partyId) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember joinedMember = partyMemberRepository.findByPartyAndUser(party, currentUser).orElse(null);
        if (joinedMember != null) {
            cancelJoinedMember(party, joinedMember);
            return;
        }

        WaitingQueueEntry waitingEntry = waitingQueueRepository.findFirstByPartyAndUserAndStatus(
                party,
                currentUser,
                WaitingQueueStatus.WAITING
        ).orElse(null);
        if (waitingEntry == null) {
            throw BusinessException.notJoinedOrWaiting();
        }

        waitingEntry.cancel();
    }

    @Transactional
    public PartyDetailResponse closeParty(Long partyId) {
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now(clock);
        Party party = partyRepository.findDetailByIdForUpdate(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember actorMember = getRequiredPartyMember(party, currentUser);
        if (!actorMember.isHost()) {
            throw BusinessException.onlyHostCanCloseParty();
        }

        validatePartyClosableByHost(party, now);
        partyLifecycleService.closePartyByHost(party, currentUser, now);
        return buildPartyDetailResponse(party, currentUser);
    }

    @Transactional
    public PartyDetailResponse updateParty(Long partyId, UpdatePartyRequest request) {
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now(clock);
        Party party = partyRepository.findDetailByIdForUpdate(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember actorMember = getRequiredPartyMember(party, currentUser);
        if (!actorMember.isHost()) {
            throw BusinessException.onlyHostCanUpdateParty();
        }

        validatePartyEditableForUpdate(party, request, now);
        PartyUpdateSnapshot previous = PartyUpdateSnapshot.from(party);
        List<Long> joinedRecipientIds = resolveJoinedRecipientIds(party, currentUser.getId());
        party.updateEditableInfo(
                request.getTitle(),
                request.getProductName(),
                request.getTotalPrice(),
                request.getTotalQuantity(),
                request.getDeadline(),
                request.getUnitLabel(),
                request.getMinimumShareUnit(),
                request.getStorageType(),
                request.getPackagingType(),
                Boolean.TRUE.equals(request.getHostProvidesPackaging()),
                Boolean.TRUE.equals(request.getOnSiteSplit()),
                request.getGuideNote(),
                now
        );
        PartyUpdateChangeSummary changeSummary = PartyUpdateChangeSummary.from(previous, party);
        if (changeSummary.hasChanges()) {
            chatService.appendSystemMessage(party, "호스트가 파티 조건을 수정했습니다. " + changeSummary.message());
        }
        if (previous.totalQuantity() < party.getTotalQuantity()) {
            promoteWaitingMembers(party);
        }

        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        if (changeSummary.hasChanges()) {
            publishPartyUpdatedSync(
                    party,
                    joinedRecipientIds,
                    resolveWaitingRecipientIds(party),
                    changeSummary.message()
            );
        }
        return buildPartyDetailResponse(party, currentUser);
    }

    public PartyDetailResponse detailsParty(Long partyId) {
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));
        return buildPartyDetailResponse(party, getCurrentUserOrNull());
    }

    public List<PartyResponse> getAllPartyResponses(User currentUser) {
        List<PartyResponse> responses = partyRepository.findAll().stream()
                .map(PartyResponse::from)
                .toList();
        return favoritePartyService.applyFavoriteFlags(currentUser, responses);
    }

    @Transactional
    public PartyDetailResponse confirmSettlement(Long partyId, ConfirmSettlementRequest request) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember hostMember = getRequiredPartyMember(party, currentUser);
        if (!hostMember.isHost()) {
            throw BusinessException.onlyHostCanManageSettlement();
        }

        List<PartyMember> joinedMembers = getJoinedMembers(party);
        validateSettlementEditable(joinedMembers);

        Map<Long, Integer> expectedAllocations = allocateAmounts(party.getExpectedTotalPrice(), joinedMembers);
        Map<Long, Integer> actualAllocations = allocateAmounts(request.getActualTotalPrice(), joinedMembers);

        party.updateActualPurchase(request.getActualTotalPrice(), request.getReceiptNote());
        joinedMembers.forEach(member -> member.applySettlement(
                expectedAllocations.getOrDefault(member.getId(), 0),
                actualAllocations.getOrDefault(member.getId(), 0)
        ));

        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.SETTLEMENT_CONFIRMED);
        return buildPartyDetailResponse(party, currentUser);
    }

    @Transactional
    public PartyDetailResponse confirmPickupSchedule(Long partyId, ConfirmPickupScheduleRequest request) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember hostMember = getRequiredPartyMember(party, currentUser);
        if (!hostMember.isHost()) {
            throw BusinessException.onlyHostCanManagePickup();
        }

        party.updatePickupSchedule(request.getPickupPlace(), request.getPickupTime());
        List<Long> joinedRecipientIds = resolveJoinedRecipientIds(party, currentUser.getId());
        if (!joinedRecipientIds.isEmpty()) {
            notificationOutboxService.publishPickupUpdated(party, joinedRecipientIds);
        }
        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.PICKUP_UPDATED);
        return buildPartyDetailResponse(party, currentUser);
    }

    @Transactional
    public PartyDetailResponse acknowledgePickup(Long partyId) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember partyMember = getRequiredPartyMember(party, currentUser);
        if (partyMember.isHost()) {
            throw BusinessException.onlyParticipantCanAcknowledgePickup();
        }
        if (!party.hasPickupSchedule()) {
            throw BusinessException.pickupNotScheduled();
        }

        partyMember.acknowledgePickup(LocalDateTime.now(clock));
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.PICKUP_UPDATED);
        return buildPartyDetailResponse(party, currentUser);
    }

    @Transactional
    public PartyDetailResponse updatePaymentStatus(Long partyId, Long memberId, UpdatePaymentStatusRequest request) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));
        PartyMember actorMember = getRequiredPartyMember(party, currentUser);
        PartyMember targetMember = partyMemberRepository.findByIdAndParty(memberId, party)
                .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다."));

        if (targetMember.isHost()) {
            throw BusinessException.hostMemberCannotBeManaged();
        }
        if (targetMember.getActualAmount() == null) {
            throw BusinessException.settlementNotConfirmed();
        }

        try {
            switch (request.getPaymentStatus()) {
                case PAID -> {
                    if (!targetMember.getUser().getId().equals(currentUser.getId())) {
                        throw BusinessException.onlyParticipantCanMarkPaid();
                    }
                    targetMember.markPaid();
                }
                case CONFIRMED -> {
                    if (!actorMember.isHost()) {
                        throw BusinessException.onlyHostCanManageSettlement();
                    }
                    targetMember.confirmPayment();
                }
                case REFUNDED -> {
                    if (!actorMember.isHost()) {
                        throw BusinessException.onlyHostCanManageSettlement();
                    }
                    targetMember.refundPayment();
                }
                default -> throw BusinessException.invalidPaymentStatusTransition();
            }
        } catch (IllegalArgumentException ex) {
            throw BusinessException.invalidPaymentStatusTransition();
        }

        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.PAYMENT_UPDATED);
        return buildPartyDetailResponse(party, currentUser);
    }

    @Transactional
    public PartyDetailResponse updateTradeStatus(Long partyId, Long memberId, UpdateTradeStatusRequest request) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));
        PartyMember actorMember = getRequiredPartyMember(party, currentUser);
        PartyMember targetMember = partyMemberRepository.findByIdAndParty(memberId, party)
                .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다."));

        if (!actorMember.isHost()) {
            throw BusinessException.onlyHostCanManageTradeStatus();
        }
        if (targetMember.isHost()) {
            throw BusinessException.hostMemberCannotBeManaged();
        }
        if (!party.hasPickupSchedule()) {
            throw BusinessException.pickupNotScheduled();
        }

        try {
            switch (request.getTradeStatus()) {
                case COMPLETED -> {
                    if (targetMember.getPaymentStatus() != PaymentStatus.CONFIRMED) {
                        throw BusinessException.invalidTradeStatusTransition();
                    }
                    targetMember.completeTrade(LocalDateTime.now(clock));
                }
                case NO_SHOW -> targetMember.markNoShow(LocalDateTime.now(clock));
                default -> throw BusinessException.invalidTradeStatusTransition();
            }
        } catch (IllegalArgumentException ex) {
            throw BusinessException.invalidTradeStatusTransition();
        }

        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.TRADE_STATUS_UPDATED);
        return buildPartyDetailResponse(party, currentUser);
    }

    @Transactional
    public PartyDetailResponse submitReview(Long partyId, CreateReviewRequest request) {
        User currentUser = getCurrentUser();
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember actorMember = getRequiredPartyMember(party, currentUser);
        PartyMember hostMember = getRequiredHostMember(party);
        User reviewee = resolveReviewee(party, actorMember, hostMember, request.getTargetUserId());

        if (reviewRepository.existsByPartyAndReviewerAndReviewee(party, currentUser, reviewee)) {
            throw BusinessException.reviewDuplicate();
        }

        try {
            reviewRepository.saveAndFlush(Review.create(
                    party,
                    currentUser,
                    reviewee,
                    request.getRating(),
                    request.getComment(),
                    LocalDateTime.now(clock)
            ));
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.reviewDuplicate();
        }

        return buildPartyDetailResponse(party, currentUser);
    }

    private JoinPartyResponse joinImmediately(Party party,
                                              User member,
                                              Integer requestedQuantity,
                                              NoShowPolicyService.Decision noShowDecision) {
        PartyMember newMember = PartyMember.joinAsMember(
                party,
                member,
                requestedQuantity
        );

        party.acceptMember(newMember);

        try {
            partyMemberRepository.saveAndFlush(newMember);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.alreadyJoined();
        }

        notificationOutboxService.publishJoinConfirmed(party, member);
        chatService.appendSystemMessage(party, member.getUsername() + "님이 파티에 참여했습니다.");
        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.JOIN_CONFIRMED);

        return JoinPartyResponse.joined(party, createJoinWarning(noShowDecision));
    }

    private JoinPartyResponse joinWaitingQueue(Party party,
                                               User member,
                                               Integer requestedQuantity,
                                               NoShowPolicyService.Decision noShowDecision) {
        WaitingQueueEntry waitingQueueEntry = WaitingQueueEntry.create(party, member, requestedQuantity);

        try {
            waitingQueueRepository.saveAndFlush(waitingQueueEntry);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.alreadyWaiting();
        }

        int waitingPosition = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                        party,
                        WaitingQueueStatus.WAITING
                ).size();

        return JoinPartyResponse.waiting(party, waitingPosition, createJoinWarning(noShowDecision));
    }

    private void cancelJoinedMember(Party party, PartyMember joinedMember) {
        if (joinedMember.isHost()) {
            throw BusinessException.hostCannotCancel();
        }

        party.removeMember(joinedMember);
        partyMemberRepository.delete(joinedMember);
        partyMemberRepository.flush();
        chatService.appendSystemMessage(party, joinedMember.getUser().getUsername() + "님이 파티에서 나갔습니다.");
        boolean promoted = promoteWaitingMembers(party);
        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        if (!promoted) {
            partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.MEMBER_CANCELLED);
        }
    }

    private boolean promoteWaitingMembers(Party party) {
        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                party,
                WaitingQueueStatus.WAITING
        );

        boolean promoted = false;
        for (WaitingQueueEntry waitingEntry : waitingEntries) {
            if (party.getRemainingQuantity() < waitingEntry.getRequestedQuantity()) {
                break;
            }

            PartyMember promotedMember = PartyMember.joinAsMember(
                    party,
                    waitingEntry.getUser(),
                    waitingEntry.getRequestedQuantity()
            );

            party.acceptMember(promotedMember);
            partyMemberRepository.saveAndFlush(promotedMember);
            waitingEntry.promote();
            notificationOutboxService.publishWaitingPromoted(party, waitingEntry.getUser(), waitingEntry.getRequestedQuantity());
            chatService.appendSystemMessage(
                    party,
                    waitingEntry.getUser().getUsername() + "님이 대기열에서 채팅방 참여 상태로 승격되었습니다."
            );
            promoted = true;

            if (!party.isRecruiting()) {
                break;
            }
        }

        if (promoted) {
            partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.WAITING_PROMOTED);
        }

        return promoted;
    }

    private void validateNotAlreadyJoinedOrWaiting(Party party, User member) {
        if (partyMemberRepository.existsByPartyAndUser(party, member)) {
            throw BusinessException.alreadyJoined();
        }

        if (waitingQueueRepository.existsByPartyAndUserAndStatus(party, member, WaitingQueueStatus.WAITING)) {
            throw BusinessException.alreadyWaiting();
        }
    }

    private JoinPartyResponse.Warning createJoinWarning(NoShowPolicyService.Decision noShowDecision) {
        if (!noShowDecision.shouldWarn()) {
            return null;
        }

        return JoinPartyResponse.Warning.of(
                "NO_SHOW_CAUTION",
                "최근 노쇼가 1회 기록되어 있습니다. 한 번 더 노쇼가 누적되면 새 거래를 1회 완료하기 전까지 참여가 제한됩니다.",
                noShowDecision.activeNoShowCount()
        );
    }

    private boolean canJoinImmediately(Party party, Integer requestedQuantity) {
        return party.isRecruiting() && party.getRemainingQuantity() >= requestedQuantity;
    }

    private void validatePartyOpenForJoin(Party party, LocalDateTime now) {
        if (party.getPartyStatus() == PartyStatus.CLOSED) {
            throw BusinessException.partyClosed();
        }

        if (party.isDeadlineExpired(now)) {
            throw BusinessException.deadlineExpired();
        }
    }

    private void validatePartyEditableForUpdate(Party party, UpdatePartyRequest request, LocalDateTime now) {
        if (party.getPartyStatus() == PartyStatus.CLOSED) {
            throw BusinessException.partyClosed();
        }

        if (party.isDeadlineExpired(now)) {
            throw BusinessException.deadlineExpired();
        }

        if ((party.hasSettlementConfirmed() || party.hasPickupSchedule()) && hasRestrictedFieldChanges(party, request)) {
            throw BusinessException.partyEditRestrictedAfterSettlementOrPickup();
        }
    }

    private void validatePartyClosableByHost(Party party, LocalDateTime now) {
        if (party.getPartyStatus() == PartyStatus.CLOSED) {
            throw BusinessException.partyClosed();
        }

        if (party.isDeadlineExpired(now)) {
            throw BusinessException.deadlineExpired();
        }

        boolean operationStarted = party.hasSettlementConfirmed()
                || party.hasPickupSchedule()
                || party.getMembers().stream()
                .filter(PartyMember::isMember)
                .anyMatch(member -> member.getActualAmount() != null
                        || member.getPaymentStatus() != PaymentStatus.PENDING
                        || member.getTradeStatus() != TradeStatus.PENDING
                        || member.isPickupAcknowledged());

        if (operationStarted) {
            throw BusinessException.partyHostCancelNotAllowedAfterOperationsStarted();
        }
    }

    private boolean hasRestrictedFieldChanges(Party party, UpdatePartyRequest request) {
        return !Objects.equals(party.getTitle(), request.getTitle())
                || !Objects.equals(party.getProductName(), request.getProductName())
                || !Objects.equals(party.getExpectedTotalPrice(), request.getTotalPrice())
                || !Objects.equals(party.getTotalQuantity(), request.getTotalQuantity())
                || !Objects.equals(party.getDeadline(), request.getDeadline())
                || !Objects.equals(party.getUnitLabel(), request.getUnitLabel())
                || !Objects.equals(party.getMinimumShareUnit(), request.getMinimumShareUnit())
                || !Objects.equals(party.getStorageType(), request.getStorageType())
                || !Objects.equals(party.getPackagingType(), request.getPackagingType())
                || party.isHostProvidesPackaging() != Boolean.TRUE.equals(request.getHostProvidesPackaging())
                || party.isOnSiteSplit() != Boolean.TRUE.equals(request.getOnSiteSplit());
    }

    private void publishPartyUpdatedSync(Party party,
                                         List<Long> joinedRecipientIds,
                                         List<Long> waitingRecipientIds,
                                         String changeSummary) {
        if (!joinedRecipientIds.isEmpty() || !waitingRecipientIds.isEmpty()) {
            notificationOutboxService.publishPartyUpdated(party, joinedRecipientIds, waitingRecipientIds, changeSummary);
        }
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.PARTY_UPDATED);
    }

    private List<Long> resolveJoinedRecipientIds(Party party, Long excludedUserId) {
        return getJoinedMembers(party).stream()
                .map(PartyMember::getUser)
                .map(User::getId)
                .filter(userId -> !Objects.equals(userId, excludedUserId))
                .distinct()
                .toList();
    }

    private List<Long> resolveWaitingRecipientIds(Party party) {
        return waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(party, WaitingQueueStatus.WAITING).stream()
                .map(WaitingQueueEntry::getUser)
                .map(User::getId)
                .toList();
    }

    private LocalDateTime resolveDeadline(LocalDateTime requestedDeadline) {
        if (requestedDeadline != null) {
            return requestedDeadline;
        }
        return LocalDateTime.now(clock).plusDays(1);
    }

    private PartyDetailResponse buildPartyDetailResponse(Party party, User currentUser) {
        PartyMember joinedMember = null;
        WaitingQueueEntry waitingEntry = null;

        if (currentUser != null) {
            joinedMember = partyMemberRepository.findByPartyAndUser(party, currentUser).orElse(null);
            if (joinedMember == null) {
                waitingEntry = waitingQueueRepository.findFirstByPartyAndUserAndStatus(
                        party,
                        currentUser,
                        WaitingQueueStatus.WAITING
                ).orElse(null);
            }
        }

        List<PartyMember> joinedMembers = getJoinedMembers(party);
        PartyMember hostMember = findHostMember(party);
        User hostUser = hostMember != null ? hostMember.getUser() : null;
        Map<Long, Integer> previewExpectedAmounts = joinedMembers.isEmpty()
                ? Map.of()
                : allocateAmounts(party.getExpectedTotalPrice(), joinedMembers);
        Set<Long> reviewedUserIds = currentUser != null && joinedMember != null && joinedMember.isHost()
                ? reviewRepository.findAllByPartyAndReviewer(party, currentUser).stream()
                .map(review -> review.getReviewee().getId())
                .collect(Collectors.toSet())
                : Set.of();

        List<PartySettlementMemberResponse> settlementMembers = joinedMember != null && joinedMember.isHost()
                ? joinedMembers.stream()
                .sorted(Comparator
                        .comparing(PartyMember::isHost).reversed()
                        .thenComparing(PartyMember::getRequestedQuantity, Comparator.reverseOrder())
                        .thenComparing(PartyMember::getId))
                .map(member -> PartySettlementMemberResponse.from(
                        member,
                        previewExpectedAmounts.get(member.getId()),
                        member.getActualAmount(),
                        member.isMember() && reviewedUserIds.contains(member.getUser().getId())
                ))
                .toList()
                : List.of();

        Integer waitingPosition = waitingEntry != null ? resolveWaitingPosition(waitingEntry) : null;
        boolean canReviewHost = joinedMember != null
                && joinedMember.isMember()
                && joinedMember.isReviewEligible()
                && hostUser != null;
        boolean hasReviewedHost = canReviewHost
                && reviewRepository.existsByPartyAndReviewerAndReviewee(party, currentUser, hostUser);
        TrustSummaryResponse hostTrust = hostUser != null ? trustScoreService.getTrustSummary(hostUser) : null;
        List<ReviewResponse> hostReviews = hostUser != null ? trustScoreService.getRecentReviews(hostUser, 3) : List.of();
        boolean favorite = currentUser != null && favoritePartyService.isFavorite(currentUser, party);

        return PartyDetailResponse.from(
                party,
                joinedMember != null ? joinedMember.getId() : null,
                joinedMember != null ? joinedMember.getRole() : null,
                joinedMember != null ? ParticipationStatus.JOINED : waitingEntry != null ? ParticipationStatus.WAITING : null,
                waitingPosition,
                joinedMember != null ? joinedMember.getRequestedQuantity() : waitingEntry != null ? waitingEntry.getRequestedQuantity() : null,
                joinedMember != null ? resolveExpectedAmount(joinedMember, previewExpectedAmounts) : null,
                joinedMember != null ? joinedMember.getActualAmount() : null,
                joinedMember != null ? joinedMember.getPaymentStatus() : null,
                joinedMember != null ? joinedMember.getTradeStatus() : null,
                joinedMember != null && joinedMember.isPickupAcknowledged(),
                joinedMember != null && joinedMember.isReviewEligible(),
                canReviewHost,
                hasReviewedHost,
                hostTrust,
                hostReviews,
                settlementMembers,
                favorite
        );
    }

    private PartyMember findHostMember(Party party) {
        return party.getMembers().stream()
                .filter(PartyMember::isHost)
                .findFirst()
                .orElse(null);
    }

    private PartyMember getRequiredHostMember(Party party) {
        return party.getMembers().stream()
                .filter(PartyMember::isHost)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("호스트 정보를 찾을 수 없습니다."));
    }

    private User resolveReviewee(Party party,
                                 PartyMember actorMember,
                                 PartyMember hostMember,
                                 Long targetUserId) {
        if (targetUserId == null || actorMember.getUser().getId().equals(targetUserId)) {
            throw BusinessException.invalidReviewTarget();
        }

        if (actorMember.isHost()) {
            PartyMember targetMember = party.getMembers().stream()
                    .filter(PartyMember::isMember)
                    .filter(member -> member.getUser().getId().equals(targetUserId))
                    .findFirst()
                    .orElseThrow(BusinessException::invalidReviewTarget);

            if (!targetMember.isReviewEligible()) {
                throw BusinessException.reviewNotEligible();
            }
            return targetMember.getUser();
        }

        if (!actorMember.isReviewEligible()) {
            throw BusinessException.reviewNotEligible();
        }
        if (!hostMember.getUser().getId().equals(targetUserId)) {
            throw BusinessException.invalidReviewTarget();
        }
        return hostMember.getUser();
    }

    private PartyMember getRequiredPartyMember(Party party, User currentUser) {
        return partyMemberRepository.findByPartyAndUser(party, currentUser)
                .orElseThrow(() -> new EntityNotFoundException("파티 참여 정보를 찾을 수 없습니다."));
    }

    private List<PartyMember> getJoinedMembers(Party party) {
        return party.getMembers().stream()
                .filter(member -> member.getRequestedQuantity() > 0)
                .toList();
    }

    private Integer resolveExpectedAmount(PartyMember partyMember, Map<Long, Integer> previewExpectedAmounts) {
        if (partyMember.getExpectedAmount() != null) {
            return partyMember.getExpectedAmount();
        }
        return previewExpectedAmounts.get(partyMember.getId());
    }

    private int resolveWaitingPosition(WaitingQueueEntry waitingEntry) {
        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                waitingEntry.getParty(),
                WaitingQueueStatus.WAITING
        );
        int waitingPosition = 1;
        for (WaitingQueueEntry currentEntry : waitingEntries) {
            if (currentEntry.getId().equals(waitingEntry.getId())) {
                return waitingPosition;
            }
            waitingPosition++;
        }
        return waitingPosition;
    }

    private Map<Long, Integer> allocateAmounts(Integer totalAmount, List<PartyMember> members) {
        if (totalAmount == null || members.isEmpty()) {
            return Map.of();
        }

        int totalRequestedQuantity = members.stream()
                .mapToInt(PartyMember::getRequestedQuantity)
                .sum();

        if (totalRequestedQuantity <= 0) {
            throw new IllegalArgumentException("정산 가능한 참여 수량이 없습니다.");
        }

        int baseUnitAmount = totalAmount / totalRequestedQuantity;
        int remainder = totalAmount % totalRequestedQuantity;

        List<PartyMember> orderedMembers = members.stream()
                .sorted(Comparator
                        .comparing(PartyMember::getRequestedQuantity, Comparator.reverseOrder())
                        .thenComparing(PartyMember::getId))
                .toList();

        Map<Long, Integer> allocations = new HashMap<>();
        for (PartyMember member : orderedMembers) {
            int bonus = Math.min(remainder, member.getRequestedQuantity());
            allocations.put(member.getId(), baseUnitAmount * member.getRequestedQuantity() + bonus);
            remainder -= bonus;
        }

        return allocations;
    }

    private void validateSettlementEditable(List<PartyMember> joinedMembers) {
        if (joinedMembers.isEmpty()) {
            throw new IllegalArgumentException("정산할 참여자가 없습니다.");
        }

        boolean alreadyProgressed = joinedMembers.stream()
                .filter(PartyMember::isMember)
                .anyMatch(member -> member.getPaymentStatus() != PaymentStatus.PENDING
                        || member.getTradeStatus() != TradeStatus.PENDING);

        if (alreadyProgressed) {
            throw BusinessException.invalidPaymentStatusTransition();
        }
    }

    private User getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            return null;
        }

        return customUserDetails.getUser();
    }

    private User getCurrentUser() {
        User currentUser = getCurrentUserOrNull();
        if (currentUser == null) {
            throw new EntityNotFoundException("현재 로그인한 사용자를 찾을 수 없습니다.");
        }
        return currentUser;
    }

    private record PartyUpdateSnapshot(
            String title,
            String productName,
            Integer totalPrice,
            Integer totalQuantity,
            LocalDateTime deadline,
            String unitLabel,
            Integer minimumShareUnit,
            com.project.partition_mate.domain.StorageType storageType,
            com.project.partition_mate.domain.PackagingType packagingType,
            boolean hostProvidesPackaging,
            boolean onSiteSplit,
            String guideNote
    ) {
        private static PartyUpdateSnapshot from(Party party) {
            return new PartyUpdateSnapshot(
                    party.getTitle(),
                    party.getProductName(),
                    party.getExpectedTotalPrice(),
                    party.getTotalQuantity(),
                    party.getDeadline(),
                    party.getUnitLabel(),
                    party.getMinimumShareUnit(),
                    party.getStorageType(),
                    party.getPackagingType(),
                    party.isHostProvidesPackaging(),
                    party.isOnSiteSplit(),
                    party.getGuideNote()
            );
        }
    }

    private record PartyUpdateChangeSummary(List<String> fields) {
        private static PartyUpdateChangeSummary from(PartyUpdateSnapshot previous, Party current) {
            List<String> changedFields = new ArrayList<>();
            if (!Objects.equals(previous.title(), current.getTitle())) {
                changedFields.add("제목");
            }
            if (!Objects.equals(previous.productName(), current.getProductName())) {
                changedFields.add("제품명");
            }
            if (!Objects.equals(previous.totalPrice(), current.getExpectedTotalPrice())) {
                changedFields.add("총 금액");
            }
            if (!Objects.equals(previous.totalQuantity(), current.getTotalQuantity())) {
                changedFields.add("총 수량");
            }
            if (!Objects.equals(previous.deadline(), current.getDeadline())) {
                changedFields.add("마감 시간");
            }
            if (!Objects.equals(previous.unitLabel(), current.getUnitLabel())) {
                changedFields.add("소분 단위");
            }
            if (!Objects.equals(previous.minimumShareUnit(), current.getMinimumShareUnit())) {
                changedFields.add("최소 소분 수량");
            }
            if (!Objects.equals(previous.storageType(), current.getStorageType())) {
                changedFields.add("보관 방식");
            }
            if (!Objects.equals(previous.packagingType(), current.getPackagingType())) {
                changedFields.add("포장 방식");
            }
            if (previous.hostProvidesPackaging() != current.isHostProvidesPackaging()) {
                changedFields.add("포장 제공 여부");
            }
            if (previous.onSiteSplit() != current.isOnSiteSplit()) {
                changedFields.add("현장 소분 여부");
            }
            if (!Objects.equals(previous.guideNote(), current.getGuideNote())) {
                changedFields.add("안내 문구");
            }
            return new PartyUpdateChangeSummary(List.copyOf(changedFields));
        }

        private boolean hasChanges() {
            return !fields.isEmpty();
        }

        private String message() {
            return "변경 항목: " + String.join(", ", fields);
        }
    }

}
