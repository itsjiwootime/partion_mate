function toNonNegativeInteger(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return 0;
  }

  return Math.max(0, Math.floor(parsed));
}

export function buildCreatePartyPreview(input) {
  const totalPrice = toNonNegativeInteger(input.totalPrice);
  const totalQuantity = toNonNegativeInteger(input.totalQuantity);
  const hostRequestedQuantity = toNonNegativeInteger(input.hostRequestedQuantity);
  const minimumShareUnit = Math.max(1, toNonNegativeInteger(input.minimumShareUnit) || 1);
  const unitLabel = input.unitLabel?.trim() || '개';

  const recruitableQuantity = Math.max(totalQuantity - hostRequestedQuantity, 0);
  const baseUnitAmount = totalQuantity > 0 ? Math.floor(totalPrice / totalQuantity) : 0;
  const remainderAmount = totalQuantity > 0 ? totalPrice % totalQuantity : 0;
  const unitAmountMax = totalQuantity > 0 && remainderAmount > 0 ? baseUnitAmount + 1 : baseUnitAmount;
  const minimumParticipantAmountMin = baseUnitAmount * minimumShareUnit;
  const minimumParticipantAmountMax = minimumParticipantAmountMin + Math.min(remainderAmount, minimumShareUnit);
  const hostExpectedAmountMin = baseUnitAmount * hostRequestedQuantity;
  const hostExpectedAmountMax = hostExpectedAmountMin + Math.min(remainderAmount, hostRequestedQuantity);
  const maxParticipantSlots = minimumShareUnit > 0 ? Math.floor(recruitableQuantity / minimumShareUnit) : 0;
  const remainderQuantity = minimumShareUnit > 0 ? recruitableQuantity % minimumShareUnit : 0;

  const warnings = [];

  if (totalQuantity > 0 && hostRequestedQuantity >= totalQuantity) {
    warnings.push({
      code: 'NO_RECRUITABLE_QUANTITY',
      tone: 'critical',
      message: '호스트 수량과 총 수량이 같아 참여자에게 열리는 수량이 없습니다.',
    });
  } else if (totalQuantity > 0 && recruitableQuantity < minimumShareUnit) {
    warnings.push({
      code: 'BELOW_MINIMUM_SHARE',
      tone: 'critical',
      message: `호스트 수량을 제외하고 남는 ${recruitableQuantity}${unitLabel}로는 최소 참여 기준 ${minimumShareUnit}${unitLabel}를 채우기 어렵습니다.`,
    });
  }

  if (totalQuantity > 0 && recruitableQuantity >= minimumShareUnit && remainderQuantity > 0) {
    warnings.push({
      code: 'LEFTOVER_QUANTITY',
      tone: 'caution',
      message: `최소 소분 단위 기준으로 나누면 ${remainderQuantity}${unitLabel}가 남아 마지막 참여 수량 조정이 필요할 수 있습니다.`,
    });
  }

  if (totalQuantity > 0 && remainderAmount > 0) {
    warnings.push({
      code: 'PRICE_REMAINDER',
      tone: 'info',
      message: `총 가격이 수량으로 정확히 나누어떨어지지 않아 정산 시 1원 단위 차이가 생길 수 있습니다. 남는 ${remainderAmount}원은 큰 수량 요청자부터 1원씩 배분됩니다.`,
    });
  }

  return {
    totalPrice,
    totalQuantity,
    hostRequestedQuantity,
    minimumShareUnit,
    unitLabel,
    recruitableQuantity,
    baseUnitAmount,
    unitAmountMax,
    remainderAmount,
    minimumParticipantAmountMin,
    minimumParticipantAmountMax,
    hostExpectedAmountMin,
    hostExpectedAmountMax,
    maxParticipantSlots,
    remainderQuantity,
    warnings,
  };
}
