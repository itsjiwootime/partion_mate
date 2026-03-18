import { useEffect, useState } from 'react';
import { MapPin, ArrowRight, ChevronDown, Search, Refrigerator, Package2, CircleDot } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { EmptyState, LoadingState } from '../components/Feedback';
import { buildPartyDiscoverySearch } from '../utils/partyDiscovery';

const DEFAULT_COORDS = { lat: 37.5665, lon: 126.978 };

const BrandBadge = ({ brand }) => {
  const isCostco = brand.includes('코스트코');
  const tone = isCostco ? 'bg-mint-500/15 text-mint-700' : 'bg-ink/5 text-ink';
  return <span className={`badge ${tone}`}>{brand}</span>;
};

function Home() {
  const navigate = useNavigate();
  const { isAuthed } = useAuth();
  const [currentLocation, setCurrentLocation] = useState('기본 위치');
  const [coordNote, setCoordNote] = useState('');
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [coords, setCoords] = useState(DEFAULT_COORDS); // 기본: 서울 시청
  const [storedCoords, setStoredCoords] = useState(null);
  const [useBrowserLocation, setUseBrowserLocation] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const handleBranchClick = (id) => navigate(`/branch/${id}`);
  const navigateToPartyDiscovery = (filters = {}) => {
    const search = buildPartyDiscoverySearch(filters).toString();
    navigate({
      pathname: '/parties',
      search: search ? `?${search}` : '',
    });
  };

  useEffect(() => {
    const fetchUserLocation = async () => {
      if (!isAuthed) return;
      try {
        const me = await api.getMe();
        if (me.latitude && me.longitude) {
          const saved = { lat: me.latitude, lon: me.longitude };
          setStoredCoords(saved);
          setCoords({ lat: me.latitude, lon: me.longitude });
          setCurrentLocation('내 주소 기준');
          setCoordNote(`(${me.latitude.toFixed(4)}, ${me.longitude.toFixed(4)})`);
          setUseBrowserLocation(false);
        }
      } catch {
        // 무시: 프로필 호출 실패시 기본 좌표 사용
      }
    };
    fetchUserLocation();
  }, [isAuthed]);

  const requestBrowserLocation = () => {
    if (!navigator.geolocation) {
      setError('이 브라우저는 위치 기능을 지원하지 않습니다.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setError('');
        setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        setCurrentLocation('내 위치(브라우저) 기준');
        setCoordNote(`(${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)})`);
        setUseBrowserLocation(true);
      },
      () => setError('위치 접근이 거부되었습니다. 내 주소 기준이나 기본 위치를 사용합니다.'),
      { enableHighAccuracy: true, timeout: 5000 },
    );
  };

  const revertToStored = () => {
    if (storedCoords) {
      setCoords(storedCoords);
      setCurrentLocation('내 주소 기준');
      setCoordNote(`(${storedCoords.lat.toFixed(4)}, ${storedCoords.lon.toFixed(4)})`);
      setUseBrowserLocation(false);
    } else {
      setCoords(DEFAULT_COORDS);
      setCurrentLocation('기본 위치');
      setCoordNote(`(${DEFAULT_COORDS.lat.toFixed(4)}, ${DEFAULT_COORDS.lon.toFixed(4)})`);
      setUseBrowserLocation(false);
    }
  };

  const handleLocationToggle = () => {
    if (useBrowserLocation) {
      revertToStored();
    } else {
      requestBrowserLocation();
    }
  };

  useEffect(() => {
    const controller = new AbortController();

    const fetchBranches = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await api.getNearbyStores(
          { latitude: coords.lat, longitude: coords.lon },
          { signal: controller.signal },
        );
        setBranches(
          data.map((item) => ({
            id: item.id, // 백엔드 store.id (숫자) 그대로 사용
            name: item.name,
            distance: item.distance ? `${item.distance.toFixed(1)}km` : '—',
            activeParties: item.partyCount ?? 0,
            brand: item.name.includes('코스트코') ? '코스트코' : '트레이더스',
          })),
        );
      } catch (e) {
        if (e.name === 'AbortError') {
          return;
        }
        setError('지점 정보를 불러오지 못했습니다.');
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };
    fetchBranches();

    return () => controller.abort();
  }, [coords]);

  return (
    <div className="space-y-5">
      <section className="card-elevated p-4">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
            <MapPin size={18} />
            <span>{currentLocation}</span>
            {coordNote && <span className="text-ink/50">{coordNote}</span>}
          </div>
          <button onClick={handleLocationToggle} className="btn-primary px-3 py-1 text-xs">
            {useBrowserLocation ? '내 주소로' : '내 위치 기반'}
            <ChevronDown size={14} />
          </button>
        </div>
        <p className="mt-2 section-subtitle">
          가까운 지점에서 함께 구매할 수 있는 파티를 찾아보세요.
        </p>
      </section>

      <section className="card-elevated p-4 space-y-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">빠른 탐색</p>
          <h2 className="section-title">원하는 파티를 바로 찾아보세요</h2>
          <p className="section-subtitle">상품명, 지점명, 파티 제목 검색과 대표 필터를 바로 적용할 수 있습니다.</p>
        </div>

        <div className="input-row">
          <Search size={16} className="text-mint-700" />
          <input
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            className="input-control"
            placeholder="예: 휴지, 코스트코 양재점, 냉동 만두"
            aria-label="홈 파티 검색"
          />
          <button
            type="button"
            onClick={() => navigateToPartyDiscovery({ query: searchQuery })}
            className="btn-primary px-4 py-2 text-sm"
          >
            검색
          </button>
        </div>

        <div className="grid gap-2 sm:grid-cols-3">
          <button onClick={() => navigateToPartyDiscovery({ status: 'active' })} className="card p-4 text-left">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <CircleDot size={16} />
              모집 중만
            </div>
            <p className="mt-2 text-sm text-ink/65">지금 바로 참여 가능한 파티만 빠르게 모아봅니다.</p>
          </button>
          <button onClick={() => navigateToPartyDiscovery({ storage: 'REFRIGERATED' })} className="card p-4 text-left">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <Refrigerator size={16} />
              냉장 식품
            </div>
            <p className="mt-2 text-sm text-ink/65">보관이 까다로운 냉장 상품 파티만 바로 찾습니다.</p>
          </button>
          <button onClick={() => navigateToPartyDiscovery({ unit: 'ONE' })} className="card p-4 text-left">
            <div className="flex items-center gap-2 text-sm font-semibold text-mint-700">
              <Package2 size={16} />
              1개 소분
            </div>
            <p className="mt-2 text-sm text-ink/65">부담이 적은 1개 단위 소분 파티부터 살펴봅니다.</p>
          </button>
        </div>
      </section>

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="section-title">내 주변 지점</h2>
          <span className="badge bg-mint-500/10 text-mint-800">거리순 정렬</span>
        </div>

        {loading && <LoadingState message="주변 지점을 불러오는 중..." />}
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!loading && !error && branches.length === 0 && (
          <EmptyState
            title="주변 지점을 찾지 못했어요"
            description="위치를 다시 불러오거나 잠시 후 다시 시도해보세요."
            action={
              <button type="button" onClick={requestBrowserLocation} className="btn-secondary px-4 py-2 text-sm">
                내 위치 다시 불러오기
              </button>
            }
          />
        )}
        <div className="grid gap-3">
          {branches.map((branch) => (
            <button
              key={branch.id}
              onClick={() => handleBranchClick(branch.id)}
              className="card card-hover flex w-full items-center justify-between rounded-xl p-4 text-left"
            >
              <div className="space-y-2">
                <BrandBadge brand={branch.brand} />
                <p className="text-base font-semibold text-ink">{branch.name}</p>
                <div className="flex items-center gap-3 text-sm text-ink/70">
                  <span className="flex items-center gap-1">
                    <MapPin size={14} className="text-mint-700" />
                    {branch.distance}
                  </span>
                  <span className="h-1 w-1 rounded-full bg-ink/10" />
                  <span className="font-medium text-mint-700">
                    진행중 파티 {branch.activeParties}개
                  </span>
                </div>
              </div>
              <ArrowRight size={18} className="text-ink/50" />
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

export default Home;
