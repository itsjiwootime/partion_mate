function SectionHeader({ title, subtitle, meta, action }) {
  return (
    <div className="glass-panel rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">{title}</p>
          {subtitle && <p className="text-sm text-ink/70">{subtitle}</p>}
          {meta && <div className="text-xs text-ink/60 space-y-0.5">{meta}</div>}
        </div>
        {action}
      </div>
    </div>
  );
}

export default SectionHeader;
