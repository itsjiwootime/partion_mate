import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Mail, Lock } from 'lucide-react';

function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (key) => (e) => setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      setLoading(true);
      await login(form);
      navigate('/');
    } catch (err) {
      setError(err.message || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-semibold text-ink">로그인</h1>
        <p className="text-sm text-ink/60">이메일과 비밀번호로 로그인하세요.</p>
      </div>
      <form onSubmit={handleSubmit} className="space-y-4 glass-panel rounded-2xl p-5">
        {error && <p className="text-sm text-red-600">{error}</p>}
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
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-mint-500 px-4 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-mint-600 disabled:cursor-not-allowed disabled:bg-ink/20"
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
