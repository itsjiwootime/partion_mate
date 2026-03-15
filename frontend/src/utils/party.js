export function normalizePartyDetail(input) {
  if (!input) return null;

  const targetQuantity = input.targetQuantity ?? input.totalQuantity ?? 0;
  const currentQuantity = input.currentQuantity ?? 0;
  const rawStatus = input.status ?? 'RECRUITING';

  return {
    partyId: input.partyId ?? input.id,
    title: input.title ?? '',
    productName: input.productName ?? input.product ?? input.title ?? '',
    totalPrice: input.totalPrice ?? 0,
    currentQuantity,
    targetQuantity,
    deadlineLabel: input.deadlineLabel ?? input.deadline ?? '미정',
    rating: input.rating ?? input.hostRating ?? 4.5,
    status: rawStatus === 'FULL' || rawStatus === 'full' ? 'full' : 'active',
    storeName: input.storeName ?? '',
    openChatUrl: input.openChatUrl ?? '',
    participationStatus: input.participationStatus ?? 'JOINED',
    waitingPosition: input.waitingPosition ?? null,
    requestedQuantity: input.requestedQuantity ?? null,
  };
}

export function normalizePartySummary(input) {
  return normalizePartyDetail(input);
}
