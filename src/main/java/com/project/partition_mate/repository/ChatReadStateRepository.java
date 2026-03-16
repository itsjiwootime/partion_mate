package com.project.partition_mate.repository;

import com.project.partition_mate.domain.ChatReadState;
import com.project.partition_mate.domain.ChatRoom;
import com.project.partition_mate.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatReadStateRepository extends JpaRepository<ChatReadState, Long> {

    Optional<ChatReadState> findByChatRoomAndUser(ChatRoom chatRoom, User user);
}
