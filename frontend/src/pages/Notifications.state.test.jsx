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

const authState = vi.hoisted(() => ({
  isAuthed: true,
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => authState,
}));

describe('Notifications states', () => {
  it('로그인하지_않은_사용자가_알림_화면에_오면_로그인으로_이동한다', async () => {
    // given
    authState.isAuthed = false;
    api.getMyNotifications.mockReset();
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/notifications']}>
        <Routes>
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/login" element={<div>로그인 화면</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.click(screen.getByRole('button', { name: '로그인 하러 가기' }));

    // then
    await waitFor(() => {
      expect(screen.getByText('로그인 화면')).toBeInTheDocument();
    });
    expect(api.getMyNotifications).not.toHaveBeenCalled();
  });

  it('알림_조회_실패_후_다시_불러오기로_복구한다', async () => {
    // given
    authState.isAuthed = true;
    api.getMyNotifications.mockReset();
    api.getMyNotifications
      .mockRejectedValueOnce(new Error('fail'))
      .mockResolvedValueOnce([
        {
          id: 3,
          type: 'PICKUP_UPDATED',
          title: '픽업 일정 확정',
          message: '픽업 일정이 업데이트되었습니다.',
          linkUrl: '/parties/3',
          createdAtLabel: '2026.03.18 19:30',
        },
      ]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/notifications']}>
        <Routes>
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/parties/:id" element={<div>파티 상세 3</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('알림을 불러오지 못했습니다.');
    await user.click(screen.getByRole('button', { name: '다시 불러오기' }));

    // then
    await waitFor(() => {
      expect(api.getMyNotifications).toHaveBeenCalledTimes(2);
    });
    expect(await screen.findByText('픽업 일정 확정')).toBeInTheDocument();
  });
});
