import { useMemo } from 'react';
import { CircleDot, Clock3, Star, ArrowRight } from 'lucide-react';

function formatCurrency(value) {
  return `${value.toLocaleString('ko-KR')}원`;
}

function PartyCard({
  partyId,
  title,
  totalPrice,
  currentQuantity,
  targetQuantity,
  deadlineLabel,
  rating,
  status = 'active',
  onViewDetail,
}) {
  const isClosed = status === 'closed';
  const isFull = status === 'full' || currentQuantity >= targetQuantity;
  const isActive = !isFull && !isClosed;

  const perPerson = useMemo(
    () => (targetQuantity > 0 ? Math.round(totalPrice / targetQuantity) : 0),
    [targetQuantity, totalPrice],
  );

  const progress = useMemo(
    () => Math.min(100, Math.round((currentQuantity / targetQuantity) * 100)),
    [currentQuantity, targetQuantity],
  );

  return (
    <article className="card card-hover p-4">
      <header className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-ink/60">공동구매</p>
          <h3 className="mt-1 text-lg font-semibold text-ink">{title}</h3>
        </div>
        <span
          className={[
            'badge',
            isActive
              ? 'bg-mint-500/15 text-mint-700'
              : isClosed
                ? 'bg-red-50 text-red-700'
                : 'bg-ink/10 text-ink/60',
          ].join(' ')}
        >
          <CircleDot size={14} />
          {isActive ? '모집 중' : isClosed ? '종료' : '마감'}
        </span>
      </header>

      <div className="mt-3 grid gap-2 text-sm text-ink/70">
        <div className="flex items-center justify-between">
          <span>총 가격</span>
          <span className="font-semibold text-ink">{formatCurrency(totalPrice)}</span>
        </div>
        <div className="flex items-center justify-between">
          <span>수량(요청/총)</span>
          <span className="font-semibold text-ink">
            {currentQuantity} / {targetQuantity}개
          </span>
        </div>
        <div className="flex items-center justify-between text-ink">
          <span>개당 가격</span>
          <span className="text-base font-bold text-mint-700">{formatCurrency(perPerson)}</span>
        </div>
      </div>

      <div className="mt-3 h-2 overflow-hidden rounded-full bg-ink/10">
        <div
          className={`h-full ${isActive ? 'bg-mint-500' : isClosed ? 'bg-red-300' : 'bg-ink/30'}`}
          style={{ width: `${progress}%` }}
        />
      </div>
      <div className="mt-2 flex items-center justify-between text-xs text-ink/60">
        <span>달성률 {progress}%</span>
        <span>
          {currentQuantity}/{targetQuantity}개
        </span>
      </div>

      <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <footer className="flex items-center gap-3 text-xs text-ink/60">
          <div className="flex items-center gap-1">
            <Clock3 size={14} />
            <span>마감: {deadlineLabel}</span>
          </div>
          <div className="flex items-center gap-1 font-semibold text-ink">
            <Star size={14} className="text-mint-600" />
            <span>{rating.toFixed(1)}</span>
          </div>
        </footer>

        <button onClick={onViewDetail} className="btn-primary px-4 py-2 text-sm">
          상세보기
          <ArrowRight size={16} />
        </button>
      </div>
    </article>
  );
}

export default PartyCard;
