package com.project.partition_mate.security;

import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.service.UserBlockPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatStompChannelInterceptor implements ChannelInterceptor {

    public static final String CHAT_ACCESS_TOKEN = "chatAccessToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final PartyMemberRepository partyMemberRepository;
    private final UserBlockPolicyService userBlockPolicyService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("채팅 연결에는 인증 토큰이 필요합니다.");
            }

            String token = authorization.substring(7);
            if (!jwtTokenProvider.validateToken(token)) {
                throw new AccessDeniedException("유효하지 않은 채팅 인증 토큰입니다.");
            }

            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            accessor.setUser(authentication);
            storeAccessToken(accessor, token);
            return message;
        }

        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            Authentication authentication = resolveAuthentication(accessor);
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
                throw new AccessDeniedException("인증된 사용자만 채팅에 접근할 수 있습니다.");
            }

            Long partyId = extractPartyId(accessor.getDestination());
            if (partyId == null) {
                return message;
            }

            if (!partyMemberRepository.existsByPartyIdAndUserId(partyId, principal.getUser().getId())) {
                throw new AccessDeniedException("파티 참여자만 채팅에 접근할 수 있습니다.");
            }
            if (userBlockPolicyService.hasBlockedParticipantInParty(partyId, principal.getUser().getId())) {
                throw new AccessDeniedException("차단 관계가 있는 사용자가 포함된 채팅방에는 접근할 수 없습니다.");
            }
        }

        return message;
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }

        String token = getStoredAccessToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return null;
        }

        Authentication restoredAuthentication = jwtTokenProvider.getAuthentication(token);
        accessor.setUser(restoredAuthentication);
        return restoredAuthentication;
    }

    private void storeAccessToken(StompHeaderAccessor accessor, String token) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }
        sessionAttributes.put(CHAT_ACCESS_TOKEN, token);
    }

    private String getStoredAccessToken(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }
        Object token = sessionAttributes.get(CHAT_ACCESS_TOKEN);
        return token instanceof String value ? value : null;
    }

    private Long extractPartyId(String destination) {
        if (destination == null) {
            return null;
        }
        String normalized = destination;
        if (normalized.startsWith("/topic/chat/")) {
            return parsePartyId(normalized.substring("/topic/chat/".length()));
        }
        if (normalized.startsWith("/app/chat/") && normalized.endsWith("/messages")) {
            String partyIdToken = normalized.substring("/app/chat/".length(), normalized.length() - "/messages".length());
            return parsePartyId(partyIdToken);
        }
        return null;
    }

    private Long parsePartyId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
