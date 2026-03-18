import { describe, expect, it } from 'vitest';
import {
  buildPartyDiscoverySearch,
  buildDiscoverySections,
  filterParties,
  hasActivePartyDiscoveryFilters,
  sortParties,
  parsePartyDiscoveryFilters,
  summarizePartyDiscoveryFilters,
} from './partyDiscovery';

const sampleParties = [
  {
    title: '양재점 연어 소분',
    productName: '연어',
    storeName: '코스트코 양재점',
    status: 'active',
    storageType: 'REFRIGERATED',
    minimumShareUnit: 1,
  },
  {
    title: '상봉점 냉동만두',
    productName: '냉동만두',
    storeName: '코스트코 상봉점',
    status: 'full',
    storageType: 'FROZEN',
    minimumShareUnit: 5,
  },
  {
    title: '휴지 공동구매',
    productName: '휴지',
    storeName: '트레이더스 월계점',
    status: 'closed',
    storageType: 'ROOM_TEMPERATURE',
    minimumShareUnit: 2,
  },
];

describe('partyDiscovery', () => {
  it('URL_파라미터를_필터_모델로_정규화한다', () => {
    // given
    const searchParams = new URLSearchParams('q=%20양재점%20&status=active&storage=FROZEN&unit=invalid');

    // when
    const filters = parsePartyDiscoveryFilters(searchParams);
    const nextSearch = buildPartyDiscoverySearch(filters);

    // then
    expect(filters).toEqual({
      query: '양재점',
      status: 'active',
      storage: 'FROZEN',
      unit: 'all',
      sort: 'recommended',
    });
    expect(nextSearch.toString()).toBe('q=%EC%96%91%EC%9E%AC%EC%A0%90&status=active&storage=FROZEN');
  });

  it('검색어_상태_보관방식_소분단위로_파티를_필터링한다', () => {
    // given
    const filters = {
      query: '양재점',
      status: 'active',
      storage: 'REFRIGERATED',
      unit: 'ONE',
    };

    // when
    const result = filterParties(sampleParties, filters);

    // then
    expect(result).toEqual([sampleParties[0]]);
  });

  it('활성_필터_요약을_제공한다', () => {
    // given
    const filters = {
      query: '연어',
      status: 'full',
      storage: 'REFRIGERATED',
      unit: 'ONE',
    };

    // when
    const active = hasActivePartyDiscoveryFilters(filters);
    const summary = summarizePartyDiscoveryFilters(filters);

    // then
    expect(active).toBe(true);
    expect(summary).toEqual(['검색어: 연어', '마감', '냉장', '1개']);
  });

  it('정렬과_발견_섹션_대표_파티를_계산한다', () => {
    // given
    const sortableParties = [
      {
        partyId: 11,
        title: '오늘 마감 연어',
        status: 'active',
        deadline: '2099-03-19T18:00:00',
        currentQuantity: 2,
        targetQuantity: 4,
        minimumShareUnit: 1,
        unitLabel: '개',
        storeName: '코스트코 양재점',
      },
      {
        partyId: 14,
        title: '인기 라면',
        status: 'active',
        deadline: '2099-03-22T12:00:00',
        currentQuantity: 8,
        targetQuantity: 10,
        minimumShareUnit: 2,
        unitLabel: '개',
        storeName: '트레이더스 월계점',
      },
      {
        partyId: 19,
        title: '신규 과자',
        status: 'active',
        deadline: '2099-03-25T12:00:00',
        currentQuantity: 1,
        targetQuantity: 6,
        minimumShareUnit: 1,
        unitLabel: '개',
        storeName: '코스트코 공세점',
      },
    ];

    // when
    const popularSorted = sortParties(sortableParties, 'popular');
    const sections = buildDiscoverySections(sortableParties);

    // then
    expect(popularSorted.map((party) => party.title)).toEqual(['인기 라면', '오늘 마감 연어', '신규 과자']);
    expect(sections.find((section) => section.key === 'deadline')?.featuredParty?.title).toBe('오늘 마감 연어');
    expect(sections.find((section) => section.key === 'popular')?.featuredParty?.title).toBe('인기 라면');
    expect(sections.find((section) => section.key === 'newest')?.featuredParty?.title).toBe('신규 과자');
  });
});
