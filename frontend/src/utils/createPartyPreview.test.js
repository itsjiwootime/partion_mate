import { describe, expect, it } from 'vitest';
import { buildCreatePartyPreview } from './createPartyPreview';

describe('buildCreatePartyPreview', () => {
  it('최소_참여_기준과_호스트_부담금을_계산한다', () => {
    const preview = buildCreatePartyPreview({
      totalPrice: 24000,
      totalQuantity: 4,
      hostRequestedQuantity: 1,
      minimumShareUnit: 2,
      unitLabel: '개',
    });

    expect(preview.baseUnitAmount).toBe(6000);
    expect(preview.unitAmountMax).toBe(6000);
    expect(preview.recruitableQuantity).toBe(3);
    expect(preview.minimumParticipantAmountMin).toBe(12000);
    expect(preview.minimumParticipantAmountMax).toBe(12000);
    expect(preview.hostExpectedAmountMin).toBe(6000);
    expect(preview.hostExpectedAmountMax).toBe(6000);
    expect(preview.maxParticipantSlots).toBe(1);
    expect(preview.warnings).toEqual([
      expect.objectContaining({
        code: 'LEFTOVER_QUANTITY',
      }),
    ]);
  });

  it('정산_나머지와_모집_불가_경고를_계산한다', () => {
    const preview = buildCreatePartyPreview({
      totalPrice: 10001,
      totalQuantity: 3,
      hostRequestedQuantity: 2,
      minimumShareUnit: 2,
      unitLabel: '팩',
    });

    expect(preview.baseUnitAmount).toBe(3333);
    expect(preview.unitAmountMax).toBe(3334);
    expect(preview.hostExpectedAmountMin).toBe(6666);
    expect(preview.hostExpectedAmountMax).toBe(6668);
    expect(preview.minimumParticipantAmountMin).toBe(6666);
    expect(preview.minimumParticipantAmountMax).toBe(6668);
    expect(preview.maxParticipantSlots).toBe(0);
    expect(preview.warnings).toEqual([
      expect.objectContaining({
        code: 'BELOW_MINIMUM_SHARE',
      }),
      expect.objectContaining({
        code: 'PRICE_REMAINDER',
      }),
    ]);
  });
});
