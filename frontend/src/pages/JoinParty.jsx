import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { SafetyFallbackCard, SafetyStatusBanner } from '../components/SafetyFeedback';
import { useAuth } from '../context/AuthContext';
import { Package, ArrowLeft } from 'lucide-react';
import { useToast } from '../context/ToastContext';
import { LoadingState } from '../components/Feedback';
import { normalizePartyDetail } from '../utils/party';
import { isBlockedPartyInteractionMessage } from '../utils/safety';

function JoinParty() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthed } = useAuth();
  const { addToast } = useToast();
  const location = useLocation();
  const stateDetail = useMemo(() => normalizePartyDetail(location.state?.detail), [location.state]);
  const [detail, setDetail] = useState(stateDetail);
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(!stateDetail);
  const [blockedFeedback, setBlockedFeedback] = useState(null);

  useEffect(() => {
    const fetchDetail = async () => {
      if (!id || stateDetail) return;
      try {
        setLoading(true);
        setError('');
        const data = await api.getPartyDetail(id);
        setDetail(normalizePartyDetail(data));
      } catch (err) {
        if (isBlockedPartyInteractionMessage(err?.message)) {
          setBlockedFeedback({
            title: '차단 관계가 있어 이 파티에 참여할 수 없어요',
            description: '상대 사용자와 차단 관계가 있으면 파티 참여가 제한됩니다. 차단 해제는 프로필의 신뢰·안전 관리에서 할 수 있습니다.',
          });
          return;
        }
        setError('파티 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [id, stateDetail]);

  useEffect(() => {
    if (!detail) return;
    setQuantity((current) => {
      const nextMinimum = Math.max(1, detail.minimumShareUnit ?? 1);
      if (!current || current < nextMinimum) {
        return nextMinimum;
      }
      return current;
    });
  }, [detail]);

  const targetQuantity = detail?.targetQuantity ?? 0;
  const currentQuantity = detail?.currentQuantity ?? 0;
  const remaining = Math.max(0, targetQuantity - currentQuantity);
  const minimumShareUnit = Math.max(1, detail?.minimumShareUnit ?? 1);
  const perUnit = detail?.targetQuantity ? Math.round((detail.totalPrice || 0) / detail.targetQuantity) : 0;
  const expectedPrice = perUnit * (quantity || 0);
  const isJoinClosed = !detail || detail.status === 'closed' || detail.status === 'full' || remaining <= 0;
  const maxRequestQuantity = targetQuantity > 0 ? targetQuantity : undefined;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isAuthed) {
      addToast('로그인이 필요합니다.', 'error');
      navigate('/login', { state: { from: `/parties/${id}/join` } });
      return;
    }
    if (isJoinClosed) {
      setError(!detail || detail.status === 'closed' ? '이미 종료된 파티입니다.' : '잔여 수량이 부족해 참여할 수 없습니다.');
      return;
    }
    const req = Math.max(minimumShareUnit, Math.min(quantity, maxRequestQuantity || quantity));
    try {
      setSubmitting(true);
      setError('');
      setBlockedFeedback(null);
      const result = await api.joinParty({ partyId: Number(id), quantity: req });
      const message = result?.message ?? '참여가 완료되었습니다.';
      addToast(message, 'success');

      navigate(`/parties/${id}`, { replace: true });
    } catch (err) {
      const msg = err.message || '참여 중 오류가 발생했습니다.';
      if (isBlockedPartyInteractionMessage(msg)) {
        setBlockedFeedback({
          title: '차단 관계가 있어 이 파티에 참여할 수 없어요',
          description: '이제 이 파티 참여는 제한됩니다. 차단 해제가 필요하면 프로필의 신뢰·안전 관리로 이동하세요.',
        });
        setError('');
        return;
      }
      setError(msg);
      addToast(msg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingState />;

  return (
    <div className="space-y-4">
      <button
        onClick={() => navigate(-1)}
        className="btn-ghost px-0"
      >
        <ArrowLeft size={16} /> 뒤로
      </button>

      <div className="card-elevated p-5 space-y-3">
        <h1 className="text-xl font-semibold text-ink">참여하기</h1>
        {detail && (
          <div className="text-sm text-ink/70 space-y-1">
            <div className="font-semibold text-ink">{detail.title}</div>
            <div className="flex items-center gap-2">
              <Package size={16} className="text-mint-700" />
              <span>
                총 {targetQuantity}개 중 현재 {currentQuantity}개 (잔여 {remaining}개)
              </span>
            </div>
            <p className="text-xs text-ink/55">
              {`최소 ${minimumShareUnit}${detail.unitLabel ?? '개'} 단위로 참여할 수 있습니다.`}
            </p>
          </div>
        )}
        {blockedFeedback && (
          <SafetyStatusBanner
            title={blockedFeedback.title}
            description={blockedFeedback.description}
            tone="danger"
            action={
              <button
                type="button"
                onClick={() =>
                  navigate('/me', {
                    state: {
                      focusSafetyCenter: true,
                      safetyNotice: {
                        title: '차단 해제는 여기서 할 수 있어요',
                        description: '차단 목록에서 사용자를 해제하면 이후 같은 파티 참여가 다시 가능해질 수 있습니다.',
                      },
                    },
                  })
                }
                className="btn-secondary px-4 py-2 text-xs"
              >
                차단 관리 열기
              </button>
            }
          />
        )}
        {blockedFeedback ? (
          <SafetyFallbackCard
            title="참여 대신 차단 상태를 먼저 확인해 주세요"
            description="차단 관계가 해제되기 전까지는 이 파티에 참여할 수 없습니다."
            action={
              <button
                type="button"
                onClick={() =>
                  navigate('/me', {
                    state: {
                      focusSafetyCenter: true,
                      safetyNotice: {
                        title: '차단 목록을 먼저 확인해 주세요',
                        description: '상대 사용자를 차단한 상태라면 여기서 해제할 수 있습니다.',
                      },
                    },
                  })
                }
                className="btn-secondary px-4 py-2 text-sm"
              >
                차단 관리 열기
              </button>
            }
            secondaryAction={
              <button type="button" onClick={() => navigate('/parties')} className="btn-primary px-4 py-2 text-sm">
                다른 파티 보기
              </button>
            }
          />
        ) : null}
        <form onSubmit={handleSubmit} className="space-y-3" hidden={Boolean(blockedFeedback)}>
          <label className="block text-sm text-ink/70">
            요청 수량
            <input
              type="number"
              min={minimumShareUnit}
              step={minimumShareUnit}
              max={maxRequestQuantity}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value) || minimumShareUnit)}
              disabled={isJoinClosed}
              className="input mt-1"
            />
          </label>
          <div className="rounded-xl bg-mint-500/10 px-4 py-3 text-sm font-semibold text-mint-800 flex items-center justify-between">
            <span>예상 부담금</span>
            <span className="text-lg font-bold text-mint-700">{expectedPrice.toLocaleString()}원</span>
          </div>
          {isJoinClosed && (
            <p className="text-sm text-ink/60">
              {!detail || detail.status === 'closed' ? '이미 종료된 파티입니다.' : '잔여 수량이 부족해 참여할 수 없습니다.'}
            </p>
          )}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={submitting || isJoinClosed}
            className="btn-primary w-full"
          >
            {submitting ? '처리중...' : '참여 확정'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default JoinParty;
