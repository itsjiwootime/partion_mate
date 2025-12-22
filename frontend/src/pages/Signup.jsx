import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Mail, Lock, User, MapPin, Navigation } from 'lucide-react';
import { useToast } from '../context/ToastContext';
import { api } from '../api/client';

function Signup() {
  const { signup } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: '',
    password: '',
    username: '',
    address: '',
    latitude: '',
    longitude: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [usernameChecked, setUsernameChecked] = useState(false);

  const handleChange = (key) => (e) => setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const handleCheckUsername = async () => {
    if (!form.username) {
      setError('닉네임을 입력해주세요.');
      return;
    }
    try {
      setLoading(true);
      setError('');
      await api.checkUsername(form.username);
      setUsernameChecked(true);
      addToast('사용 가능한 닉네임입니다.', 'success');
    } catch (err) {
      setUsernameChecked(false);
      setError(err.message || '닉네임 확인에 실패했습니다.');
      addToast(err.message || '닉네임 확인에 실패했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    const lat = parseFloat(form.latitude);
    const lon = parseFloat(form.longitude);
    if (Number.isNaN(lat) || Number.isNaN(lon)) {
      setError('위도/경도를 확인해주세요.');
      return;
    }
    if (!usernameChecked) {
      setError('닉네임 중복 확인이 필요합니다.');
      return;
    }
    try {
      setLoading(true);
      await signup({
        email: form.email,
        password: form.password,
        username: form.username,
        address: form.address,
        latitude: lat,
        longitude: lon,
      });
      setSuccess(true);
      addToast('회원가입이 완료되었습니다. 로그인 해주세요.', 'success');
      setTimeout(() => navigate('/login'), 800);
    } catch (err) {
      setError(err.message || '회원가입에 실패했습니다.');
      addToast(err.message || '회원가입에 실패했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleGeoLocation = () => {
    if (!navigator.geolocation) {
      setError('이 브라우저는 위치 기능을 지원하지 않습니다.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setError('');
        setForm((prev) => ({
          ...prev,
          latitude: pos.coords.latitude.toFixed(6),
          longitude: pos.coords.longitude.toFixed(6),
        }));
      },
      () => setError('위치 접근이 거부되었습니다. 수동 입력을 해주세요.'),
      { enableHighAccuracy: true, timeout: 5000 },
    );
  };

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-semibold text-ink">회원가입</h1>
        <p className="text-sm text-ink/60">이메일과 비밀번호를 설정하세요.</p>
      </div>
      <form onSubmit={handleSubmit} className="space-y-4 glass-panel rounded-2xl p-5">
        {error && <p className="text-sm text-red-600">{error}</p>}
        {success && <p className="text-sm text-mint-700">가입 완료! 로그인 페이지로 이동합니다.</p>}
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">이메일</span>
          <div className="flex items-center gap-2 rounded-xl border border-mint-100 bg-white px-4 py-3 shadow-sm">
            <Mail size={16} className="text-mint-700" />
            <input
              type="email"
              required
              value={form.email}
              onChange={handleChange('email')}
              className="w-full bg-transparent text-sm outline-none"
              placeholder="you@example.com"
            />
          </div>
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">비밀번호</span>
          <div className="flex items-center gap-2 rounded-xl border border-mint-100 bg-white px-4 py-3 shadow-sm">
            <Lock size={16} className="text-mint-700" />
            <input
              type="password"
              required
              value={form.password}
              onChange={handleChange('password')}
              className="w-full bg-transparent text-sm outline-none"
              placeholder="********"
            />
          </div>
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">닉네임</span>
          <div className="flex items-center gap-2 rounded-xl border border-mint-100 bg-white px-3 py-2 shadow-sm">
            <User size={16} className="text-mint-700" />
            <input
              type="text"
              required
              value={form.username}
              onChange={(e) => {
                setUsernameChecked(false);
                handleChange('username')(e);
              }}
              className="w-full bg-transparent text-sm outline-none"
              placeholder="닉네임"
            />
            <button
              type="button"
              onClick={handleCheckUsername}
              className={[
                'whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold',
                usernameChecked ? 'bg-mint-500 text-white' : 'bg-ink/5 text-ink',
              ].join(' ')}
            >
              {usernameChecked ? '확인됨' : '중복 확인'}
            </button>
          </div>
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">주소(위치)</span>
          <div className="flex items-center gap-2 rounded-xl border border-mint-100 bg-white px-4 py-3 shadow-sm">
            <MapPin size={16} className="text-mint-700" />
            <input
              type="text"
              required
              value={form.address}
              onChange={handleChange('address')}
              className="w-full bg-transparent text-sm outline-none"
              placeholder="예: 서울 마포구"
            />
          </div>
        </label>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="block space-y-1 text-sm">
            <span className="text-ink/70">위도</span>
            <input
              type="number"
              step="any"
              required
              value={form.latitude}
              onChange={handleChange('latitude')}
              className="w-full rounded-xl border border-mint-100 bg-white px-4 py-3 text-sm shadow-sm outline-none ring-mint-200 focus:ring-2"
              placeholder="자동 채우기 또는 직접 입력"
            />
          </label>
          <label className="block space-y-1 text-sm">
            <span className="text-ink/70">경도</span>
            <input
              type="number"
              step="any"
              required
              value={form.longitude}
              onChange={handleChange('longitude')}
              className="w-full rounded-xl border border-mint-100 bg-white px-4 py-3 text-sm shadow-sm outline-none ring-mint-200 focus:ring-2"
              placeholder="자동 채우기 또는 직접 입력"
            />
          </label>
          <button
            type="button"
            onClick={handleGeoLocation}
            className="flex items-center justify-center gap-2 rounded-xl bg-mint-500 px-4 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-mint-600 sm:col-span-2"
          >
            <Navigation size={16} />
            현재 위치로 자동 입력
          </button>
        </div>
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-mint-500 px-4 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-mint-600 disabled:cursor-not-allowed disabled:bg-ink/20"
        >
          {loading ? '가입 중...' : '회원가입'}
        </button>
      </form>
      <p className="text-center text-sm text-ink/60">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="font-semibold text-mint-700 hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}

export default Signup;
