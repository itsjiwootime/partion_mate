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

describe('JoinParty safety feedback', () => {
  it('차단_관계로_참여가_막히면_fallback을_보여준다', async () => {
    // given
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.joinParty.mockReset();

    api.getPartyDetail.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      totalPrice: 10000,
      totalQuantity: 5,
      currentQuantity: 2,
      status: 'RECRUITING',
      minimumShareUnit: 1,
      unitLabel: '개',
    });
    api.joinParty.mockRejectedValue(new Error('차단한 사용자 또는 나를 차단한 사용자가 포함된 파티에는 참여할 수 없습니다.'));
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/5/join']}>
        <Routes>
          <Route path="/parties/:id/join" element={<JoinParty />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('참여하기');
    await user.click(screen.getByRole('button', { name: '참여 확정' }));

    // then
    await waitFor(() => {
      expect(api.joinParty).toHaveBeenCalledWith({ partyId: 5, quantity: 1 });
    });
    expect(screen.getByText('차단 관계가 있어 이 파티에 참여할 수 없어요')).toBeInTheDocument();
    expect(screen.getByText('참여 대신 차단 상태를 먼저 확인해 주세요')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: '차단 관리 열기' }).length).toBeGreaterThan(0);
  });
});
