import { useCallback, useEffect, useState } from 'react';
import { Bell, Clock3, ExternalLink } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { EmptyState, LoadingState } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

function Notifications() {
  const { isAuthed } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchNotifications = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await api.getMyNotifications();
      setNotifications(data);
    } catch (e) {
      setError('알림을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthed) return;
    fetchNotifications();
  }, [fetchNotifications, isAuthed]);

  if (!isAuthed) {
    return (
      <div className="space-y-3">
        <p className="section-subtitle">로그인 후 알림 내역을 확인할 수 있습니다.</p>
        <button onClick={() => navigate('/login', { state: { from: `${location.pathname}${location.search}` } })} className="btn-primary px-4 py-2 text-sm">
          로그인 하러 가기
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="card-elevated p-4">
        <h2 className="text-xl font-semibold text-ink">알림 내역</h2>
        <p className="section-subtitle">참여 완료, 조건 변경, 대기열 승격, 마감 알림을 확인하세요.</p>
      </div>

      {loading && <LoadingState />}
      {error && <p className="text-sm text-red-600">{error}</p>}
      {!loading && error && (
        <button onClick={fetchNotifications} className="btn-secondary px-4 py-2 text-sm">
          다시 불러오기
        </button>
      )}

      {!loading && !error && notifications.length === 0 && (
        <EmptyState
          title="도착한 알림이 없어요"
          description="파티 참여, 조건 변경, 승격, 마감 알림이 생기면 여기에 표시됩니다."
          action={
            <button onClick={() => navigate('/parties')} className="btn-secondary px-4 py-2 text-sm">
              파티 둘러보기
            </button>
          }
        />
      )}

      <div className="space-y-3">
        {notifications.map((notification) => (
          <article key={notification.id} className="card p-4 space-y-2">
            <div className="flex items-start justify-between gap-3">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-xs font-semibold text-mint-700">
                  <Bell size={14} />
                  {notification.type}
                </div>
                <h3 className="text-base font-semibold text-ink">{notification.title}</h3>
              </div>
              <div className="text-xs text-ink/50 flex items-center gap-1">
                <Clock3 size={14} />
                {notification.createdAtLabel}
              </div>
            </div>
            <p className="text-sm text-ink/70">{notification.message}</p>
            {notification.linkUrl && (
              <button
                onClick={() => navigate(notification.linkUrl)}
                className="btn-secondary px-3 py-2 text-sm"
              >
                관련 파티 보기
                <ExternalLink size={14} />
              </button>
            )}
          </article>
        ))}
      </div>
    </div>
  );
}

export default Notifications;
