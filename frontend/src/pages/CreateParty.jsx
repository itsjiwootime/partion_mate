import { useEffect, useMemo, useState } from 'react';
import { Calendar, Check, ChevronDown, Coins, MapPin, Users } from 'lucide-react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { buildCreatePartyPreview } from '../utils/createPartyPreview';

const storageOptions = [
  { value: 'ROOM_TEMPERATURE', label: '상온' },
  { value: 'REFRIGERATED', label: '냉장' },
  { value: 'FROZEN', label: '냉동' },
];

const packagingOptions = [
  { value: 'ORIGINAL_PACKAGE', label: '원포장' },
  { value: 'ZIP_BAG', label: '지퍼백' },
  { value: 'CONTAINER', label: '용기' },
  { value: 'OTHER', label: '기타' },
];

const createSteps = [
  {
    key: 'basic',
    title: '기본 정보',
    description: '지점과 파티 기본 정보를 정리합니다.',
  },
  {
    key: 'share',
    title: '소분 조건',
    description: '가격, 수량, 소분 방식을 정합니다.',
  },
  {
    key: 'operate',
    title: '운영 정보',
    description: '마감과 안내 문구를 확인하고 바로 생성합니다.',
  },
];

function formatCurrency(value) {
  return `${value.toLocaleString('ko-KR')}원`;
}

function formatDeadlinePreview(deadlineDate, deadlineTime) {
  if (!deadlineDate || !deadlineTime) {
    return '아직 설정되지 않음';
  }

  return `${deadlineDate} ${deadlineTime}`;
}

function formatAmountRange(minAmount, maxAmount) {
  if (minAmount === maxAmount) {
    return formatCurrency(minAmount);
  }

  return `${formatCurrency(minAmount)} ~ ${formatCurrency(maxAmount)}`;
}

function getWarningToneClass(tone) {
  switch (tone) {
    case 'critical':
      return 'border-red-200 bg-red-50 text-red-700';
    case 'caution':
      return 'border-amber-200 bg-amber-50 text-amber-700';
    default:
      return 'border-sky-200 bg-sky-50 text-sky-700';
  }
}

