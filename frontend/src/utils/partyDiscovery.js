export const PARTY_STATUS_FILTERS = [
  { value: 'all', label: '전체' },
  { value: 'active', label: '모집 중' },
  { value: 'full', label: '마감' },
  { value: 'closed', label: '종료' },
];

export const PARTY_STORAGE_FILTERS = [
  { value: 'all', label: '전체 보관 방식' },
  { value: 'ROOM_TEMPERATURE', label: '상온' },
  { value: 'REFRIGERATED', label: '냉장' },
  { value: 'FROZEN', label: '냉동' },
];

export const PARTY_UNIT_FILTERS = [
  { value: 'all', label: '전체 소분 단위' },
  { value: 'ONE', label: '1개' },
  { value: 'TWO_PLUS', label: '2개 이상' },
  { value: 'FIVE_PLUS', label: '5개 이상' },
];

export const PARTY_SORT_OPTIONS = [
  { value: 'recommended', label: '추천순' },
  { value: 'deadline', label: '마감 임박순' },
  { value: 'popular', label: '인기순' },
  { value: 'newest', label: '최신순' },
];

const PARTY_DISCOVERY_SECTION_META = [
  {
    key: 'deadline',
    label: '마감 임박',
    description: '곧 마감될 가능성이 큰 파티를 먼저 확인하세요.',
    emptyDescription: '현재 조건에서 먼저 보여줄 마감 임박 파티가 없습니다.',
    sort: 'deadline',
  },
  {
    key: 'popular',
    label: '인기 파티',
    description: '참여 수량이 빠르게 차는 파티를 우선 살펴보세요.',
    emptyDescription: '현재 조건에서 먼저 보여줄 인기 파티가 없습니다.',
    sort: 'popular',
  },
  {
    key: 'newest',
    label: '신규 파티',
    description: '최근에 올라온 파티를 빠르게 확인하세요.',
    emptyDescription: '현재 조건에서 먼저 보여줄 신규 파티가 없습니다.',
    sort: 'newest',
  },
];

export function parsePartyDiscoveryFilters(searchParams) {
  return {
    query: searchParams.get('q')?.trim() ?? '',
    status: normalizeFilterValue(searchParams.get('status'), PARTY_STATUS_FILTERS, 'all'),
    storage: normalizeFilterValue(searchParams.get('storage'), PARTY_STORAGE_FILTERS, 'all'),
    unit: normalizeFilterValue(searchParams.get('unit'), PARTY_UNIT_FILTERS, 'all'),
    sort: normalizeFilterValue(searchParams.get('sort'), PARTY_SORT_OPTIONS, 'recommended'),
  };
}

export function buildPartyDiscoverySearch(filters) {
  const searchParams = new URLSearchParams();

  if (filters.query?.trim()) {
    searchParams.set('q', filters.query.trim());
  }
  if (filters.status && filters.status !== 'all') {
    searchParams.set('status', filters.status);
  }
  if (filters.storage && filters.storage !== 'all') {
    searchParams.set('storage', filters.storage);
  }
  if (filters.unit && filters.unit !== 'all') {
    searchParams.set('unit', filters.unit);
  }
  if (filters.sort && filters.sort !== 'recommended') {
    searchParams.set('sort', filters.sort);
  }

  return searchParams;
}

export function filterParties(parties, filters) {
  return parties.filter((party) => {
    if (filters.query && !matchesQuery(party, filters.query)) {
      return false;
    }

    if (filters.status !== 'all' && party.status !== filters.status) {
      return false;
    }

    if (filters.storage !== 'all' && party.storageType !== filters.storage) {
      return false;
    }

    if (!matchesUnitFilter(party.minimumShareUnit, filters.unit)) {
      return false;
    }

    return true;
  });
}

export function hasActivePartyDiscoveryFilters(filters) {
  return Boolean(filters.query) || filters.status !== 'all' || filters.storage !== 'all' || filters.unit !== 'all';
}

export function summarizePartyDiscoveryFilters(filters) {
  const summary = [];

  if (filters.query) {
    summary.push(`검색어: ${filters.query}`);
  }

  if (filters.status !== 'all') {
    summary.push(findLabel(PARTY_STATUS_FILTERS, filters.status));
  }

  if (filters.storage !== 'all') {
    summary.push(findLabel(PARTY_STORAGE_FILTERS, filters.storage));
  }

  if (filters.unit !== 'all') {
    summary.push(findLabel(PARTY_UNIT_FILTERS, filters.unit));
  }

  return summary;
}

