import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { LoadingState } from '../components/Feedback';
import {
  getCurrentPushSubscription,
  getNotificationPermissionState,
  isWebPushSupported,
  serializePushSubscription,
  subscribeToWebPush,
  unsubscribeFromWebPush,
} from '../utils/webPush';
import { Bell, Heart, Mail, MapPin, ShieldCheck, Star, User } from 'lucide-react';

function formatRating(value) {
  return Number(value ?? 0).toFixed(1);
}

function resolvePushSummary({ pushConfigEnabled, pushSupported, permission, currentBrowserSubscribed, subscriptionCount }) {
  if (!pushConfigEnabled) {
    return {
      title: '브라우저 푸시가 아직 비활성화되어 있습니다.',
      description: '서버 설정이 준비되면 현재 브라우저를 연결해 외부 알림을 받을 수 있습니다.',
    };
  }

  if (!pushSupported) {
    return {
      title: '현재 브라우저는 웹 푸시를 지원하지 않습니다.',
      description: '설정은 계정 기준으로 저장되지만, 이 브라우저에서는 외부 알림을 받을 수 없습니다.',
    };
  }

  if (permission === 'denied') {
    return {
      title: '브라우저 알림 권한이 차단되어 있습니다.',
      description: '브라우저 사이트 권한에서 알림을 허용해야 현재 브라우저에서 외부 알림을 받을 수 있습니다.',
    };
  }

  if (currentBrowserSubscribed) {
    return {
      title: '현재 브라우저가 외부 알림 수신에 연결되어 있습니다.',
      description: `계정에 연결된 브라우저 ${subscriptionCount}개에서 같은 설정을 사용합니다.`,
    };
  }

  if (subscriptionCount > 0) {
    return {
      title: '다른 브라우저는 연결되어 있지만 현재 브라우저는 미연결 상태입니다.',
      description: '이 브라우저도 연결하면 같은 계정 알림 설정으로 외부 푸시를 받을 수 있습니다.',
    };
  }

  return {
    title: '현재 브라우저가 외부 알림 수신에 연결되어 있지 않습니다.',
    description: '브라우저 연결을 켜면 승격, 픽업 확정, 종료 알림을 앱 밖에서도 받을 수 있습니다.',
  };
}

