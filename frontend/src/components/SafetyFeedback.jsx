import { AlertTriangle, ShieldAlert } from 'lucide-react';

const toneClassByType = {
  info: 'border-mint-100 bg-mint-50 text-mint-950',
  caution: 'border-amber-200 bg-amber-50 text-amber-950',
  danger: 'border-rose-200 bg-rose-50 text-rose-950',
};

export function SafetyStatusBanner({ title, description, tone = 'info', action, secondaryAction }) {
  const toneClass = toneClassByType[tone] ?? toneClassByType.info;

  return (
    <div className={`rounded-2xl border px-4 py-3 text-sm ${toneClass}`}>
      <div className="flex items-start gap-3">
        <div className="mt-0.5">
          <ShieldAlert size={16} />
        </div>
        <div className="min-w-0 flex-1 space-y-1">
          <p className="font-semibold">{title}</p>
          <p className="leading-6 opacity-85">{description}</p>
          {(action || secondaryAction) && (
            <div className="flex flex-wrap gap-2 pt-1">
              {action}
              {secondaryAction}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function SafetyFallbackCard({ title, description, action, secondaryAction }) {
  return (
    <div className="rounded-3xl border border-amber-200 bg-amber-50 px-5 py-6 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-amber-500/15 text-amber-700">
        <AlertTriangle size={22} />
      </div>
      <p className="mt-4 text-base font-semibold text-ink">{title}</p>
      <p className="mt-2 text-sm leading-6 text-ink/70">{description}</p>
      {(action || secondaryAction) && (
        <div className="mt-4 flex flex-wrap justify-center gap-2">
          {action}
          {secondaryAction}
        </div>
      )}
    </div>
  );
}
