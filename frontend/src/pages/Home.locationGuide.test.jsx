import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import Home from './Home';

const { api } = vi.hoisted(() => ({
  api: {
    getMe: vi.fn(),
    getAllParties: vi.fn(),
    getNearbyStores: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
  }),
}));

describe('Home location guide', () => {
  it('저장된_좌표가_없으면_프로필에서_위치를_다시_설정하도록_안내한다', async () => {
    // given
    api.getMe.mockReset();
    api.getAllParties.mockReset();
    api.getNearbyStores.mockReset();

    api.getMe.mockResolvedValue({
      name: '테스터',
      address: '서울 송파구',
      latitude: null,
      longitude: null,
    });
    api.getAllParties.mockResolvedValue([]);
    api.getNearbyStores.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/me" element={<div>프로필 화면</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('프로필 주소는 있지만 기준 좌표가 없어 기본 위치를 사용 중입니다. 프로필에서 주소 검색으로 위치를 다시 설정할 수 있습니다.');
    await user.click(screen.getByRole('button', { name: '프로필에서 위치 다시 설정' }));

    // then
    await waitFor(() => {
      expect(screen.getByText('프로필 화면')).toBeInTheDocument();
    });
  });
});
