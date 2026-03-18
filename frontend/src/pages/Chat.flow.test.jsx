import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import Chat from './Chat';

const { addToastMock, api, connectChatRoomMock, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getMyChatRooms: vi.fn(),
    getChatRoomDetail: vi.fn(),
    markChatRoomRead: vi.fn(),
    updatePinnedNotice: vi.fn(),
  },
  connectChatRoomMock: vi.fn(() => ({
    connected: true,
    publish: vi.fn(),
    deactivate: vi.fn(),
  })),
  subscribeToPartyStreamMock: vi.fn(() => vi.fn()),
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../utils/chatClient', () => ({
  connectChatRoom: connectChatRoomMock,
}));
vi.mock('../utils/partyRealtime', () => ({
  subscribeToPartyStream: subscribeToPartyStreamMock,
}));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
    token: 'token',
    userName: '호스트',
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));

describe('Chat flow', () => {
  it('채팅방을_선택하면_상세를_불러오고_읽음_처리를_한다', async () => {
    // given
    addToastMock.mockReset();
    api.getMyChatRooms.mockReset();
    api.getChatRoomDetail.mockReset();
    api.markChatRoomRead.mockReset();
    api.updatePinnedNotice.mockReset();
    connectChatRoomMock.mockClear();
    subscribeToPartyStreamMock.mockClear();

    api.getMyChatRooms.mockResolvedValue([
      {
        partyId: 5,
        partyTitle: '광명점 우유 소분',
        storeName: '코스트코 광명점',
        unreadCount: 2,
        pinnedNotice: null,
        lastMessagePreview: '픽업 일정 확인 부탁드려요.',
        lastMessageCreatedAtLabel: '방금 전',
      },
    ]);
    api.getChatRoomDetail.mockResolvedValue({
      roomId: 7,
      partyId: 5,
      partyTitle: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      host: false,
      pinnedNotice: null,
      unreadCount: 2,
      messages: [
        {
          messageId: 11,
          type: 'TEXT',
          senderId: 44,
          senderName: '참여자',
          content: '픽업 일정 확인 부탁드려요.',
          mine: false,
          createdAtLabel: '방금 전',
        },
      ],
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/chat']}>
        <Routes>
          <Route path="/chat" element={<Chat />} />
          <Route path="/chat/:partyId" element={<Chat />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByRole('heading', { level: 2, name: '내 채팅방' });
    await user.click(screen.getByRole('button', { name: /광명점 우유 소분/i }));

    // then
    await screen.findByRole('heading', { level: 2, name: '광명점 우유 소분' });
    await waitFor(() => {
      expect(api.getChatRoomDetail).toHaveBeenCalledWith(5);
    });
    await waitFor(() => {
      expect(api.markChatRoomRead).toHaveBeenCalledWith(5);
    });
    const messageBubble = screen.getByText('참여자').closest('div');
    expect(within(messageBubble).getByText('픽업 일정 확인 부탁드려요.')).toBeInTheDocument();
  });

  it('호스트가_공지_저장에_성공한다', async () => {
    // given
    addToastMock.mockReset();
    api.getMyChatRooms.mockReset();
    api.getChatRoomDetail.mockReset();
    api.markChatRoomRead.mockReset();
    api.updatePinnedNotice.mockReset();
    connectChatRoomMock.mockClear();
    subscribeToPartyStreamMock.mockClear();

    api.getMyChatRooms.mockResolvedValue([
      {
        partyId: 5,
        partyTitle: '광명점 우유 소분',
        storeName: '코스트코 광명점',
        unreadCount: 0,
        pinnedNotice: null,
        lastMessagePreview: '안녕하세요',
        lastMessageCreatedAtLabel: '방금 전',
      },
    ]);
    api.getChatRoomDetail.mockResolvedValue({
      roomId: 7,
      partyId: 5,
      partyTitle: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      host: true,
      pinnedNotice: null,
      unreadCount: 0,
      messages: [],
    });
    api.updatePinnedNotice.mockResolvedValue({
      roomId: 7,
      partyId: 5,
      partyTitle: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      host: true,
      pinnedNotice: '픽업은 1층 정문 앞에서 진행합니다.',
      pinnedNoticeUpdatedAtLabel: '방금 전',
      unreadCount: 0,
      messages: [],
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/chat/5']}>
        <Routes>
          <Route path="/chat/:partyId" element={<Chat />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByRole('button', { name: '공지 저장' });
    await user.type(
      screen.getByPlaceholderText('픽업 시간, 준비물, 정산 안내를 고정해보세요. 비우면 공지가 해제됩니다.'),
      '픽업은 1층 정문 앞에서 진행합니다.',
    );
    await user.click(screen.getByRole('button', { name: '공지 저장' }));

    // then
    await waitFor(() => {
      expect(api.updatePinnedNotice).toHaveBeenCalledWith({
        partyId: 5,
        pinnedNotice: '픽업은 1층 정문 앞에서 진행합니다.',
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('호스트 공지를 저장했습니다.', 'success');
    expect(screen.getAllByText('픽업은 1층 정문 앞에서 진행합니다.').length).toBeGreaterThan(0);
  });
});
