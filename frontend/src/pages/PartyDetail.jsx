import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { Package, Users, Clock3, ArrowLeft, Link as LinkIcon, Copy, ExternalLink } from 'lucide-react';
import { LoadingState } from '../components/Feedback';
import { mergeRealtimeParty, normalizePartyDetail } from '../utils/party';
import { subscribeToPartyStream } from '../utils/partyRealtime';

function PartyDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const stateParty = useMemo(() => normalizePartyDetail(location.state?.party), [location.state]);
  const fromMyParties = location.state?.fromMyParties;
  const [detail, setDetail] = useState(stateParty || null);
  const [loading, setLoading] = useState(!stateParty);
  const [error, setError] = useState('');
  const [realtimeState, setRealtimeState] = useState('connecting');

  useEffect(() => {
    let active = true;
    const fetchDetail = async () => {
      if (!id) return;
      try {
        setLoading(true);
        setError('');
        const data = await api.getPartyDetail(id);
        if (active) {
          setDetail(normalizePartyDetail(data));
        }
      } catch (e) {
        if (active) {
          setError('파티 정보를 불러오지 못했습니다.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };
    if (!stateParty) {
      fetchDetail();
    }

    const unsubscribe = subscribeToPartyStream({
      partyId: id ? Number(id) : null,
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
        setDetail((current) => mergeRealtimeParty(current, event));
      },
      onFallback: () => {
        if (active) {
          fetchDetail();
        }
      },
    });

    return () => {
      active = false;
      unsubscribe?.();
    };
  }, [id, stateParty]);

  const perUnit = useMemo(() => {
    if (!detail) return 0;
    const qty = detail.targetQuantity ?? detail.totalQuantity;
    if (!qty) return 0;
    return detail.totalPrice ? Math.round(detail.totalPrice / qty) : 0;
  }, [detail]);

  const remaining = useMemo(() => {
    if (!detail) return 0;
    return Math.max(0, (detail.targetQuantity ?? 0) - (detail.currentQuantity ?? 0));
  }, [detail]);
  const isWaitingParty = detail.participationStatus === 'WAITING';
  const isClosedParty = detail.status === 'closed';
  const isJoinable = !fromMyParties && detail.status !== 'full' && !isClosedParty && remaining > 0;

  if (loading) return <LoadingState />;
  if (error) return <p className="text-sm text-red-600">{error}</p>;
  if (!detail) return <p className="text-sm text-ink/60">파티 정보를 찾을 수 없습니다.</p>;

  return (
    <div className="space-y-4">
      <button
        onClick={() => navigate(-1)}
        className="btn-ghost px-0"
      >
        <ArrowLeft size={16} /> 뒤로
      </button>

      <div className="card-elevated p-5 space-y-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">
          {detail.status === 'closed' ? '거래 종료' : detail.status === 'full' ? '모집 완료' : '모집 중'}
        </p>
        <p className="text-xs text-ink/50">
          {realtimeState === 'reconnecting' ? '실시간 연결을 다시 시도하는 중입니다.' : '실시간 모집 현황을 반영하고 있습니다.'}
        </p>
        <h1 className="text-xl font-semibold text-ink">{detail.title}</h1>
        <p className="section-subtitle">{detail.storeName}</p>
        {detail.productName && (
          <p className="text-sm font-semibold text-ink">
            제품명: <span className="text-ink/80">{detail.productName}</span>
          </p>
        )}
        <div className="grid gap-3 text-sm text-ink/80">
          <div className="flex items-center gap-2">
            <Package size={16} className="text-mint-700" />
            <span>총 수량 {detail.targetQuantity}개 · 현재 {detail.currentQuantity}개</span>
          </div>
          <div className="flex items-center gap-2">
            <Users size={16} className="text-mint-700" />
            <span>개당 가격 {perUnit.toLocaleString()}원</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock3 size={16} className="text-mint-700" />
            <span>마감: {detail.deadlineLabel}</span>
          </div>
          {detail.openChatUrl && (
            <div className="flex items-center gap-2">
              <LinkIcon size={16} className="text-mint-700" />
              <span className="truncate text-ink/80">{detail.openChatUrl}</span>
              <button
                onClick={() => navigator.clipboard.writeText(detail.openChatUrl)}
                className="btn-pill px-2 py-1 text-[11px]"
              >
                <Copy size={12} />
                복사
              </button>
              <a
                href={detail.openChatUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-secondary px-2 py-1 text-[11px]"
              >
                <ExternalLink size={12} />
                열기
              </a>
            </div>
          )}
        </div>
      </div>

      <div className="card-elevated p-5 space-y-3">
        <h2 className="section-title">참여</h2>
        <p className="section-subtitle">
          요청 수량을 선택하고 참여하세요. 남은 수량 {remaining}개 · 개당 {perUnit.toLocaleString()}원
        </p>
        {isJoinable && (
          <button
            onClick={() => navigate(`/parties/${detail.partyId}/join`, { state: { detail } })}
            className="btn-primary w-full"
          >
            참여하기
          </button>
        )}
        {!fromMyParties && !isJoinable && (
          <div className="rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink/70">
            {isClosedParty ? '이미 종료된 파티입니다.' : '모집이 마감된 파티입니다.'}
          </div>
        )}
        {fromMyParties && !isWaitingParty && (
          <div className="rounded-xl border border-mint-100 bg-white px-4 py-3 text-sm text-ink/70">
            이미 참여 중인 파티입니다.
          </div>
        )}
        {fromMyParties && isWaitingParty && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            대기열 {detail.waitingPosition ?? '-'}번입니다. 빈 자리가 생기면 자동으로 승격됩니다.
          </div>
        )}
      </div>
    </div>
  );
}

export default PartyDetail;
