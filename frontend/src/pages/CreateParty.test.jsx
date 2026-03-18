import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import CreateParty from './CreateParty';

const { addToastMock, api } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getNearbyStores: vi.fn(),
    createParty: vi.fn(),
  },
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthed: true,
  }),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));

describe('CreateParty', () => {
  it('기본_정보가_없으면_다음_단계로_이동하지_않는다', async () => {
    // given
    addToastMock.mockReset();
    api.getNearbyStores.mockReset();
    api.createParty.mockReset();

    api.getNearbyStores.mockResolvedValue([
      {
        id: 1,
        name: '코스트코 양재점',
        distance: 1.2,
      },
    ]);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/create?storeId=1']}>
        <Routes>
          <Route path="/parties/create" element={<CreateParty />} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('파티 기본 정보');
    await user.click(screen.getByRole('button', { name: '다음 단계' }));

    // then
    expect(screen.getByText('제품명을 입력해 주세요.')).toBeInTheDocument();
    expect(addToastMock).toHaveBeenCalledWith('제품명을 입력해 주세요.', 'error');
    expect(screen.getByText('파티 기본 정보')).toBeInTheDocument();
  });

  it('3단계로_입력한_후_내부_채팅방_기준으로_파티를_생성한다', async () => {
    // given
    addToastMock.mockReset();
    api.getNearbyStores.mockReset();
    api.createParty.mockReset();

    api.getNearbyStores.mockResolvedValue([
      {
        id: 1,
        name: '코스트코 양재점',
        distance: 1.2,
      },
    ]);
    api.createParty.mockResolvedValue({});
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/parties/create?storeId=1']}>
        <Routes>
          <Route path="/parties/create" element={<CreateParty />} />
          <Route path="/branch/:id" element={<div>지점 상세</div>} />
        </Routes>
      </MemoryRouter>,
    );

    // when
    await screen.findByText('파티 기본 정보');
    expect(screen.queryByText('카카오톡 오픈채팅 링크')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('제품명'), '올리브 오일 2L');
    await user.type(screen.getByLabelText('파티 제목'), '올리브 오일 소분');
    await user.click(screen.getByRole('button', { name: '다음 단계' }));

    await screen.findByText('가격 및 수량 설정');
    await user.clear(screen.getByLabelText('제품 총 가격'));
    await user.type(screen.getByLabelText('제품 총 가격'), '24000');
    await user.click(screen.getByRole('button', { name: '다음 단계' }));

    const deadlineDateInput = await screen.findByLabelText('모집 마감 날짜');
    fireEvent.change(deadlineDateInput, { target: { value: '2099-03-20' } });
    fireEvent.change(screen.getByLabelText('모집 마감 시간'), { target: { value: '18:30' } });
    await user.type(screen.getByLabelText('거래 안내'), '냉장 보관 필수, 생성 후 채팅방에서 픽업 공지 예정');
    await user.click(screen.getByRole('button', { name: '파티 생성하기' }));

    // then
    await waitFor(() => {
      expect(api.createParty).toHaveBeenCalledWith({
        title: '올리브 오일 소분',
        storeId: 1,
        productName: '올리브 오일 2L',
        totalPrice: 24000,
        totalQuantity: 4,
        hostRequestedQuantity: 1,
        deadline: '2099-03-20T18:30',
        unitLabel: '개',
        minimumShareUnit: 1,
        storageType: 'ROOM_TEMPERATURE',
        packagingType: 'ORIGINAL_PACKAGE',
        hostProvidesPackaging: true,
        onSiteSplit: false,
        guideNote: '냉장 보관 필수, 생성 후 채팅방에서 픽업 공지 예정',
      });
    });
    expect(api.createParty.mock.calls[0][0]).not.toHaveProperty('openChatUrl');
    expect(addToastMock).toHaveBeenCalledWith('파티가 생성되었습니다.', 'success');
    await waitFor(() => {
      expect(screen.getByText('지점 상세')).toBeInTheDocument();
    });
  });
});
