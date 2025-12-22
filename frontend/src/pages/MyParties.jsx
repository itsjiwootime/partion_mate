import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

const roleLabel = {
  LEADER: '호스트',
  MEMBER: '참여자',
};

function MyParties() {
  const { isAuthed } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAuthed) return;
    const fetchData = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await api.getMyParties();
        setList(data);
      } catch (e) {
        setError('내 파티를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [isAuthed]);

  if (!isAuthed) {
    return (
      <div className="space-y-3">
        <p className="text-sm text-ink/60">로그인 후 내 파티를 확인할 수 있습니다.</p>
        <button
          onClick={() => navigate('/login')}
          className="rounded-full bg-mint-500 px-4 py-2 text-sm font-semibold text-white hover:bg-mint-600"
        >
          로그인 하러 가기
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="glass-panel rounded-2xl p-4">
        <h2 className="text-xl font-semibold text-ink">내 파티</h2>
        <p className="text-sm text-ink/60">내가 만든/참여 중인 파티 목록</p>
      </div>

      {loading && <p className="text-sm text-ink/60">불러오는 중...</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}

      {!loading && !error && list.length === 0 && (
        <div className="glass-panel rounded-2xl p-5 text-center text-sm text-ink/60 space-y-2">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-mint-500/15 text-mint-700">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
              <path strokeLinecap="round" strokeLinejoin="round" d="M7 9l5 5 5-5" />
            </svg>
          </div>
          <p className="font-semibold text-ink">아직 등록된 파티가 없어요!</p>
          <p>새로운 파티에 참여하거나 직접 만들어보세요.</p>
        </div>
      )}

      <div className="grid gap-3 md:grid-cols-2">
        {list.map((p) => (
          <button
            key={p.id}
            onClick={() =>
              navigate(`/parties/${p.id}`, {
                state: { party: p, fromMyParties: true },
              })
            }
            className="glass-panel w-full rounded-xl p-4 text-left shadow-sm transition hover:-translate-y-1 hover:shadow-xl"
          >
            <div className="flex items-center justify-between">
              <div className="text-xs font-semibold text-mint-700">
                {roleLabel[p.userRole] ?? p.userRole}
              </div>
              <div
                className={[
                  'rounded-full px-3 py-1 text-xs font-semibold',
                  p.status === 'FULL' ? 'bg-ink/10 text-ink/50' : 'bg-mint-500/15 text-mint-700',
                ].join(' ')}
              >
                {p.status === 'FULL' ? '마감' : '모집 중'}
              </div>
            </div>
            <h3 className="mt-1 text-lg font-semibold text-ink">{p.title}</h3>
            <p className="text-sm text-ink/60">{p.productName}</p>
            <p className="text-xs text-ink/60">{p.storeName}</p>
            <div className="mt-2 text-sm text-ink/80">
              {p.currentQuantity ?? 0} / {p.totalQuantity}개
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

export default MyParties;
