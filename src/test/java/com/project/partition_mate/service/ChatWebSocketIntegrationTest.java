package com.project.partition_mate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.ChatRoom;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ChatMessageResponse;
import com.project.partition_mate.repository.ChatMessageRepository;
import com.project.partition_mate.repository.ChatReadStateRepository;
import com.project.partition_mate.repository.ChatRoomRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatReadStateRepository chatReadStateRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        stompClient.setMessageConverter(messageConverter);
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());
    }

    @AfterEach
    void tearDown() {
        chatReadStateRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 참여자는_stomp로_텍스트_메시지를_송수신한다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(createStore("웹소켓 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        chatService.initializePartyChatRoom(party, host);
        String token = jwtTokenProvider.createToken(member.getId());
        BlockingQueue<ChatMessageResponse> queue = new LinkedBlockingQueue<>();
        CountDownLatch connectedLatch = new CountDownLatch(1);
        AtomicReference<Throwable> transportError = new AtomicReference<>();
        AtomicReference<Throwable> handlerError = new AtomicReference<>();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws-chat",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        connectedLatch.countDown();
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        transportError.compareAndSet(null, exception);
                    }

                    @Override
                    public void handleException(StompSession session,
                                                org.springframework.messaging.simp.stomp.StompCommand command,
                                                StompHeaders headers,
                                                byte[] payload,
                                                Throwable exception) {
                        handlerError.compareAndSet(null, exception);
                    }
                }
        ).get(5, TimeUnit.SECONDS);
        session.setAutoReceipt(true);
        assertThat(connectedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        session.subscribe("/topic/chat/" + party.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((ChatMessageResponse) payload);
            }
        });
        TimeUnit.MILLISECONDS.sleep(200);
        assertThat(session.isConnected()).isTrue();
        assertThat(transportError.get()).isNull();
        assertThat(handlerError.get()).isNull();

        // when
        session.send("/app/chat/" + party.getId() + "/messages", Map.of("content", "안녕하세요. 채팅 연결 테스트입니다."));
        ChatMessageResponse response = queue.poll(5, TimeUnit.SECONDS);

        // then
        assertThat(response).isNotNull();
        assertThat(handlerError.get()).isNull();
        assertThat(response.getType().name()).isEqualTo("TEXT");
        assertThat(response.getSenderName()).isEqualTo("member");
        assertThat(response.getContent()).isEqualTo("안녕하세요. 채팅 연결 테스트입니다.");

        ChatRoom chatRoom = chatRoomRepository.findByPartyId(party.getId()).orElseThrow();
        assertThat(chatMessageRepository.findTopByChatRoomOrderByIdDesc(chatRoom))
                .isPresent()
                .get()
                .extracting(message -> message.getContent(), message -> message.getSenderName())
                .containsExactly("안녕하세요. 채팅 연결 테스트입니다.", "member");

        session.disconnect();
    }

    private Party createParty(Store store, User host, User member) {
        Party party = new Party(
                "웹소켓 채팅 테스트 파티",
                "대용량 견과류",
                20000,
                store,
                2,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(5),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                "웹소켓 검증용",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        return partyRepository.saveAndFlush(party);
    }

    private Store createStore(String name) {
        return new Store(
                name,
                "서울시 테스트로",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        );
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }
}
