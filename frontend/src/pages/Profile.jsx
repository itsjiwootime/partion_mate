import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { LoadingState } from '../components/Feedback';
import { Mail, MapPin, ShieldCheck, Star, User } from 'lucide-react';

function formatRating(value) {
  return Number(value ?? 0).toFixed(1);
}

function Profile() {
  const { isAuthed, logout } = useAuth();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAuthed) {
      navigate('/login');
      return;
    }
    const fetchMe = async () => {
      try {
        setError('');
        const res = await api.getMe();
        setUser(res);
      } catch (e) {
        setError('프로필을 불러오지 못했습니다.');
      }
    };
    fetchMe();
  }, [isAuthed, navigate]);

  if (!isAuthed) {
    return null;
  }

  return (
    <div className="space-y-4">
      <div className="card-elevated p-4 space-y-3">
        <h2 className="section-title">내 정보</h2>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!user && !error && <LoadingState />}
        {user && (
          <div className="space-y-2 text-sm text-ink">
            <p className="flex items-center gap-2">
              <User size={16} className="text-mint-700" />
              <span className="font-semibold">{user.name}</span>
            </p>
            <p className="flex items-center gap-2">
              <Mail size={16} className="text-mint-700" />
              <span>{user.email}</span>
            </p>
            <p className="flex items-center gap-2">
              <MapPin size={16} className="text-mint-700" />
              <span>{user.address}</span>
            </p>
          </div>
        )}
        <button
          onClick={logout}
          className="btn-secondary w-full"
        >
          로그아웃
        </button>
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
