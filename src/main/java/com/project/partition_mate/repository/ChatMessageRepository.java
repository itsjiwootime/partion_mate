package com.project.partition_mate.repository;

import com.project.partition_mate.domain.ChatMessage;
import com.project.partition_mate.domain.ChatRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findTop100ByChatRoomOrderByIdDesc(ChatRoom chatRoom);

    @EntityGraph(attributePaths = {"sender"})
    Optional<ChatMessage> findTopByChatRoomOrderByIdDesc(ChatRoom chatRoom);

    long countByChatRoom(ChatRoom chatRoom);

    long countByChatRoomAndIdGreaterThan(ChatRoom chatRoom, Long id);
}
