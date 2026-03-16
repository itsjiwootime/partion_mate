import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PartyCard from '../components/PartyCard';
import { api } from '../api/client';
import SectionHeader from '../components/SectionHeader';
import { LoadingState, EmptyState } from '../components/Feedback';
import { applyPartyListRealtimeUpdate, normalizePartySummary } from '../utils/party';
import { subscribeToPartyStream } from '../utils/partyRealtime';

function PartyList() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isAll = id === undefined;
  const branchName = useMemo(() => (isAll ? '전체 파티' : id ?? '지점 선택'), [id, isAll]);
  const [storeInfo, setStoreInfo] = useState(null);
  const [parties, setParties] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [realtimeState, setRealtimeState] = useState('connecting');

  useEffect(() => {
    let active = true;
    const numericId = id ? Number(id) : null;
    if (id && Number.isNaN(numericId)) {
      setError('유효하지 않은 지점 ID입니다.');
      return;
    }
    const fetchStore = async () => {
      if (!numericId) {
        setStoreInfo(null);
        return;
      }
      try {
        const data = await api.getStoreDetail(numericId);
        if (active) {
          setStoreInfo(data);
        }
      } catch {
        if (active) {
          setStoreInfo(null);
        }
      }
    };
    const fetchParties = async () => {
      try {
        setLoading(true);
        setError('');
        const data = isAll ? await api.getAllParties() : await api.getStoreParties(numericId);
        const normalized = data.map(normalizePartySummary);
        if (active) {
          setParties(normalized);
        }
      } catch (e) {
        if (active) {
          setError('파티 목록을 불러오지 못했습니다.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };
    fetchStore();
    fetchParties();

    const unsubscribe = subscribeToPartyStream({
      storeId: isAll ? null : numericId,
      onConnected: () => {
        if (active) {
          setRealtimeState('live');
        }
      },
      onReconnectStateChange: (state) => {
        if (active) {
          setRealtimeState(state);
        }
      },
      onPartyUpdated: (event) => {
        if (!active) return;
        setParties((current) =>
          applyPartyListRealtimeUpdate(current, event, {
            storeId: isAll ? null : numericId,
          }),
        );
      },
      onFallback: () => {
        if (active) {
          fetchParties();
        }
      },
    });

    return () => {
      active = false;
      unsubscribe?.();
    };
  }, [id, isAll]);

  return (
    <div className="space-y-4">
      <SectionHeader
        eyebrow={isAll ? '파티' : '지점'}
        title={storeInfo ? storeInfo.name : branchName}
        subtitle={isAll ? '모든 지점의 파티를 확인하세요.' : '해당 지점에서 진행 중인 파티를 확인하세요.'}
        meta={
          storeInfo
            ? [
                <div key="addr">{storeInfo.address}</div>,
                <div key="time">
                  영업시간: {storeInfo.openTime} ~ {storeInfo.closeTime} · 연락처: {storeInfo.phone}
                </div>,
              ]
            : null
        }
        action={
          id && (
            <button
              onClick={() => navigate(`/parties/create?storeId=${id}`)}
              className="btn-primary whitespace-nowrap px-4 py-2 text-sm"
            >
              파티 만들기
            </button>
          )
        }
      />

      <p className="text-xs text-ink/50">
        {realtimeState === 'reconnecting' ? '실시간 연결을 다시 시도하는 중입니다.' : '실시간 모집 현황을 반영하고 있습니다.'}
      </p>

      {loading && <LoadingState />}
      {error && <p className="text-sm text-red-600">{error}</p>}
      {!loading && !error && parties.length === 0 && (
        <EmptyState
          title="아직 등록된 파티가 없어요!"
          description="새로운 파티를 만들어보거나 다른 지점을 살펴보세요."
          action={
            id && (
              <button
                onClick={() => navigate(`/parties/create?storeId=${id}`)}
                className="btn-primary px-4 py-2 text-sm"
              >
                파티 만들기
              </button>
            )
          }
        />
      )}
      <div className="grid gap-3 md:grid-cols-2">
        {parties.map((party) => (
          <PartyCard
            key={party.partyId}
            partyId={party.partyId}
            {...party}
            onViewDetail={() => navigate(`/parties/${party.partyId}`, { state: { party } })}
          />
        ))}
      </div>
    </div>
  );
}

export default PartyList;
