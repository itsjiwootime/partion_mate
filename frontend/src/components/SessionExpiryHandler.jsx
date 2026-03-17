import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

function SessionExpiryHandler() {
  const { authFailure, consumeAuthFailure } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (!authFailure) {
      return;
    }

    addToast(authFailure.message, 'error', 3200);

    if (location.pathname.startsWith('/login') || location.pathname.startsWith('/signup')) {
      consumeAuthFailure();
      return;
    }

    navigate('/login', {
      replace: true,
      state: {
        from: authFailure.returnTo,
        authMessage: authFailure.message,
      },
    });
    consumeAuthFailure();
  }, [addToast, authFailure, consumeAuthFailure, location.pathname, navigate]);

  return null;
}

export default SessionExpiryHandler;
