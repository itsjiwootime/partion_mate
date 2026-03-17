import { useMemo, useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Mail, Lock } from 'lucide-react';

function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const returnTo = useMemo(() => {
    const requestedPath = location.state?.from;
    if (typeof requestedPath !== 'string' || requestedPath.startsWith('/login') || requestedPath.startsWith('/signup')) {
      return '/';
    }
    return requestedPath;
  }, [location.state]);

  const authMessage = useMemo(() => {
    const message = location.state?.authMessage;
    return typeof message === 'string' ? message : '';
  }, [location.state]);

  const handleChange = (key) => (e) => setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      setLoading(true);
      await login(form);
      navigate(returnTo, { replace: true });
    } catch (err) {
      setError(err.message || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-semibold text-ink">로그인</h1>
        <p className="section-subtitle">이메일과 비밀번호로 로그인하세요.</p>
      </div>
      <form onSubmit={handleSubmit} className="card-elevated space-y-4 p-5">
        {authMessage && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            {authMessage}
          </div>
        )}
        {error && <p className="text-sm text-red-600">{error}</p>}
        <label htmlFor="login-email" className="block space-y-1 text-sm">
          <span className="text-ink/70">이메일</span>
          <div className="input-row">
            <Mail size={16} className="text-mint-700" />
            <input
              id="login-email"
              type="email"
              required
              value={form.email}
              onChange={handleChange('email')}
              className="input-control"
              placeholder="you@example.com"
            />
          </div>
        </label>
        <label htmlFor="login-password" className="block space-y-1 text-sm">
          <span className="text-ink/70">비밀번호</span>
          <div className="input-row">
            <Lock size={16} className="text-mint-700" />
            <input
              id="login-password"
              type="password"
              required
              value={form.password}
              onChange={handleChange('password')}
              className="input-control"
              placeholder="********"
            />
          </div>
          <p className="text-xs text-ink/50">비밀번호는 6자 이상 20자 이하로 입력해주세요.</p>
        </label>
        <button
          type="submit"
          disabled={loading}
          className="btn-primary w-full"
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>
      </form>
      <p className="text-center text-sm text-ink/60">
        계정이 없나요?{' '}
        <Link to="/signup" className="font-semibold text-mint-700 hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  );
}

export default Login;
