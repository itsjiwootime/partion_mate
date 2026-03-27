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
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
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
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
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
    const checkbox = await screen.findByRole('checkbox', { name: '픽업 일정 확정 브라우저 푸시' });
    await user.click(checkbox);

    // then
    await waitFor(() => {
      expect(api.updateMyNotificationPreferences).toHaveBeenCalledWith({
        preferences: [
          {
            type: 'PICKUP_UPDATED',
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
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
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
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
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
            type: 'PICKUP_UPDATED',
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

  it('현재_브라우저_연결을_저장한다', async () => {
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
    api.upsertPushSubscription.mockReset();
    api.deletePushSubscription.mockReset();
    webPush.getCurrentPushSubscription.mockReset();
    webPush.getNotificationPermissionState.mockReset();
    webPush.isWebPushSupported.mockReset();
    webPush.subscribeToWebPush.mockReset();
    webPush.serializePushSubscription.mockReset();
    webPush.unsubscribeFromWebPush.mockReset();

    api.getMe.mockResolvedValue({
      name: '테스터',
      email: 'tester@test.com',
      address: '서울',
      trustSummary: null,
      recentReviews: [],
    });
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.getMyNotificationPreferences.mockResolvedValue([
      {
        type: 'PICKUP_UPDATED',
        label: '픽업 일정 확정',
        description: '픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.',
        deepLinkTargetLabel: '파티 상세',
        webPushSupported: true,
        webPushEnabled: false,
      },
    ]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: true, publicKey: 'public-key' });
    api.getPushSubscriptions
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ id: 7, endpoint: 'https://push.example/sub-1' }]);
    api.upsertPushSubscription.mockResolvedValue(null);
    webPush.getCurrentPushSubscription
      .mockResolvedValueOnce(null)
      .mockResolvedValueOnce({ endpoint: 'https://push.example/sub-1' });
    webPush.getNotificationPermissionState.mockReturnValue('default');
    webPush.isWebPushSupported.mockReturnValue(true);
    webPush.subscribeToWebPush.mockResolvedValue({ endpoint: 'https://push.example/sub-1' });
    webPush.serializePushSubscription.mockReturnValue({ endpoint: 'https://push.example/sub-1' });

    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/me']}>
        <Profile />
      </MemoryRouter>,
    );

    // when
    await user.click(await screen.findByRole('button', { name: '현재 브라우저 연결' }));

    // then
    await waitFor(() => {
      expect(webPush.subscribeToWebPush).toHaveBeenCalledWith('public-key');
    });
    expect(api.upsertPushSubscription).toHaveBeenCalledWith({ endpoint: 'https://push.example/sub-1' });
    expect(addToastMock).toHaveBeenCalledWith('현재 브라우저에서 외부 알림을 받을 수 있게 되었습니다.', 'success');
    await screen.findByRole('button', { name: '현재 브라우저 연결 해제' });
  });

  it('현재_브라우저_연결을_해제한다', async () => {
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
    api.upsertPushSubscription.mockReset();
    api.deletePushSubscription.mockReset();
    webPush.getCurrentPushSubscription.mockReset();
    webPush.getNotificationPermissionState.mockReset();
    webPush.isWebPushSupported.mockReset();
    webPush.subscribeToWebPush.mockReset();
    webPush.serializePushSubscription.mockReset();
    webPush.unsubscribeFromWebPush.mockReset();

    api.getMe.mockResolvedValue({
      name: '테스터',
      email: 'tester@test.com',
      address: '서울',
      trustSummary: null,
      recentReviews: [],
    });
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.getMyNotificationPreferences.mockResolvedValue([
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
    api.getPushSubscriptions
      .mockResolvedValueOnce([{ id: 9, endpoint: 'https://push.example/sub-2' }])
      .mockResolvedValueOnce([{ id: 9, endpoint: 'https://push.example/sub-2' }])
      .mockResolvedValueOnce([]);
    api.deletePushSubscription.mockResolvedValue(null);
    webPush.getCurrentPushSubscription
      .mockResolvedValueOnce({ endpoint: 'https://push.example/sub-2' })
      .mockResolvedValueOnce({ endpoint: 'https://push.example/sub-2' })
      .mockResolvedValueOnce(null);
    webPush.getNotificationPermissionState.mockReturnValue('default');
    webPush.isWebPushSupported.mockReturnValue(true);
    webPush.unsubscribeFromWebPush.mockResolvedValue(null);

    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/me']}>
        <Profile />
      </MemoryRouter>,
    );

    // when
    await user.click(await screen.findByRole('button', { name: '현재 브라우저 연결 해제' }));

    // then
    await waitFor(() => {
      expect(api.deletePushSubscription).toHaveBeenCalledWith(9);
    });
    expect(webPush.unsubscribeFromWebPush).toHaveBeenCalled();
    expect(addToastMock).toHaveBeenCalledWith('현재 브라우저의 외부 알림 연결을 해제했습니다.', 'success');
    await screen.findByRole('button', { name: '현재 브라우저 연결' });
  });
});