export function sortParties(parties, sort = 'recommended') {
  return [...parties].sort((left, right) => {
    const statusCompare = compareStatusRank(left, right);
    if (statusCompare !== 0) {
      return statusCompare;
    }

    switch (sort) {
      case 'deadline': {
        return compareByDeadline(left, right) || compareByPopularity(left, right) || compareByNewest(left, right);
      }
      case 'popular': {
        return compareByPopularity(left, right) || compareByDeadline(left, right) || compareByNewest(left, right);
      }
      case 'newest': {
        return compareByNewest(left, right) || compareByPopularity(left, right) || compareByDeadline(left, right);
      }
      default: {
        return compareByDeadline(left, right) || compareByPopularity(left, right) || compareByNewest(left, right);
      }
    }
  });
}

export function buildDiscoverySections(parties) {
  const activeParties = parties.filter((party) => party.status === 'active');

  return PARTY_DISCOVERY_SECTION_META.map((section) => {
    const source = section.key === 'newest' ? parties : activeParties;
    const topParties = sortParties(source, section.sort).slice(0, 3);

    return {
      ...section,
      parties: topParties,
      featuredParty: topParties[0] ?? null,
    };
  });
}

export function getPartySortHighlight(sort, rank) {
  if (rank > 2) {
    return null;
  }

  switch (sort) {
    case 'deadline':
      return { label: '마감 임박', tone: 'deadline' };
    case 'popular':
      return { label: '인기 파티', tone: 'popular' };
    case 'newest':
      return { label: '신규', tone: 'newest' };
    default:
      return { label: '추천', tone: 'recommended' };
  }
}

function normalizeFilterValue(value, options, fallback) {
  if (!value) {
    return fallback;
  }

  return options.some((option) => option.value === value) ? value : fallback;
}

function matchesQuery(party, query) {
  const normalizedQuery = query.trim().toLowerCase();
  if (!normalizedQuery) {
    return true;
  }

  const haystack = [party.title, party.productName, party.storeName]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();

  return haystack.includes(normalizedQuery);
}

function matchesUnitFilter(minimumShareUnit, unitFilter) {
  const unit = Number(minimumShareUnit ?? 0);

  switch (unitFilter) {
    case 'ONE':
      return unit === 1;
    case 'TWO_PLUS':
      return unit >= 2;
    case 'FIVE_PLUS':
      return unit >= 5;
    default:
      return true;
  }
}

function findLabel(options, value) {
  return options.find((option) => option.value === value)?.label ?? value;
}

function compareStatusRank(left, right) {
  return getStatusRank(left) - getStatusRank(right);
}

function getStatusRank(party) {
  switch (party.status) {
    case 'active':
      return 0;
    case 'full':
      return 1;
    default:
      return 2;
  }
}

function compareByDeadline(left, right) {
  const leftTime = getDeadlineTimestamp(left);
  const rightTime = getDeadlineTimestamp(right);
  return leftTime - rightTime;
}

function compareByPopularity(left, right) {
  const ratioDiff = getProgressRatio(right) - getProgressRatio(left);
  if (ratioDiff !== 0) {
    return ratioDiff;
  }

  const remainingDiff = getRemainingQuantity(left) - getRemainingQuantity(right);
  if (remainingDiff !== 0) {
    return remainingDiff;
  }

  return 0;
}

function compareByNewest(left, right) {
  return Number(right.partyId ?? right.id ?? 0) - Number(left.partyId ?? left.id ?? 0);
}

function getProgressRatio(party) {
  const target = Number(party.targetQuantity ?? party.totalQuantity ?? 0);
  if (target <= 0) {
    return 0;
  }

  return Math.round(((Number(party.currentQuantity ?? 0) / target) * 1000)) / 1000;
}

function getRemainingQuantity(party) {
  const target = Number(party.targetQuantity ?? party.totalQuantity ?? 0);
  const current = Number(party.currentQuantity ?? 0);
  return Math.max(0, target - current);
}

function getDeadlineTimestamp(party) {
  if (!party.deadline) {
    return Number.MAX_SAFE_INTEGER;
  }

  const parsed = new Date(party.deadline).getTime();
  return Number.isNaN(parsed) ? Number.MAX_SAFE_INTEGER : parsed;
}
