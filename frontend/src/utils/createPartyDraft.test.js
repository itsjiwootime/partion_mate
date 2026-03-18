import { describe, expect, it, vi } from 'vitest';
import {
  clearCreatePartyDraft,
  CREATE_PARTY_DRAFT_KEY,
  CREATE_PARTY_DRAFT_VERSION,
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
  it('입력값이_있으면_초안을_저장하고_복구한다', () => {
    const form = {
      ...initialForm,
      productName: '올리브 오일 2L',
      totalPrice: '25000',
    };

    saveCreatePartyDraft({ form, initialForm, currentStep: 1 });

    expect(hasCreatePartyDraftContent(form, initialForm)).toBe(true);
    expect(loadCreatePartyDraft({ initialForm, maxStep: 2 })).toEqual({
      savedAt: expect.any(String),
      currentStep: 1,
      form,
    });
  });

  it('버전이_다르거나_만료된_초안은_삭제한다', () => {
    localStorage.setItem(
      CREATE_PARTY_DRAFT_KEY,
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

    expect(loadCreatePartyDraft({ initialForm, maxStep: 2 })).toBeNull();
    expect(localStorage.getItem(CREATE_PARTY_DRAFT_KEY)).toBeNull();

    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-18T12:00:00Z'));
    localStorage.setItem(
      CREATE_PARTY_DRAFT_KEY,
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

    expect(loadCreatePartyDraft({ initialForm, maxStep: 2 })).toBeNull();
    expect(localStorage.getItem(CREATE_PARTY_DRAFT_KEY)).toBeNull();
    vi.useRealTimers();
  });

  it('초기값과_같은_입력은_저장하지_않는다', () => {
    saveCreatePartyDraft({ form: initialForm, initialForm, currentStep: 0 });

    expect(localStorage.getItem(CREATE_PARTY_DRAFT_KEY)).toBeNull();

    clearCreatePartyDraft();
    expect(localStorage.getItem(CREATE_PARTY_DRAFT_KEY)).toBeNull();
  });
});
