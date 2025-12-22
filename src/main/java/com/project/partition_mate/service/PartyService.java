package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
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
    public Party joinParty(Long partyId, JoinPartyRequest request) {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User member = customUserDetails.getUser();

        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        PartyMember newMember = PartyMember.joinAsMember(
                party,
                member,
                request.getMemberRequestQuantity()
        );

        party.acceptMember(newMember);


        return party;
    }

    public PartyDetailResponse detailsParty(Long partyId) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        return PartyDetailResponse.from(party);


    }

    public List<Party> getAllParties() {
        return partyRepository.findAll();
    }

}
