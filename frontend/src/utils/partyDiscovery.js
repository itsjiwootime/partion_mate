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

export function parsePartyDiscoveryFilters(searchParams) {
  return {
    query: searchParams.get('q')?.trim() ?? '',
    status: normalizeFilterValue(searchParams.get('status'), PARTY_STATUS_FILTERS, 'all'),
    storage: normalizeFilterValue(searchParams.get('storage'), PARTY_STORAGE_FILTERS, 'all'),
    unit: normalizeFilterValue(searchParams.get('unit'), PARTY_UNIT_FILTERS, 'all'),
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
