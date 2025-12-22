package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.MyJoinedPartyResponse;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PartyMemberRepository partyMemberRepository;

    public User getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를  찾을 수 없습니다."));

        return user;
    }

    public List<MyJoinedPartyResponse> getMyParties(User user) {
        return partyMemberRepository.findByUser(user).stream()
                .map(pm -> MyJoinedPartyResponse.of(
                        pm.getParty().getId(),
                        pm.getParty().getTitle(),
                        pm.getParty().getProductName(),
                        pm.getParty().getStore() != null ? pm.getParty().getStore().getName() : null,
                        pm.getParty().getPartyStatus(),
                        pm.getParty().getTotalQuantity(),
                        pm.getParty().getRequestedQuantity(),
                        pm.getRole(),
                        pm.getParty().getTotalPrice(),
                        pm.getParty().getOpenChatUrl()
                ))
                .toList();
    }
}
