import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import FavoriteParties from './FavoriteParties';

const { addToastMock, api, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getFavoriteParties: vi.fn(),
    removeFavoriteParty: vi.fn(),
    getMyChatRooms: vi.fn(),
  },
  subscribeToPartyStreamMock: vi.fn(() => vi.fn()),
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
vi.mock('../utils/partyRealtime', () => ({
  subscribeToPartyStream: subscribeToPartyStreamMock,
}));

describe('FavoriteParties', () => {
  it('저장한_관심_파티를_해제하면_목록에서_사라진다', async () => {
    // given
    addToastMock.mockReset();
    api.getFavoriteParties.mockReset();
    api.removeFavoriteParty.mockReset();
    api.getMyChatRooms.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getFavoriteParties.mockResolvedValue([
      {
        partyId: 11,
        title: '양재점 연어 소분',
        productName: '연어',
        storeName: '코스트코 양재점',
        totalPrice: 29900,
        totalQuantity: 4,
        currentQuantity: 2,
        deadline: '2099-03-20T18:00:00',
        deadlineLabel: '2099.03.20 18:00',
        rating: 4.8,
        status: 'RECRUITING',
        favorite: true,
      },
    ]);
    api.getMyChatRooms.mockResolvedValue([]);
    api.removeFavoriteParty.mockResolvedValue(null);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/favorite-parties']}>
        <Routes>
          <Route path="/favorite-parties" element={<FavoriteParties />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    const title = await screen.findByRole('heading', { level: 3, name: '양재점 연어 소분' });
    const card = title.closest('article');
    await user.click(within(card).getByRole('button', { name: '관심 파티 해제' }));

    // then
    await waitFor(() => {
      expect(api.removeFavoriteParty).toHaveBeenCalledWith(11);
    });
    await waitFor(() => {
      expect(screen.getByText('저장한 관심 파티가 없어요')).toBeInTheDocument();
    });
    expect(addToastMock).toHaveBeenCalledWith('관심 파티에서 제거했습니다.', 'success');
  });
});
