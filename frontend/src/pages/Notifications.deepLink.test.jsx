import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import Notifications from './Notifications';

const { api } = vi.hoisted(() => ({
  api: {
    getMyNotifications: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
  }),
}));

describe('Notifications deep link', () => {
  it('채팅_딥링크_알림을_누르면_채팅_화면으로_이동한다', async () => {
    // given
    api.getMyNotifications.mockReset();
    api.getMyNotifications.mockResolvedValue([
      {
        id: 1,
        type: 'PARTY_JOIN_CONFIRMED',
        title: '참여 확정',
        message: '채팅방으로 이동해 안내를 확인하세요.',
        linkUrl: '/chat/7',
        createdAtLabel: '2026.03.18 14:00',
      },
    ]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/notifications']}>
        <Routes>
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/chat/:partyId" element={<div>채팅 화면 7</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    const actionButton = await screen.findByRole('button', { name: /관련 채팅 보기/i });
    await user.click(actionButton);

    // then
    await waitFor(() => {
      expect(screen.getByText('채팅 화면 7')).toBeInTheDocument();
    });
  });
});
