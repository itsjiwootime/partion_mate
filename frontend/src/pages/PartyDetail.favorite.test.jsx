import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import PartyDetail from './PartyDetail';

const { addToastMock, api, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getPartyDetail: vi.fn(),
    saveFavoriteParty: vi.fn(),
    removeFavoriteParty: vi.fn(),
  },
  subscribeToPartyStreamMock: vi.fn(() => vi.fn()),
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: false,
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));
vi.mock('../utils/partyRealtime', () => ({
  subscribeToPartyStream: subscribeToPartyStreamMock,
}));

describe('PartyDetail favorite', () => {
  it('로그인하지_않은_사용자가_관심_저장을_누르면_로그인으로_이동한다', async () => {
    // given
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.saveFavoriteParty.mockReset();
    api.removeFavoriteParty.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getPartyDetail.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 2,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      rating: 4.4,
      status: 'RECRUITING',
      favorite: false,
      hostReviews: [],
      settlementMembers: [],
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/5']}>
        <Routes>
          <Route path="/parties/:id" element={<PartyDetail />} />
          <Route path="/login" element={<div>로그인 화면</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    const favoriteButton = await screen.findByRole('button', { name: '관심 파티 저장' });
    await user.click(favoriteButton);

    // then
    await waitFor(() => {
      expect(screen.getByText('로그인 화면')).toBeInTheDocument();
    });
    expect(api.saveFavoriteParty).not.toHaveBeenCalled();
    expect(addToastMock).not.toHaveBeenCalled();
  });
});
