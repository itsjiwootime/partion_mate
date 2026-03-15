import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Package, ArrowLeft } from 'lucide-react';
import { useToast } from '../context/ToastContext';
import { LoadingState } from '../components/Feedback';
import { normalizePartyDetail } from '../utils/party';

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

  useEffect(() => {
    const fetchDetail = async () => {
      if (!id || stateDetail) return;
      try {
        setLoading(true);
        setError('');
        const data = await api.getPartyDetail(id);
        setDetail(normalizePartyDetail(data));
      } catch (err) {
        setError('파티 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [id, stateDetail]);

  const targetQuantity = detail?.targetQuantity ?? 0;
  const currentQuantity = detail?.currentQuantity ?? 0;
  const remaining = Math.max(0, targetQuantity - currentQuantity);
  const perUnit = detail?.targetQuantity ? Math.round((detail.totalPrice || 0) / detail.targetQuantity) : 0;
  const expectedPrice = perUnit * (quantity || 0);
  const isJoinClosed = !detail || remaining <= 0 || detail.status === 'full';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isAuthed) {
      addToast('로그인이 필요합니다.', 'error');
      navigate('/login');
      return;
    }
    if (isJoinClosed) {
      setError('모집이 마감된 파티입니다.');
      return;
    }
    const req = Math.max(1, Math.min(quantity, remaining || quantity));
    try {
      setSubmitting(true);
      setError('');
      const result = await api.joinParty({ partyId: Number(id), quantity: req });
      const message = result?.message ?? '참여가 완료되었습니다.';
      addToast(message, 'success');

      if (result?.joinStatus === 'WAITING') {
        navigate('/my-parties', { replace: true });
        return;
      }

      navigate(`/parties/${id}`, { replace: true });
    } catch (err) {
      const msg = err.message || '참여 중 오류가 발생했습니다.';
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
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-3">
          <label className="block text-sm text-ink/70">
            요청 수량
            <input
              type="number"
              min="1"
              max={remaining || undefined}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value) || 1)}
              disabled={isJoinClosed}
              className="input mt-1"
            />
          </label>
          <div className="rounded-xl bg-mint-500/10 px-4 py-3 text-sm font-semibold text-mint-800 flex items-center justify-between">
            <span>예상 부담금</span>
            <span className="text-lg font-bold text-mint-700">{expectedPrice.toLocaleString()}원</span>
          </div>
          {isJoinClosed && <p className="text-sm text-ink/60">모집이 마감된 파티입니다.</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={submitting || isJoinClosed}
            className="btn-primary w-full"
          >
            {submitting ? '참여중...' : '참여 확정'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default JoinParty;
