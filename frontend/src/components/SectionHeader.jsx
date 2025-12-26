function SectionHeader({ title, subtitle, meta, action, eyebrow = '지점' }) {
  return (
    <div className="card-elevated p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">{eyebrow}</p>
          <h2 className="section-title">{title}</h2>
          {subtitle && <p className="section-subtitle">{subtitle}</p>}
          {meta && <div className="helper-text space-y-0.5">{meta}</div>}
        </div>
        {action}
      </div>
    </div>
  );
}

export default SectionHeader;
