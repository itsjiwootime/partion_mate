function normalizeTrustSummary(input) {
  if (!input) return null;

  return {
    userId: input.userId ?? null,
    username: input.username ?? '',
    averageRating: input.averageRating ?? 0,
    reviewCount: input.reviewCount ?? 0,
    completedTradeCount: input.completedTradeCount ?? 0,
    noShowCount: input.noShowCount ?? 0,
    completionRate: input.completionRate ?? 0,
    trustScore: input.trustScore ?? 0,
    trustLevel: input.trustLevel ?? 'NEW',
    trustLevelLabel: input.trustLevelLabel ?? '신규 사용자',
  };
}

function normalizeReview(input) {
  if (!input) return null;

  return {
    reviewId: input.reviewId ?? null,
    partyId: input.partyId ?? null,
    partyTitle: input.partyTitle ?? '',
    reviewerId: input.reviewerId ?? null,
    reviewerName: input.reviewerName ?? '',
    revieweeId: input.revieweeId ?? null,
    rating: input.rating ?? 0,
    comment: input.comment ?? '',
    createdAt: input.createdAt ?? null,
    createdAtLabel: input.createdAtLabel ?? input.createdAt ?? '',
  };
}

function normalizeSettlementMember(input) {
  if (!input) return null;

  return {
    memberId: input.memberId ?? null,
    userId: input.userId ?? null,
    username: input.username ?? '',
    role: input.role ?? null,
    requestedQuantity: input.requestedQuantity ?? 0,
    expectedAmount: input.expectedAmount ?? null,
    actualAmount: input.actualAmount ?? null,
    paymentStatus: input.paymentStatus ?? null,
    paymentStatusLabel: input.paymentStatusLabel ?? null,
    tradeStatus: input.tradeStatus ?? null,
    tradeStatusLabel: input.tradeStatusLabel ?? null,
    pickupAcknowledged: input.pickupAcknowledged ?? false,
    reviewEligible: input.reviewEligible ?? false,
    reviewWritten: input.reviewWritten ?? false,
  };
}

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
    expectedTotalPrice: input.expectedTotalPrice ?? input.totalPrice ?? 0,
    actualTotalPrice: input.actualTotalPrice ?? null,
    currentQuantity,
    targetQuantity,
    deadlineLabel: input.deadlineLabel ?? input.deadline ?? '미정',
    deadline: input.deadline ?? null,
    rating: input.rating ?? input.hostRating ?? 4.5,
    status: normalizedStatus,
    storeName: input.storeName ?? '',
    openChatUrl: input.openChatUrl ?? '',
    participationStatus: input.participationStatus ?? null,
    waitingPosition: input.waitingPosition ?? null,
    requestedQuantity: input.requestedQuantity ?? null,
    closeReason: input.closeReason ?? null,
    closedAt: input.closedAt ?? null,
    storeId: input.storeId ?? null,
    realtimeTrigger: input.realtimeTrigger ?? null,
    unitLabel: input.unitLabel ?? '개',
    minimumShareUnit: input.minimumShareUnit ?? 1,
    storageType: input.storageType ?? 'ROOM_TEMPERATURE',
    storageTypeLabel: input.storageTypeLabel ?? '상온',
    packagingType: input.packagingType ?? 'ORIGINAL_PACKAGE',
    packagingTypeLabel: input.packagingTypeLabel ?? '원포장',
    hostProvidesPackaging: input.hostProvidesPackaging ?? false,
    onSiteSplit: input.onSiteSplit ?? false,
    guideNote: input.guideNote ?? '',
    receiptNote: input.receiptNote ?? '',
    pickupPlace: input.pickupPlace ?? '',
    pickupTime: input.pickupTime ?? null,
    pickupTimeLabel: input.pickupTimeLabel ?? input.pickupTime ?? '미정',
    expectedAmount: input.expectedAmount ?? null,
    actualAmount: input.actualAmount ?? null,
    paymentStatus: input.paymentStatus ?? null,
    paymentStatusLabel: input.paymentStatusLabel ?? null,
    tradeStatus: input.tradeStatus ?? null,
    tradeStatusLabel: input.tradeStatusLabel ?? null,
    memberId: input.memberId ?? null,
    pickupAcknowledged: input.pickupAcknowledged ?? false,
    reviewEligible: input.reviewEligible ?? false,
    canReviewHost: input.canReviewHost ?? false,
    hasReviewedHost: input.hasReviewedHost ?? false,
    userRole: input.userRole ?? null,
    hostTrust: normalizeTrustSummary(input.hostTrust),
    hostReviews: Array.isArray(input.hostReviews) ? input.hostReviews.map(normalizeReview).filter(Boolean) : [],
    settlementMembers: Array.isArray(input.settlementMembers)
      ? input.settlementMembers.map(normalizeSettlementMember).filter(Boolean)
      : [],
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
