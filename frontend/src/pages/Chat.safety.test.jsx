import { render, screen, waitFor } from '@testing-library/react';
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
    createReport: vi.fn(),
    blockUser: vi.fn(),
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
    userName: '테스터',
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));

describe('Chat safety', () => {
  it('다른_사용자_메시지에서_차단을_진행한다', async () => {
    // given
    addToastMock.mockReset();
    api.getMyChatRooms.mockReset();
    api.getChatRoomDetail.mockReset();
    api.markChatRoomRead.mockReset();
    api.blockUser.mockReset();
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
      host: false,
      pinnedNotice: null,
      unreadCount: 0,
      messages: [
        {
          messageId: 11,
          type: 'TEXT',
          senderId: 77,
          senderName: '민수',
          content: '외부 계좌로 먼저 보내주세요.',
          mine: false,
          createdAtLabel: '방금 전',
        },
      ],
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/chat/5']}>
        <Routes>
          <Route path="/chat/:partyId" element={<Chat />} />
          <Route path="/chat" element={<div>채팅 목록</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('외부 계좌로 먼저 보내주세요.');
    await user.click(screen.getByRole('button', { name: '민수님 메시지 차단' }));
    await user.click(screen.getByRole('button', { name: '차단하기' }));

    // then
    await waitFor(() => {
      expect(api.blockUser).toHaveBeenCalledWith({
        targetUserId: 77,
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('민수님을 차단했습니다. 이후 같은 파티와 채팅 참여가 제한됩니다.', 'success');
  });
});
