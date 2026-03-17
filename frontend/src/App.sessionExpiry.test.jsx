import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi } from 'vitest';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';

vi.mock('./utils/partyRealtime', () => ({
  subscribeToPartyStream: () => () => {},
}));

describe('SessionExpiryHandler', () => {
  it('재발급까지_실패하면_로그인_화면으로_이동하고_안내를_노출한다', async () => {
    // given
    localStorage.setItem('pm_token', 'expired-access-token');
    localStorage.setItem('pm_email', 'test@example.com');
    window.history.pushState({}, '', '/my-parties');

    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: 'TOKEN_EXPIRED',
            message: '로그인이 만료되었습니다. 다시 로그인해주세요.',
          }),
          {
            status: 401,
            headers: {
              'Content-Type': 'application/json',
            },
          },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: 'INVALID_REFRESH_TOKEN',
            message: '세션이 만료되었습니다. 다시 로그인해주세요.',
          }),
          {
            status: 401,
            headers: {
              'Content-Type': 'application/json',
            },
          },
        ),
      );

    render(
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <App />
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>,
    );

    // when
    // then
    await waitFor(() => {
      expect(window.location.pathname).toBe('/login');
    });
    expect(screen.getAllByText('세션이 만료되었습니다. 다시 로그인해주세요.').length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { name: '로그인' })).toBeInTheDocument();
  });
});
