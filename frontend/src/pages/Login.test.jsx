import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import Login from './Login';

const loginMock = vi.fn();

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    login: loginMock,
  }),
}));

describe('Login', () => {
  it('로그인에_성공하면_원래_보려던_경로로_복귀한다', async () => {
    // given
    loginMock.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/login',
            state: {
              from: '/my-parties',
              authMessage: '로그인이 만료되었습니다. 다시 로그인해주세요.',
            },
          },
        ]}
      >
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/my-parties" element={<div>내 파티 화면</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('로그인이 만료되었습니다. 다시 로그인해주세요.')).toBeInTheDocument();

    // when
    await user.type(screen.getByLabelText(/^이메일/), 'test@example.com');
    await user.type(screen.getByLabelText(/^비밀번호/), 'password123');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    // then
    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      });
      expect(screen.getByText('내 파티 화면')).toBeInTheDocument();
    });
  });

  it('로그인에_실패하면_공통_실패_메시지를_노출한다', async () => {
    // given
    loginMock.mockRejectedValueOnce(new Error('이메일 또는 비밀번호를 확인해주세요.'));
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.type(screen.getByLabelText(/^이메일/), 'member@test.com');
    await user.type(screen.getByLabelText(/^비밀번호/), 'wrongpass');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    // then
    await waitFor(() => {
      expect(screen.getByText('이메일 또는 비밀번호를 확인해주세요.')).toBeInTheDocument();
    });
    expect(screen.getByText('비밀번호는 6자 이상 20자 이하로 입력해주세요.')).toBeInTheDocument();
  });
});
