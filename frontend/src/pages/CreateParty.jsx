import { useMemo, useState, useEffect } from 'react';
import { Calendar, Coins, MapPin, Users, ChevronDown } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const categories = ['식품', '건강/의약', '생활용품', '주방', '기타'];

function CreateParty() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthed } = useAuth();
  const { addToast } = useToast();
  const [branches, setBranches] = useState([]);
  const [branchesLoading, setBranchesLoading] = useState(false);
  const defaultStoreId = searchParams.get('storeId') ?? '';
  const [form, setForm] = useState({
    branchId: defaultStoreId,
    productName: '',
    category: categories[0],
    totalPrice: '',
    totalQuantity: 4,
    date: '',
    time: '',
    description: '',
    title: '',
    hostRequestedQuantity: 1,
    openChatUrl: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [coords, setCoords] = useState({ lat: 37.5665, lon: 126.978 });

  const perUnit = useMemo(() => {
    const total = Number(form.totalPrice) || 0;
    const qty = Number(form.totalQuantity) || 0;
    return total > 0 && qty > 0 ? Math.round(total / qty) : 0;
  }, [form.totalPrice, form.totalQuantity]);

  const hostExpected = useMemo(() => {
    return perUnit * (Number(form.hostRequestedQuantity) || 0);
  }, [perUnit, form.hostRequestedQuantity]);

  const handleChange = (key) => (e) => {
    setForm((prev) => ({ ...prev, [key]: e.target.value }));
  };

  const handleQuantityChange = (delta) => {
    setForm((prev) => {
      const next = Math.min(50, Math.max(1, Number(prev.totalQuantity) + delta));
      const adjustedHost = Math.min(next, Number(prev.hostRequestedQuantity));
      return { ...prev, totalQuantity: next, hostRequestedQuantity: adjustedHost };
    });
  };

  useEffect(() => {
    const fetchBranches = async () => {
      try {
        setBranchesLoading(true);
        const data = await api.getNearbyStores({ latitude: coords.lat, longitude: coords.lon });
        setBranches(data);
        if (!form.branchId && data.length > 0) {
          setForm((prev) => ({ ...prev, branchId: data[0].id }));
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

  const requestBrowserLocation = () => {
    if (!navigator.geolocation) {
      setError('이 브라우저는 위치 기능을 지원하지 않습니다.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
      },
      () => setError('위치 접근이 거부되었습니다. 기본 위치를 사용합니다.'),
      { enableHighAccuracy: true, timeout: 5000 },
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!isAuthed) {
      addToast('로그인이 필요합니다.', 'error');
      navigate('/login');
      return;
    }
    try {
      setSubmitting(true);
      await api.createParty({
        title: form.title || form.productName,
        storeId: Number(form.branchId),
        productName: form.productName,
        totalPrice: Number(form.totalPrice),
        totalQuantity: Number(form.totalQuantity),
        hostRequestedQuantity: Number(form.hostRequestedQuantity) || 1,
        openChatUrl: form.openChatUrl,
      });
      addToast('파티가 생성되었습니다.', 'success');
      navigate(`/branch/${form.branchId}`);
    } catch (err) {
      setError(err.message || '파티 생성 중 오류가 발생했습니다.');
      addToast(err.message || '파티 생성 중 오류가 발생했습니다.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-5">
      <section className="card-elevated p-4 space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
          <MapPin size={18} />
          <span>지점 선택</span>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label className="block text-sm font-medium text-ink">내 주변 지점</label>
            <button
              type="button"
              onClick={requestBrowserLocation}
              className="btn-ghost text-xs"
            >
              내 위치로 갱신
            </button>
          </div>
          <div className="relative">
            <select
              value={form.branchId}
              onChange={handleChange('branchId')}
              className="input appearance-none text-sm font-medium"
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
        <h2 className="section-title">제품 정보</h2>
        <div className="space-y-2">
          <label className="block text-sm text-ink/70">파티 제목</label>
          <input
            type="text"
            value={form.title}
            onChange={handleChange('title')}
            placeholder="예) 비타민 B 소분 모임"
            className="input"
          />
        </div>
        <div className="space-y-2">
          <label className="block text-sm text-ink/70">제품명</label>
          <input
            type="text"
            value={form.productName}
            onChange={handleChange('productName')}
            placeholder="예) 올리브 오일 2L"
            className="input"
          />
        </div>
        <div className="space-y-2">
          <label className="block text-sm text-ink/70">카테고리</label>
          <div className="relative">
            <select
              value={form.category}
              onChange={handleChange('category')}
              className="input appearance-none text-sm font-medium"
            >
              {categories.map((cat) => (
                <option key={cat}>{cat}</option>
              ))}
            </select>
            <ChevronDown size={16} className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-ink/50" />
          </div>
        </div>
      </section>

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
              />
              <span className="text-sm font-semibold text-ink">{form.totalQuantity}개</span>
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
          <div>개당 예상 가격: <span className="text-lg font-bold text-mint-700">{perUnit.toLocaleString()}원</span></div>
          <div className="text-xs text-ink/70">총 가격 ÷ 총 수량으로 계산됩니다.</div>
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
          />
          <p className="helper-text">생성자가 가져갈 수량입니다.</p>
        </div>
        <div className="rounded-xl bg-ink/5 px-4 py-3 text-sm font-semibold text-ink">
          호스트 예상 부담금: <span className="text-lg font-bold text-ink">{hostExpected.toLocaleString()}원</span>
        </div>
      </section>

      <section className="card-elevated p-4 space-y-4">
        <h2 className="section-title">추가 정보</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-2">
            <label className="block text-sm text-ink/70">모임 날짜</label>
            <div className="input-row">
              <Calendar size={16} className="text-mint-700" />
              <input
                type="date"
                value={form.date}
                onChange={handleChange('date')}
                className="input-control"
              />
            </div>
          </div>
          <div className="space-y-2">
            <label className="block text-sm text-ink/70">모임 시간</label>
            <div className="input-row">
              <Calendar size={16} className="text-mint-700" />
              <input
                type="time"
                value={form.time}
                onChange={handleChange('time')}
                className="input-control"
              />
            </div>
          </div>
        </div>
        <div className="space-y-2">
          <label className="block text-sm text-ink/70">상세 설명</label>
          <textarea
            rows="4"
            value={form.description}
            onChange={handleChange('description')}
            placeholder="예) 현장에서 소분 예정, 비닐/지퍼백 제공합니다."
            className="input"
          />
        </div>
        <div className="space-y-2">
          <label className="block text-sm text-ink/70">카카오톡 오픈채팅 링크</label>
          <input
            type="url"
            value={form.openChatUrl}
            onChange={handleChange('openChatUrl')}
            placeholder="https://open.kakao.com/..."
            className="input"
          />
          <p className="helper-text">참여자들이 바로 입장할 수 있도록 오픈채팅 링크를 입력하세요.</p>
        </div>
      </section>

      <form onSubmit={handleSubmit} className="pb-10">
        {error && <p className="mb-2 text-sm text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="btn-primary w-full"
        >
          {submitting ? '생성 중...' : '파티 생성하기'}
        </button>
      </form>
    </div>
  );
}

export default CreateParty;
