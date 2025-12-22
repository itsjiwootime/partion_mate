import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { MapPin, Package, Users, Clock3, ArrowLeft, Link as LinkIcon, Copy, ExternalLink } from 'lucide-react';

function PartyDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const stateParty = location.state?.party;
  const fromMyParties = location.state?.fromMyParties;
  const [detail, setDetail] = useState(stateParty || null);
  const [loading, setLoading] = useState(!stateParty);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchDetail = async () => {
      if (!id || stateParty) return;
      try {
        setLoading(true);
        const data = await api.getPartyDetail(id);
        setDetail({
          partyId: data.id,
          title: data.title,
          totalPrice: data.totalPrice,
          currentQuantity: data.currentQuantity ?? 0,
          targetQuantity: data.totalQuantity,
          deadlineLabel: data.deadline ?? '미정',
          rating: data.hostRating ?? 4.5,
          status: data.status === 'FULL' ? 'full' : 'active',
          storeName: data.storeName,
          productName: data.productName ?? data.title,
        });
      } catch (e) {
        setError('파티 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
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

  if (loading) return <p className="text-sm text-ink/60">불러오는 중...</p>;
  if (error) return <p className="text-sm text-red-600">{error}</p>;
  if (!detail) return <p className="text-sm text-ink/60">파티 정보를 찾을 수 없습니다.</p>;

  return (
    <div className="space-y-4">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-sm font-semibold text-mint-700"
      >
        <ArrowLeft size={16} /> 뒤로
      </button>

      <div className="glass-panel rounded-2xl p-5 space-y-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">
          {detail.status === 'full' ? '모집 완료' : '모집 중'}
        </p>
        <h1 className="text-xl font-semibold text-ink">{detail.title}</h1>
        <p className="text-sm text-ink/60">{detail.storeName}</p>
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
                className="flex items-center gap-1 rounded-full bg-mint-500/15 px-2 py-1 text-[11px] font-semibold text-mint-700 hover:bg-mint-500/25"
              >
                <Copy size={12} />
                복사
              </button>
              <a
                href={detail.openChatUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1 rounded-full bg-ink/5 px-2 py-1 text-[11px] font-semibold text-ink hover:bg-ink/10"
              >
                <ExternalLink size={12} />
                열기
              </a>
            </div>
          )}
        </div>
      </div>

      <div className="glass-panel rounded-2xl p-5 space-y-3">
        <h2 className="text-lg font-semibold text-ink">참여</h2>
        <p className="text-sm text-ink/60">
          요청 수량을 선택하고 참여하세요. 남은 수량 {remaining}개 · 개당 {perUnit.toLocaleString()}원
        </p>
        {!fromMyParties && (
          <button
            onClick={() => navigate(`/parties/${detail.partyId}/join`, { state: { detail } })}
            className="w-full rounded-xl bg-mint-500 px-4 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-mint-600"
          >
            참여하기
          </button>
        )}
        {fromMyParties && (
          <div className="rounded-xl border border-mint-100 bg-white px-4 py-3 text-sm text-ink/70">
            이미 참여 중인 파티입니다.
          </div>
        )}
      </div>
    </div>
  );
}

export default PartyDetail;
