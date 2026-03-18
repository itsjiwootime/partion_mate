export const LEGACY_CREATE_PARTY_DRAFT_KEY = 'pm_create_party_draft';
export const CREATE_PARTY_DRAFT_KEY_PREFIX = 'pm_create_party_draft';
export const CREATE_PARTY_DRAFT_VERSION = 1;
const CREATE_PARTY_DRAFT_TTL_MS = 7 * 24 * 60 * 60 * 1000;

function isRecord(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function toStringValue(value, fallback = '') {
  return typeof value === 'string' ? value : fallback;
}

function toBooleanValue(value, fallback = false) {
  return typeof value === 'boolean' ? value : fallback;
}

function toNumberString(value, fallback) {
  if (typeof value === 'string' && value.trim() !== '') {
    return value;
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value);
  }

  return fallback;
}

function normalizeDraftForm(form, initialForm) {
  if (!isRecord(form)) {
    return { ...initialForm };
  }

  return {
    branchId: toStringValue(form.branchId, initialForm.branchId),
    productName: toStringValue(form.productName, initialForm.productName),
    totalPrice: toNumberString(form.totalPrice, initialForm.totalPrice),
    totalQuantity: Number(form.totalQuantity) || initialForm.totalQuantity,
    deadlineDate: toStringValue(form.deadlineDate, initialForm.deadlineDate),
    deadlineTime: toStringValue(form.deadlineTime, initialForm.deadlineTime),
    description: toStringValue(form.description, initialForm.description),
    title: toStringValue(form.title, initialForm.title),
    hostRequestedQuantity: Number(form.hostRequestedQuantity) || 0,
    unitLabel: toStringValue(form.unitLabel, initialForm.unitLabel),
    minimumShareUnit: Number(form.minimumShareUnit) || initialForm.minimumShareUnit,
    storageType: toStringValue(form.storageType, initialForm.storageType),
    packagingType: toStringValue(form.packagingType, initialForm.packagingType),
    hostProvidesPackaging: toBooleanValue(form.hostProvidesPackaging, initialForm.hostProvidesPackaging),
    onSiteSplit: toBooleanValue(form.onSiteSplit, initialForm.onSiteSplit),
  };
}

function normalizeStep(step, maxStep) {
  const parsed = Number(step);
  if (!Number.isInteger(parsed)) {
    return 0;
  }

  return Math.max(0, Math.min(maxStep, parsed));
}

function normalizeDraftUserKey(userKey) {
  if (typeof userKey !== 'string') {
    return 'guest';
  }

  const normalized = userKey.trim().toLowerCase();
  return normalized || 'guest';
}

function normalizeStoreId(storeId) {
  if (storeId == null) {
    return '';
  }

  return String(storeId).trim();
}

export function buildCreatePartyDraftKey({ userKey } = {}) {
  return `${CREATE_PARTY_DRAFT_KEY_PREFIX}:${normalizeDraftUserKey(userKey)}`;
}

export function hasCreatePartyDraftContent(form, initialForm) {
  return JSON.stringify(normalizeDraftForm(form, initialForm)) !== JSON.stringify(normalizeDraftForm(initialForm, initialForm));
}

export function saveCreatePartyDraft({ form, initialForm, currentStep, userKey }) {
  const storageKey = buildCreatePartyDraftKey({ userKey });
  localStorage.removeItem(LEGACY_CREATE_PARTY_DRAFT_KEY);

  if (!hasCreatePartyDraftContent(form, initialForm)) {
    localStorage.removeItem(storageKey);
    return;
  }

  const payload = {
    version: CREATE_PARTY_DRAFT_VERSION,
    savedAt: new Date().toISOString(),
    currentStep,
    storeId: normalizeStoreId(form?.branchId),
    form: normalizeDraftForm(form, initialForm),
  };

  localStorage.setItem(storageKey, JSON.stringify(payload));
}

export function loadCreatePartyDraft({ initialForm, maxStep, userKey, expectedStoreId }) {
  const storageKey = buildCreatePartyDraftKey({ userKey });
  localStorage.removeItem(LEGACY_CREATE_PARTY_DRAFT_KEY);

  const raw = localStorage.getItem(storageKey);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw);
    if (!isRecord(parsed) || parsed.version !== CREATE_PARTY_DRAFT_VERSION) {
      localStorage.removeItem(storageKey);
      return null;
    }

    const savedAt = Date.parse(parsed.savedAt);
    if (!Number.isFinite(savedAt) || Date.now() - savedAt > CREATE_PARTY_DRAFT_TTL_MS) {
      localStorage.removeItem(storageKey);
      return null;
    }

    const normalizedExpectedStoreId = normalizeStoreId(expectedStoreId);
    const normalizedDraftStoreId = normalizeStoreId(parsed.storeId ?? parsed.form?.branchId);
    if (normalizedExpectedStoreId && normalizedDraftStoreId !== normalizedExpectedStoreId) {
      return null;
    }

    const form = normalizeDraftForm(parsed.form, initialForm);
    if (!hasCreatePartyDraftContent(form, initialForm)) {
      localStorage.removeItem(storageKey);
      return null;
    }

    return {
      savedAt: parsed.savedAt,
      currentStep: normalizeStep(parsed.currentStep, maxStep),
      form,
    };
  } catch {
    localStorage.removeItem(storageKey);
    return null;
  }
}

export function clearCreatePartyDraft({ userKey } = {}) {
  localStorage.removeItem(buildCreatePartyDraftKey({ userKey }));
  localStorage.removeItem(LEGACY_CREATE_PARTY_DRAFT_KEY);
}
