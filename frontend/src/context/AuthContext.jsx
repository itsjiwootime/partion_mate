import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api, setAuthToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('pm_token'));
  const [userEmail, setUserEmail] = useState(() => localStorage.getItem('pm_email') || '');
  const [userName, setUserName] = useState(() => localStorage.getItem('pm_username') || '');

  useEffect(() => {
    if (token) {
      const trimmed = token.trim();
      setAuthToken(trimmed);
      localStorage.setItem('pm_token', trimmed);
    } else {
      setAuthToken(null);
      localStorage.removeItem('pm_token');
    }
  }, [token]);

  useEffect(() => {
    if (userEmail) localStorage.setItem('pm_email', userEmail);
  }, [userEmail]);

  useEffect(() => {
    if (userName) localStorage.setItem('pm_username', userName);
  }, [userName]);

  const value = useMemo(() => {
    const setProfileFromServer = async () => {
      try {
        const me = await api.getMe();
        setUserName(me.name || me.email || '');
        setUserEmail(me.email || '');
      } catch {
        // ignore
      }
    };

    return {
      token,
      userEmail,
      isAuthed: Boolean(token),
      userName,
      async login({ email, password }) {
        const res = await api.login({ email, password });
        const trimmed = res.accessToken?.trim();
        setToken(trimmed);
        setAuthToken(trimmed);
        setUserEmail(email);
        await setProfileFromServer();
      },
      async signup({ email, password, username, address, latitude, longitude }) {
        await api.signup({ email, password, username, address, latitude, longitude });
        // 회원가입 후 자동 로그인은 백엔드 토큰 발급 없음 → 로그인 페이지로 유도
      },
      logout() {
        setToken(null);
        setUserEmail('');
        setUserName('');
        localStorage.removeItem('pm_email');
        localStorage.removeItem('pm_username');
      },
    };
  }, [token, userEmail, userName]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
