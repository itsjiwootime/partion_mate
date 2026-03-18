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

describe('Profile edit', () => {
  it('프로필을_수정하고_전역_프로필을_동기화한다', async () => {
    // given
    logoutMock.mockReset();
    refreshProfileMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.updateMe.mockReset();
    api.getMyNotificationPreferences.mockReset();
    api.getBlockedUsers.mockReset();
    api.getMyReports.mockReset();
    api.getWebPushConfiguration.mockReset();
    api.getPushSubscriptions.mockReset();
    api.getMySettlementSettings.mockReset();
    webPush.getCurrentPushSubscription.mockReset();
    webPush.getNotificationPermissionState.mockReset();
    webPush.isWebPushSupported.mockReset();
    addressLocation.geocodeAddress.mockReset();
    addressLocation.searchAddressWithPostcode.mockReset();

    api.getMe.mockResolvedValue({
      name: '테스터',
      email: 'tester@test.com',
      address: '서울 서초구',
      latitude: 37.5,
      longitude: 127.0,
      trustSummary: null,
      recentReviews: [],
    });
    api.updateMe.mockResolvedValue({
      name: '수정된닉네임',
      email: 'tester@test.com',
      address: '서울 송파구',
      latitude: null,
      longitude: null,
      trustSummary: null,
      recentReviews: [],
    });
    api.getMyNotificationPreferences.mockResolvedValue([]);
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: false, publicKey: '' });
    api.getPushSubscriptions.mockResolvedValue([]);
    webPush.getCurrentPushSubscription.mockResolvedValue(null);
    webPush.getNotificationPermissionState.mockReturnValue('default');
    webPush.isWebPushSupported.mockReturnValue(true);
    refreshProfileMock.mockResolvedValue(null);

    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/me']}>
        <Profile />
      </MemoryRouter>,
    );

    // when
    await screen.findByText('테스터');
    await user.click(screen.getByRole('button', { name: '프로필 수정' }));
    await user.clear(screen.getByLabelText('닉네임'));
    await user.type(screen.getByLabelText('닉네임'), '수정된닉네임');
    await user.clear(screen.getByLabelText('주소'));
    await user.type(screen.getByLabelText('주소'), '서울 송파구');
    await user.click(screen.getByRole('button', { name: '프로필 저장' }));

    // then
    await waitFor(() => {
      expect(api.updateMe).toHaveBeenCalledWith({
        name: '수정된닉네임',
        address: '서울 송파구',
      });
    });
    expect(refreshProfileMock).toHaveBeenCalled();
    expect(addToastMock).toHaveBeenCalledWith('프로필을 저장했습니다.', 'success');
    await screen.findByText('수정된닉네임');
    expect(screen.getByText('서울 송파구')).toBeInTheDocument();
  });

  it('주소_검색으로_위치를_다시_설정한_후_저장한다', async () => {
    // given
    logoutMock.mockReset();
    refreshProfileMock.mockReset();
    addToastMock.mockReset();
    api.getMe.mockReset();
    api.updateMe.mockReset();
    api.getMyNotificationPreferences.mockReset();
    api.getBlockedUsers.mockReset();
    api.getMyReports.mockReset();
    api.getWebPushConfiguration.mockReset();
    api.getPushSubscriptions.mockReset();
    api.getMySettlementSettings.mockReset();
    webPush.getCurrentPushSubscription.mockReset();
    webPush.getNotificationPermissionState.mockReset();
    webPush.isWebPushSupported.mockReset();
    addressLocation.geocodeAddress.mockReset();
    addressLocation.searchAddressWithPostcode.mockReset();

    api.getMe.mockResolvedValue({
      name: '테스터',
      email: 'tester@test.com',
      address: '서울 서초구',
      latitude: 37.5,
      longitude: 127.0,
      trustSummary: null,
      recentReviews: [],
    });
    api.updateMe.mockResolvedValue({
      name: '테스터',
      email: 'tester@test.com',
      address: '서울 송파구',
      latitude: 37.512345,
      longitude: 127.123456,
      trustSummary: null,
      recentReviews: [],
    });
    api.getMyNotificationPreferences.mockResolvedValue([]);
    api.getBlockedUsers.mockResolvedValue([]);
    api.getMyReports.mockResolvedValue([]);
    api.getMySettlementSettings.mockResolvedValue({ settlementGuide: '' });
    api.getWebPushConfiguration.mockResolvedValue({ enabled: false, publicKey: '' });
    api.getPushSubscriptions.mockResolvedValue([]);
    webPush.getCurrentPushSubscription.mockResolvedValue(null);
    webPush.getNotificationPermissionState.mockReturnValue('default');
    webPush.isWebPushSupported.mockReturnValue(true);
    refreshProfileMock.mockResolvedValue(null);
    addressLocation.searchAddressWithPostcode.mockResolvedValue('서울 송파구');
    addressLocation.geocodeAddress.mockResolvedValue({
      latitude: '37.512345',
      longitude: '127.123456',
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/me']}>
        <Profile />
      </MemoryRouter>,
    );

    // when
    await screen.findByText('테스터');
    await user.click(screen.getByRole('button', { name: '프로필 수정' }));
    await user.click(screen.getByRole('button', { name: '주소 검색으로 위치 다시 설정' }));
    await user.click(screen.getByRole('button', { name: '프로필 저장' }));

    // then
    await waitFor(() => {
      expect(api.updateMe).toHaveBeenCalledWith({
        name: '테스터',
        address: '서울 송파구',
        latitude: 37.512345,
        longitude: 127.123456,
      });
    });
    expect(addressLocation.searchAddressWithPostcode).toHaveBeenCalled();
    expect(addressLocation.geocodeAddress).toHaveBeenCalledWith('서울 송파구', expect.any(String));
  });
});
