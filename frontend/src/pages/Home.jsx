import { useEffect, useMemo, useState } from 'react';
import { MapPin, ArrowRight, ChevronDown, Navigation } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

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
  const [coords, setCoords] = useState({ lat: 37.5665, lon: 126.978 }); // 기본: 서울 시청
  const [storedCoords, setStoredCoords] = useState(null);
  const [useBrowserLocation, setUseBrowserLocation] = useState(false);

  const handleBranchClick = (id) => navigate(`/branch/${id}`);

  useEffect(() => {
    const fetchUserLocation = async () => {
      if (!isAuthed) return;
      try {
        const me = await api.getMe();
        if (me.latitude && me.longitude) {
          const saved = { lat: me.latitude, lon: me.longitude };
          setStoredCoords(saved);
          setCoords({ lat: me.latitude, lon: me.longitude });
          setCurrentLocation('저장 위치 기준');
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
    if (!window.confirm('브라우저 위치를 사용해 주변 지점을 다시 불러올까요?')) {
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
      () => setError('위치 접근이 거부되었습니다. 저장 위치나 기본 위치를 사용합니다.'),
      { enableHighAccuracy: true, timeout: 5000 },
    );
  };

  const revertToStored = () => {
    if (storedCoords) {
      setCoords(storedCoords);
      setCurrentLocation('저장 위치 기준');
      setCoordNote(`(${storedCoords.lat.toFixed(4)}, ${storedCoords.lon.toFixed(4)})`);
      setUseBrowserLocation(false);
    } else {
      setCurrentLocation('기본 위치');
      setCoordNote(`(${coords.lat.toFixed(4)}, ${coords.lon.toFixed(4)})`);
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
    const fetchBranches = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await api.getNearbyStores({ latitude: coords.lat, longitude: coords.lon });
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
        setError('지점 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    fetchBranches();
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
            {useBrowserLocation ? '저장 위치로' : '내 위치 기반'}
            <ChevronDown size={14} />
          </button>
        </div>
        <p className="mt-2 section-subtitle">
          가까운 지점에서 함께 구매할 수 있는 파티를 찾아보세요.
        </p>
      </section>

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="section-title">내 주변 지점</h2>
          <button className="btn-ghost text-xs">
            <Navigation size={14} />
            거리순
          </button>
        </div>

        {loading && <p className="text-sm text-ink/60">지점 불러오는 중...</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}
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
