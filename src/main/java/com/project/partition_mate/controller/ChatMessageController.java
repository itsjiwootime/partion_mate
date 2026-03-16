package com.project.partition_mate.controller;

import com.project.partition_mate.dto.ChatMessageRequest;
import com.project.partition_mate.security.ChatStompChannelInterceptor;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.security.JwtTokenProvider;
import com.project.partition_mate.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @MessageMapping("/chat/{partyId}/messages")
    public void sendMessage(@DestinationVariable Long partyId,
                            @Valid ChatMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = resolveAuthentication(headerAccessor.getSessionAttributes());
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        chatService.sendTextMessage(partyId, request, userDetails.getUser());
    }

    private Authentication resolveAuthentication(Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) {
            throw new AccessDeniedException("인증된 사용자만 채팅 메시지를 보낼 수 있습니다.");
        }

        Object accessToken = sessionAttributes.get(ChatStompChannelInterceptor.CHAT_ACCESS_TOKEN);
        if (!(accessToken instanceof String token) || !jwtTokenProvider.validateToken(token)) {
            throw new AccessDeniedException("유효한 채팅 인증 토큰이 필요합니다.");
        }

        return jwtTokenProvider.getAuthentication(token);
    }
}
