export const REPORT_REASON_OPTIONS = {
  PARTY: [
    {
      type: 'PAYMENT_ISSUE',
      label: '정산 문제',
      description: '금액, 송금, 환불 등 정산 과정에서 문제가 있었어요.',
    },
    {
      type: 'NO_SHOW',
      label: '노쇼',
      description: '약속된 픽업이나 거래가 정상적으로 진행되지 않았어요.',
    },
    {
      type: 'FRAUD_SUSPECTED',
      label: '사기 의심',
      description: '거래 유도 방식이나 요구사항이 일반적이지 않았어요.',
    },
    {
      type: 'SPAM',
      label: '스팸',
      description: '반복 홍보나 무관한 안내가 많았어요.',
    },
    {
      type: 'OTHER',
      label: '기타',
      description: '위 항목으로 설명되지 않는 문제가 있었어요.',
    },
  ],
  USER: [
    {
      type: 'NO_SHOW',
      label: '노쇼',
      description: '약속된 거래나 픽업에 나타나지 않았어요.',
    },
    {
      type: 'PAYMENT_ISSUE',
      label: '정산 문제',
      description: '정산 합의나 송금 과정에 문제가 있었어요.',
    },
    {
      type: 'FRAUD_SUSPECTED',
      label: '사기 의심',
      description: '거래 방식이나 요구가 비정상적으로 느껴졌어요.',
    },
    {
      type: 'INAPPROPRIATE_CHAT',
      label: '부적절한 채팅',
      description: '대화 태도나 표현이 불쾌하거나 공격적이었어요.',
    },
    {
      type: 'OTHER',
      label: '기타',
      description: '위 항목으로 설명되지 않는 문제가 있었어요.',
    },
  ],
  CHAT: [
    {
      type: 'INAPPROPRIATE_CHAT',
      label: '부적절한 채팅',
      description: '욕설, 비난, 압박 등 대화 내용이 불편했어요.',
    },
    {
      type: 'SPAM',
      label: '스팸',
      description: '같은 내용 반복, 광고, 무관한 메시지가 이어졌어요.',
    },
    {
      type: 'FRAUD_SUSPECTED',
      label: '사기 의심',
      description: '채팅에서 비정상적인 거래나 외부 유도를 시도했어요.',
    },
    {
      type: 'OTHER',
      label: '기타',
      description: '위 항목으로 설명되지 않는 문제가 있었어요.',
    },
  ],
};

export const BLOCKED_PARTY_INTERACTION_MESSAGE = '차단한 사용자 또는 나를 차단한 사용자가 포함된 파티에는 참여할 수 없습니다.';
export const BLOCKED_CHAT_ACCESS_MESSAGE = '차단 관계가 있는 사용자가 포함된 채팅방에는 접근할 수 없습니다.';

export function getReportReasonOptions(targetType) {
  return REPORT_REASON_OPTIONS[targetType] ?? REPORT_REASON_OPTIONS.PARTY;
}

export function isBlockedPartyInteractionMessage(message) {
  return typeof message === 'string' && message.includes(BLOCKED_PARTY_INTERACTION_MESSAGE);
}

export function isBlockedChatAccessMessage(message) {
  return typeof message === 'string' && message.includes(BLOCKED_CHAT_ACCESS_MESSAGE);
}

export function formatReportTargetSummary(report) {
  if (!report) {
    return '';
  }

  if (report.targetType === 'PARTY') {
    return report.partyTitle ? `${report.partyTitle} 파티` : '파티';
  }

  if (report.targetUsername) {
    return report.targetUsername;
  }

  if (report.partyTitle) {
    return `${report.partyTitle} 채팅`;
  }

  return report.targetTypeLabel ?? '신고 대상';
}
