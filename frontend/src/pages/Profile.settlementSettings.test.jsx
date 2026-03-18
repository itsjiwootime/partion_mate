import { render, screen, waitFor } from '@testing-library/react';
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

describe('Profile settlement settings', () => {
  it('정산_기본_안내를_저장한다', async () => {
    // given
    logoutMock.mockReset();
    refreshProfileMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.getMyNotificationPreferences.mockReset();
    api.getMySettlementSettings.mockReset();
    api.updateMySettlementSettings.mockReset();
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
    api.getMyNotificationPreferences.mockResolvedValue([]);
    api.getMySettlementSettings.mockResolvedValue({
      settlementGuide: '입금 후 채팅에 마지막 4자리를 남겨주세요.',
    });
    api.updateMySettlementSettings.mockResolvedValue({
      settlementGuide: '입금 후 채팅 공지를 확인해주세요.',
    });
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
    const textarea = await screen.findByRole('textbox', { name: '정산 기본 안내' });
    await user.clear(textarea);
    await user.type(textarea, '입금 후 채팅 공지를 확인해주세요.');
    await user.click(screen.getByRole('button', { name: '정산 기본 안내 저장' }));

    // then
    await waitFor(() => {
      expect(api.updateMySettlementSettings).toHaveBeenCalledWith({
        settlementGuide: '입금 후 채팅 공지를 확인해주세요.',
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('정산 기본 안내를 저장했습니다.', 'success');
    expect(screen.getByRole('textbox', { name: '정산 기본 안내' })).toHaveValue('입금 후 채팅 공지를 확인해주세요.');
  });
});
