import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import Home from './Home';
import PartyList from './PartyList';
import CreateParty from './CreateParty';

const { addToastMock, api, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getAllParties: vi.fn(),
    getMe: vi.fn(),
    getMyChatRooms: vi.fn(),
    getNearbyStores: vi.fn(),
    getStoreDetail: vi.fn(),
    getStoreParties: vi.fn(),
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

describe('Discovery and create flow', () => {
  it('홈_지점_탐색에서_생성_화면까지_선택한_지점을_유지한다', async () => {
    // given
    localStorage.clear();
    addToastMock.mockReset();
    api.getAllParties.mockReset();
    api.getMe.mockReset();
    api.getMyChatRooms.mockReset();
    api.getNearbyStores.mockReset();
    api.getStoreDetail.mockReset();
    api.getStoreParties.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getMe.mockResolvedValue({});
    api.getAllParties.mockResolvedValue([]);
    api.getNearbyStores.mockResolvedValue([
      {
        id: 1,
        name: '코스트코 양재점',
        distance: 1.2,
        partyCount: 4,
      },
      {
        id: 3,
        name: '트레이더스 월계점',
        distance: 2.8,
        partyCount: 2,
      },
    ]);
    api.getStoreDetail.mockResolvedValue({
      id: 3,
      name: '트레이더스 월계점',
      address: '서울 노원구 동일로 123',
      openTime: '10:00',
      closeTime: '22:00',
      phone: '02-1234-5678',
    });
    api.getStoreParties.mockResolvedValue([
      {
        partyId: 31,
        title: '월계점 라면 소분',
        productName: '라면',
        storeName: '트레이더스 월계점',
        totalPrice: 18000,
        totalQuantity: 6,
        currentQuantity: 2,
        deadline: '2099-03-24T12:00:00',
        deadlineLabel: '2099.03.24 12:00',
        rating: 4.3,
        status: 'RECRUITING',
        storageType: 'ROOM_TEMPERATURE',
        storageTypeLabel: '상온',
        minimumShareUnit: 1,
      },
    ]);
    api.getMyChatRooms.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/branch/:id" element={<PartyList />} />
          <Route path="/parties/create" element={<CreateParty />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.click(await screen.findByRole('button', { name: /트레이더스 월계점/i }));
    await screen.findByRole('heading', { level: 2, name: '트레이더스 월계점' });
    await user.click(screen.getByRole('button', { name: '파티 만들기' }));

    // then
    await screen.findByText('파티 기본 정보');
    expect(screen.getByRole('combobox', { name: '내 주변 지점' })).toHaveValue('3');
    expect(screen.getByRole('option', { name: /코스트코 양재점/i }).selected).toBe(false);
    expect(screen.getByRole('option', { name: /트레이더스 월계점/i }).selected).toBe(true);
  });
});
