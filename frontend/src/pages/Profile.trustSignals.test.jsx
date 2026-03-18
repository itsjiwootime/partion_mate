import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import Profile from './Profile';

const { logoutMock, refreshProfileMock, addToastMock, api, webPush, addressLocation } = vi.hoisted(() => ({
  logoutMock: vi.fn(),
  refreshProfileMock: vi.fn(),
  addToastMock: vi.fn(),
  api: {
    getMe: vi.fn(),
    updateMe: vi.fn(),
    getBlockedUsers: vi.fn(),
    getMyReports: vi.fn(),
    unblockUser: vi.fn(),
    getMyNotificationPreferences: vi.fn(),
    updateMyNotificationPreferences: vi.fn(),
    getMySettlementSettings: vi.fn(),
    updateMySettlementSettings: vi.fn(),
    getWebPushConfiguration: vi.fn(),
    getPushSubscriptions: vi.fn(),
    upsertPushSubscription: vi.fn(),
    deletePushSubscription: vi.fn(),
  },
  webPush: {
    getCurrentPushSubscription: vi.fn(),
    getNotificationPermissionState: vi.fn(),
    isWebPushSupported: vi.fn(),
    serializePushSubscription: vi.fn(),
    subscribeToWebPush: vi.fn(),
    unsubscribeFromWebPush: vi.fn(),
  },
  addressLocation: {
    geocodeAddress: vi.fn(),
    searchAddressWithPostcode: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
    logout: logoutMock,
    refreshProfile: refreshProfileMock,
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));
vi.mock('../utils/webPush', () => webPush);
vi.mock('../utils/addressLocation', () => addressLocation);

describe('Profile trust signals', () => {
  it('신뢰도_배지와_안정_메시지를_보여준다', async () => {
    // given
    logoutMock.mockReset();
    refreshProfileMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.getBlockedUsers.mockReset();
    api.getMyReports.mockReset();
    api.getMyNotificationPreferences.mockReset();
    api.getMySettlementSettings.mockReset();
    api.getWebPushConfiguration.mockReset();
    api.getPushSubscriptions.mockReset();
    webPush.getCurrentPushSubscription.mockReset();
    webPush.getNotificationPermissionState.mockReset();
    webPush.isWebPushSupported.mockReset();

    api.getMe.mockResolvedValue({
      name: '테스터',
      email: 'tester@test.com',
      address: '서울',
      trustSummary: {
        userId: 1,
        username: '테스터',
        averageRating: 4.9,
        reviewCount: 14,
        completedTradeCount: 18,
        noShowCount: 0,
        completionRate: 100,
        trustScore: 97,
        trustLevel: 'TOP',
        trustLevelLabel: '매우 신뢰',
      },
      recentReviews: [],
    });
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.getMyNotificationPreferences.mockResolvedValue([]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: false, publicKey: '' });
    api.getPushSubscriptions.mockResolvedValue([]);
    webPush.getCurrentPushSubscription.mockResolvedValue(null);
    webPush.getNotificationPermissionState.mockReturnValue('default');
    webPush.isWebPushSupported.mockReturnValue(true);

    render(
      <MemoryRouter initialEntries={['/me']}>
        <Profile />
      </MemoryRouter>,
    );

    // when
    await screen.findByText('신뢰도');

    // then
    expect(screen.getAllByText('매우 신뢰').length).toBeGreaterThan(0);
    expect(screen.getByText('후기 14개')).toBeInTheDocument();
    expect(screen.getByText('안정적인 최근 거래 흐름')).toBeInTheDocument();
    expect(screen.getByText('최근 거래 기준 위험 신호 없이 안정적으로 거래를 이어가고 있어요.')).toBeInTheDocument();
  });
});
