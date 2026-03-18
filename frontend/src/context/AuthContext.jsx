import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, setAccessTokenRefreshHandler, setAuthFailureHandler, setAuthToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('pm_token'));
  const [userEmail, setUserEmail] = useState(() => localStorage.getItem('pm_email') || '');
  const [userName, setUserName] = useState(() => localStorage.getItem('pm_username') || '');
  const [authFailure, setAuthFailure] = useState(null);

  const clearSession = () => {
    setToken(null);
    setUserEmail('');
    setUserName('');
    localStorage.removeItem('pm_email');
    localStorage.removeItem('pm_username');
  };

  const applyProfile = useCallback((profile) => {
    setUserName(profile?.name || profile?.email || '');
    setUserEmail(profile?.email || '');
  }, []);

  const refreshProfile = useCallback(async () => {
    try {
      const me = await api.getMe();
      applyProfile(me);
      return me;
    } catch {
      return null;
    }
  }, [applyProfile]);

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
    setAuthFailureHandler((error) => {
      setAuthFailure((prev) => {
        if (prev) {
          return prev;
        }

        const currentPath = `${window.location.pathname}${window.location.search}`;
        const returnTo =
          currentPath.startsWith('/login') || currentPath.startsWith('/signup') ? '/' : currentPath;

        return {
          message: error.message || '세션이 만료되었습니다. 다시 로그인해주세요.',
          returnTo,
        };
      });
      clearSession();
    });

    return () => {
      setAuthFailureHandler(null);
    };
  }, []);

  useEffect(() => {
    setAccessTokenRefreshHandler((session) => {
      const trimmed = session?.accessToken?.trim() || null;
      setToken(trimmed);
      setAuthFailure(null);
    });

    return () => {
      setAccessTokenRefreshHandler(null);
    };
  }, []);

  useEffect(() => {
    if (userEmail) localStorage.setItem('pm_email', userEmail);
  }, [userEmail]);

  useEffect(() => {
    if (userName) localStorage.setItem('pm_username', userName);
  }, [userName]);

  useEffect(() => {
    let cancelled = false;

    const syncProfileFromServer = async () => {
      if (!token) {
        return;
      }

      try {
        const me = await api.getMe();
        if (cancelled) {
          return;
        }
        applyProfile(me);
      } catch (err) {
        if (cancelled || err?.status === 401) {
          return;
        }
      }
    };

    syncProfileFromServer();

    return () => {
      cancelled = true;
    };
  }, [applyProfile, token]);

  const value = useMemo(() => {
    return {
      token,
      userEmail,
      authFailure,
      isAuthed: Boolean(token),
      userName,
      refreshProfile,
      async login({ email, password }) {
        const res = await api.login({ email, password });
        const trimmed = res.accessToken?.trim();
        setToken(trimmed);
        setAuthToken(trimmed);
        setUserEmail(email);
        setAuthFailure(null);
        await refreshProfile();
      },
      async signup({ email, password, username, address, latitude, longitude }) {
        await api.signup({ email, password, username, address, latitude, longitude });
        // 회원가입 후 자동 로그인은 백엔드 토큰 발급 없음 → 로그인 페이지로 유도
      },
      consumeAuthFailure() {
        setAuthFailure(null);
      },
      async logout() {
        setAuthFailure(null);
        try {
          await api.logout();
        } catch {
          // ignore logout API failure and clear local session regardless
        }
        clearSession();
      },
    };
  }, [authFailure, refreshProfile, token, userEmail, userName]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
