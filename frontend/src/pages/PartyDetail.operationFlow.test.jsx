import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import PartyDetail from './PartyDetail';

const { addToastMock, api, subscribeToPartyStreamMock } = vi.hoisted(() => ({
  addToastMock: vi.fn(),
  api: {
    getPartyDetail: vi.fn(),
    confirmSettlement: vi.fn(),
    confirmPickupSchedule: vi.fn(),
    updatePaymentStatus: vi.fn(),
    acknowledgePickup: vi.fn(),
    submitReview: vi.fn(),
  },
  subscribeToPartyStreamMock: vi.fn(() => vi.fn()),
}));

const authState = vi.hoisted(() => ({
  isAuthed: true,
  userName: '참여자',
}));

vi.mock('../api/client', () => ({ api }));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => authState,
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    addToast: addToastMock,
  }),
}));
vi.mock('../utils/partyRealtime', () => ({
  subscribeToPartyStream: subscribeToPartyStreamMock,
}));

function renderPartyDetail() {
  return render(
    <MemoryRouter initialEntries={['/parties/5']}>
      <Routes>
        <Route path="/parties/:id" element={<PartyDetail />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('PartyDetail operation flow', () => {
  it('호스트가_정산과_픽업_일정을_확정한다', async () => {
    // given
    authState.isAuthed = true;
    authState.userName = '호스트';
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.confirmSettlement.mockReset();
    api.confirmPickupSchedule.mockReset();
    api.updatePaymentStatus.mockReset();
    api.acknowledgePickup.mockReset();
    api.submitReview.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getPartyDetail.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 2,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'RECRUITING',
      userRole: 'LEADER',
      actualTotalPrice: null,
      receiptNote: '',
      pickupPlace: '',
      pickupTime: null,
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 88,
        username: '호스트',
        reviewCount: 0,
      },
    });
    api.confirmSettlement.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 2,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'RECRUITING',
      userRole: 'LEADER',
      actualTotalPrice: 9600,
      receiptNote: '행사 적용가 반영',
      pickupPlace: '',
      pickupTime: null,
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 88,
        username: '호스트',
        reviewCount: 0,
      },
    });
    api.confirmPickupSchedule.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 2,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'RECRUITING',
      userRole: 'LEADER',
      actualTotalPrice: 9600,
      receiptNote: '행사 적용가 반영',
      pickupPlace: '광명점 1층 주차장',
      pickupTime: '2099-03-20T19:00:00',
      pickupTimeLabel: '2099.03.20 19:00',
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 88,
        username: '호스트',
        reviewCount: 0,
      },
    });
    const user = userEvent.setup();

    renderPartyDetail();

    // when
    await screen.findByRole('heading', { level: 2, name: '호스트 운영' });
    await user.type(screen.getByLabelText('실구매 총액'), '9600');
    await user.type(screen.getByLabelText('영수증 메모'), '행사 적용가 반영');
    await user.click(screen.getByRole('button', { name: '정산 확정' }));

    await user.type(screen.getByLabelText('픽업 장소'), '광명점 1층 주차장');
    fireEvent.change(screen.getByLabelText('픽업 시간'), { target: { value: '2099-03-20T19:00' } });
    await user.click(screen.getByRole('button', { name: '픽업 일정 확정' }));

    // then
    await waitFor(() => {
      expect(api.confirmSettlement).toHaveBeenCalledWith({
        partyId: 5,
        actualTotalPrice: 9600,
        receiptNote: '행사 적용가 반영',
      });
    });
    await waitFor(() => {
      expect(api.confirmPickupSchedule).toHaveBeenCalledWith({
        partyId: 5,
        pickupPlace: '광명점 1층 주차장',
        pickupTime: '2099-03-20T19:00',
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('실구매 총액을 확정했습니다.', 'success');
    expect(addToastMock).toHaveBeenCalledWith('픽업 일정을 확정했습니다.', 'success');
    expect(screen.getByDisplayValue('9600')).toBeInTheDocument();
    expect(screen.getByDisplayValue('광명점 1층 주차장')).toBeInTheDocument();
  });

  it('참여자가_송금과_픽업_확인_후_호스트_후기를_작성한다', async () => {
    // given
    authState.isAuthed = true;
    authState.userName = '참여자';
    addToastMock.mockReset();
    api.getPartyDetail.mockReset();
    api.confirmSettlement.mockReset();
    api.confirmPickupSchedule.mockReset();
    api.updatePaymentStatus.mockReset();
    api.acknowledgePickup.mockReset();
    api.submitReview.mockReset();
    subscribeToPartyStreamMock.mockClear();

    api.getPartyDetail.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 5,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'FULL',
      userRole: 'MEMBER',
      memberId: 91,
      expectedAmount: 3600,
      actualAmount: 3800,
      paymentStatus: 'PENDING',
      paymentStatusLabel: '입금 전',
      pickupPlace: '광명점 1층 주차장',
      pickupTime: '2099-03-20T19:00:00',
      pickupTimeLabel: '2099.03.20 19:00',
      pickupAcknowledged: false,
      canReviewHost: true,
      hasReviewedHost: false,
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 42,
        username: '호스트',
        reviewCount: 12,
      },
    });
    api.updatePaymentStatus.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 5,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'FULL',
      userRole: 'MEMBER',
      memberId: 91,
      expectedAmount: 3600,
      actualAmount: 3800,
      paymentStatus: 'PAID',
      paymentStatusLabel: '입금 완료',
      pickupPlace: '광명점 1층 주차장',
      pickupTime: '2099-03-20T19:00:00',
      pickupTimeLabel: '2099.03.20 19:00',
      pickupAcknowledged: false,
      canReviewHost: true,
      hasReviewedHost: false,
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 42,
        username: '호스트',
        reviewCount: 12,
      },
    });
    api.acknowledgePickup.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 5,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'FULL',
      userRole: 'MEMBER',
      memberId: 91,
      expectedAmount: 3600,
      actualAmount: 3800,
      paymentStatus: 'PAID',
      paymentStatusLabel: '입금 완료',
      pickupPlace: '광명점 1층 주차장',
      pickupTime: '2099-03-20T19:00:00',
      pickupTimeLabel: '2099.03.20 19:00',
      pickupAcknowledged: true,
      canReviewHost: true,
      hasReviewedHost: false,
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 42,
        username: '호스트',
        reviewCount: 12,
      },
    });
    api.submitReview.mockResolvedValue({
      partyId: 5,
      title: '광명점 우유 소분',
      storeName: '코스트코 광명점',
      productName: '우유',
      totalPrice: 8900,
      totalQuantity: 5,
      currentQuantity: 5,
      deadline: '2099-03-20T09:00:00',
      deadlineLabel: '2099.03.20 09:00',
      status: 'FULL',
      userRole: 'MEMBER',
      memberId: 91,
      expectedAmount: 3600,
      actualAmount: 3800,
      paymentStatus: 'PAID',
      paymentStatusLabel: '입금 완료',
      pickupPlace: '광명점 1층 주차장',
      pickupTime: '2099-03-20T19:00:00',
      pickupTimeLabel: '2099.03.20 19:00',
      pickupAcknowledged: true,
      canReviewHost: true,
      hasReviewedHost: true,
      settlementMembers: [],
      hostReviews: [],
      hostTrust: {
        userId: 42,
        username: '호스트',
        reviewCount: 12,
      },
    });
    const user = userEvent.setup();

    renderPartyDetail();

    // when
    await screen.findByRole('heading', { level: 2, name: '내 거래 상태' });
    await user.click(screen.getByRole('button', { name: '송금 완료 표시' }));
    await user.click(screen.getByRole('button', { name: '픽업 일정 확인' }));
    await user.selectOptions(screen.getByLabelText('평점'), '4');
    await user.type(screen.getByLabelText('후기'), '픽업 안내가 빨랐어요.');
    await user.click(screen.getByRole('button', { name: '호스트 후기 등록' }));

    // then
    await waitFor(() => {
      expect(api.updatePaymentStatus).toHaveBeenCalledWith({
        partyId: 5,
        memberId: 91,
        paymentStatus: 'PAID',
      });
    });
    await waitFor(() => {
      expect(api.acknowledgePickup).toHaveBeenCalledWith(5);
    });
    await waitFor(() => {
      expect(api.submitReview).toHaveBeenCalledWith({
        partyId: 5,
        targetUserId: 42,
        rating: 4,
        comment: '픽업 안내가 빨랐어요.',
      });
    });
    expect(addToastMock).toHaveBeenCalledWith('송금 완료로 표시했습니다.', 'success');
    expect(addToastMock).toHaveBeenCalledWith('픽업 일정을 확인했습니다.', 'success');
    expect(addToastMock).toHaveBeenCalledWith('호스트 후기를 등록했습니다.', 'success');
    expect(screen.getByText('호스트 후기를 이미 작성했습니다.')).toBeInTheDocument();
  });
});
