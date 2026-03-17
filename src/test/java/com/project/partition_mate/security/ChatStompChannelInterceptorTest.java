package com.project.partition_mate.security;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.service.UserBlockPolicyService;
import com.project.partition_mate.repository.PartyMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatStompChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private UserBlockPolicyService userBlockPolicyService;

    @InjectMocks
    private ChatStompChannelInterceptor chatStompChannelInterceptor;

    @Test
    void 차단관계가_있으면_STOMP_구독을_막는다() {
        // given
        User user = new User("member", "member@test.com", "pw", "서울", 37.5, 127.0);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/5");
        accessor.setUser(authentication);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        given(partyMemberRepository.existsByPartyIdAndUserId(5L, 1L)).willReturn(true);
        given(userBlockPolicyService.hasBlockedParticipantInParty(5L, 1L)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> chatStompChannelInterceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("차단 관계가 있는 사용자가 포함된 채팅방에는 접근할 수 없습니다.");
    }
}
