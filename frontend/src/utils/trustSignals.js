const BADGE_BY_LEVEL = {
  TOP: {
    label: '매우 신뢰',
    className: 'bg-mint-600 text-white',
    description: '후기와 완료 거래가 충분히 쌓인 안정적인 거래 상대예요.',
  },
  GOOD: {
    label: '신뢰',
    className: 'bg-mint-100 text-mint-900',
    description: '최근 거래 흐름이 안정적이고 기본 신뢰 지표가 좋아요.',
  },
  NORMAL: {
    label: '보통',
    className: 'bg-sky-100 text-sky-900',
    description: '특별한 위험 신호는 없지만 최근 거래 조건은 한 번 더 확인하는 편이 좋아요.',
  },
  NEW: {
    label: '신규 거래자',
    className: 'bg-ink/5 text-ink/70',
    description: '아직 쌓인 거래 이력이 많지 않아 첫 거래 기준으로 판단해야 해요.',
  },
  CAUTION: {
    label: '주의 필요',
    className: 'bg-amber-100 text-amber-900',
    description: '노쇼나 완료율 지표를 먼저 확인하고 거래 조건을 한 번 더 맞춰보세요.',
  },
};

const HIGHLIGHT_TONE_CLASS = {
  positive: 'border-mint-100 bg-mint-50 text-mint-900',
  neutral: 'border-ink/10 bg-white text-ink',
  caution: 'border-amber-200 bg-amber-50 text-amber-900',
  danger: 'border-rose-200 bg-rose-50 text-rose-900',
};

const WARNING_TONE_CLASS = {
  info: 'border-ink/10 bg-ink/5 text-ink/75',
  caution: 'border-amber-200 bg-amber-50 text-amber-900',
  danger: 'border-rose-200 bg-rose-50 text-rose-900',
};

export function getTrustBadge(summary) {
  const base = BADGE_BY_LEVEL[summary?.trustLevel] ?? BADGE_BY_LEVEL.NEW;
  return {
    label: base.label,
    className: base.className,
    description: base.description,
  };
}

export function getTrustHighlights(summary) {
  if (!summary) {
    return [];
  }

  const reviewTone = summary.reviewCount >= 10 ? 'positive' : summary.reviewCount >= 3 ? 'neutral' : 'caution';
  const completionTone =
    summary.completionRate >= 95 ? 'positive' : summary.completionRate >= 80 ? 'neutral' : summary.completionRate >= 65 ? 'caution' : 'danger';
  const noShowTone = summary.noShowCount === 0 ? 'positive' : summary.noShowCount === 1 ? 'caution' : 'danger';

  return [
    {
      label: '후기',
      value: `${summary.reviewCount}개`,
      tone: reviewTone,
    },
    {
      label: '완료율',
      value: `${summary.completionRate}%`,
      tone: completionTone,
    },
    {
      label: '노쇼',
      value: `${summary.noShowCount}건`,
      tone: noShowTone,
    },
  ].map((item) => ({
    ...item,
    className: HIGHLIGHT_TONE_CLASS[item.tone],
  }));
}

export function getTrustWarnings(summary) {
  if (!summary) {
    return [];
  }

  const warnings = [];

  if (summary.noShowCount >= 2) {
    warnings.push({
      tone: 'danger',
      title: `노쇼 기록 ${summary.noShowCount}건`,
      message: '최근 거래에서 약속 불이행이 반복됐습니다. 픽업 시간과 정산 조건을 다시 확인하세요.',
    });
  } else if (summary.noShowCount === 1) {
    warnings.push({
      tone: 'caution',
      title: '최근 노쇼 기록 1건',
      message: '직전 거래에서 약속 이행 문제가 있었습니다. 거래 전에 일정과 역할을 다시 맞춰보세요.',
    });
  }

  if (summary.completedTradeCount >= 3 && summary.completionRate < 70) {
    warnings.push({
      tone: 'danger',
      title: `거래 완료율 ${summary.completionRate}%`,
      message: '완료율이 낮은 편입니다. 참여 전 정산 방식과 픽업 계획을 더 구체적으로 확인하는 편이 좋습니다.',
    });
  } else if (summary.completedTradeCount >= 3 && summary.completionRate < 85) {
    warnings.push({
      tone: 'caution',
      title: `거래 완료율 ${summary.completionRate}%`,
      message: '완료율이 아주 안정적이지는 않습니다. 진행 방식과 마감 조건을 한 번 더 확인하세요.',
    });
  }

  if (warnings.length === 0) {
    warnings.push({
      tone: 'info',
      title: '안정적인 최근 거래 흐름',
      message: '최근 거래 기준 위험 신호 없이 안정적으로 거래를 이어가고 있어요.',
    });
  }

  return warnings.map((warning) => ({
    ...warning,
    className: WARNING_TONE_CLASS[warning.tone],
  }));
}
