import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PartyCard from '../components/PartyCard';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import SectionHeader from '../components/SectionHeader';
import { LoadingState, EmptyState } from '../components/Feedback';

function PartyList() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthed } = useAuth();
  const isAll = id === undefined;
  const branchName = useMemo(() => (isAll ? '전체 파티' : id ?? '지점 선택'), [id, isAll]);
  const [storeInfo, setStoreInfo] = useState(null);
  const [parties, setParties] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
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
        setStoreInfo(data);
      } catch {
        setStoreInfo(null);
      }
    };
    const fetchParties = async () => {
      try {
        setLoading(true);
        setError('');
        const data = isAll ? await api.getAllParties() : await api.getStoreParties(numericId);
        // 백엔드 응답에 currentQuantity 정보가 없으면 0으로 표시
        const normalized = data.map((p) => ({
          partyId: p.id,
          title: p.title,
          totalPrice: p.totalPrice,
          currentQuantity: p.currentQuantity ?? 0,
          targetQuantity: p.totalQuantity,
          deadlineLabel: p.deadline ?? '미정',
          rating: p.hostRating ?? 4.5,
          status: p.status === 'FULL' ? 'full' : 'active',
          storeName: p.storeName,
          raw: p,
        }));
        setParties(normalized);
      } catch (e) {
        setError('파티 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    fetchStore();
    fetchParties();
  }, [id, isAll]);

  return (
    <div className="space-y-4">
      <SectionHeader
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
              className="whitespace-nowrap rounded-full bg-mint-500 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-mint-600"
            >
              파티 만들기
            </button>
          )
        }
      />

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
                className="rounded-full bg-mint-500 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-mint-600"
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
