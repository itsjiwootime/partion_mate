export function normalizePartyDetail(input) {
  if (!input) return null;

  const targetQuantity = input.targetQuantity ?? input.totalQuantity ?? 0;
  const currentQuantity = input.currentQuantity ?? 0;
  const rawStatus = input.status ?? 'RECRUITING';
  const normalizedStatus =
    rawStatus === 'CLOSED' || rawStatus === 'closed'
      ? 'closed'
      : rawStatus === 'FULL' || rawStatus === 'full'
        ? 'full'
        : 'active';

  return {
    partyId: input.partyId ?? input.id,
    title: input.title ?? '',
    productName: input.productName ?? input.product ?? input.title ?? '',
    totalPrice: input.totalPrice ?? 0,
    currentQuantity,
    targetQuantity,
    deadlineLabel: input.deadlineLabel ?? input.deadline ?? '미정',
    deadline: input.deadline ?? null,
    rating: input.rating ?? input.hostRating ?? 4.5,
    status: normalizedStatus,
    storeName: input.storeName ?? '',
    openChatUrl: input.openChatUrl ?? '',
    participationStatus: input.participationStatus ?? 'JOINED',
    waitingPosition: input.waitingPosition ?? null,
    requestedQuantity: input.requestedQuantity ?? null,
    closeReason: input.closeReason ?? null,
    closedAt: input.closedAt ?? null,
    storeId: input.storeId ?? null,
    realtimeTrigger: input.realtimeTrigger ?? null,
  };
}

export function normalizePartySummary(input) {
  return normalizePartyDetail(input);
}

export function mergeRealtimeParty(current, event) {
  if (!event) return current;

  return normalizePartyDetail({
    ...(current ?? {}),
    ...event,
    partyId: event.partyId ?? event.id ?? current?.partyId ?? current?.id,
    id: event.id ?? event.partyId ?? current?.id ?? current?.partyId,
    targetQuantity: event.totalQuantity ?? event.targetQuantity ?? current?.targetQuantity,
    totalQuantity: event.totalQuantity ?? event.targetQuantity ?? current?.totalQuantity,
    currentQuantity: event.currentQuantity ?? current?.currentQuantity ?? 0,
  });
}

export function applyPartyListRealtimeUpdate(currentList, event, options = {}) {
  if (!event) return currentList;

  const eventPartyId = event.partyId ?? event.id;
  if (eventPartyId == null) return currentList;
  if (options.storeId && event.storeId !== options.storeId) return currentList;

  const index = currentList.findIndex((party) => party.partyId === eventPartyId || party.id === eventPartyId);
  const nextParty = mergeRealtimeParty(index >= 0 ? currentList[index] : null, event);

  if (index >= 0) {
    return currentList.map((party, currentIndex) => (currentIndex === index ? nextParty : party));
  }

  if (event.realtimeTrigger === 'PARTY_CREATED') {
    return [nextParty, ...currentList];
  }

  return currentList;
}
