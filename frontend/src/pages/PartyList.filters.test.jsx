import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import PartyList from './PartyList';

const { api, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  api: {
    getAllParties: vi.fn(),
    getStoreParties: vi.fn(),
    getStoreDetail: vi.fn(),
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
vi.mock('../utils/partyRealtime', () => ({
  subscribeToPartyStream: subscribeToPartyStreamMock,
}));

const partyFixtures = [
  {
    partyId: 1,
    title: '양재점 연어 소분',
    productName: '연어',
    storeName: '코스트코 양재점',
    totalPrice: 29900,
    totalQuantity: 4,
    currentQuantity: 2,
    deadlineLabel: '2026.03.20 18:00',
    rating: 4.8,
    status: 'RECRUITING',
    storageType: 'REFRIGERATED',
    storageTypeLabel: '냉장',
    minimumShareUnit: 1,
  },
  {
    partyId: 2,
    title: '상봉점 냉동만두',
    productName: '냉동만두',
    storeName: '코스트코 상봉점',
    totalPrice: 18000,
    totalQuantity: 6,
    currentQuantity: 6,
    deadlineLabel: '2026.03.21 12:00',
    rating: 4.3,
    status: 'FULL',
    storageType: 'FROZEN',
    storageTypeLabel: '냉동',
    minimumShareUnit: 5,
  },
  {
    partyId: 3,
    title: '월계점 휴지 공동구매',
    productName: '휴지',
    storeName: '트레이더스 월계점',
    totalPrice: 24000,
    totalQuantity: 8,
    currentQuantity: 8,
    deadlineLabel: '2026.03.17 20:00',
    rating: 4.1,
    status: 'CLOSED',
    storageType: 'ROOM_TEMPERATURE',
    storageTypeLabel: '상온',
    minimumShareUnit: 2,
  },
];

describe('PartyList filters', () => {
  it('URL_쿼리_필터에_맞는_파티만_보여준다', async () => {
    // given
    api.getAllParties.mockReset();
    api.getStoreParties.mockReset();
    api.getStoreDetail.mockReset();
    api.getMyChatRooms.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getAllParties.mockResolvedValue(partyFixtures);
    api.getMyChatRooms.mockResolvedValue([]);

    render(
      <MemoryRouter initialEntries={['/parties?q=양재점&status=active&storage=REFRIGERATED&unit=ONE']}>
        <Routes>
          <Route path="/parties" element={<PartyList />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('양재점 연어 소분');

    // then
    expect(screen.getByDisplayValue('양재점')).toBeInTheDocument();
    expect(screen.getByText('전체 3개 중 1개 표시')).toBeInTheDocument();
    expect(screen.getByText('검색어: 양재점')).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: '보관 방식 필터' })).toHaveValue('REFRIGERATED');
    expect(screen.getByRole('combobox', { name: '소분 단위 필터' })).toHaveValue('ONE');
    expect(screen.queryByText('상봉점 냉동만두')).not.toBeInTheDocument();
    expect(screen.queryByText('월계점 휴지 공동구매')).not.toBeInTheDocument();
    expect(subscribeToPartyStreamMock).toHaveBeenCalled();
  });

  it('조건에_맞는_결과가_없으면_빈상태와_초기화_동작을_보여준다', async () => {
    // given
    api.getAllParties.mockReset();
    api.getStoreParties.mockReset();
    api.getStoreDetail.mockReset();
    api.getMyChatRooms.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getAllParties.mockResolvedValue(partyFixtures);
    api.getMyChatRooms.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties?q=없는파티']}>
        <Routes>
          <Route path="/parties" element={<PartyList />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    const emptyTitle = await screen.findByText('조건에 맞는 파티가 없어요');
    await user.click(within(emptyTitle.parentElement).getByRole('button', { name: '필터 초기화' }));

    // then
    await waitFor(() => {
      expect(screen.getByText('전체 3개 중 3개 표시')).toBeInTheDocument();
    });
    expect(screen.getByDisplayValue('')).toBeInTheDocument();
    expect(screen.getByText('양재점 연어 소분')).toBeInTheDocument();
    expect(screen.getByText('상봉점 냉동만두')).toBeInTheDocument();
    expect(screen.getByText('월계점 휴지 공동구매')).toBeInTheDocument();
  });
});
