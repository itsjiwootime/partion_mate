import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import PartyList from './PartyList';

const { addToastMock, api, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getAllParties: vi.fn(),
    getStoreParties: vi.fn(),
    getStoreDetail: vi.fn(),
    getMyChatRooms: vi.fn(),
    saveFavoriteParty: vi.fn(),
    removeFavoriteParty: vi.fn(),
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

describe('PartyList favorite', () => {
  it('목록에서_관심_파티를_저장한다', async () => {
    // given
    addToastMock.mockReset();
    api.getAllParties.mockReset();
    api.getStoreParties.mockReset();
    api.getStoreDetail.mockReset();
    api.getMyChatRooms.mockReset();
    api.saveFavoriteParty.mockReset();
    api.removeFavoriteParty.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getAllParties.mockResolvedValue([
      {
        partyId: 7,
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
        favorite: false,
      },
    ]);
    api.getMyChatRooms.mockResolvedValue([]);
    api.saveFavoriteParty.mockResolvedValue(null);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties']}>
        <Routes>
          <Route path="/parties" element={<PartyList />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    const title = await screen.findByRole('heading', { level: 3, name: '양재점 연어 소분' });
    const card = title.closest('article');
    await user.click(within(card).getByRole('button', { name: '관심 파티 저장' }));

    // then
    await waitFor(() => {
      expect(api.saveFavoriteParty).toHaveBeenCalledWith(7);
    });
    expect(within(card).getByRole('button', { name: '관심 파티 해제' })).toBeInTheDocument();
    expect(addToastMock).toHaveBeenCalledWith('관심 파티에 저장했습니다.', 'success');
  });
});
