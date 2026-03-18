import { useCallback, useEffect, useRef, useState } from 'react';
import { Heart } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import PartyCard from '../components/PartyCard';
import { EmptyState, LoadingState } from '../components/Feedback';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { applyPartyListRealtimeUpdate, normalizePartySummary } from '../utils/party';
import { subscribeToPartyStream } from '../utils/partyRealtime';

function FavoriteParties() {
  const { isAuthed } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [parties, setParties] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [realtimeState, setRealtimeState] = useState('connecting');
  const [chatUnreadMap, setChatUnreadMap] = useState({});
  const [favoriteLoadingId, setFavoriteLoadingId] = useState(null);
  const partyIdsRef = useRef(new Set());

  const returnTo = `${location.pathname}${location.search}`;

  const fetchData = useCallback(async (activeRef = { current: true }) => {
    try {
      setLoading(true);
      setError('');
      const data = await api.getFavoriteParties();
      if (activeRef.current) {
        setParties(data.map(normalizePartySummary));
      }
    } catch (e) {
      if (activeRef.current) {
        setError('관심 파티를 불러오지 못했습니다.');
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
    partyIdsRef.current = new Set(parties.map((party) => party.partyId));
  }, [parties]);

  useEffect(() => {
    if (!isAuthed) {
      return;
    }

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
        const eventPartyId = event.partyId ?? event.id;
        if (!activeRef.current || !partyIdsRef.current.has(eventPartyId)) {
          return;
        }
        setParties((current) => applyPartyListRealtimeUpdate(current, event));
        fetchChatRooms(activeRef);
      },
      onFallback: () => {
        if (activeRef.current) {
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

  const handleRemoveFavorite = async (partyId) => {
    if (!isAuthed) {
      navigate('/login', { state: { from: returnTo } });
      return;
    }

    setFavoriteLoadingId(partyId);
    try {
      await api.removeFavoriteParty(partyId);
      setParties((current) => current.filter((party) => party.partyId !== partyId));
      addToast('관심 파티에서 제거했습니다.', 'success');
    } catch (e) {
      addToast(e?.message || '관심 파티를 해제하지 못했습니다.', 'error');
    } finally {
      setFavoriteLoadingId(null);
    }
  };

  if (!isAuthed) {
    return (
      <div className="space-y-3">
        <p className="section-subtitle">로그인 후 저장한 관심 파티를 다시 확인할 수 있습니다.</p>
        <button onClick={() => navigate('/login', { state: { from: returnTo } })} className="btn-primary px-4 py-2 text-sm">
          로그인 하러 가기
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="card-elevated p-4 space-y-3">
        <div className="flex items-center gap-2">
          <Heart size={18} className="text-rose-500" />
          <h2 className="section-title">저장한 관심 파티</h2>
        </div>
        <p className="section-subtitle">파티를 저장해두면 모집 현황이 바뀌어도 다시 쉽게 확인할 수 있습니다.</p>
        <p className="text-xs text-ink/50">
          {realtimeState === 'reconnecting' ? '실시간 연결을 다시 시도하는 중입니다.' : '저장한 파티의 모집 상태를 실시간으로 반영하고 있습니다.'}
        </p>
      </div>

      {loading && <LoadingState />}
      {error && <p className="text-sm text-red-600">{error}</p>}
      {!loading && error && (
        <button onClick={() => fetchData()} className="btn-secondary px-4 py-2 text-sm">
          다시 불러오기
        </button>
      )}

      {!loading && !error && parties.length === 0 && (
        <EmptyState
          title="저장한 관심 파티가 없어요"
          description="마음에 드는 파티를 저장해두면 여기서 다시 확인할 수 있습니다."
          action={
            <button onClick={() => navigate('/parties')} className="btn-secondary px-4 py-2 text-sm">
              파티 둘러보기
            </button>
          }
        />
      )}

      <div className="grid gap-3 md:grid-cols-2">
        {parties.map((party) => (
          <PartyCard
            key={party.partyId}
            chatUnreadCount={chatUnreadMap[party.partyId] ?? 0}
            favorite
            favoriteBusy={favoriteLoadingId === party.partyId}
            {...party}
            onToggleFavorite={() => handleRemoveFavorite(party.partyId)}
            onViewDetail={() => navigate(`/parties/${party.partyId}`, { state: { party } })}
          />
        ))}
      </div>
    </div>
  );
}

export default FavoriteParties;
