import { MapPin, Sparkles, LogIn, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function Header({ title, subtitle }) {
  const navigate = useNavigate();
  const { isAuthed, userName } = useAuth();

  return (
    <header className="glass-panel sticky top-0 z-10 rounded-2xl px-4 py-3 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="flex items-center gap-1 text-xs font-semibold uppercase tracking-wide text-mint-700">
            <Sparkles size={14} />
            Partition Mate
          </p>
          <h1 className="text-xl font-semibold text-ink">{title}</h1>
          {subtitle && <p className="text-sm text-ink/60">{subtitle}</p>}
        </div>
        <div className="flex items-center gap-2">
          {!isAuthed ? (
            <button
              onClick={() => navigate('/login')}
              className="flex items-center gap-1 rounded-full bg-ink/5 px-3 py-1 text-xs font-semibold text-ink transition hover:bg-ink/10"
            >
              <LogIn size={16} />
              로그인
            </button>
          ) : (
            <div className="flex items-center gap-1 rounded-full bg-ink/5 px-3 py-1 text-xs font-semibold text-ink">
              <User size={16} />
              {userName || '닉네임 확인중'}
            </div>
          )}
          <button className="flex items-center gap-1 rounded-full bg-mint-500/15 px-3 py-1 text-xs font-medium text-mint-700 transition hover:bg-mint-500/25">
            <MapPin size={16} />
            가까운 지점
          </button>
        </div>
      </div>
    </header>
  );
}

export default Header;
