import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Package, ArrowLeft } from 'lucide-react';
import { useToast } from '../context/ToastContext';

function JoinParty() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthed } = useAuth();
  const { addToast } = useToast();
  const detail = useLocation().state?.detail;
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const targetQuantity = detail?.targetQuantity ?? 0;
  const currentQuantity = detail?.currentQuantity ?? 0;
  const remaining = Math.max(0, targetQuantity - currentQuantity);
  const perUnit = detail?.targetQuantity ? Math.round((detail.totalPrice || 0) / detail.targetQuantity) : 0;
  const expectedPrice = perUnit * (quantity || 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isAuthed) {
      addToast('로그인이 필요합니다.', 'error');
      navigate('/login');
      return;
    }
    const req = Math.max(1, Math.min(quantity, remaining || quantity));
    try {
      setSubmitting(true);
      setError('');
      await api.joinParty({ partyId: Number(id), quantity: req });
      addToast('참여가 완료되었습니다.', 'success');
      navigate(`/parties/${id}`, { replace: true });
    } catch (err) {
      const msg = err.message || '참여 중 오류가 발생했습니다.';
      setError(msg);
      addToast(msg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-4">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-sm font-semibold text-mint-700"
      >
        <ArrowLeft size={16} /> 뒤로
      </button>

      <div className="glass-panel rounded-2xl p-5 space-y-3">
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
              className="mt-1 w-full rounded-xl border border-mint-100 bg-white px-4 py-3 text-sm shadow-sm outline-none ring-mint-200 focus:ring-2"
            />
          </label>
          <div className="rounded-xl bg-mint-500/10 px-4 py-3 text-sm font-semibold text-mint-800 flex items-center justify-between">
            <span>예상 부담금</span>
            <span className="text-lg font-bold text-mint-700">{expectedPrice.toLocaleString()}원</span>
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-mint-500 px-4 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-mint-600 disabled:cursor-not-allowed disabled:bg-ink/20"
          >
            {submitting ? '참여중...' : '참여 확정'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default JoinParty;
