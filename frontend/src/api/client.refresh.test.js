import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, setAccessTokenRefreshHandler, setAuthFailureHandler, setAuthToken } from './client';

describe('client refresh flow', () => {
  beforeEach(() => {
    setAuthToken('expired-access-token');
    setAuthFailureHandler(null);
    setAccessTokenRefreshHandler(null);
  });

  afterEach(() => {
    setAuthToken(null);
    setAuthFailureHandler(null);
    setAccessTokenRefreshHandler(null);
    vi.restoreAllMocks();
  });

  it('보호_API가_401이면_refresh_후_원래요청을_한번_재시도한다', async () => {
    // given
    const authFailureHandler = vi.fn();
    const refreshHandler = vi.fn();
    setAuthFailureHandler(authFailureHandler);
    setAccessTokenRefreshHandler(refreshHandler);

    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(
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
      ))
      .mockResolvedValueOnce(new Response(
        JSON.stringify({
          accessToken: 'refreshed-access-token',
          accessTokenExpiresInMs: 3600000,
        }),
        {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      ))
      .mockResolvedValueOnce(new Response(
        JSON.stringify({
          email: 'test@example.com',
          name: '테스트유저',
        }),
        {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      ));

    // when
    const response = await api.getMe();

    // then
    expect(response.email).toBe('test@example.com');
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toBe('http://localhost:8080/api/auth/refresh');
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer refreshed-access-token');
    expect(refreshHandler).toHaveBeenCalledWith({
      accessToken: 'refreshed-access-token',
      accessTokenExpiresInMs: 3600000,
    });
    expect(authFailureHandler).not.toHaveBeenCalled();
  });
});
