import { render, screen } from '@testing-library/react';
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
    userName: '',
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

describe('PartyDetail trust signals', () => {
  it('호스트_주의_배지와_경고_문구를_보여준다', async () => {
    // given
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
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
      status: 'RECRUITING',
      favorite: false,
      hostTrust: {
        userId: 42,
        username: '호스트',
        averageRating: 3.9,
        reviewCount: 4,
        completedTradeCount: 7,
        noShowCount: 2,
        completionRate: 68,
        trustScore: 42,
        trustLevel: 'CAUTION',
        trustLevelLabel: '주의',
      },
      hostReviews: [],
      settlementMembers: [],
    });

    render(
      <MemoryRouter initialEntries={['/parties/5']}>
        <Routes>
          <Route path="/parties/:id" element={<PartyDetail />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('호스트 신뢰도');

    // then
    expect(screen.getAllByText('주의 필요').length).toBeGreaterThan(0);
    expect(screen.getByText('노쇼 기록 2건')).toBeInTheDocument();
    expect(screen.getAllByText('거래 완료율 68%').length).toBeGreaterThan(0);
  });
});
