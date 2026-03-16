package com.project.partition_mate.repository;

import com.project.partition_mate.domain.ChatRoom;
import com.project.partition_mate.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByParty(Party party);

    Optional<ChatRoom> findByPartyId(Long partyId);
}
