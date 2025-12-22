export function LoadingState({ message = '불러오는 중...' }) {
  return <p className="text-sm text-ink/60">{message}</p>;
}

export function EmptyState({ title, description, action }) {
  return (
    <div className="glass-panel rounded-2xl p-5 text-center text-sm text-ink/60 space-y-2">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-mint-500/15 text-mint-700">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-7 w-7"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth="1.8"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4" />
        </svg>
      </div>
      {title && <p className="font-semibold text-ink">{title}</p>}
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}
