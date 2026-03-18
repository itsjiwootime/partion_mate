import { useEffect, useState } from 'react';
import { AlertTriangle, Flag, ShieldBan } from 'lucide-react';
import { getReportReasonOptions } from '../utils/safety';

function DialogFrame({ children, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/45 px-4 py-8">
      <button type="button" aria-label="닫기" className="absolute inset-0 cursor-default" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        className="relative z-10 w-full max-w-lg rounded-3xl border border-mint-100 bg-white p-5 shadow-2xl"
      >
        {children}
      </div>
    </div>
  );
}

export function ReportDialog({ open, title, description, targetType, submitting, onClose, onSubmit }) {
  const reasonOptions = getReportReasonOptions(targetType);
  const [reasonType, setReasonType] = useState(reasonOptions[0]?.type ?? 'OTHER');
  const [memo, setMemo] = useState('');

  useEffect(() => {
    if (!open) {
      return;
    }

    setReasonType(reasonOptions[0]?.type ?? 'OTHER');
    setMemo('');
  }, [open, reasonOptions, targetType]);

  if (!open) {
    return null;
  }

  const handleSubmit = async (event) => {
    event.preventDefault();
    await onSubmit({
      reasonType,
      memo: memo.trim(),
    });
  };

  return (
    <DialogFrame onClose={onClose}>
      <form className="space-y-4" onSubmit={handleSubmit}>
        <div className="flex items-start gap-3">
          <div className="rounded-full bg-rose-500/10 p-3 text-rose-600">
            <Flag size={18} />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-semibold text-ink">{title}</h3>
            <p className="text-sm leading-6 text-ink/65">{description}</p>
          </div>
        </div>

        <div className="space-y-3">
          <p className="text-sm font-semibold text-ink">신고 사유</p>
          <div className="space-y-2">
            {reasonOptions.map((reason) => (
              <label key={reason.type} className="flex items-start gap-3 rounded-2xl border border-ink/10 px-4 py-3">
                <input
                  type="radio"
                  name="report-reason"
                  value={reason.type}
                  checked={reasonType === reason.type}
                  onChange={() => setReasonType(reason.type)}
                  className="mt-1 h-4 w-4 accent-rose-500"
                />
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-ink">{reason.label}</p>
                  <p className="text-sm text-ink/60">{reason.description}</p>
                </div>
              </label>
            ))}
          </div>
        </div>

        <label className="block space-y-2 text-sm text-ink/75">
          <span className="font-semibold text-ink">메모</span>
          <textarea
            rows="4"
            value={memo}
            onChange={(event) => setMemo(event.target.value)}
            className="input min-h-[112px]"
            placeholder="상황을 간단히 적어두면 검토에 도움이 됩니다."
          />
          <span className="block text-right text-xs text-ink/50">{memo.length}/1000</span>
        </label>

        <div className="flex gap-2">
          <button type="button" className="btn-secondary flex-1" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="submit" className="btn-primary flex-1 bg-rose-500 hover:bg-rose-600" disabled={submitting}>
            {submitting ? '접수 중...' : '신고 접수'}
          </button>
        </div>
      </form>
    </DialogFrame>
  );
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  confirmTone = 'warning',
  notice,
  submitting,
  onClose,
  onConfirm,
}) {
  if (!open) {
    return null;
  }

  const confirmClass =
    confirmTone === 'danger'
      ? 'btn-primary flex-1 bg-rose-500 hover:bg-rose-600'
      : 'btn-primary flex-1 bg-amber-500 hover:bg-amber-600';

  const resolvedNotice =
    notice ?? '차단 후에는 서로 같은 파티 참여와 채팅 접근이 제한됩니다. 신고는 차단과 별개로 접수할 수 있습니다.';

  return (
    <DialogFrame onClose={onClose}>
      <div className="space-y-4">
        <div className="flex items-start gap-3">
          <div className="rounded-full bg-amber-500/10 p-3 text-amber-700">
            {confirmTone === 'danger' ? <ShieldBan size={18} /> : <AlertTriangle size={18} />}
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-semibold text-ink">{title}</h3>
            <p className="text-sm leading-6 text-ink/65">{description}</p>
          </div>
        </div>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-ink/75">{resolvedNotice}</div>

        <div className="flex gap-2">
          <button type="button" className="btn-secondary flex-1" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className={confirmClass} onClick={onConfirm} disabled={submitting}>
            {submitting ? '처리 중...' : confirmLabel}
          </button>
        </div>
      </div>
    </DialogFrame>
  );
}
