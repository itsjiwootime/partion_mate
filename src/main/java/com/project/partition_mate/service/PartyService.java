package com.project.partition_mate.service;

import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PaymentStatus;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.dto.ConfirmSettlementRequest;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.PartySettlementMemberResponse;
import com.project.partition_mate.dto.PartyRealtimeTrigger;
import com.project.partition_mate.dto.UpdatePaymentStatusRequest;
import com.project.partition_mate.dto.UpdateTradeStatusRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final StoreRepository storeRepository;
    private final StoreQueryCacheSupport storeQueryCacheSupport;
    private final NotificationOutboxService notificationOutboxService;
    private final PartyRealtimeService partyRealtimeService;
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
                request.getOpenChatUrl(),
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

        validatePartyOpenForJoin(party, now);
        validateNotAlreadyJoinedOrWaiting(party, member);

        if (canJoinImmediately(party, request.getMemberRequestQuantity())) {
            return joinImmediately(party, member, request.getMemberRequestQuantity());
        }

        return joinWaitingQueue(party, member, request.getMemberRequestQuantity());
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

    public PartyDetailResponse detailsParty(Long partyId) {
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));
        return buildPartyDetailResponse(party, getCurrentUserOrNull());
    }

    public List<Party> getAllParties() {
        return partyRepository.findAll();
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
                    targetMember.completeTrade();
                }
                case NO_SHOW -> targetMember.markNoShow();
                default -> throw BusinessException.invalidTradeStatusTransition();
            }
        } catch (IllegalArgumentException ex) {
            throw BusinessException.invalidTradeStatusTransition();
        }

        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.TRADE_STATUS_UPDATED);
        return buildPartyDetailResponse(party, currentUser);
    }

    private JoinPartyResponse joinImmediately(Party party, User member, Integer requestedQuantity) {
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
        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.JOIN_CONFIRMED);

        return JoinPartyResponse.joined(party);
    }

    private JoinPartyResponse joinWaitingQueue(Party party, User member, Integer requestedQuantity) {
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

        return JoinPartyResponse.waiting(party, waitingPosition);
    }

    private void cancelJoinedMember(Party party, PartyMember joinedMember) {
        if (joinedMember.isHost()) {
            throw BusinessException.hostCannotCancel();
        }

        party.removeMember(joinedMember);
        partyMemberRepository.delete(joinedMember);
        partyMemberRepository.flush();
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
        Map<Long, Integer> previewExpectedAmounts = joinedMembers.isEmpty()
                ? Map.of()
                : allocateAmounts(party.getExpectedTotalPrice(), joinedMembers);

        List<PartySettlementMemberResponse> settlementMembers = joinedMember != null && joinedMember.isHost()
                ? joinedMembers.stream()
                .sorted(Comparator
                        .comparing(PartyMember::isHost).reversed()
                        .thenComparing(PartyMember::getRequestedQuantity, Comparator.reverseOrder())
                        .thenComparing(PartyMember::getId))
                .map(member -> PartySettlementMemberResponse.from(
                        member,
                        previewExpectedAmounts.get(member.getId()),
                        member.getActualAmount()
                ))
                .toList()
                : List.of();

        Integer waitingPosition = waitingEntry != null ? resolveWaitingPosition(waitingEntry) : null;

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
                settlementMembers
        );
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

}
