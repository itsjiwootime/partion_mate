import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  buildCreatePartyDraftKey,
  clearCreatePartyDraft,
  CREATE_PARTY_DRAFT_VERSION,
  LEGACY_CREATE_PARTY_DRAFT_KEY,
  hasCreatePartyDraftContent,
  loadCreatePartyDraft,
  saveCreatePartyDraft,
} from './createPartyDraft';

const initialForm = {
  branchId: '1',
  productName: '',
  totalPrice: '',
  totalQuantity: 4,
  deadlineDate: '',
  deadlineTime: '',
  description: '',
  title: '',
  hostRequestedQuantity: 1,
  unitLabel: '개',
  minimumShareUnit: 1,
  storageType: 'ROOM_TEMPERATURE',
  packagingType: 'ORIGINAL_PACKAGE',
  hostProvidesPackaging: true,
  onSiteSplit: false,
};

describe('createPartyDraft', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.useRealTimers();
  });

  it('입력값이_있으면_초안을_저장하고_복구한다', () => {
    const userKey = 'tester@test.com';
    const form = {
      ...initialForm,
      productName: '올리브 오일 2L',
      totalPrice: '25000',
    };

    saveCreatePartyDraft({ form, initialForm, currentStep: 1, userKey });

    expect(hasCreatePartyDraftContent(form, initialForm)).toBe(true);
    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey, expectedStoreId: '1' })).toEqual({
      savedAt: expect.any(String),
      currentStep: 1,
      form,
    });
    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey, expectedStoreId: '2' })).toBeNull();
  });

  it('버전이_다르거나_만료된_초안은_삭제한다', () => {
    const userKey = 'tester@test.com';
    const storageKey = buildCreatePartyDraftKey({ userKey });
    localStorage.setItem(
      storageKey,
      JSON.stringify({
        version: CREATE_PARTY_DRAFT_VERSION + 1,
        savedAt: new Date().toISOString(),
        currentStep: 1,
        form: {
          ...initialForm,
          productName: '오래된 초안',
        },
      }),
    );

    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey })).toBeNull();
    expect(localStorage.getItem(storageKey)).toBeNull();

    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-18T12:00:00Z'));
    localStorage.setItem(
      storageKey,
      JSON.stringify({
        version: CREATE_PARTY_DRAFT_VERSION,
        savedAt: '2026-03-01T12:00:00.000Z',
        currentStep: 1,
        form: {
          ...initialForm,
          productName: '만료 초안',
        },
      }),
    );

    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey })).toBeNull();
    expect(localStorage.getItem(storageKey)).toBeNull();
    vi.useRealTimers();
  });

  it('초기값과_같은_입력은_저장하지_않는다', () => {
    const userKey = 'tester@test.com';
    const storageKey = buildCreatePartyDraftKey({ userKey });
    saveCreatePartyDraft({ form: initialForm, initialForm, currentStep: 0, userKey });

    expect(localStorage.getItem(storageKey)).toBeNull();

    clearCreatePartyDraft({ userKey });
    expect(localStorage.getItem(storageKey)).toBeNull();
  });

  it('다른_사용자_초안은_복구하지_않는다', () => {
    const ownerKey = 'owner@test.com';
    const otherUserKey = 'other@test.com';
    const form = {
      ...initialForm,
      productName: '올리브 오일 2L',
      totalPrice: '25000',
    };

    saveCreatePartyDraft({ form, initialForm, currentStep: 1, userKey: ownerKey });

    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey: otherUserKey, expectedStoreId: '1' })).toBeNull();
    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey: ownerKey, expectedStoreId: '1' })).not.toBeNull();
  });

  it('레거시_전역_초안은_읽지_않고_정리한다', () => {
    const userKey = 'tester@test.com';
    localStorage.setItem(
      LEGACY_CREATE_PARTY_DRAFT_KEY,
      JSON.stringify({
        version: CREATE_PARTY_DRAFT_VERSION,
        savedAt: new Date().toISOString(),
        currentStep: 1,
        form: {
          ...initialForm,
          productName: '예전 초안',
        },
      }),
    );

    expect(loadCreatePartyDraft({ initialForm, maxStep: 2, userKey, expectedStoreId: '1' })).toBeNull();
    expect(localStorage.getItem(LEGACY_CREATE_PARTY_DRAFT_KEY)).toBeNull();
  });
});