function Profile() {
  const { isAuthed, logout, refreshProfile } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState(null);
  const [profileForm, setProfileForm] = useState({ name: '', address: '' });
  const [editingProfile, setEditingProfile] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [error, setError] = useState('');
  const [notificationPreferences, setNotificationPreferences] = useState([]);
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsError, setSettingsError] = useState('');
  const [savingPreferenceType, setSavingPreferenceType] = useState(null);
  const [pushConfigEnabled, setPushConfigEnabled] = useState(false);
  const [pushConfigPublicKey, setPushConfigPublicKey] = useState('');
  const [subscriptionCount, setSubscriptionCount] = useState(0);
  const [currentBrowserSubscribed, setCurrentBrowserSubscribed] = useState(false);
  const [pushPermission, setPushPermission] = useState(getNotificationPermissionState());
  const [pushActionLoading, setPushActionLoading] = useState(false);

  const fetchMe = useCallback(async () => {
    try {
      setError('');
      const res = await api.getMe();
      setUser(res);
    } catch (e) {
      setError('프로필을 불러오지 못했습니다.');
    }
  }, []);

  useEffect(() => {
    if (!user) {
      return;
    }

    setProfileForm({
      name: user.name || '',
      address: user.address || '',
    });
  }, [user]);

  const fetchNotificationSettings = useCallback(async () => {
    try {
      setSettingsLoading(true);
      setSettingsError('');

      const [preferences, config, subscriptions] = await Promise.all([
        api.getMyNotificationPreferences(),
        api.getWebPushConfiguration(),
        api.getPushSubscriptions(),
      ]);

      const currentSubscription = await getCurrentPushSubscription().catch(() => null);

      setNotificationPreferences(preferences);
      setPushConfigEnabled(Boolean(config?.enabled));
      setPushConfigPublicKey(config?.publicKey ?? '');
      setSubscriptionCount(Array.isArray(subscriptions) ? subscriptions.length : 0);
      setCurrentBrowserSubscribed(Boolean(currentSubscription?.endpoint));
      setPushPermission(getNotificationPermissionState());
    } catch (e) {
      setSettingsError('알림 설정을 불러오지 못했습니다.');
    } finally {
      setSettingsLoading(false);
    }
  }, []);

  const handleTogglePreference = useCallback(
    async (type, nextValue) => {
      const previousPreferences = notificationPreferences;
      const nextPreferences = notificationPreferences.map((preference) =>
        preference.type === type ? { ...preference, webPushEnabled: nextValue } : preference,
      );

      setNotificationPreferences(nextPreferences);
      setSavingPreferenceType(type);
      setSettingsError('');

      try {
        const savedPreferences = await api.updateMyNotificationPreferences({
          preferences: nextPreferences.map((preference) => ({
            type: preference.type,
            webPushEnabled: preference.webPushEnabled,
          })),
        });
        setNotificationPreferences(savedPreferences);
        addToast('알림 설정을 저장했습니다.', 'success');
      } catch (e) {
        setNotificationPreferences(previousPreferences);
        setSettingsError(e?.message || '알림 설정을 저장하지 못했습니다.');
        addToast(e?.message || '알림 설정을 저장하지 못했습니다.', 'error');
      } finally {
        setSavingPreferenceType(null);
      }
    },
    [addToast, notificationPreferences],
  );

  const enableCurrentBrowserPush = useCallback(async () => {
    try {
      setPushActionLoading(true);
      setSettingsError('');
      const subscription = await subscribeToWebPush(pushConfigPublicKey);
      await api.upsertPushSubscription(serializePushSubscription(subscription));
      await fetchNotificationSettings();
      addToast('현재 브라우저에서 외부 알림을 받을 수 있게 되었습니다.', 'success');
    } catch (e) {
      setSettingsError(e?.message || '현재 브라우저를 외부 알림 수신에 연결하지 못했습니다.');
      addToast(e?.message || '현재 브라우저를 외부 알림 수신에 연결하지 못했습니다.', 'error');
    } finally {
      setPushActionLoading(false);
    }
  }, [addToast, fetchNotificationSettings, pushConfigPublicKey]);

  const disableCurrentBrowserPush = useCallback(async () => {
    try {
      setPushActionLoading(true);
      setSettingsError('');
      const currentSubscription = await getCurrentPushSubscription();
      const subscriptions = await api.getPushSubscriptions();
      const matchedSubscription = subscriptions.find(
        (subscription) => subscription.endpoint === currentSubscription?.endpoint,
      );

      if (matchedSubscription) {
        await api.deletePushSubscription(matchedSubscription.id);
      }

      await unsubscribeFromWebPush();
      await fetchNotificationSettings();
      addToast('현재 브라우저의 외부 알림 연결을 해제했습니다.', 'success');
    } catch (e) {
      setSettingsError(e?.message || '현재 브라우저 외부 알림 연결을 해제하지 못했습니다.');
      addToast(e?.message || '현재 브라우저 외부 알림 연결을 해제하지 못했습니다.', 'error');
    } finally {
      setPushActionLoading(false);
    }
  }, [addToast, fetchNotificationSettings]);

  useEffect(() => {
    if (!isAuthed) {
      navigate('/login', { replace: true, state: { from: `${location.pathname}${location.search}` } });
      return;
    }
    fetchMe();
    fetchNotificationSettings();
  }, [fetchMe, fetchNotificationSettings, isAuthed, location.pathname, location.search, navigate]);

  const handleProfileFieldChange = (key) => (event) => {
    setProfileForm((prev) => ({
      ...prev,
      [key]: event.target.value,
    }));
  };

  const handleCancelProfileEdit = () => {
    setEditingProfile(false);
    setError('');
    setProfileForm({
      name: user?.name || '',
      address: user?.address || '',
    });
  };

  const handleProfileSave = async (event) => {
    event.preventDefault();

    const nextName = profileForm.name.trim();
    const nextAddress = profileForm.address.trim();

    if (!nextName) {
      setError('닉네임을 입력해 주세요.');
      addToast('닉네임을 입력해 주세요.', 'error');
      return;
    }

    if (!nextAddress) {
      setError('주소를 입력해 주세요.');
      addToast('주소를 입력해 주세요.', 'error');
      return;
    }

    try {
      setSavingProfile(true);
      setError('');
      const updatedUser = await api.updateMe({
        name: nextName,
        address: nextAddress,
      });
      setUser(updatedUser);
      await refreshProfile?.();
      setEditingProfile(false);
      addToast('프로필을 저장했습니다.', 'success');
    } catch (e) {
      const message = e?.message || '프로필을 저장하지 못했습니다.';
      setError(message);
      addToast(message, 'error');
    } finally {
      setSavingProfile(false);
    }
  };

  if (!isAuthed) {
    return null;
  }

  const pushSupported = isWebPushSupported();
  const pushSummary = resolvePushSummary({
    pushConfigEnabled,
    pushSupported,
    permission: pushPermission,
    currentBrowserSubscribed,
    subscriptionCount,
  });

  return (
    <div className="space-y-4">
      <div className="card-elevated p-4 space-y-3">
        <h2 className="section-title">내 정보</h2>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!user && !error && <LoadingState />}
        {error && (
          <button onClick={fetchMe} className="btn-secondary w-full">
            다시 불러오기
          </button>
        )}
        {user && (
          <form className="space-y-3 text-sm text-ink" onSubmit={handleProfileSave}>
            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm font-medium text-ink" htmlFor="profile-name">
                <User size={16} className="text-mint-700" />
                <span>닉네임</span>
              </label>
              {editingProfile ? (
                <input
                  id="profile-name"
                  type="text"
                  value={profileForm.name}
                  onChange={handleProfileFieldChange('name')}
                  className="input"
                  aria-label="닉네임"
                />
              ) : (
                <p className="font-semibold">{user.name}</p>
              )}
            </div>
            <div className="space-y-2">
              <p className="flex items-center gap-2 text-sm font-medium text-ink">
                <Mail size={16} className="text-mint-700" />
                <span>이메일</span>
              </p>
              <p>{user.email}</p>
            </div>
            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm font-medium text-ink" htmlFor="profile-address">
                <MapPin size={16} className="text-mint-700" />
                <span>주소</span>
              </label>
              {editingProfile ? (
                <textarea
                  id="profile-address"
                  rows="3"
                  value={profileForm.address}
                  onChange={handleProfileFieldChange('address')}
                  className="input"
                  aria-label="주소"
                />
              ) : (
                <p>{user.address}</p>
              )}
            </div>
            <div className="rounded-2xl border border-ink/10 bg-ink/5 px-4 py-3 text-xs text-ink/60">
              주소를 수정하면 저장된 위치 좌표는 초기화될 수 있습니다. 주변 지점 기준 위치 재설정은 다음 단계에서 보강합니다.
            </div>
            {editingProfile ? (
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleCancelProfileEdit}
                  className="btn-secondary flex-1"
                  disabled={savingProfile}
                >
                  취소
                </button>
                <button type="submit" className="btn-primary flex-1" disabled={savingProfile}>
                  {savingProfile ? '저장 중...' : '프로필 저장'}
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setEditingProfile(true);
                  setError('');
                }}
                className="btn-secondary w-full"
              >
                프로필 수정
              </button>
            )}
          </form>
        )}
        <button
          onClick={() => navigate('/favorite-parties')}
          className="btn-secondary w-full"
        >
          <Heart size={16} />
          관심 파티 보기
        </button>
        <button
          onClick={logout}
          className="btn-secondary w-full"
        >
          로그아웃
        </button>
      </div>

      <div className="card-elevated p-4 space-y-4">
        <div className="flex items-center gap-2">
          <Bell size={18} className="text-mint-700" />
          <h2 className="section-title">알림 설정</h2>
        </div>
        <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3 space-y-2">
          <p className="text-sm font-semibold text-ink">{pushSummary.title}</p>
          <p className="text-sm text-ink/70">{pushSummary.description}</p>
          <p className="text-xs text-ink/55">
            앱 내 알림은 계속 저장되고, 여기서는 브라우저 푸시 수신 여부만 조정합니다.
          </p>
        </div>

        {settingsLoading && <LoadingState message="알림 설정을 불러오는 중..." />}
        {settingsError && <p className="text-sm text-red-600">{settingsError}</p>}

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={currentBrowserSubscribed ? disableCurrentBrowserPush : enableCurrentBrowserPush}
            className="btn-secondary px-4 py-2 text-sm"
            disabled={
              pushActionLoading ||
              !pushConfigEnabled ||
              !pushSupported ||
              (!currentBrowserSubscribed && pushPermission === 'denied')
            }
          >
            {pushActionLoading
              ? '처리 중...'
              : currentBrowserSubscribed
                ? '현재 브라우저 연결 해제'
                : '현재 브라우저 연결'}
          </button>
          <span className="rounded-full border border-ink/10 px-3 py-2 text-xs text-ink/60">
            연결된 브라우저 {subscriptionCount}개
          </span>
        </div>

        <div className="space-y-3">
          {notificationPreferences.map((preference) => (
            <label
              key={preference.type}
              className="flex items-center justify-between gap-4 rounded-2xl border border-ink/10 px-4 py-3"
            >
              <div className="space-y-1">
                <p className="text-sm font-semibold text-ink">{preference.label}</p>
                <p className="text-sm text-ink/65">{preference.description}</p>
                <p className="text-xs text-ink/50">알림 클릭 시 이동: {preference.deepLinkTargetLabel}</p>
              </div>
              {preference.webPushSupported ? (
                <div className="text-right">
                  <input
                    type="checkbox"
                    checked={preference.webPushEnabled}
                    onChange={(event) => handleTogglePreference(preference.type, event.target.checked)}
                    disabled={!pushConfigEnabled || savingPreferenceType === preference.type}
                    aria-label={`${preference.label} 브라우저 푸시`}
                    className="h-4 w-4 accent-mint-500"
                  />
                  <p className="mt-2 text-[11px] text-ink/50">
                    {savingPreferenceType === preference.type ? '저장 중...' : '브라우저 푸시'}
                  </p>
                </div>
              ) : (
                <span className="rounded-full bg-ink/5 px-3 py-2 text-xs text-ink/55">앱 내 알림만 지원</span>
              )}
            </label>
          ))}
        </div>
      </div>

      {user?.trustSummary && (
        <div className="card-elevated p-4 space-y-3">
          <div className="flex items-center gap-2">
            <ShieldCheck size={18} className="text-mint-700" />
            <h2 className="section-title">신뢰도</h2>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">신뢰 등급</p>
              <p className="mt-1 text-lg font-semibold text-ink">{user.trustSummary.trustLevelLabel}</p>
              <p className="text-sm text-ink/60">신뢰 점수 {user.trustSummary.trustScore}점</p>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm text-ink/75">
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">평균 평점</p>
                <p className="mt-1 flex items-center gap-1 font-semibold text-ink">
                  <Star size={14} className="fill-amber-400 text-amber-400" />
                  {formatRating(user.trustSummary.averageRating)}
                </p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">후기 수</p>
                <p className="mt-1 font-semibold text-ink">{user.trustSummary.reviewCount}개</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">거래 완료</p>
                <p className="mt-1 font-semibold text-ink">{user.trustSummary.completedTradeCount}건</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">노쇼 기록</p>
                <p className="mt-1 font-semibold text-ink">{user.trustSummary.noShowCount}건</p>
              </div>
            </div>
          </div>
          <p className="text-xs text-ink/55">거래 완료율 {user.trustSummary.completionRate}%</p>
        </div>
      )}

      <div className="card-elevated p-4 space-y-3">
        <h2 className="section-title">최근 받은 후기</h2>
        {!user?.recentReviews?.length && (
          <p className="text-sm text-ink/60">아직 받은 후기가 없습니다. 거래가 완료되면 이곳에 표시됩니다.</p>
        )}
        <div className="space-y-3">
          {user?.recentReviews?.map((review) => (
            <div key={review.reviewId} className="rounded-2xl border border-ink/10 px-4 py-3">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-semibold text-ink">{review.reviewerName}</p>
                  <p className="text-xs text-ink/50">{review.partyTitle}</p>
                </div>
                <div className="text-right text-xs text-ink/55">
                  <p className="flex items-center justify-end gap-1 font-semibold text-ink">
                    <Star size={13} className="fill-amber-400 text-amber-400" />
                    {review.rating}점
                  </p>
                  <p>{review.createdAtLabel}</p>
                </div>
              </div>
              {review.comment && <p className="mt-3 text-sm leading-6 text-ink/75">{review.comment}</p>}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default Profile;
