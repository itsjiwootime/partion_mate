import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { vi } from 'vitest';
import Home from './Home';

const { api } = vi.hoisted(() => ({
  api: {
    getMe: vi.fn(),
    getAllParties: vi.fn(),
    getNearbyStores: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: false,
  }),
}));

function PartyDiscoveryLocation() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  return (
    <div>
      <div>query:{params.get('q') ?? ''}</div>
      <div>status:{params.get('status') ?? ''}</div>
      <div>storage:{params.get('storage') ?? ''}</div>
      <div>unit:{params.get('unit') ?? ''}</div>
      <div>sort:{params.get('sort') ?? ''}</div>
    </div>
  );
}

describe('Home quick discovery', () => {
  it('검색어를_입력하면_파티_목록으로_쿼리를_전달한다', async () => {
    // given
    api.getMe.mockReset();
    api.getAllParties.mockReset();
    api.getNearbyStores.mockReset();
    api.getAllParties.mockResolvedValue([]);
    api.getNearbyStores.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/parties" element={<PartyDiscoveryLocation />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.type(screen.getByRole('textbox', { name: '홈 파티 검색' }), '휴지');
    await user.click(screen.getByRole('button', { name: '검색' }));

    // then
    await waitFor(() => {
      expect(screen.getByText('query:휴지')).toBeInTheDocument();
    });
    expect(screen.getByText('status:')).toBeInTheDocument();
  });

  it('빠른_필터_버튼으로_대표_조건을_즉시_적용한다', async () => {
    // given
    api.getMe.mockReset();
    api.getAllParties.mockReset();
    api.getNearbyStores.mockReset();
    api.getAllParties.mockResolvedValue([]);
    api.getNearbyStores.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/parties" element={<PartyDiscoveryLocation />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.click(screen.getByRole('button', { name: /냉장 식품/i }));

    // then
    await waitFor(() => {
      expect(screen.getByText('storage:REFRIGERATED')).toBeInTheDocument();
    });
    expect(screen.getByText('query:')).toBeInTheDocument();
  });

  it('발견_섹션_카드를_누르면_정렬_쿼리를_전달한다', async () => {
    // given
    api.getMe.mockReset();
    api.getAllParties.mockReset();
    api.getNearbyStores.mockReset();
    api.getAllParties.mockResolvedValue([
      {
        partyId: 11,
        title: '오늘 마감 연어',
        productName: '연어',
        storeName: '코스트코 양재점',
        totalPrice: 29900,
        totalQuantity: 4,
        currentQuantity: 2,
        deadline: '2099-03-19T18:00:00',
        deadlineLabel: '2099.03.19 18:00',
        rating: 4.8,
        status: 'RECRUITING',
        storageType: 'REFRIGERATED',
        minimumShareUnit: 1,
      },
      {
        partyId: 14,
        title: '인기 라면',
        productName: '라면',
        storeName: '트레이더스 월계점',
        totalPrice: 18000,
        totalQuantity: 10,
        currentQuantity: 8,
        deadline: '2099-03-22T12:00:00',
        deadlineLabel: '2099.03.22 12:00',
        rating: 4.3,
        status: 'RECRUITING',
        storageType: 'ROOM_TEMPERATURE',
        minimumShareUnit: 2,
      },
      {
        partyId: 21,
        title: '신규 과자',
        productName: '과자',
        storeName: '코스트코 공세점',
        totalPrice: 9900,
        totalQuantity: 6,
        currentQuantity: 1,
        deadline: '2099-03-26T12:00:00',
        deadlineLabel: '2099.03.26 12:00',
        rating: 4.1,
        status: 'RECRUITING',
        storageType: 'ROOM_TEMPERATURE',
        minimumShareUnit: 1,
      },
    ]);
    api.getNearbyStores.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/parties" element={<PartyDiscoveryLocation />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('신규 과자');
    await user.click(screen.getByText('인기 파티').closest('button'));

    // then
    await waitFor(() => {
      expect(screen.getByText('sort:popular')).toBeInTheDocument();
    });
  });
});
