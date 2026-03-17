package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBlockPolicyService {

    private final PartyMemberRepository partyMemberRepository;
    private final UserBlockRepository userBlockRepository;

    public boolean hasBlockedParticipantInParty(Party party, Long currentUserId) {
        List<PartyMember> partyMembers = partyMemberRepository.findByParty(party);
        return partyMembers.stream()
                .map(PartyMember::getUser)
                .map(user -> user.getId())
                .filter(userId -> !userId.equals(currentUserId))
                .anyMatch(otherUserId -> userBlockRepository.existsBlockingRelation(currentUserId, otherUserId));
    }

    public boolean hasBlockedParticipantInParty(Long partyId, Long currentUserId) {
        List<PartyMember> partyMembers = partyMemberRepository.findAllByPartyId(partyId);
        return partyMembers.stream()
                .map(PartyMember::getUser)
                .map(user -> user.getId())
                .filter(userId -> !userId.equals(currentUserId))
                .anyMatch(otherUserId -> userBlockRepository.existsBlockingRelation(currentUserId, otherUserId));
    }
}
