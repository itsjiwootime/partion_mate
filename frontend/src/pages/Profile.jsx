import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { LoadingState } from '../components/Feedback';
import { Mail, MapPin, User } from 'lucide-react';

function Profile() {
  const { isAuthed, logout } = useAuth();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAuthed) {
      navigate('/login');
      return;
    }
    const fetchMe = async () => {
      try {
        setError('');
        const res = await api.getMe();
        setUser(res);
      } catch (e) {
        setError('프로필을 불러오지 못했습니다.');
      }
    };
    fetchMe();
  }, [isAuthed, navigate]);

  if (!isAuthed) {
    return null;
  }

  return (
    <div className="space-y-4">
      <div className="card-elevated p-4 space-y-3">
        <h2 className="section-title">내 정보</h2>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!user && !error && <LoadingState />}
        {user && (
          <div className="space-y-2 text-sm text-ink">
            <p className="flex items-center gap-2">
              <User size={16} className="text-mint-700" />
              <span className="font-semibold">{user.name}</span>
            </p>
            <p className="flex items-center gap-2">
              <Mail size={16} className="text-mint-700" />
              <span>{user.email}</span>
            </p>
            <p className="flex items-center gap-2">
              <MapPin size={16} className="text-mint-700" />
              <span>{user.address}</span>
            </p>
          </div>
        )}
        <button
          onClick={logout}
          className="btn-secondary w-full"
        >
          로그아웃
        </button>
      </div>
    </div>
  );
}

export default Profile;
