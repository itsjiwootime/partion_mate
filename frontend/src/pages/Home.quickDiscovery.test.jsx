import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { vi } from 'vitest';
import Home from './Home';

const { api } = vi.hoisted(() => ({
  api: {
    getMe: vi.fn(),
    getNearbyStores: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: false,
  }),
}));

function PartyDiscoveryLocation() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  return (
    <div>
      <div>query:{params.get('q') ?? ''}</div>
      <div>status:{params.get('status') ?? ''}</div>
      <div>storage:{params.get('storage') ?? ''}</div>
      <div>unit:{params.get('unit') ?? ''}</div>
    </div>
  );
}

describe('Home quick discovery', () => {
  it('검색어를_입력하면_파티_목록으로_쿼리를_전달한다', async () => {
    // given
    api.getMe.mockReset();
    api.getNearbyStores.mockReset();
    api.getNearbyStores.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/parties" element={<PartyDiscoveryLocation />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.type(screen.getByRole('textbox', { name: '홈 파티 검색' }), '휴지');
    await user.click(screen.getByRole('button', { name: '검색' }));

    // then
    await waitFor(() => {
      expect(screen.getByText('query:휴지')).toBeInTheDocument();
    });
    expect(screen.getByText('status:')).toBeInTheDocument();
  });

  it('빠른_필터_버튼으로_대표_조건을_즉시_적용한다', async () => {
    // given
    api.getMe.mockReset();
    api.getNearbyStores.mockReset();
    api.getNearbyStores.mockResolvedValue([]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/parties" element={<PartyDiscoveryLocation />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await user.click(screen.getByRole('button', { name: /냉장 식품/i }));

    // then
    await waitFor(() => {
      expect(screen.getByText('storage:REFRIGERATED')).toBeInTheDocument();
    });
    expect(screen.getByText('query:')).toBeInTheDocument();
  });
});
