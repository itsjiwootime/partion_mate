import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

describe('Profile safety center', () => {
  it('차단_목록과_신고_내역을_보여주고_차단을_해제한다', async () => {
    // given
    logoutMock.mockReset();
    refreshProfileMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.getBlockedUsers.mockReset();
    api.getMyReports.mockReset();
    api.unblockUser.mockReset();
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
      trustSummary: null,
      recentReviews: [],
    });
    api.getBlockedUsers.mockResolvedValue([
      {
        id: 1,
        targetUserId: 77,
        targetUsername: '민수',
        createdAtLabel: '2026.03.18 18:00',
      },
    ]);
    api.getMyReports.mockResolvedValue([
      {
        id: 9,
        targetType: 'USER',
        targetTypeLabel: '상대 사용자',
        targetUsername: '민수',
        reasonTypeLabel: '부적절한 채팅',
        statusLabel: '접수됨',
        createdAtLabel: '2026.03.18 18:10',
        memo: '거친 표현이 반복됐습니다.',
      },
    ]);
    api.getMyNotificationPreferences.mockResolvedValue([]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: false, publicKey: '' });
    api.getPushSubscriptions.mockResolvedValue([]);
    webPush.getCurrentPushSubscription.mockResolvedValue(null);
    webPush.getNotificationPermissionState.mockReturnValue('default');
    webPush.isWebPushSupported.mockReturnValue(true);

    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/me']}>
        <Profile />
      </MemoryRouter>,
    );

    // when
    await screen.findByText('차단한 사용자');
    await user.click(screen.getByRole('button', { name: '차단 해제' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: '차단 해제' }));

    // then
    expect(screen.getByText('민수')).toBeInTheDocument();
    expect(screen.getByText('사유: 부적절한 채팅')).toBeInTheDocument();
    await waitFor(() => {
      expect(api.unblockUser).toHaveBeenCalledWith(77);
    });
    expect(addToastMock).toHaveBeenCalledWith('차단을 해제했습니다.', 'success');
  });

  it('안전_화면_focus_state로_안내_배너를_보여준다', async () => {
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
      trustSummary: null,
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
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/me',
            state: {
              focusSafetyCenter: true,
              safetyNotice: {
                title: '차단 해제는 여기서 할 수 있어요',
                description: '차단 목록에서 사용자를 해제하면 이후 같은 채팅 접근이 다시 가능해질 수 있습니다.',
              },
            },
          },
        ]}
      >
        <Profile />
      </MemoryRouter>,
    );

    // when
    await screen.findByText('신뢰·안전 관리');

    // then
    expect(screen.getByText('차단 해제는 여기서 할 수 있어요')).toBeInTheDocument();
    expect(screen.getByText('차단 목록에서 사용자를 해제하면 이후 같은 채팅 접근이 다시 가능해질 수 있습니다.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '확인' })).toBeInTheDocument();
  });
});
