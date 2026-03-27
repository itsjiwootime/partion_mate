import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { EmptyState, LoadingState } from '../components/Feedback';
import { subscribeToPartyStream } from '../utils/partyRealtime';

const roleLabel = {
  LEADER: '호스트',
  MEMBER: '참여자',
};

function MyParties() {
  const { isAuthed } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { addToast } = useToast();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [cancellingId, setCancellingId] = useState(null);
  const [realtimeState, setRealtimeState] = useState('connecting');
  const [chatUnreadMap, setChatUnreadMap] = useState({});
  const partyIdsRef = useRef(new Set());

  const returnTo = `${location.pathname}${location.search}`;

  const fetchData = useCallback(async (activeRef = { current: true }) => {
    try {
      setLoading(true);
      setError('');
      const data = await api.getMyParties();
      if (activeRef.current) {
        setList(data);
      }
    } catch (e) {
      if (activeRef.current) {
        setError('내 파티를 불러오지 못했습니다.');
      }
    } finally {
      if (activeRef.current) {
        setLoading(false);
      }
    }
  }, []);

  const fetchChatRooms = useCallback(async (activeRef = { current: true }) => {
    try {
      const rooms = await api.getMyChatRooms();
      if (activeRef.current) {
        setChatUnreadMap(
          rooms.reduce((acc, room) => {
            acc[room.partyId] = room.unreadCount ?? 0;
            return acc;
          }, {}),
        );
      }
    } catch {
      if (activeRef.current) {
        setChatUnreadMap({});
      }
    }
  }, []);

  useEffect(() => {
    partyIdsRef.current = new Set(list.map((party) => party.id));
  }, [list]);

  useEffect(() => {
    if (!isAuthed) return;
    const activeRef = { current: true };
    fetchData(activeRef);
    fetchChatRooms(activeRef);

    const unsubscribe = subscribeToPartyStream({
      onConnected: () => {
        if (activeRef.current) {
          setRealtimeState('live');
        }
      },
      onReconnectStateChange: (state) => {
        if (activeRef.current) {
          setRealtimeState(state);
        }
      },
      onPartyUpdated: (event) => {
        if (!activeRef.current) return;
        if (!partyIdsRef.current.has(event.id)) return;
        fetchData(activeRef);
        fetchChatRooms(activeRef);
      },
      onFallback: () => {
        if (activeRef.current && partyIdsRef.current.size > 0) {
          fetchData(activeRef);
          fetchChatRooms(activeRef);
        }
      },
    });

    return () => {
      activeRef.current = false;
      unsubscribe?.();
    };
  }, [fetchChatRooms, fetchData, isAuthed]);

  const handleCancel = async (partyId) => {
    try {
      setCancellingId(partyId);
      await api.cancelJoin(partyId);
      setList((current) => current.filter((party) => party.id !== partyId));
      setChatUnreadMap((current) => {
        const next = { ...current };
        delete next[partyId];
        return next;
      });
      addToast('참여 내역을 취소했습니다.', 'success');
    } catch (e) {
      addToast(e.message || '취소 중 오류가 발생했습니다.', 'error');
    } finally {
      setCancellingId(null);
    }
  };

  if (!isAuthed) {
    return (
      <div className="space-y-3">
        <p className="section-subtitle">로그인 후 내 파티를 확인할 수 있습니다.</p>
        <button
          onClick={() => navigate('/login', { state: { from: returnTo } })}
          className="btn-primary px-4 py-2 text-sm"
        >
          로그인 하러 가기
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="card-elevated p-4">
        <h2 className="text-xl font-semibold text-ink">내 파티</h2>
        <p className="section-subtitle">내가 만든/참여 중인 파티 목록</p>
        <p className="mt-2 text-xs text-ink/50">
          {realtimeState === 'reconnecting' ? '실시간 연결을 다시 시도하는 중입니다.' : '실시간 상태를 자동으로 동기화하고 있습니다.'}
        </p>
      </div>

      {loading && <LoadingState />}
      {error && <p className="text-sm text-red-600">{error}</p>}
      {!loading && error && (
        <button onClick={() => fetchData()} className="btn-secondary px-4 py-2 text-sm">
          다시 불러오기
        </button>
      )}

      {!loading && !error && list.length === 0 && (
        <EmptyState
          title="아직 등록된 파티가 없어요!"
          description="새로운 파티에 참여하거나 직접 만들어보세요."
          action={
            <button onClick={() => navigate('/parties')} className="btn-secondary px-4 py-2 text-sm">
              파티 둘러보기
            </button>
          }
        />
      )}

      <div className="grid gap-3 md:grid-cols-2">
        {list.map((p) => {
          const canCancel = p.userRole === 'MEMBER';

          return (
            <div
              key={p.id}
              className="card w-full rounded-xl p-4 text-left space-y-3"
            >
            <div className="flex items-center justify-between">
              <div className="text-xs font-semibold text-mint-700">{roleLabel[p.userRole] ?? p.userRole}</div>
              <div
                className={[
                  'badge',
                  p.status === 'CLOSED'
                    ? 'bg-red-50 text-red-700'
                    : p.status === 'FULL'
                      ? 'bg-ink/10 text-ink/50'
                      : 'bg-mint-500/15 text-mint-700',
                ].join(' ')}
              >
                {p.status === 'CLOSED' ? '종료' : p.status === 'FULL' ? '마감' : '모집 중'}
              </div>
            </div>
            <h3 className="mt-1 text-lg font-semibold text-ink">{p.title}</h3>
            <p className="text-sm text-ink/60">{p.productName}</p>
            <p className="text-xs text-ink/60">{p.storeName}</p>
            <div className="mt-2 text-sm text-ink/80">
              {p.currentQuantity ?? 0} / {p.totalQuantity}개
            </div>
            <div className="text-xs text-ink/60">
              {`내 참여 수량 ${p.requestedQuantity ?? 0}개`}
            </div>
            <div className="text-xs text-ink/60">
              정산: {p.actualAmount != null ? `${p.actualAmount.toLocaleString()}원` : '확정 전'} · {p.paymentStatusLabel ?? '정산 대기'}
            </div>
            <div className="text-xs text-ink/60">
              거래 상태: {p.tradeStatusLabel ?? '거래 전'}
            </div>
            {p.pickupPlace && (
              <div className="text-xs text-ink/60">
                픽업: {p.pickupPlace} · {p.pickupTimeLabel ?? '미정'}
              </div>
            )}
            <div className="text-xs text-ink/60">
              마감: {p.deadlineLabel ?? p.deadline ?? '미정'}
            </div>
            {(chatUnreadMap[p.id] ?? 0) > 0 && (
              <div className="text-xs font-semibold text-mint-700">새 메시지 {chatUnreadMap[p.id]}개</div>
            )}
            <div className="flex gap-2">
              <button
                onClick={() =>
                  navigate(`/parties/${p.id}`, {
                    state: { party: p, fromMyParties: true },
                  })
                }
                className="btn-secondary flex-1"
              >
                상세 보기
              </button>
              <button
                onClick={() => navigate(`/chat/${p.id}`)}
                className="btn-secondary flex-1"
              >
                채팅
              </button>
              {canCancel && p.status !== 'CLOSED' && (
                <button
                  onClick={() => handleCancel(p.id)}
                  disabled={cancellingId === p.id}
                  className="btn-ghost flex-1"
                >
                  {cancellingId === p.id ? '취소중...' : '취소'}
                </button>
              )}
            </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default MyParties;
