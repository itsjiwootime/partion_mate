import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Package, Users, Clock3, ArrowLeft, Link as LinkIcon, Copy, ExternalLink, MapPin, Wallet, ShieldCheck, Star, MessageSquareText } from 'lucide-react';
import { LoadingState } from '../components/Feedback';
import { mergeRealtimeParty, normalizePartyDetail } from '../utils/party';
import { subscribeToPartyStream } from '../utils/partyRealtime';

function toDateTimeLocalValue(value) {
  if (!value) return '';
  return value.slice(0, 16);
}

function formatCurrency(value) {
  if (value == null) return '미정';
  return `${value.toLocaleString()}원`;
}

function createReviewFormState() {
  return { rating: '5', comment: '' };
}

function formatRating(value) {
  return Number(value ?? 0).toFixed(1);
}

function PartyDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthed } = useAuth();
  const { addToast } = useToast();
  const stateParty = useMemo(() => normalizePartyDetail(location.state?.party), [location.state]);
  const [detail, setDetail] = useState(stateParty || null);
  const [loading, setLoading] = useState(!stateParty);
  const [error, setError] = useState('');
  const [realtimeState, setRealtimeState] = useState('connecting');
  const [actionLoading, setActionLoading] = useState('');
  const [settlementForm, setSettlementForm] = useState({ actualTotalPrice: '', receiptNote: '' });
  const [pickupForm, setPickupForm] = useState({ pickupPlace: '', pickupTime: '' });
  const [hostReviewForm, setHostReviewForm] = useState(createReviewFormState);
  const [memberReviewForms, setMemberReviewForms] = useState({});

  useEffect(() => {
    setSettlementForm({
      actualTotalPrice: detail?.actualTotalPrice != null ? String(detail.actualTotalPrice) : '',
      receiptNote: detail?.receiptNote ?? '',
    });
    setPickupForm({
      pickupPlace: detail?.pickupPlace ?? '',
      pickupTime: toDateTimeLocalValue(detail?.pickupTime),
    });
  }, [detail?.actualTotalPrice, detail?.receiptNote, detail?.pickupPlace, detail?.pickupTime]);

  useEffect(() => {
    if (!detail?.partyId) return;
    setHostReviewForm(createReviewFormState());
  }, [detail?.partyId]);

  useEffect(() => {
    if (!detail?.settlementMembers?.length) return;
    setMemberReviewForms((current) => {
      const next = {};
      detail.settlementMembers
        .filter((member) => member.role !== 'LEADER')
        .forEach((member) => {
          next[member.memberId] = current[member.memberId] ?? createReviewFormState();
        });
      return next;
    });
  }, [detail?.settlementMembers]);

  useEffect(() => {
    let active = true;

    const fetchDetail = async ({ showLoading = false } = {}) => {
      if (!id) return;
      try {
        if (showLoading) {
          setLoading(true);
        }
        setError('');
        const data = await api.getPartyDetail(id);
        if (active) {
          setDetail(normalizePartyDetail(data));
        }
      } catch (e) {
        if (active) {
          setError('파티 정보를 불러오지 못했습니다.');
        }
      } finally {
        if (active && showLoading) {
          setLoading(false);
        }
      }
    };

    fetchDetail({ showLoading: !stateParty });

    const unsubscribe = subscribeToPartyStream({
      partyId: id ? Number(id) : null,
      onConnected: () => {
        if (active) {
          setRealtimeState('live');
        }
      },
      onReconnectStateChange: (state) => {
        if (active) {
          setRealtimeState(state);
        }
      },
      onPartyUpdated: (event) => {
        if (!active) return;
        if (isAuthed) {
          fetchDetail();
          return;
        }
        setDetail((current) => mergeRealtimeParty(current, event));
      },
      onFallback: () => {
        if (active) {
          fetchDetail();
        }
      },
    });

    return () => {
      active = false;
      unsubscribe?.();
    };
  }, [id, isAuthed, stateParty]);

  const perUnit = useMemo(() => {
    if (!detail) return 0;
    const qty = detail.targetQuantity ?? detail.totalQuantity;
    if (!qty) return 0;
    return detail.totalPrice ? Math.round(detail.totalPrice / qty) : 0;
  }, [detail]);

  const remaining = useMemo(() => {
    if (!detail) return 0;
    return Math.max(0, (detail.targetQuantity ?? 0) - (detail.currentQuantity ?? 0));
  }, [detail]);

  const isWaitingParty = detail?.participationStatus === 'WAITING';
  const isClosedParty = detail?.status === 'closed';
  const isHost = detail?.userRole === 'LEADER';
  const isJoinedMember = detail?.userRole === 'MEMBER';
  const isJoinable = !detail?.participationStatus && detail?.status !== 'full' && !isClosedParty && remaining > 0;

  const runAction = async (key, action, successMessage) => {
    try {
      setActionLoading(key);
      const next = await action();
      if (next) {
        setDetail(normalizePartyDetail(next));
      }
      if (successMessage) {
        addToast(successMessage, 'success');
      }
    } catch (e) {
      addToast(e.message || '작업 중 오류가 발생했습니다.', 'error');
    } finally {
      setActionLoading('');
    }
  };

  const handleSettlementSubmit = async (e) => {
    e.preventDefault();
    await runAction(
      'settlement',
      () =>
        api.confirmSettlement({
          partyId: detail.partyId,
          actualTotalPrice: Number(settlementForm.actualTotalPrice),
          receiptNote: settlementForm.receiptNote,
        }),
      '실구매 총액을 확정했습니다.',
    );
  };

  const handlePickupSubmit = async (e) => {
    e.preventDefault();
    await runAction(
      'pickup',
      () =>
        api.confirmPickupSchedule({
          partyId: detail.partyId,
          pickupPlace: pickupForm.pickupPlace,
          pickupTime: pickupForm.pickupTime,
        }),
      '픽업 일정을 확정했습니다.',
    );
  };

  const handleReviewSubmit = async ({ targetUserId, rating, comment, actionKey, successMessage }) => {
    await runAction(
      actionKey,
      () =>
        api.submitReview({
          partyId: detail.partyId,
          targetUserId,
          rating: Number(rating),
          comment: comment.trim(),
        }),
      successMessage,
    );
  };

  if (loading) return <LoadingState />;
  if (error) return <p className="text-sm text-red-600">{error}</p>;
  if (!detail) return <p className="text-sm text-ink/60">파티 정보를 찾을 수 없습니다.</p>;

  return (
    <div className="space-y-4">
      <button onClick={() => navigate(-1)} className="btn-ghost px-0">
        <ArrowLeft size={16} /> 뒤로
      </button>

      <div className="card-elevated p-5 space-y-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">
          {detail.status === 'closed' ? '거래 종료' : detail.status === 'full' ? '모집 완료' : '모집 중'}
        </p>
        <p className="text-xs text-ink/50">
          {realtimeState === 'reconnecting' ? '실시간 연결을 다시 시도하는 중입니다.' : '실시간 모집 현황을 반영하고 있습니다.'}
        </p>
        <h1 className="text-xl font-semibold text-ink">{detail.title}</h1>
        <p className="section-subtitle">{detail.storeName}</p>
        {detail.productName && (
          <p className="text-sm font-semibold text-ink">
            제품명: <span className="text-ink/80">{detail.productName}</span>
          </p>
        )}
        <div className="grid gap-3 text-sm text-ink/80">
          <div className="flex items-center gap-2">
            <Package size={16} className="text-mint-700" />
            <span>총 수량 {detail.targetQuantity}개 · 현재 {detail.currentQuantity}개</span>
          </div>
          <div className="flex items-center gap-2">
            <Users size={16} className="text-mint-700" />
            <span>개당 예상 가격 {perUnit.toLocaleString()}원</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock3 size={16} className="text-mint-700" />
            <span>마감: {detail.deadlineLabel}</span>
          </div>
          {detail.pickupPlace && (
            <div className="flex items-center gap-2">
              <MapPin size={16} className="text-mint-700" />
              <span>
                픽업: {detail.pickupPlace} · {detail.pickupTimeLabel}
              </span>
            </div>
          )}
          {detail.openChatUrl && (
            <div className="flex items-center gap-2">
              <LinkIcon size={16} className="text-mint-700" />
              <span className="truncate text-ink/80">{detail.openChatUrl}</span>
              <button
                onClick={() => navigator.clipboard.writeText(detail.openChatUrl)}
                className="btn-pill px-2 py-1 text-[11px]"
              >
                <Copy size={12} />
                복사
              </button>
              <a
                href={detail.openChatUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-secondary px-2 py-1 text-[11px]"
              >
                <ExternalLink size={12} />
                열기
              </a>
            </div>
          )}
        </div>
      </div>

      {detail.hostTrust && (
        <div className="card-elevated p-5 space-y-3">
          <div className="flex items-center gap-2">
            <ShieldCheck size={18} className="text-mint-700" />
            <h2 className="section-title">호스트 신뢰도</h2>
          </div>
          <div className="grid gap-3 md:grid-cols-[1.2fr_1fr]">
            <div className="rounded-2xl border border-mint-100 bg-mint-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">신뢰 등급</p>
              <p className="mt-1 text-lg font-semibold text-ink">{detail.hostTrust.trustLevelLabel}</p>
              <p className="text-sm text-ink/60">신뢰 점수 {detail.hostTrust.trustScore}점</p>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm text-ink/75">
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">평균 평점</p>
                <p className="mt-1 flex items-center gap-1 font-semibold text-ink">
                  <Star size={14} className="fill-amber-400 text-amber-400" />
                  {formatRating(detail.hostTrust.averageRating)}
                </p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">후기 수</p>
                <p className="mt-1 font-semibold text-ink">{detail.hostTrust.reviewCount}개</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">거래 완료</p>
                <p className="mt-1 font-semibold text-ink">{detail.hostTrust.completedTradeCount}건</p>
              </div>
              <div className="rounded-2xl border border-ink/10 px-4 py-3">
                <p className="text-xs text-ink/50">노쇼 기록</p>
                <p className="mt-1 font-semibold text-ink">{detail.hostTrust.noShowCount}건</p>
              </div>
            </div>
          </div>
          <p className="text-xs text-ink/55">거래 완료율 {detail.hostTrust.completionRate}%</p>
        </div>
      )}

      <div className="card-elevated p-5 space-y-3">
        <h2 className="section-title">소분 정보</h2>
        <div className="grid gap-2 text-sm text-ink/75">
          <div className="flex items-center justify-between gap-4">
            <span>최소 소분 단위</span>
            <span className="font-semibold text-ink">
              {detail.minimumShareUnit}
              {detail.unitLabel}
            </span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span>보관 방식</span>
            <span className="font-semibold text-ink">{detail.storageTypeLabel}</span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span>포장 방식</span>
            <span className="font-semibold text-ink">{detail.packagingTypeLabel}</span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span>포장재 제공</span>
            <span className="font-semibold text-ink">{detail.hostProvidesPackaging ? '제공' : '미제공'}</span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span>현장 소분</span>
            <span className="font-semibold text-ink">{detail.onSiteSplit ? '진행' : '미진행'}</span>
          </div>
        </div>
      </div>

      <div className="card-elevated p-5 space-y-3">
        <h2 className="section-title">가격 정보</h2>
        <div className="grid gap-2 text-sm text-ink/75">
          <div className="flex items-center justify-between gap-4">
            <span>예상 총액</span>
            <span className="font-semibold text-ink">{formatCurrency(detail.expectedTotalPrice)}</span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span>실구매 총액</span>
            <span className="font-semibold text-ink">{formatCurrency(detail.actualTotalPrice)}</span>
          </div>
          {detail.receiptNote && <p className="text-xs text-ink/60">실구매 메모: {detail.receiptNote}</p>}
        </div>
      </div>

      {detail.guideNote && (
        <div className="card-elevated p-5 space-y-2">
          <h2 className="section-title">거래 안내</h2>
          <p className="text-sm leading-6 text-ink/75 whitespace-pre-line">{detail.guideNote}</p>
        </div>
      )}

      <div className="card-elevated p-5 space-y-3">
        <div className="flex items-center gap-2">
          <MessageSquareText size={18} className="text-mint-700" />
          <h2 className="section-title">최근 호스트 후기</h2>
        </div>
        {!detail.hostReviews.length && (
          <p className="text-sm text-ink/60">아직 공개된 후기가 없습니다. 거래가 완료되면 후기가 쌓입니다.</p>
        )}
        <div className="space-y-3">
          {detail.hostReviews.map((review) => (
            <div key={review.reviewId} className="rounded-2xl border border-ink/10 px-4 py-3">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-semibold text-ink">{review.reviewerName}</p>
                  <p className="text-xs text-ink/50">{review.partyTitle}</p>
                </div>
                <div className="text-right text-xs text-ink/55">
                  <p className="flex items-center justify-end gap-1 font-semibold text-ink">
                    <Star size={13} className="fill-amber-400 text-amber-400" />
                    {review.rating}점
                  </p>
                  <p>{review.createdAtLabel}</p>
                </div>
              </div>
              {review.comment && <p className="mt-3 text-sm leading-6 text-ink/75">{review.comment}</p>}
            </div>
          ))}
        </div>
      </div>

      {isHost && (
        <>
          <div className="card-elevated p-5 space-y-4">
            <h2 className="section-title">호스트 운영</h2>

            <form onSubmit={handleSettlementSubmit} className="space-y-3 rounded-2xl border border-ink/10 p-4">
              <h3 className="text-sm font-semibold text-ink">정산 확정</h3>
              <label className="block text-sm text-ink/75">
                <span className="mb-1 block">실구매 총액</span>
                <input
                  type="number"
                  min="0"
                  value={settlementForm.actualTotalPrice}
                  onChange={(e) => setSettlementForm((current) => ({ ...current, actualTotalPrice: e.target.value }))}
                  className="input-field"
                  required
                />
              </label>
              <label className="block text-sm text-ink/75">
                <span className="mb-1 block">영수증 메모</span>
                <textarea
                  rows="3"
                  value={settlementForm.receiptNote}
                  onChange={(e) => setSettlementForm((current) => ({ ...current, receiptNote: e.target.value }))}
                  className="input-field min-h-[96px]"
                  placeholder="예) 행사 카드 할인 적용, 현장 가격 변동"
                />
              </label>
              <button disabled={actionLoading === 'settlement'} className="btn-primary">
                <Wallet size={16} />
                {actionLoading === 'settlement' ? '저장 중...' : '정산 확정'}
              </button>
            </form>

            <form onSubmit={handlePickupSubmit} className="space-y-3 rounded-2xl border border-ink/10 p-4">
              <h3 className="text-sm font-semibold text-ink">픽업 일정</h3>
              <label className="block text-sm text-ink/75">
                <span className="mb-1 block">픽업 장소</span>
                <input
                  type="text"
                  value={pickupForm.pickupPlace}
                  onChange={(e) => setPickupForm((current) => ({ ...current, pickupPlace: e.target.value }))}
                  className="input-field"
                  required
                />
              </label>
              <label className="block text-sm text-ink/75">
                <span className="mb-1 block">픽업 시간</span>
                <input
                  type="datetime-local"
                  value={pickupForm.pickupTime}
                  onChange={(e) => setPickupForm((current) => ({ ...current, pickupTime: e.target.value }))}
                  className="input-field"
                  required
                />
              </label>
              <button disabled={actionLoading === 'pickup'} className="btn-primary">
                <MapPin size={16} />
                {actionLoading === 'pickup' ? '저장 중...' : '픽업 일정 확정'}
              </button>
            </form>
          </div>

          <div className="card-elevated p-5 space-y-3">
            <h2 className="section-title">참여자 정산 현황</h2>
            <div className="space-y-3">
              {detail.settlementMembers.map((member) => (
                <div key={member.memberId} className="rounded-2xl border border-ink/10 p-4 space-y-3">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-sm font-semibold text-ink">{member.username}</p>
                      <p className="text-xs text-ink/55">
                        {member.role === 'LEADER' ? '호스트' : '참여자'} · 요청 {member.requestedQuantity}
                        {detail.unitLabel}
                      </p>
                    </div>
                    <div className="text-right text-xs text-ink/55">
                      <p>예상 {formatCurrency(member.expectedAmount)}</p>
                      <p>확정 {formatCurrency(member.actualAmount)}</p>
                    </div>
                  </div>

                  <div className="grid gap-2 text-xs text-ink/60 md:grid-cols-3">
                    <p>송금 상태: {member.paymentStatusLabel ?? '미정'}</p>
                    <p>거래 상태: {member.tradeStatusLabel ?? '미정'}</p>
                    <p>픽업 확인: {member.pickupAcknowledged ? '확인함' : '미확인'}</p>
                  </div>

                  {member.role !== 'LEADER' && (
                    <div className="flex flex-wrap gap-2">
                      {member.paymentStatus === 'PAID' && (
                        <button
                          onClick={() =>
                            runAction(
                              `payment-confirm-${member.memberId}`,
                              () => api.updatePaymentStatus({ partyId: detail.partyId, memberId: member.memberId, paymentStatus: 'CONFIRMED' }),
                              '송금 확인을 완료했습니다.',
                            )
                          }
                          className="btn-secondary px-3 py-2 text-xs"
                          disabled={actionLoading === `payment-confirm-${member.memberId}`}
                        >
                          송금 확인
                        </button>
                      )}
                      {(member.paymentStatus === 'PAID' || member.paymentStatus === 'CONFIRMED') && (
                        <button
                          onClick={() =>
                            runAction(
                              `payment-refund-${member.memberId}`,
                              () => api.updatePaymentStatus({ partyId: detail.partyId, memberId: member.memberId, paymentStatus: 'REFUNDED' }),
                              '환불 상태로 변경했습니다.',
                            )
                          }
                          className="btn-ghost px-3 py-2 text-xs"
                          disabled={actionLoading === `payment-refund-${member.memberId}`}
                        >
                          환불 처리
                        </button>
                      )}
                      {member.paymentStatus === 'CONFIRMED' && member.tradeStatus === 'PENDING' && (
                        <button
                          onClick={() =>
                            runAction(
                              `trade-complete-${member.memberId}`,
                              () => api.updateTradeStatus({ partyId: detail.partyId, memberId: member.memberId, tradeStatus: 'COMPLETED' }),
                              '거래 완료로 처리했습니다.',
                            )
                          }
                          className="btn-primary px-3 py-2 text-xs"
                          disabled={actionLoading === `trade-complete-${member.memberId}`}
                        >
                          거래 완료
                        </button>
                      )}
                      {member.tradeStatus === 'PENDING' && (
                        <button
                          onClick={() =>
                            runAction(
                              `trade-noshow-${member.memberId}`,
                              () => api.updateTradeStatus({ partyId: detail.partyId, memberId: member.memberId, tradeStatus: 'NO_SHOW' }),
                              '노쇼로 기록했습니다.',
                            )
                          }
                          className="btn-ghost px-3 py-2 text-xs"
                          disabled={actionLoading === `trade-noshow-${member.memberId}`}
                        >
                          노쇼 처리
                        </button>
                      )}
                    </div>
                  )}

                  {member.role !== 'LEADER' && member.reviewEligible && (
                    <div className="rounded-2xl border border-mint-100 bg-mint-50/60 p-4 space-y-3">
                      <p className="text-sm font-semibold text-ink">참여자 후기</p>
                      {member.reviewWritten ? (
                        <p className="text-sm text-mint-900">이미 이 참여자에 대한 후기를 작성했습니다.</p>
                      ) : (
                        <>
                          <div className="grid gap-3 md:grid-cols-[120px_1fr]">
                            <label className="block text-sm text-ink/75">
                              <span className="mb-1 block">평점</span>
                              <select
                                value={memberReviewForms[member.memberId]?.rating ?? '5'}
                                onChange={(e) =>
                                  setMemberReviewForms((current) => ({
                                    ...current,
                                    [member.memberId]: {
                                      ...(current[member.memberId] ?? createReviewFormState()),
                                      rating: e.target.value,
                                    },
                                  }))
                                }
                                className="input-field"
                              >
                                {[5, 4, 3, 2, 1].map((score) => (
                                  <option key={score} value={String(score)}>
                                    {score}점
                                  </option>
                                ))}
                              </select>
                            </label>
                            <label className="block text-sm text-ink/75">
                              <span className="mb-1 block">후기</span>
                              <textarea
                                rows="3"
                                value={memberReviewForms[member.memberId]?.comment ?? ''}
                                onChange={(e) =>
                                  setMemberReviewForms((current) => ({
                                    ...current,
                                    [member.memberId]: {
                                      ...(current[member.memberId] ?? createReviewFormState()),
                                      comment: e.target.value,
                                    },
                                  }))
                                }
                                className="input-field min-h-[96px]"
                                placeholder="정산 속도, 약속 준수, 응답 매너를 남겨주세요."
                              />
                            </label>
                          </div>
                          <button
                            onClick={() =>
                              handleReviewSubmit({
                                targetUserId: member.userId,
                                rating: memberReviewForms[member.memberId]?.rating ?? '5',
                                comment: memberReviewForms[member.memberId]?.comment ?? '',
                                actionKey: `review-member-${member.memberId}`,
                                successMessage: '참여자 후기를 등록했습니다.',
                              })
                            }
                            className="btn-secondary px-4 py-2 text-sm"
                            disabled={actionLoading === `review-member-${member.memberId}`}
                          >
                            {actionLoading === `review-member-${member.memberId}` ? '등록 중...' : '참여자 후기 등록'}
                          </button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </>
      )}

      {isJoinedMember && (
        <div className="card-elevated p-5 space-y-3">
          <h2 className="section-title">내 거래 상태</h2>
          <div className="grid gap-2 text-sm text-ink/75">
            <div className="flex items-center justify-between gap-4">
              <span>예상 분담금</span>
              <span className="font-semibold text-ink">{formatCurrency(detail.expectedAmount)}</span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <span>확정 분담금</span>
              <span className="font-semibold text-ink">{formatCurrency(detail.actualAmount)}</span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <span>송금 상태</span>
              <span className="font-semibold text-ink">{detail.paymentStatusLabel ?? '정산 대기'}</span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <span>거래 상태</span>
              <span className="font-semibold text-ink">{detail.tradeStatusLabel ?? '거래 전'}</span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <span>픽업 일정 확인</span>
              <span className="font-semibold text-ink">{detail.pickupAcknowledged ? '확인 완료' : '미확인'}</span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {detail.actualAmount != null && detail.paymentStatus === 'PENDING' && (
              <button
                onClick={() =>
                  runAction(
                    'mark-paid-self',
                    () => api.updatePaymentStatus({ partyId: detail.partyId, memberId: detail.memberId, paymentStatus: 'PAID' }),
                    '송금 완료로 표시했습니다.',
                  )
                }
                className="btn-primary px-4 py-2 text-sm"
                disabled={!detail.memberId || actionLoading === 'mark-paid-self'}
              >
                송금 완료 표시
              </button>
            )}
            {detail.pickupPlace && !detail.pickupAcknowledged && (
              <button
                onClick={() => runAction('pickup-ack', () => api.acknowledgePickup(detail.partyId), '픽업 일정을 확인했습니다.')}
                className="btn-secondary px-4 py-2 text-sm"
                disabled={actionLoading === 'pickup-ack'}
              >
                픽업 일정 확인
              </button>
            )}
          </div>
          {detail.hasReviewedHost && (
            <p className="rounded-xl border border-mint-100 bg-mint-50 px-4 py-3 text-sm text-mint-900">
              호스트 후기를 이미 작성했습니다.
            </p>
          )}
          {detail.canReviewHost && !detail.hasReviewedHost && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleReviewSubmit({
                  targetUserId: detail.hostTrust?.userId,
                  rating: hostReviewForm.rating,
                  comment: hostReviewForm.comment,
                  actionKey: 'review-host',
                  successMessage: '호스트 후기를 등록했습니다.',
                });
              }}
              className="space-y-3 rounded-2xl border border-mint-100 bg-mint-50/60 p-4"
            >
              <h3 className="text-sm font-semibold text-ink">호스트 후기 작성</h3>
              <div className="grid gap-3 md:grid-cols-[120px_1fr]">
                <label className="block text-sm text-ink/75">
                  <span className="mb-1 block">평점</span>
                  <select
                    value={hostReviewForm.rating}
                    onChange={(e) => setHostReviewForm((current) => ({ ...current, rating: e.target.value }))}
                    className="input-field"
                  >
                    {[5, 4, 3, 2, 1].map((score) => (
                      <option key={score} value={String(score)}>
                        {score}점
                      </option>
                    ))}
                  </select>
                </label>
                <label className="block text-sm text-ink/75">
                  <span className="mb-1 block">후기</span>
                  <textarea
                    rows="3"
                    value={hostReviewForm.comment}
                    onChange={(e) => setHostReviewForm((current) => ({ ...current, comment: e.target.value }))}
                    className="input-field min-h-[96px]"
                    placeholder="분배 정확도, 약속 준수, 응답 속도를 남겨주세요."
                  />
                </label>
              </div>
              <button className="btn-primary px-4 py-2 text-sm" disabled={actionLoading === 'review-host' || !detail.hostTrust?.userId}>
                {actionLoading === 'review-host' ? '등록 중...' : '호스트 후기 등록'}
              </button>
            </form>
          )}
        </div>
      )}

      <div className="card-elevated p-5 space-y-3">
        <h2 className="section-title">참여</h2>
        <p className="section-subtitle">
          요청 수량을 선택하고 참여하세요. 최소 {detail.minimumShareUnit}
          {detail.unitLabel} 단위 · 남은 수량 {remaining}개 · 개당 {perUnit.toLocaleString()}원
        </p>
        {isJoinable && (
          <button
            onClick={() => navigate(`/parties/${detail.partyId}/join`, { state: { detail } })}
            className="btn-primary w-full"
          >
            참여하기
          </button>
        )}
        {!detail.participationStatus && !isJoinable && (
          <div className="rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink/70">
            {isClosedParty ? '이미 종료된 파티입니다.' : '모집이 마감된 파티입니다.'}
          </div>
        )}
        {detail.userRole && !isWaitingParty && (
          <div className="rounded-xl border border-mint-100 bg-white px-4 py-3 text-sm text-ink/70">
            이미 참여 중인 파티입니다.
          </div>
        )}
        {detail.userRole && !isWaitingParty && (
          <button
            onClick={() => navigate(`/chat/${detail.partyId}`)}
            className="btn-secondary w-full"
          >
            채팅방 이동
          </button>
        )}
        {isWaitingParty && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            대기열 {detail.waitingPosition ?? '-'}번입니다. 빈 자리가 생기면 자동으로 승격됩니다.
          </div>
        )}
      </div>
    </div>
  );
}

export default PartyDetail;