function CreateParty() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { isAuthed } = useAuth();
  const { addToast } = useToast();
  const [branches, setBranches] = useState([]);
  const [branchesLoading, setBranchesLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const defaultStoreId = searchParams.get('storeId') ?? '';
  const [form, setForm] = useState({
    branchId: defaultStoreId,
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
    storageType: storageOptions[0].value,
    packagingType: packagingOptions[0].value,
    hostProvidesPackaging: true,
    onSiteSplit: false,
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [coords, setCoords] = useState({ lat: 37.5665, lon: 126.978 });

  const preview = useMemo(
    () =>
      buildCreatePartyPreview({
        totalPrice: form.totalPrice,
        totalQuantity: form.totalQuantity,
        hostRequestedQuantity: form.hostRequestedQuantity,
        minimumShareUnit: form.minimumShareUnit,
        unitLabel: form.unitLabel,
      }),
    [form.totalPrice, form.totalQuantity, form.hostRequestedQuantity, form.minimumShareUnit, form.unitLabel],
  );

  const selectedBranch = useMemo(
    () => branches.find((branch) => String(branch.id) === String(form.branchId)) ?? null,
    [branches, form.branchId],
  );

  const stepProgressLabel = useMemo(
    () => `${currentStep + 1} / ${createSteps.length}`,
    [currentStep],
  );

  useEffect(() => {
    const fetchBranches = async () => {
      try {
        setBranchesLoading(true);
        const data = await api.getNearbyStores({ latitude: coords.lat, longitude: coords.lon });
        setBranches(data);
        if (!form.branchId && data.length > 0) {
          setForm((prev) => ({ ...prev, branchId: String(data[0].id) }));
        }
      } catch {
        // ignore
      } finally {
        setBranchesLoading(false);
      }
    };

    fetchBranches();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [coords.lat, coords.lon]);

  const handleChange = (key) => (event) => {
    setForm((prev) => ({ ...prev, [key]: event.target.value }));
  };

  const handleBooleanChange = (key) => (event) => {
    setForm((prev) => ({ ...prev, [key]: event.target.checked }));
  };

  const handleQuantityChange = (delta) => {
    setForm((prev) => {
      const nextQuantity = Math.min(50, Math.max(1, Number(prev.totalQuantity) + delta));
      const nextHostQuantity = Math.min(nextQuantity, Number(prev.hostRequestedQuantity));
      return {
        ...prev,
        totalQuantity: nextQuantity,
        hostRequestedQuantity: nextHostQuantity,
      };
    });
  };

  const requestBrowserLocation = () => {
    if (!navigator.geolocation) {
      setError('이 브라우저는 위치 기능을 지원하지 않습니다.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setError('');
        setCoords({ lat: position.coords.latitude, lon: position.coords.longitude });
      },
      () => setError('위치 접근이 거부되었습니다. 기본 위치를 사용합니다.'),
      { enableHighAccuracy: true, timeout: 5000 },
    );
  };

  const failStepValidation = (stepIndex, message) => {
    setError(message);
    setCurrentStep(stepIndex);
    addToast(message, 'error');
    return false;
  };

  const validateStep = (stepIndex) => {
    if (stepIndex === 0) {
      if (!form.branchId) {
        return failStepValidation(stepIndex, '지점을 먼저 선택해 주세요.');
      }
      if (!form.productName.trim()) {
        return failStepValidation(stepIndex, '제품명을 입력해 주세요.');
      }
      return true;
    }

    if (stepIndex === 1) {
      if ((Number(form.totalPrice) || 0) <= 0) {
        return failStepValidation(stepIndex, '총 가격은 1원 이상이어야 합니다.');
      }
      if ((Number(form.totalQuantity) || 0) < 1) {
        return failStepValidation(stepIndex, '총 수량은 1개 이상이어야 합니다.');
      }
      if (!form.unitLabel.trim()) {
        return failStepValidation(stepIndex, '소분 단위 표기를 입력해 주세요.');
      }
      if ((Number(form.minimumShareUnit) || 0) < 1) {
        return failStepValidation(stepIndex, '최소 소분 단위는 1 이상이어야 합니다.');
      }
      if ((Number(form.hostRequestedQuantity) || 0) < 0 || Number(form.hostRequestedQuantity) > Number(form.totalQuantity)) {
        return failStepValidation(stepIndex, '호스트 수량은 0개 이상, 총 수량 이하여야 합니다.');
      }
      return true;
    }

    if (!form.deadlineDate || !form.deadlineTime) {
      return failStepValidation(stepIndex, '모집 마감 날짜와 시간을 모두 입력해 주세요.');
    }
    return true;
  };

  const goToNextStep = () => {
    if (!validateStep(currentStep)) {
      return;
    }
    setError('');
    setCurrentStep((prev) => Math.min(createSteps.length - 1, prev + 1));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!isAuthed) {
      addToast('로그인이 필요합니다.', 'error');
      navigate('/login', { state: { from: `${location.pathname}${location.search}` } });
      return;
    }

    for (let stepIndex = 0; stepIndex < createSteps.length; stepIndex += 1) {
      if (!validateStep(stepIndex)) {
        return;
      }
    }

    try {
      setSubmitting(true);
      await api.createParty({
        title: form.title.trim() || form.productName.trim(),
        storeId: Number(form.branchId),
        productName: form.productName.trim(),
        totalPrice: Number(form.totalPrice),
        totalQuantity: Number(form.totalQuantity),
        hostRequestedQuantity: Number(form.hostRequestedQuantity),
        deadline: `${form.deadlineDate}T${form.deadlineTime}`,
        unitLabel: form.unitLabel.trim(),
        minimumShareUnit: Number(form.minimumShareUnit),
        storageType: form.storageType,
        packagingType: form.packagingType,
        hostProvidesPackaging: Boolean(form.hostProvidesPackaging),
        onSiteSplit: Boolean(form.onSiteSplit),
        guideNote: form.description.trim(),
      });
      addToast('파티가 생성되었습니다.', 'success');
      navigate(`/branch/${form.branchId}`);
    } catch (requestError) {
      const message = requestError.message || '파티 생성 중 오류가 발생했습니다.';
      setError(message);
      addToast(message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5 pb-10">
      <section className="card-elevated p-4 space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">생성 단계</p>
            <h2 className="section-title">{createSteps[currentStep].title}</h2>
            <p className="section-subtitle">{createSteps[currentStep].description}</p>
          </div>
          <span className="badge bg-mint-500/15 text-mint-700">{stepProgressLabel}</span>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          {createSteps.map((step, index) => {
            const done = index < currentStep;
            const active = index === currentStep;

            return (
              <button
                key={step.key}
                type="button"
                onClick={() => {
                  if (index <= currentStep) {
                    setCurrentStep(index);
                    setError('');
                  }
                }}
                disabled={index > currentStep}
                className={[
                  'rounded-2xl border px-4 py-3 text-left transition',
                  active ? 'border-mint-300 bg-mint-50' : 'border-ink/10 bg-white',
                  index > currentStep ? 'cursor-not-allowed opacity-60' : 'hover:border-mint-200',
                ].join(' ')}
              >
                <div className="flex items-center gap-2">
                  <span
                    className={[
                      'flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold',
                      done || active ? 'bg-mint-600 text-white' : 'bg-ink/10 text-ink/60',
                    ].join(' ')}
                  >
                    {done ? <Check size={14} /> : index + 1}
                  </span>
                  <span className="text-sm font-semibold text-ink">{step.title}</span>
                </div>
                <p className="mt-2 text-xs text-ink/55">{step.description}</p>
              </button>
            );
          })}
        </div>
      </section>

      {currentStep === 0 && (
        <>
          <section className="card-elevated p-4 space-y-3">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <MapPin size={18} />
              <span>지점 선택</span>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="block text-sm font-medium text-ink">내 주변 지점</label>
                <button type="button" onClick={requestBrowserLocation} className="btn-ghost text-xs">
                  내 위치로 갱신
                </button>
              </div>
              <div className="relative">
                <select
                  value={form.branchId}
                  onChange={handleChange('branchId')}
                  className="input appearance-none text-sm font-medium"
                  aria-label="내 주변 지점"
                >
                  {branches.map((branch) => (
                    <option key={branch.id} value={branch.id}>
                      {branch.name} · {branch.distance ? `${branch.distance.toFixed(1)}km` : ''}
                    </option>
                  ))}
                </select>
                <ChevronDown size={16} className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-ink/50" />
              </div>
              {branchesLoading && <p className="text-xs text-ink/60">지점 불러오는 중...</p>}
            </div>
          </section>

          <section className="card-elevated p-4 space-y-3">
            <h2 className="section-title">파티 기본 정보</h2>
            <div className="space-y-2">
              <label className="block text-sm text-ink/70">파티 제목</label>
              <input
                type="text"
                value={form.title}
                onChange={handleChange('title')}
                placeholder="예) 비타민 B 소분 모임"
                className="input"
                aria-label="파티 제목"
              />
              <p className="helper-text">비워두면 제품명을 그대로 파티 제목으로 사용합니다.</p>
            </div>
            <div className="space-y-2">
              <label className="block text-sm text-ink/70">제품명</label>
              <input
                type="text"
                value={form.productName}
                onChange={handleChange('productName')}
                placeholder="예) 올리브 오일 2L"
                className="input"
                aria-label="제품명"
              />
            </div>
            <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3 text-sm text-ink/75">
              파티를 생성하면 해당 파티 전용 채팅방이 자동으로 열립니다. 외부 오픈채팅 링크는 더 이상 받지 않습니다.
            </div>
          </section>
        </>
      )}

      {currentStep === 1 && (
        <>
          <section className="card-elevated p-4 space-y-4">
            <h2 className="section-title">가격 및 수량 설정</h2>
            <div className="space-y-2">
              <label className="block text-sm text-ink/70">제품 총 가격</label>
              <div className="input-row">
                <Coins size={16} className="text-mint-700" />
                <input
                  type="number"
                  inputMode="numeric"
                  min="0"
                  value={form.totalPrice}
                  onChange={handleChange('totalPrice')}
                  placeholder="숫자만 입력"
                  className="input-control"
                  aria-label="제품 총 가격"
                />
                <span className="text-xs font-semibold text-ink/60">원</span>
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-sm text-ink/70">제품 총 수량 (1~50개)</label>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => handleQuantityChange(-1)}
                  className="h-10 w-10 rounded-full bg-ink/5 text-lg font-bold text-ink transition hover:bg-ink/10"
                >
                  -
                </button>
                <div className="flex flex-1 items-center gap-3 rounded-xl border border-mint-100 bg-white px-4 py-3 shadow-sm">
                  <Users size={16} className="text-mint-700" />
                  <input
                    type="range"
                    min="1"
                    max="50"
                    value={form.totalQuantity}
                    onChange={handleChange('totalQuantity')}
                    className="flex-1 accent-mint-500"
                    aria-label="제품 총 수량"
                  />
                  <span className="text-sm font-semibold text-ink">
                    {form.totalQuantity}
                    {preview.unitLabel}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => handleQuantityChange(1)}
                  className="h-10 w-10 rounded-full bg-ink/5 text-lg font-bold text-ink transition hover:bg-ink/10"
                >
                  +
                </button>
              </div>
            </div>

            <div className="rounded-xl bg-mint-500/10 px-4 py-3 text-sm font-semibold text-mint-800 space-y-1">
              <div>
                1단위 기준 예상 금액:{' '}
                <span className="text-lg font-bold text-mint-700">
                  {formatAmountRange(preview.baseUnitAmount, preview.unitAmountMax)}
                </span>
              </div>
              <div className="text-xs text-ink/70">
                총 가격과 총 수량 기준입니다.
                {preview.remainderAmount > 0 ? ` 남는 ${preview.remainderAmount}원은 정산 시 수량이 큰 쪽부터 1원씩 배분됩니다.` : ''}
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-sm text-ink/70">호스트 가져갈 수량</label>
              <input
                type="number"
                min="0"
                max={form.totalQuantity}
                value={form.hostRequestedQuantity}
                onChange={handleChange('hostRequestedQuantity')}
                className="input"
                aria-label="호스트 가져갈 수량"
              />
              <p className="helper-text">생성자가 먼저 가져갈 수량입니다.</p>
            </div>
            <div className="rounded-xl bg-ink/5 px-4 py-3 text-sm font-semibold text-ink">
              호스트 예상 부담금:{' '}
              <span className="text-lg font-bold text-ink">
                {formatAmountRange(preview.hostExpectedAmountMin, preview.hostExpectedAmountMax)}
              </span>
            </div>
          </section>

          <section className="card-elevated p-4 space-y-4">
            <h2 className="section-title">소분 방식</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-2">
                <label className="block text-sm text-ink/70">단위 표기</label>
                <input
                  type="text"
                  value={form.unitLabel}
                  onChange={handleChange('unitLabel')}
                  placeholder="예) 개, 팩, g, ml"
                  className="input"
                />
              </div>
              <div className="space-y-2">
                <label className="block text-sm text-ink/70">최소 소분 단위</label>
                <input
                  type="number"
                  min="1"
                  value={form.minimumShareUnit}
                  onChange={handleChange('minimumShareUnit')}
                  className="input"
                  aria-label="최소 소분 단위"
                />
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-2">
                <label className="block text-sm text-ink/70">보관 방식</label>
                <div className="relative">
                  <select
                    value={form.storageType}
                    onChange={handleChange('storageType')}
                    className="input appearance-none text-sm font-medium"
                    aria-label="보관 방식"
                  >
                    {storageOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <ChevronDown size={16} className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-ink/50" />
                </div>
              </div>
              <div className="space-y-2">
                <label className="block text-sm text-ink/70">포장 방식</label>
                <div className="relative">
                  <select
                    value={form.packagingType}
                    onChange={handleChange('packagingType')}
                    className="input appearance-none text-sm font-medium"
                    aria-label="포장 방식"
                  >
                    {packagingOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <ChevronDown size={16} className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-ink/50" />
                </div>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label className="flex items-center justify-between rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink">
                <span>호스트가 포장재 제공</span>
                <input
                  type="checkbox"
                  checked={form.hostProvidesPackaging}
                  onChange={handleBooleanChange('hostProvidesPackaging')}
                  className="h-4 w-4 accent-mint-500"
                />
              </label>
              <label className="flex items-center justify-between rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink">
                <span>현장 소분 진행</span>
                <input
                  type="checkbox"
                  checked={form.onSiteSplit}
                  onChange={handleBooleanChange('onSiteSplit')}
                  className="h-4 w-4 accent-mint-500"
                />
              </label>
            </div>

            <div className="rounded-xl bg-mint-500/10 px-4 py-3 text-sm text-mint-800">
              최소 참여 기준: {Number(form.minimumShareUnit) || 1}
              {form.unitLabel || '개'} 단위로 참여하게 됩니다.
            </div>
          </section>

          <section className="card-elevated p-4 space-y-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <Coins size={18} />
              <span>참여 조건 미리보기</span>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">참여자에게 열리는 수량</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {preview.recruitableQuantity}
                  {preview.unitLabel}
                </p>
                <p className="mt-1 text-xs text-ink/60">총 수량에서 호스트 수량을 제외한 값입니다.</p>
              </div>

              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">최소 기준 참여 1명 예상 금액</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {formatAmountRange(preview.minimumParticipantAmountMin, preview.minimumParticipantAmountMax)}
                </p>
                <p className="mt-1 text-xs text-ink/60">
                  최소 {preview.minimumShareUnit}
                  {preview.unitLabel} 기준입니다.
                </p>
              </div>

              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">최소 단위 기준 최대 참여자 수</p>
                <p className="mt-1 text-lg font-semibold text-ink">{preview.maxParticipantSlots}명</p>
                <p className="mt-1 text-xs text-ink/60">모든 참여자가 최소 단위만 가져간다고 가정한 값입니다.</p>
              </div>

              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">호스트 수량 미리보기</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {preview.hostRequestedQuantity}
                  {preview.unitLabel}
                </p>
                <p className="mt-1 text-xs text-ink/60">호스트가 먼저 가져갈 수량입니다.</p>
              </div>
            </div>

            <div className="space-y-2">
              {preview.warnings.length > 0 ? (
                preview.warnings.map((warning) => (
                  <div
                    key={warning.code}
                    className={`rounded-2xl border px-4 py-3 text-sm leading-6 ${getWarningToneClass(warning.tone)}`}
                  >
                    {warning.message}
                  </div>
                ))
              ) : (
                <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3 text-sm text-mint-800">
                  현재 입력 기준으로는 참여 조건이 자연스럽게 보입니다.
                </div>
              )}
            </div>
          </section>
        </>
      )}

      {currentStep === 2 && (
        <>
          <section className="card-elevated p-4 space-y-4">
            <h2 className="section-title">운영 정보</h2>
            <p className="helper-text">여기서 입력하는 시간은 픽업 시간이 아니라 모집 마감 시각입니다. 픽업 일정은 파티 생성 후 호스트가 채팅 공지와 함께 확정합니다.</p>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-2">
                <label className="block text-sm text-ink/70">모집 마감 날짜</label>
                <div className="input-row">
                  <Calendar size={16} className="text-mint-700" />
                  <input
                    type="date"
                    value={form.deadlineDate}
                    onChange={handleChange('deadlineDate')}
                    className="input-control"
                    aria-label="모집 마감 날짜"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <label className="block text-sm text-ink/70">모집 마감 시간</label>
                <div className="input-row">
                  <Calendar size={16} className="text-mint-700" />
                  <input
                    type="time"
                    value={form.deadlineTime}
                    onChange={handleChange('deadlineTime')}
                    className="input-control"
                    aria-label="모집 마감 시간"
                  />
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <label className="block text-sm text-ink/70">거래 안내</label>
              <textarea
                rows="4"
                value={form.description}
                onChange={handleChange('description')}
                placeholder="예) 행사 가격 기준이라 실제 결제 후 정산 예정, 냉장 보관 필수, 지퍼백 제공합니다."
                className="input"
                aria-label="거래 안내"
              />
            </div>
          </section>

          <section className="card-elevated p-4 space-y-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">최종 확인</p>
                <h2 className="section-title">입력 내용을 확인하고 생성하세요</h2>
              </div>
              <span className="badge bg-ink/5 text-ink/70">전용 채팅방 자동 생성</span>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
                <p className="text-xs text-ink/50">지점</p>
                <p className="mt-1 font-semibold text-ink">{selectedBranch?.name ?? '미선택'}</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
                <p className="text-xs text-ink/50">파티 제목</p>
                <p className="mt-1 font-semibold text-ink">{form.title.trim() || form.productName.trim() || '미입력'}</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
                <p className="text-xs text-ink/50">제품명</p>
                <p className="mt-1 font-semibold text-ink">{form.productName.trim() || '미입력'}</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
                <p className="text-xs text-ink/50">모집 마감</p>
                <p className="mt-1 font-semibold text-ink">{formatDeadlinePreview(form.deadlineDate, form.deadlineTime)}</p>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">총 가격</p>
                <p className="mt-1 text-lg font-semibold text-ink">{formatCurrency(Number(form.totalPrice) || 0)}</p>
              </div>
              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">1단위 기준 예상 금액</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {formatAmountRange(preview.baseUnitAmount, preview.unitAmountMax)}
                </p>
              </div>
              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">최소 기준 참여 1명 예상 금액</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {formatAmountRange(preview.minimumParticipantAmountMin, preview.minimumParticipantAmountMax)}
                </p>
              </div>
              <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <p className="text-xs text-mint-800">호스트 예상 부담금</p>
                <p className="mt-1 text-lg font-semibold text-ink">
                  {formatAmountRange(preview.hostExpectedAmountMin, preview.hostExpectedAmountMax)}
                </p>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
                <p className="text-xs text-ink/50">소분 기준</p>
                <p className="mt-1 font-semibold text-ink">
                  총 {form.totalQuantity}
                  {preview.unitLabel} · 최소 {form.minimumShareUnit}
                  {preview.unitLabel}
                </p>
                <p className="mt-2 text-xs text-ink/55">
                  호스트 {preview.hostRequestedQuantity}
                  {preview.unitLabel} 제외 후 참여자에게 {preview.recruitableQuantity}
                  {preview.unitLabel}가 열립니다.
                </p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
                <p className="text-xs text-ink/50">보관 및 포장</p>
                <p className="mt-1 font-semibold text-ink">
                  {storageOptions.find((option) => option.value === form.storageType)?.label} ·{' '}
                  {packagingOptions.find((option) => option.value === form.packagingType)?.label}
                </p>
                <p className="mt-2 text-xs text-ink/55">
                  {preview.maxParticipantSlots > 0
                    ? `최소 단위 기준 최대 ${preview.maxParticipantSlots}명까지 자연스럽게 모집할 수 있습니다.`
                    : '현재 입력 기준으로는 최소 단위 참여자를 더 받기 어렵습니다.'}
                </p>
              </div>
            </div>

            {preview.warnings.length > 0 && (
              <div className="space-y-2">
                {preview.warnings.map((warning) => (
                  <div
                    key={`summary-${warning.code}`}
                    className={`rounded-2xl border px-4 py-3 text-sm leading-6 ${getWarningToneClass(warning.tone)}`}
                  >
                    {warning.message}
                  </div>
                ))}
              </div>
            )}

            <div className="rounded-2xl border border-ink/10 px-4 py-3 text-sm text-ink/75">
              <p className="text-xs text-ink/50">거래 안내</p>
              <p className="mt-2 whitespace-pre-line leading-6 text-ink">{form.description.trim() || '별도 안내 없음'}</p>
            </div>
          </section>
        </>
      )}

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <button
          type="button"
          onClick={() => {
            setError('');
            setCurrentStep((prev) => Math.max(0, prev - 1));
          }}
          className="btn-secondary px-4 py-2 text-sm"
          disabled={currentStep === 0 || submitting}
        >
          이전 단계
        </button>

        {currentStep < createSteps.length - 1 ? (
          <button type="button" onClick={goToNextStep} className="btn-primary px-4 py-2 text-sm" disabled={submitting}>
            다음 단계
          </button>
        ) : (
          <button type="submit" disabled={submitting} className="btn-primary px-4 py-2 text-sm">
            {submitting ? '생성 중...' : '파티 생성하기'}
          </button>
        )}
      </div>
    </form>
  );
}

export default CreateParty;
