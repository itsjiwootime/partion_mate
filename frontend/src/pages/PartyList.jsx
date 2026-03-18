import { useEffect, useMemo, useState } from 'react';
import { Search, SlidersHorizontal, Sparkles } from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import PartyCard from '../components/PartyCard';
import { api } from '../api/client';
import SectionHeader from '../components/SectionHeader';
import { LoadingState, EmptyState } from '../components/Feedback';
import { applyPartyListRealtimeUpdate, normalizePartySummary } from '../utils/party';
import {
  buildPartyDiscoverySearch,
  buildDiscoverySections,
  filterParties,
  getPartySortHighlight,
  hasActivePartyDiscoveryFilters,
  parsePartyDiscoveryFilters,
  PARTY_STATUS_FILTERS,
  PARTY_SORT_OPTIONS,
  PARTY_STORAGE_FILTERS,
  PARTY_UNIT_FILTERS,
  sortParties,
  summarizePartyDiscoveryFilters,
} from '../utils/partyDiscovery';
import { subscribeToPartyStream } from '../utils/partyRealtime';
import { useAuth } from '../context/AuthContext';

function PartyList() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthed } = useAuth();
  const isAll = id === undefined;
  const branchName = useMemo(() => (isAll ? '전체 파티' : id ?? '지점 선택'), [id, isAll]);
  const [storeInfo, setStoreInfo] = useState(null);
  const [parties, setParties] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [realtimeState, setRealtimeState] = useState('connecting');
  const [chatUnreadMap, setChatUnreadMap] = useState({});
  const filters = useMemo(() => parsePartyDiscoveryFilters(searchParams), [searchParams]);
  const filterSummary = useMemo(() => summarizePartyDiscoveryFilters(filters), [filters]);
  const hasActiveFilters = useMemo(() => hasActivePartyDiscoveryFilters(filters), [filters]);
  const filteredParties = useMemo(() => filterParties(parties, filters), [parties, filters]);
  const sortedParties = useMemo(() => sortParties(filteredParties, filters.sort), [filteredParties, filters.sort]);
  const discoverySections = useMemo(() => buildDiscoverySections(filteredParties), [filteredParties]);
  const sortLabel = useMemo(
    () => PARTY_SORT_OPTIONS.find((option) => option.value === filters.sort)?.label ?? '추천순',
    [filters.sort],
  );

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
    const fetchChatRooms = async () => {
      if (!isAuthed) {
        setChatUnreadMap({});
        return;
      }
      try {
        const rooms = await api.getMyChatRooms();
        if (active) {
          setChatUnreadMap(
            rooms.reduce((acc, room) => {
              acc[room.partyId] = room.unreadCount ?? 0;
              return acc;
            }, {}),
          );
        }
      } catch {
        if (active) {
          setChatUnreadMap({});
        }
      }
    };
    fetchStore();
    fetchParties();
    fetchChatRooms();

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
  }, [id, isAll, isAuthed]);

  const updateFilters = (patch) => {
    const nextFilters = { ...filters, ...patch };
    setSearchParams(buildPartyDiscoverySearch(nextFilters), { replace: true });
  };

  const resetFilters = () => {
    setSearchParams(new URLSearchParams(), { replace: true });
  };

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

      <section className="card-elevated p-4 space-y-4">
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <SlidersHorizontal size={16} />
              탐색 필터
            </div>
            <p className="section-subtitle">지점명, 상품명, 파티 제목으로 검색하고 상태와 소분 조건으로 좁혀보세요.</p>
          </div>
          {hasActiveFilters && (
            <button onClick={resetFilters} className="btn-secondary px-3 py-2 text-xs">
              필터 초기화
            </button>
          )}
        </div>

        <div className="input-row">
          <Search size={16} className="text-mint-700" />
          <input
            value={filters.query}
            onChange={(event) => updateFilters({ query: event.target.value })}
            className="input-control"
            placeholder="파티 제목, 상품명, 지점명 검색"
            aria-label="파티 검색"
          />
        </div>

        <div className="space-y-2">
          <p className="helper-text">상태</p>
          <div className="flex flex-wrap gap-2">
            {PARTY_STATUS_FILTERS.map((option) => (
              <button
                key={option.value}
                onClick={() => updateFilters({ status: option.value })}
                className={[
                  'btn-pill',
                  filters.status === option.value ? 'bg-mint-500 text-white hover:bg-mint-600' : '',
                ].join(' ')}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <label className="space-y-2">
            <span className="helper-text">보관 방식</span>
            <select
              value={filters.storage}
              onChange={(event) => updateFilters({ storage: event.target.value })}
              className="input"
              aria-label="보관 방식 필터"
            >
              {PARTY_STORAGE_FILTERS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-2">
            <span className="helper-text">소분 단위</span>
            <select
              value={filters.unit}
              onChange={(event) => updateFilters({ unit: event.target.value })}
              className="input"
              aria-label="소분 단위 필터"
            >
              {PARTY_UNIT_FILTERS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="flex flex-wrap items-center gap-2 text-xs text-ink/55">
          <span>전체 {parties.length}개 중 {filteredParties.length}개 표시</span>
          {filterSummary.map((item) => (
            <span key={item} className="badge bg-ink/5 text-ink/70">
              {item}
            </span>
          ))}
        </div>
      </section>

      <section className="card-elevated p-4 space-y-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <Sparkles size={16} />
              발견 섹션
            </div>
            <p className="section-subtitle">마감 임박, 인기, 신규 관점으로 빠르게 훑고 원하는 기준으로 다시 정렬하세요.</p>
          </div>
          <label className="space-y-2 lg:w-56">
            <span className="helper-text">정렬 방식</span>
            <select
              value={filters.sort}
              onChange={(event) => updateFilters({ sort: event.target.value })}
              className="input"
              aria-label="정렬 방식"
            >
              {PARTY_SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="grid gap-3 lg:grid-cols-3">
          {discoverySections.map((section) => {
            const featured = section.featuredParty;
            const selected = filters.sort === section.sort;

            return (
              <button
                key={section.key}
                type="button"
                onClick={() => updateFilters({ sort: section.sort })}
                className={[
                  'card p-4 text-left transition',
                  selected ? 'border-mint-300 shadow-md ring-1 ring-mint-200' : 'hover:-translate-y-1 hover:shadow-md',
                ].join(' ')}
              >
                <div className="flex items-center justify-between gap-3">
                  <span
                    className={[
                      'badge',
                      section.key === 'deadline'
                        ? 'bg-amber-100 text-amber-900'
                        : section.key === 'popular'
                          ? 'bg-sky-100 text-sky-800'
                          : 'bg-mint-500/15 text-mint-700',
                    ].join(' ')}
                  >
                    {section.label}
                  </span>
                  <span className="text-xs text-ink/45">{section.parties.length}개 후보</span>
                </div>

                {featured ? (
                  <>
                    <p className="mt-3 text-base font-semibold text-ink">{featured.title}</p>
                    <p className="mt-1 text-xs text-ink/50">{featured.storeName}</p>
                    <p className="mt-3 text-sm text-ink/70">
                      {section.key === 'deadline'
                        ? `마감 ${featured.deadlineLabel ?? '미정'}`
                        : section.key === 'popular'
                          ? `달성률 ${Math.min(100, Math.round(((featured.currentQuantity ?? 0) / (featured.targetQuantity || 1)) * 100))}%`
                          : `최소 ${featured.minimumShareUnit}${featured.unitLabel ?? '개'}부터 참여 가능`}
                    </p>
                  </>
                ) : (
                  <p className="mt-3 text-sm text-ink/55">{section.emptyDescription}</p>
                )}

                <div className="mt-4 text-xs font-semibold text-mint-700">{selected ? `${sortLabel} 적용 중` : `${section.label} 보기`}</div>
              </button>
            );
          })}
        </div>

        <div className="flex flex-wrap items-center gap-2 text-xs text-ink/55">
          <span>{sortLabel}으로 정렬 중</span>
          <span className="badge bg-mint-500/10 text-mint-800">상위 3개 카드 강조</span>
        </div>
      </section>

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
      {!loading && !error && parties.length > 0 && filteredParties.length === 0 && (
        <EmptyState
          title="조건에 맞는 파티가 없어요"
          description="검색어를 바꾸거나 필터를 초기화해서 다른 파티를 찾아보세요."
          action={
            <button onClick={resetFilters} className="btn-secondary px-4 py-2 text-sm">
              필터 초기화
            </button>
          }
        />
      )}
      <div className="grid gap-3 md:grid-cols-2">
        {sortedParties.map((party, index) => {
          const highlight = getPartySortHighlight(filters.sort, index);

          return (
            <PartyCard
              key={party.partyId}
              partyId={party.partyId}
              chatUnreadCount={chatUnreadMap[party.partyId] ?? 0}
              discoveryBadgeLabel={highlight?.label}
              discoveryBadgeTone={highlight?.tone}
              {...party}
              onViewDetail={() => navigate(`/parties/${party.partyId}`, { state: { party } })}
            />
          );
        })}
      </div>
    </div>
  );
}

export default PartyList;
