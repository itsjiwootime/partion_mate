import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Mail, Lock, User, MapPin } from 'lucide-react';
import { useToast } from '../context/ToastContext';
import { api } from '../api/client';
import { loadKakaoMapsSdk } from '../utils/kakaoMaps';
import { loadKakaoPostcode } from '../utils/kakaoPostcode';

function Signup() {
  const { signup } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: '',
    password: '',
    passwordConfirm: '',
    username: '',
    address: '',
    latitude: '',
    longitude: '',
  });
  const [loading, setLoading] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [usernameChecked, setUsernameChecked] = useState(false);
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailStatus, setEmailStatus] = useState('idle');
  const [emailStatusMessage, setEmailStatusMessage] = useState('');
  const [emailChecking, setEmailChecking] = useState(false);
  const [usernameStatus, setUsernameStatus] = useState('idle');
  const [usernameStatusMessage, setUsernameStatusMessage] = useState('');
  const [lastCheckedUsername, setLastCheckedUsername] = useState('');
  const [lastCheckedStatus, setLastCheckedStatus] = useState('idle');
  const [lastCheckedMessage, setLastCheckedMessage] = useState('');
  const usernameCheckSeq = useRef(0);
  const kakaoMapKey = import.meta.env.VITE_KAKAO_MAP_KEY;

  const handleChange = (key) => (e) => setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const handleEmailChange = (e) => {
    const nextValue = e.target.value;
    setEmailChecked(false);
    setEmailStatus('idle');
    setEmailStatusMessage('');
    setForm((prev) => ({ ...prev, email: nextValue }));
  };

  const handleCheckEmail = async () => {
    if (!form.email) {
      setEmailStatus('invalid');
      setEmailStatusMessage('이메일을 입력해주세요.');
      return;
    }
    if (emailChecking) return;
    setError('');
    setEmailChecking(true);
    setEmailStatus('checking');
    setEmailStatusMessage('확인 중...');
    try {
      await api.checkEmail(form.email);
      setEmailChecked(true);
      setEmailStatus('valid');
      setEmailStatusMessage('사용 가능한 이메일입니다.');
    } catch (err) {
      setEmailChecked(false);
      setEmailStatus('invalid');
      setEmailStatusMessage(err.message || '이메일 확인에 실패했습니다.');
    } finally {
      setEmailChecking(false);
    }
  };

  const runUsernameCheck = async (username) => {
    if (!username || username.trim().length < 2) {
      setUsernameChecked(false);
      setUsernameStatus('idle');
      setUsernameStatusMessage(username ? '닉네임은 2자 이상 입력해주세요.' : '');
      return false;
    }
    if (username === lastCheckedUsername) {
      setUsernameStatus(lastCheckedStatus);
      setUsernameStatusMessage(lastCheckedMessage);
      setUsernameChecked(lastCheckedStatus === 'valid');
      return lastCheckedStatus === 'valid';
    }
    const seq = (usernameCheckSeq.current += 1);
    setUsernameStatus('checking');
    setUsernameStatusMessage('확인 중...');
    try {
      await api.checkUsername(username);
      if (seq !== usernameCheckSeq.current) return false;
      setUsernameChecked(true);
      setUsernameStatus('valid');
      setUsernameStatusMessage('사용 가능한 닉네임입니다.');
      setLastCheckedUsername(username);
      setLastCheckedStatus('valid');
      setLastCheckedMessage('사용 가능한 닉네임입니다.');
      return true;
    } catch (err) {
      if (seq !== usernameCheckSeq.current) return false;
      const message = err.message || '닉네임 확인에 실패했습니다.';
      setUsernameChecked(false);
      setUsernameStatus('invalid');
      setUsernameStatusMessage(message);
      setLastCheckedUsername(username);
      setLastCheckedStatus('invalid');
      setLastCheckedMessage(message);
      return false;
    }
  };

  useEffect(() => {
    if (!form.username || form.username.trim().length < 2) {
      setUsernameChecked(false);
      setUsernameStatus('idle');
      setUsernameStatusMessage(form.username ? '닉네임은 2자 이상 입력해주세요.' : '');
      return;
    }
    const timer = setTimeout(() => {
      runUsernameCheck(form.username);
    }, 700);
    return () => clearTimeout(timer);
  }, [form.username]);

  const handleUsernameChange = (e) => {
    const nextValue = e.target.value;
    setUsernameChecked(false);
    setUsernameStatus('idle');
    setUsernameStatusMessage('');
    setForm((prev) => ({ ...prev, username: nextValue }));
  };

  const handleUsernameBlur = () => {
    if (!form.username || form.username.trim().length < 2) return;
    runUsernameCheck(form.username);
  };

  const geocodeAddress = async (address) => {
    const kakao = await loadKakaoMapsSdk(kakaoMapKey);
    const geocoder = new kakao.maps.services.Geocoder();
    return new Promise((resolve, reject) => {
      geocoder.addressSearch(address, (result, status) => {
        if (status !== kakao.maps.services.Status.OK || !result || result.length === 0) {
          reject(new Error('주소를 찾지 못했습니다. 정확한 주소를 입력해주세요.'));
          return;
        }
        const { y, x } = result[0];
        resolve({ latitude: Number(y).toFixed(6), longitude: Number(x).toFixed(6) });
      });
    });
  };

  const handleAddressChange = (e) => {
    const nextAddress = e.target.value;
    setForm((prev) => ({
      ...prev,
      address: nextAddress,
      latitude: '',
      longitude: '',
    }));
  };

  const handleAddressBlur = async () => {
    if (!form.address || locationLoading) return;
    setError('');
    setLocationLoading(true);
    try {
      const coords = await geocodeAddress(form.address);
      setForm((prev) => ({ ...prev, ...coords }));
    } catch (err) {
      setError(err.message || '주소 검색을 먼저 해주세요.');
    } finally {
      setLocationLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    let lat = parseFloat(form.latitude);
    let lon = parseFloat(form.longitude);
    if (Number.isNaN(lat) || Number.isNaN(lon)) {
      if (!form.address) {
        setError('주소 검색을 먼저 해주세요.');
        return;
      }
      try {
        const coords = await geocodeAddress(form.address);
        setForm((prev) => ({ ...prev, ...coords }));
        lat = parseFloat(coords.latitude);
        lon = parseFloat(coords.longitude);
      } catch (err) {
        setError(err.message || '주소 검색을 먼저 해주세요.');
        return;
      }
    }
    if (!usernameChecked) {
      setError('닉네임 중복 확인이 필요합니다.');
      return;
    }
    if (!emailChecked) {
      setError('이메일 중복 확인이 필요합니다.');
      return;
    }
    if (!form.passwordConfirm || form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
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

  const handleOpenPostcode = async () => {
    setError('');
    setLocationLoading(true);
    try {
      const daum = await loadKakaoPostcode();
      new daum.Postcode({
        oncomplete: async (data) => {
          const selectedAddress = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;
          const nextAddress = selectedAddress || data.address;
          if (!nextAddress) {
            setError('주소를 확인해주세요.');
            setLocationLoading(false);
            return;
          }
          setForm((prev) => ({ ...prev, address: nextAddress }));
          try {
            const coords = await geocodeAddress(nextAddress);
            setForm((prev) => ({ ...prev, ...coords }));
          } catch (err) {
            setError(err.message || '카카오맵 위치 변환에 실패했습니다.');
          } finally {
            setLocationLoading(false);
          }
        },
        onclose: () => {
          setLocationLoading(false);
        },
      }).open();
    } catch (err) {
      setLocationLoading(false);
      setError(err.message || '주소 검색을 불러오지 못했습니다.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-semibold text-ink">회원가입</h1>
        <p className="section-subtitle">이메일과 비밀번호를 설정하세요.</p>
      </div>
      <form onSubmit={handleSubmit} className="card-elevated space-y-4 p-5">
        {error && <p className="text-sm text-red-600">{error}</p>}
        {success && <p className="text-sm text-mint-700">가입 완료! 로그인 페이지로 이동합니다.</p>}
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">이메일</span>
          <div className="input-row">
            <Mail size={16} className="text-mint-700" />
            <input
              type="email"
              required
              value={form.email}
              onChange={handleEmailChange}
              className="input-control"
              placeholder="you@example.com"
            />
            <button
              type="button"
              onClick={handleCheckEmail}
              disabled={emailChecking || !form.email}
              className="btn-secondary whitespace-nowrap px-3 py-1 text-xs"
            >
              중복 확인
            </button>
          </div>
          {emailStatusMessage && (
            <p
              className={[
                'text-xs',
                emailStatus === 'valid' ? 'text-mint-700' : '',
                emailStatus === 'invalid' ? 'text-red-600' : '',
                emailStatus === 'checking' ? 'text-ink/60' : '',
              ].join(' ')}
            >
              {emailStatusMessage}
            </p>
          )}
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">비밀번호</span>
          <div className="input-row">
            <Lock size={16} className="text-mint-700" />
            <input
              type="password"
              required
              value={form.password}
              onChange={handleChange('password')}
              className="input-control"
              placeholder="********"
            />
          </div>
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">비밀번호 확인</span>
          <div className="input-row">
            <Lock size={16} className="text-mint-700" />
            <input
              type="password"
              required
              value={form.passwordConfirm}
              onChange={handleChange('passwordConfirm')}
              className="input-control"
              placeholder="********"
            />
          </div>
          {form.passwordConfirm && (
            <p
              className={[
                'text-xs',
                form.password === form.passwordConfirm ? 'text-mint-700' : 'text-red-600',
              ].join(' ')}
            >
              {form.password === form.passwordConfirm ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
            </p>
          )}
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">닉네임</span>
          <div className="input-row px-3 py-2">
            <User size={16} className="text-mint-700" />
            <input
              type="text"
              required
              value={form.username}
              onChange={handleUsernameChange}
              onBlur={handleUsernameBlur}
              className="input-control"
              placeholder="닉네임"
            />
          </div>
          {usernameStatusMessage && (
            <p
              className={[
                'text-xs',
                usernameStatus === 'valid' ? 'text-mint-700' : '',
                usernameStatus === 'invalid' ? 'text-red-600' : '',
                usernameStatus === 'checking' ? 'text-ink/60' : '',
              ].join(' ')}
            >
              {usernameStatusMessage}
            </p>
          )}
        </label>
        <label className="block space-y-1 text-sm">
          <span className="text-ink/70">주소(위치)</span>
          <div className="input-row">
            <MapPin size={16} className="text-mint-700" />
            <input
              type="text"
              required
              value={form.address}
              onChange={handleAddressChange}
              onBlur={handleAddressBlur}
              onClick={handleOpenPostcode}
              className="input-control"
              placeholder="주소를 입력하거나 검색하세요"
            />
            <button
              type="button"
              onClick={handleOpenPostcode}
              disabled={locationLoading}
              className="btn-primary whitespace-nowrap px-3 py-1 text-xs"
            >
              주소 검색
            </button>
          </div>
        </label>
        <button
          type="submit"
          disabled={loading}
          className="btn-primary w-full"
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
