package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ChatReadReceiptResponse;
import com.project.partition_mate.dto.ChatRoomDetailResponse;
import com.project.partition_mate.dto.ChatRoomSummaryResponse;
import com.project.partition_mate.dto.UpdatePinnedNoticeRequest;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyChatRooms() {
        return ResponseEntity.ok(chatService.getMyChatRooms(getCurrentUser()));
    }

    @GetMapping("/rooms/{partyId}")
    public ResponseEntity<ChatRoomDetailResponse> getChatRoomDetail(@PathVariable Long partyId) {
        return ResponseEntity.ok(chatService.getChatRoomDetail(partyId, getCurrentUser()));
    }

    @PostMapping("/rooms/{partyId}/read")
    public ResponseEntity<ChatReadReceiptResponse> markChatRoomRead(@PathVariable Long partyId) {
        return ResponseEntity.ok(chatService.markRoomRead(partyId, getCurrentUser()));
    }

    @PutMapping("/rooms/{partyId}/notice")
    public ResponseEntity<ChatRoomDetailResponse> updatePinnedNotice(@PathVariable Long partyId,
                                                                     @RequestBody @Valid UpdatePinnedNoticeRequest request) {
        return ResponseEntity.ok(chatService.updatePinnedNotice(partyId, request, getCurrentUser()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return principal.getUser();
    }
}
