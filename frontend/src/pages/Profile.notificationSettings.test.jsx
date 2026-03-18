import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import Profile from './Profile';

const { logoutMock, addToastMock, api, webPush } = vi.hoisted(() => ({
  logoutMock: vi.fn(),
  addToastMock: vi.fn(),
  api: {
    getMe: vi.fn(),
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
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
    logout: logoutMock,
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));
vi.mock('../utils/webPush', () => webPush);

describe('Profile notification settings', () => {
  it('브라우저_푸시_설정을_저장한다', async () => {
    // given
    logoutMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.getBlockedUsers.mockReset();
    api.getMyReports.mockReset();
    api.getMyNotificationPreferences.mockReset();
    api.updateMyNotificationPreferences.mockReset();
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
    api.getMyNotificationPreferences.mockResolvedValue([
      {
        type: 'WAITING_PROMOTED',
        label: '대기열 승격',
        description: '대기열에서 참여로 승격되면 알려줍니다.',
        deepLinkTargetLabel: '채팅방',
        webPushSupported: true,
        webPushEnabled: true,
      },
      {
        type: 'PARTY_UPDATED',
        label: '파티 조건 변경',
        description: '모집 조건이 바뀌면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
        webPushSupported: false,
        webPushEnabled: false,
      },
    ]);
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.updateMyNotificationPreferences.mockResolvedValue([
      {
        type: 'WAITING_PROMOTED',
        label: '대기열 승격',
        description: '대기열에서 참여로 승격되면 알려줍니다.',
        deepLinkTargetLabel: '채팅방',
        webPushSupported: true,
        webPushEnabled: false,
      },
      {
        type: 'PARTY_UPDATED',
        label: '파티 조건 변경',
        description: '모집 조건이 바뀌면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
        webPushSupported: false,
        webPushEnabled: false,
      },
    ]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: true, publicKey: 'public-key' });
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
    const checkbox = await screen.findByRole('checkbox', { name: '대기열 승격 브라우저 푸시' });
    await user.click(checkbox);

    // then
    await waitFor(() => {
      expect(api.updateMyNotificationPreferences).toHaveBeenCalledWith({
        preferences: [
          {
            type: 'WAITING_PROMOTED',
            webPushEnabled: false,
          },
          {
            type: 'PARTY_UPDATED',
            webPushEnabled: false,
          },
        ],
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('알림 설정을 저장했습니다.', 'success');
  });

  it('브라우저_푸시_전체_토글을_저장한다', async () => {
    // given
    logoutMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.getBlockedUsers.mockReset();
    api.getMyReports.mockReset();
    api.getMyNotificationPreferences.mockReset();
    api.updateMyNotificationPreferences.mockReset();
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
    api.getMyNotificationPreferences.mockResolvedValue([
      {
        type: 'WAITING_PROMOTED',
        label: '대기열 승격',
        description: '대기열에서 참여로 승격되면 알려줍니다.',
        deepLinkTargetLabel: '채팅방',
        webPushSupported: true,
        webPushEnabled: false,
      },
      {
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
        webPushSupported: true,
        webPushEnabled: false,
      },
    ]);
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.updateMyNotificationPreferences.mockResolvedValue([
      {
        type: 'WAITING_PROMOTED',
        label: '대기열 승격',
        description: '대기열에서 참여로 승격되면 알려줍니다.',
        deepLinkTargetLabel: '채팅방',
        webPushSupported: true,
        webPushEnabled: true,
      },
      {
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
        webPushSupported: true,
        webPushEnabled: true,
      },
    ]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: true, publicKey: 'public-key' });
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
    const checkbox = await screen.findByRole('checkbox', { name: '브라우저 푸시 전체 받기' });
    await user.click(checkbox);

    // then
    await waitFor(() => {
      expect(api.updateMyNotificationPreferences).toHaveBeenCalledWith({
        preferences: [
          {
            type: 'WAITING_PROMOTED',
            webPushEnabled: true,
          },
          {
            type: 'PICKUP_UPDATED',
            webPushEnabled: true,
          },
        ],
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('브라우저 푸시를 전체 활성화했습니다.', 'success');
  });
});
