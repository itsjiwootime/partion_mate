package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final StoreRepository storeRepository;

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
                request.getOpenChatUrl()
        );

        PartyMember hostMember = PartyMember.joinAsHost(
                party,
                host,
                request.getHostRequestedQuantity()
        );

        party.acceptMember(hostMember);

        return partyRepository.save(party);
    }

    @Transactional
    public JoinPartyResponse joinParty(Long partyId, JoinPartyRequest request) {
        User member = getCurrentUser();

        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

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

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        return PartyDetailResponse.from(party);


    }

    public List<Party> getAllParties() {
        return partyRepository.findAll();
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
        promoteWaitingMembers(party);
    }

    private void promoteWaitingMembers(Party party) {
        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                party,
                WaitingQueueStatus.WAITING
        );

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

            if (!party.isRecruiting()) {
                break;
            }
        }
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

    private User getCurrentUser() {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return customUserDetails.getUser();
    }

}
