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
    expect(localStorage.getItem('pm_create_party_draft')).toBeNull();
    await waitFor(() => {
      expect(screen.getByText('지점 상세')).toBeInTheDocument();
    });
  });

  it('입력값에_따라_참여_조건_미리보기를_갱신한다', async () => {
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
    await user.type(screen.getByLabelText('제품명'), '올리브 오일 2L');
    await user.click(screen.getByRole('button', { name: '다음 단계' }));

    await screen.findByText('참여 조건 미리보기');
    await user.clear(screen.getByLabelText('제품 총 가격'));
    await user.type(screen.getByLabelText('제품 총 가격'), '25000');
    await user.clear(screen.getByLabelText('호스트 가져갈 수량'));
    await user.type(screen.getByLabelText('호스트 가져갈 수량'), '3');
    await user.clear(screen.getByLabelText('최소 소분 단위'));
    await user.type(screen.getByLabelText('최소 소분 단위'), '2');

    // then
    expect(screen.getByText('참여자에게 열리는 수량')).toBeInTheDocument();
    expect(screen.getByText('1개')).toBeInTheDocument();
    expect(screen.getByText('12,500원')).toBeInTheDocument();
    expect(screen.getByText('0명')).toBeInTheDocument();
    expect(screen.getByText('호스트 수량을 제외하고 남는 1개로는 최소 참여 기준 2개를 채우기 어렵습니다.')).toBeInTheDocument();
  });

  it('저장된_초안을_배너에서_복구한다', async () => {
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

    const { unmount } = render(
      <MemoryRouter initialEntries={['/parties/create?storeId=1']}>
        <Routes>
          <Route path="/parties/create" element={<CreateParty />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByText('파티 기본 정보');
    await user.type(screen.getByLabelText('제품명'), '올리브 오일 2L');
    await user.click(screen.getByRole('button', { name: '다음 단계' }));
    await screen.findByText('가격 및 수량 설정');
    await user.clear(screen.getByLabelText('제품 총 가격'));
    await user.type(screen.getByLabelText('제품 총 가격'), '32000');

    await waitFor(() => {
      expect(localStorage.getItem('pm_create_party_draft')).toContain('올리브 오일 2L');
    });

    unmount();

    // when
    render(
      <MemoryRouter initialEntries={['/parties/create?storeId=1']}>
        <Routes>
          <Route path="/parties/create" element={<CreateParty />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByText('작성 중이던 파티 초안이 있습니다.');
    await user.click(screen.getByRole('button', { name: '초안 복구' }));

    // then
    expect(addToastMock).toHaveBeenCalledWith('작성 중이던 파티 초안을 복구했습니다.', 'success');
    await screen.findByText('가격 및 수량 설정');
    expect(screen.getByLabelText('제품 총 가격')).toHaveValue(32000);
    await user.click(screen.getByRole('button', { name: '이전 단계' }));
    expect(screen.getByLabelText('제품명')).toHaveValue('올리브 오일 2L');
  });
});
