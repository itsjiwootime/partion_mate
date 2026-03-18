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
    createReport: vi.fn(),
    blockUser: vi.fn(),
  },
  subscribeToPartyStreamMock: vi.fn(() => vi.fn()),
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
    userName: '참여자',
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

describe('PartyDetail safety', () => {
  it('호스트_신고를_접수한다', async () => {
    // given
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.createReport.mockReset();
    api.blockUser.mockReset();
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
        averageRating: 4.8,
        reviewCount: 11,
        completedTradeCount: 18,
        noShowCount: 0,
        completionRate: 100,
        trustScore: 95,
        trustLevel: 'TRUSTED',
        trustLevelLabel: '믿음직한 호스트',
      },
      hostReviews: [],
      settlementMembers: [],
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/5']}>
        <Routes>
          <Route path="/parties/:id" element={<PartyDetail />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('신뢰·안전');
    await user.click(screen.getByRole('button', { name: '호스트 신고' }));
    await user.click(screen.getByRole('radio', { name: /^사기 의심/ }));
    await user.type(screen.getByRole('textbox', { name: /^메모/ }), '입금 유도 방식이 이상했어요.');
    await user.click(screen.getByRole('button', { name: '신고 접수' }));

    // then
    await waitFor(() => {
      expect(api.createReport).toHaveBeenCalledWith({
        targetType: 'USER',
        partyId: 5,
        targetUserId: 42,
        reasonType: 'FRAUD_SUSPECTED',
        memo: '입금 유도 방식이 이상했어요.',
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('신고를 접수했습니다. 운영 검토 후 필요한 조치를 진행합니다.', 'success');
  });
});
