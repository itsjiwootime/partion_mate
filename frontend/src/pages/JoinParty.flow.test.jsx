import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import JoinParty from './JoinParty';

const { addToastMock, api } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getPartyDetail: vi.fn(),
    joinParty: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));

describe('JoinParty flow', () => {
  it('즉시_참여에_성공하면_파티_상세로_이동한다', async () => {
    // given
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.joinParty.mockReset();

    api.getPartyDetail.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      totalPrice: 10000,
      totalQuantity: 6,
      currentQuantity: 2,
      status: 'RECRUITING',
      minimumShareUnit: 2,
      unitLabel: '개',
    });
    api.joinParty.mockResolvedValue({
      joinStatus: 'JOINED',
      message: '참여가 완료되었습니다.',
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/5/join']}>
        <Routes>
          <Route path="/parties/:id/join" element={<JoinParty />} />
          <Route path="/parties/:id" element={<div>파티 상세 화면</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('참여하기');
    expect(screen.getByLabelText('요청 수량')).toHaveValue(2);
    await user.click(screen.getByRole('button', { name: '참여 확정' }));

    // then
    await waitFor(() => {
      expect(api.joinParty).toHaveBeenCalledWith({ partyId: 5, quantity: 2 });
    });
    expect(addToastMock).toHaveBeenCalledWith('참여가 완료되었습니다.', 'success');
    await waitFor(() => {
      expect(screen.getByText('파티 상세 화면')).toBeInTheDocument();
    });
  });

  it('잔여_수량이_없으면_대기열_등록_후_내_파티로_이동한다', async () => {
    // given
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.joinParty.mockReset();

    api.getPartyDetail.mockResolvedValue({
      partyId: 9,
      title: '월계점 라면 소분',
      totalPrice: 18000,
      totalQuantity: 4,
      currentQuantity: 4,
      status: 'FULL',
      minimumShareUnit: 1,
      unitLabel: '개',
    });
    api.joinParty.mockResolvedValue({
      joinStatus: 'WAITING',
      message: '대기열에 등록되었습니다.',
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/9/join']}>
        <Routes>
          <Route path="/parties/:id/join" element={<JoinParty />} />
          <Route path="/my-parties" element={<div>내 파티 화면</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByRole('heading', { level: 1, name: '대기열 등록' });
    expect(screen.getByText('잔여 수량이 부족해 즉시 참여 대신 대기열로 등록됩니다.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '대기열 등록' }));

    // then
    await waitFor(() => {
      expect(api.joinParty).toHaveBeenCalledWith({ partyId: 9, quantity: 1 });
    });
    expect(addToastMock).toHaveBeenCalledWith('대기열에 등록되었습니다.', 'success');
    await waitFor(() => {
      expect(screen.getByText('내 파티 화면')).toBeInTheDocument();
    });
  });
});
