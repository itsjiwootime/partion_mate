import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Pin, Send, ArrowLeft, Flag, ShieldAlert, ShieldCheck, UserX } from 'lucide-react';
import { api } from '../api/client';
import { SafetyFallbackCard, SafetyStatusBanner } from '../components/SafetyFeedback';
import { ConfirmDialog, ReportDialog } from '../components/SafetyDialogs';
import { connectChatRoom } from '../utils/chatClient';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { EmptyState, LoadingState } from '../components/Feedback';
import { subscribeToPartyStream } from '../utils/partyRealtime';
import { isBlockedChatAccessMessage } from '../utils/safety';

function formatPreview(room) {
  if (room.lastMessagePreview) {
    return room.lastMessagePreview;
  }
  return '아직 메시지가 없습니다. 먼저 대화를 시작해보세요.';
}

function Chat() {
  const { partyId } = useParams();
  const selectedPartyId = partyId ? Number(partyId) : null;
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthed, token, userName } = useAuth();
  const { addToast } = useToast();
  const clientRef = useRef(null);
  const messageListRef = useRef(null);
  const roomPartyIdsRef = useRef(new Set());
  const [rooms, setRooms] = useState([]);
  const [roomsLoading, setRoomsLoading] = useState(false);
  const [roomsError, setRoomsError] = useState('');
  const [roomDetail, setRoomDetail] = useState(null);
  const [roomLoading, setRoomLoading] = useState(false);
  const [roomError, setRoomError] = useState('');
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [connectionState, setConnectionState] = useState(selectedPartyId ? 'connecting' : 'idle');
  const [updatingNotice, setUpdatingNotice] = useState(false);
  const [roomsReloadToken, setRoomsReloadToken] = useState(0);
  const [roomReloadToken, setRoomReloadToken] = useState(0);
  const [safetyActionLoading, setSafetyActionLoading] = useState('');
  const [reportDialogState, setReportDialogState] = useState(null);
  const [blockDialogState, setBlockDialogState] = useState(null);
  const [reportFeedback, setReportFeedback] = useState(null);
  const [blockedChatFeedback, setBlockedChatFeedback] = useState(null);

  useEffect(() => {
    roomPartyIdsRef.current = new Set(rooms.map((room) => room.partyId));
  }, [rooms]);

  useEffect(() => {
    if (!isAuthed) {
      navigate('/login', { replace: true, state: { from: `${location.pathname}${location.search}` } });
      return;
    }

    let active = true;
    const fetchRooms = async () => {
      try {
        setRoomsLoading(true);
        setRoomsError('');
        const data = await api.getMyChatRooms();
        if (active) {
          setRooms(data);
        }
      } catch (error) {
        if (active) {
          setRoomsError('내 채팅방을 불러오지 못했습니다.');
        }
      } finally {
        if (active) {
          setRoomsLoading(false);
        }
      }
    };

    fetchRooms();
    return () => {
      active = false;
    };
  }, [isAuthed, location.pathname, location.search, navigate, roomsReloadToken]);

  useEffect(() => {
    if (!isAuthed || !selectedPartyId) {
      setRoomDetail(null);
      setRoomError('');
      setNotice('');
      setConnectionState('idle');
      setReportFeedback(null);
      setBlockedChatFeedback(null);
      return;
    }

    let active = true;
    const fetchRoomDetail = async () => {
      try {
        setRoomLoading(true);
        setRoomError('');
        setBlockedChatFeedback(null);
        const data = await api.getChatRoomDetail(selectedPartyId);
        if (!active) return;
        setRoomDetail(data);
        setNotice(data.pinnedNotice ?? '');
        setRooms((current) =>
          current.map((room) => (room.partyId === selectedPartyId ? { ...room, unreadCount: 0, pinnedNotice: data.pinnedNotice } : room)),
        );
        try {
          await api.markChatRoomRead(selectedPartyId);
        } catch {
          // ignore read sync errors
        }
      } catch (error) {
        if (active) {
          if (isBlockedChatAccessMessage(error?.message)) {
            setRoomDetail(null);
            setBlockedChatFeedback({
              title: '차단 관계가 있어 이 채팅방에 접근할 수 없어요',
              description: '현재는 이 채팅방 메시지를 볼 수 없습니다. 차단 해제가 필요하면 프로필의 신뢰·안전 관리로 이동하세요.',
            });
            return;
          }
          setRoomError(error.message || '채팅방을 불러오지 못했습니다.');
        }
      } finally {
        if (active) {
          setRoomLoading(false);
        }
      }
    };

    fetchRoomDetail();
    return () => {
      active = false;
    };
  }, [isAuthed, roomReloadToken, selectedPartyId]);

  useEffect(() => {
    if (!isAuthed || !token || !selectedPartyId || blockedChatFeedback) {
      clientRef.current?.deactivate?.();
      clientRef.current = null;
      return undefined;
    }

    setConnectionState('connecting');
    const client = connectChatRoom({
      token,
      partyId: selectedPartyId,
      onStateChange: (state) => setConnectionState(state),
      onMessage: async (incoming) => {
        const nextIncoming = {
          ...incoming,
          mine: incoming.senderName === userName,
        };
        setRoomDetail((current) => {
          if (!current || current.partyId !== selectedPartyId) {
            return current;
          }
          const nextMessages = [...current.messages, nextIncoming].slice(-100);
          const nextPinnedNotice =
            nextIncoming.pinnedNotice !== null && nextIncoming.pinnedNotice !== undefined
              ? nextIncoming.pinnedNotice || null
              : current.pinnedNotice;
          return {
            ...current,
            pinnedNotice: nextPinnedNotice,
            messages: nextMessages,
            unreadCount: 0,
          };
        });
        setRooms((current) =>
          current.map((room) =>
            room.partyId === selectedPartyId
              ? {
                  ...room,
                  unreadCount: 0,
                  pinnedNotice:
                    nextIncoming.pinnedNotice !== null && nextIncoming.pinnedNotice !== undefined
                      ? nextIncoming.pinnedNotice || null
                      : room.pinnedNotice,
                  lastMessagePreview: nextIncoming.content,
                  lastMessageType: nextIncoming.type,
                  lastMessageCreatedAt: nextIncoming.createdAt,
                  lastMessageCreatedAtLabel: nextIncoming.createdAtLabel,
                }
              : room,
          ),
        );
        if (!nextIncoming.mine) {
          try {
            await api.markChatRoomRead(selectedPartyId);
          } catch {
            // ignore read sync errors
          }
        }
      },
      onError: (error) => {
        if (isBlockedChatAccessMessage(error?.message)) {
          setRoomDetail(null);
          setBlockedChatFeedback({
            title: '차단 관계가 있어 채팅이 제한되었어요',
            description: '이제 이 채팅방 접근과 새 메시지 수신이 중단됩니다. 차단 해제가 필요하면 프로필의 신뢰·안전 관리로 이동하세요.',
          });
          clientRef.current?.deactivate?.();
          return;
        }
        addToast(error.message || '채팅 연결 중 오류가 발생했습니다.', 'error');
      },
    });

    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [addToast, blockedChatFeedback, isAuthed, selectedPartyId, token, userName]);

  const openSafetyCenter = (notice) => {
    navigate('/me', {
      state: {
        focusSafetyCenter: true,
        safetyNotice: notice,
      },
    });
  };

  useEffect(() => {
    if (!isAuthed) {
      return undefined;
    }

    const unsubscribe = subscribeToPartyStream({
      onPartyUpdated: (event) => {
        const eventPartyId = event?.partyId ?? event?.id;
        if (!eventPartyId) return;
        if (!roomPartyIdsRef.current.has(eventPartyId) && selectedPartyId !== eventPartyId) {
          return;
        }

        setRoomsReloadToken((current) => current + 1);
        if (selectedPartyId === eventPartyId) {
          setRoomReloadToken((current) => current + 1);
        }
      },
      onFallback: () => {
        if (roomPartyIdsRef.current.size === 0 && !selectedPartyId) {
          return;
        }

        setRoomsReloadToken((current) => current + 1);
        if (selectedPartyId) {
          setRoomReloadToken((current) => current + 1);
        }
      },
    });

    return () => {
      unsubscribe?.();
    };
  }, [isAuthed, selectedPartyId]);

  useEffect(() => {
    if (!roomDetail?.messages?.length) return;
    const frameId = window.requestAnimationFrame(() => {
      if (!messageListRef.current) return;
      messageListRef.current.scrollTop = messageListRef.current.scrollHeight;
    });
    return () => window.cancelAnimationFrame(frameId);
  }, [roomDetail?.messages, selectedPartyId]);

  const handleSendMessage = (e) => {
    e.preventDefault();
    const content = message.trim();
    if (!content || !selectedPartyId) {
      return;
    }
    if (!clientRef.current?.connected) {
      addToast('채팅 연결이 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.', 'error');
      return;
    }

    clientRef.current.publish({
      destination: `/app/chat/${selectedPartyId}/messages`,
      body: JSON.stringify({ content }),
    });
    setMessage('');
  };

  const handleUpdateNotice = async (e) => {
    e.preventDefault();
    if (!selectedPartyId) return;

    try {
      setUpdatingNotice(true);
      const data = await api.updatePinnedNotice({
        partyId: selectedPartyId,
        pinnedNotice: notice,
      });
      setRoomDetail(data);
      setNotice(data.pinnedNotice ?? '');
      setRooms((current) =>
        current.map((room) =>
          room.partyId === selectedPartyId
            ? {
                ...room,
                pinnedNotice: data.pinnedNotice,
              }
            : room,
        ),
      );
      addToast('호스트 공지를 저장했습니다.', 'success');
    } catch (error) {
      addToast(error.message || '호스트 공지를 저장하지 못했습니다.', 'error');
    } finally {
      setUpdatingNotice(false);
    }
  };

  const handleSubmitReport = async ({ reasonType, memo }) => {
    if (!reportDialogState) {
      return;
    }

    try {
      setSafetyActionLoading('report');
      await api.createReport({
        targetType: reportDialogState.targetType,
        partyId: selectedPartyId,
        targetUserId: reportDialogState.targetUserId ?? null,
        reasonType,
        memo,
      });
      setReportDialogState(null);
      setReportFeedback({
        title: '신고가 접수되었어요',
        description: '처리 상태는 프로필의 신뢰·안전 관리에서 다시 확인할 수 있습니다. 추가로 불편하다면 차단도 바로 진행할 수 있습니다.',
      });
      addToast('신고를 접수했습니다. 운영 검토 후 필요한 조치를 진행합니다.', 'success');
    } catch (error) {
      addToast(error.message || '신고를 접수하지 못했습니다.', 'error');
    } finally {
      setSafetyActionLoading('');
    }
  };

  const handleConfirmBlock = async () => {
    if (!blockDialogState) {
      return;
    }

    try {
      setSafetyActionLoading('block');
      await api.blockUser({
        targetUserId: blockDialogState.targetUserId,
      });
      setBlockDialogState(null);
      setReportFeedback(null);
      setRoomDetail(null);
      setBlockedChatFeedback({
        title: `${blockDialogState.targetUsername}님을 차단했습니다`,
        description: '이 채팅방 접근과 이후 같은 파티 채팅 참여가 제한됩니다. 차단 해제는 프로필의 신뢰·안전 관리에서 할 수 있습니다.',
      });
      clientRef.current?.deactivate?.();
      addToast(`${blockDialogState.targetUsername}님을 차단했습니다. 이후 같은 파티와 채팅 참여가 제한됩니다.`, 'success');
      setRoomsReloadToken((current) => current + 1);
    } catch (error) {
      addToast(error.message || '사용자를 차단하지 못했습니다.', 'error');
    } finally {
      setSafetyActionLoading('');
    }
  };

  if (!isAuthed) {
    return null;
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
      <div className={['card-elevated p-4 space-y-3', selectedPartyId ? 'hidden lg:block' : 'block'].join(' ')}>
        <div className="flex items-center justify-between">
          <div>
            <h2 className="section-title">내 채팅방</h2>
            <p className="section-subtitle">참여 중인 파티 채팅을 확인하세요.</p>
          </div>
          <span className="text-xs text-ink/50">
            {connectionState === 'reconnecting' ? '재연결 중' : connectionState === 'live' ? '실시간 연결' : '대기'}
          </span>
        </div>

        {roomsLoading && <LoadingState />}
        {roomsError && (
          <div className="space-y-2">
            <p className="text-sm text-red-600">{roomsError}</p>
            <button onClick={() => setRoomsReloadToken((current) => current + 1)} className="btn-secondary px-4 py-2 text-sm">
              채팅방 다시 불러오기
            </button>
          </div>
        )}
        {!roomsLoading && !roomsError && rooms.length === 0 && (
          <EmptyState
            title="참여 중인 채팅방이 없어요"
            description="파티에 참여하면 채팅방이 자동으로 생성됩니다."
            action={
              <button onClick={() => navigate('/parties')} className="btn-secondary px-4 py-2 text-sm">
                파티 둘러보기
              </button>
            }
          />
        )}

        <div className="space-y-3">
          {rooms.map((room) => {
            const selected = room.partyId === selectedPartyId;
            return (
              <button
                key={room.partyId}
                onClick={() => navigate(`/chat/${room.partyId}`)}
                className={[
                  'w-full rounded-2xl border px-4 py-3 text-left transition',
                  selected ? 'border-mint-300 bg-mint-50' : 'border-ink/10 bg-white hover:border-mint-200',
                ].join(' ')}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-ink">{room.partyTitle}</p>
                    <p className="text-xs text-ink/50">{room.storeName}</p>
                  </div>
                  {room.unreadCount > 0 && (
                    <span className="rounded-full bg-mint-600 px-2 py-1 text-[11px] font-semibold text-white">
                      {room.unreadCount}
                    </span>
                  )}
                </div>
                {room.pinnedNotice && <p className="mt-2 text-xs text-mint-800">공지: {room.pinnedNotice}</p>}
                <p className="mt-2 text-sm text-ink/70">{formatPreview(room)}</p>
                <p className="mt-2 text-xs text-ink/45">{room.lastMessageCreatedAtLabel ?? '방금 생성된 채팅방'}</p>
              </button>
            );
          })}
        </div>
      </div>

      <div className={['card-elevated min-h-[640px] flex-col p-4', selectedPartyId ? 'flex' : 'hidden lg:flex'].join(' ')}>
        {!selectedPartyId && (
          <div className="flex flex-1 items-center justify-center">
            <EmptyState
              title="채팅방을 선택하세요"
              description="왼쪽 목록에서 채팅방을 선택하면 실시간 대화를 시작할 수 있습니다."
            />
          </div>
        )}

        {selectedPartyId && roomLoading && <LoadingState />}
        {selectedPartyId && !roomLoading && blockedChatFeedback && (
          <SafetyFallbackCard
            title={blockedChatFeedback.title}
            description={blockedChatFeedback.description}
            action={
              <button
                type="button"
                onClick={() =>
                  openSafetyCenter({
                    title: '차단 해제는 신뢰·안전 관리에서 할 수 있어요',
                    description: '차단 목록에서 사용자를 해제하면 이후 이 채팅방 접근이 다시 가능해질 수 있습니다.',
                  })
                }
                className="btn-secondary px-4 py-2 text-sm"
              >
                차단 관리 열기
              </button>
            }
            secondaryAction={
              <button type="button" onClick={() => navigate('/chat')} className="btn-primary px-4 py-2 text-sm">
                채팅 목록으로
              </button>
            }
          />
        )}
        {selectedPartyId && !blockedChatFeedback && roomError && (
          <div className="space-y-2">
            <p className="text-sm text-red-600">{roomError}</p>
            <button onClick={() => setRoomReloadToken((current) => current + 1)} className="btn-secondary px-4 py-2 text-sm">
              채팅방 다시 불러오기
            </button>
          </div>
        )}

        {selectedPartyId && roomDetail && !roomLoading && !blockedChatFeedback && (
          <>
            {reportFeedback && (
              <SafetyStatusBanner
                title={reportFeedback.title}
                description={reportFeedback.description}
                action={
                  <button
                    type="button"
                    onClick={() =>
                      openSafetyCenter({
                        title: '최근 신고 내역을 여기서 다시 확인할 수 있어요',
                        description: '접수된 신고 상태와 메모는 신뢰·안전 관리에서 계속 확인할 수 있습니다.',
                      })
                    }
                    className="btn-secondary px-4 py-2 text-xs"
                  >
                    내 신고 내역 보기
                  </button>
                }
              />
            )}
            <div className="flex items-center justify-between gap-3 border-b border-ink/10 pb-4">
              <div className="space-y-1">
                <button onClick={() => navigate('/chat')} className="btn-ghost px-0 text-xs lg:hidden">
                  <ArrowLeft size={14} /> 목록으로
                </button>
                <h2 className="text-lg font-semibold text-ink">{roomDetail.partyTitle}</h2>
                <p className="text-xs text-ink/50">{roomDetail.storeName}</p>
              </div>
              <button
                type="button"
                onClick={() =>
                  setReportDialogState({
                    targetType: 'PARTY',
                    targetUserId: null,
                    title: '이 파티 채팅을 신고할까요?',
                    description: '파티 운영이나 대화 흐름 전반에 문제가 있었다면 사유를 선택해 접수해 주세요.',
                  })
                }
                className="btn-secondary px-4 py-2 text-sm"
              >
                <Flag size={15} />
                파티 신고
              </button>
            </div>

            {roomDetail.pinnedNotice && (
              <div className="mt-4 rounded-2xl border border-mint-100 bg-mint-50 px-4 py-3">
                <div className="flex items-center gap-2 text-sm font-semibold text-mint-900">
                  <Pin size={14} />
                  고정 공지
                </div>
                <p className="mt-2 text-sm leading-6 text-ink/80 whitespace-pre-line">{roomDetail.pinnedNotice}</p>
                <p className="mt-2 text-xs text-ink/45">{roomDetail.pinnedNoticeUpdatedAtLabel}</p>
              </div>
            )}

            {roomDetail.host && (
              <form onSubmit={handleUpdateNotice} className="mt-4 space-y-3 rounded-2xl border border-ink/10 p-4">
                <div className="flex items-center gap-2 text-sm font-semibold text-ink">
                  <ShieldCheck size={16} className="text-mint-700" />
                  호스트 공지 고정
                </div>
                <textarea
                  rows="3"
                  value={notice}
                  onChange={(e) => setNotice(e.target.value)}
                  className="input min-h-[96px]"
                  placeholder="픽업 시간, 준비물, 정산 안내를 고정해보세요. 비우면 공지가 해제됩니다."
                />
                <button className="btn-secondary px-4 py-2 text-sm" disabled={updatingNotice}>
                  {updatingNotice ? '저장 중...' : '공지 저장'}
                </button>
              </form>
            )}

            <div ref={messageListRef} className="mt-4 flex-1 overflow-y-auto rounded-2xl border border-ink/10 bg-clean-white p-4">
              {roomDetail.messages.length === 0 ? (
                <div className="flex h-full items-center justify-center">
                  <EmptyState
                    title="첫 메시지를 보내보세요"
                    description="실시간 채팅과 시스템 메시지가 여기에 쌓입니다."
                  />
                </div>
              ) : (
                <div className="space-y-3">
                  {roomDetail.messages.map((item) => {
                    const isSystem = item.type === 'SYSTEM';
                    return (
                      <div key={item.messageId} className={isSystem ? 'flex justify-center' : item.mine ? 'flex justify-end' : 'flex justify-start'}>
                        {isSystem ? (
                          <div className="rounded-full bg-ink/10 px-3 py-2 text-xs text-ink/60">
                            {item.content} · {item.createdAtLabel}
                          </div>
                        ) : (
                          <div className="space-y-2">
                            <div
                              className={[
                                'max-w-[80%] rounded-2xl px-4 py-3',
                                item.mine ? 'bg-mint-600 text-white' : 'border border-ink/10 bg-white text-ink',
                              ].join(' ')}
                            >
                              <p className={`text-xs font-semibold ${item.mine ? 'text-white/80' : 'text-ink/55'}`}>{item.senderName}</p>
                              <p className="mt-1 whitespace-pre-line text-sm leading-6">{item.content}</p>
                              <p className={`mt-2 text-[11px] ${item.mine ? 'text-white/70' : 'text-ink/40'}`}>{item.createdAtLabel}</p>
                            </div>
                            {!item.mine && item.senderId != null && (
                              <div className="flex flex-wrap gap-2">
                                <button
                                  type="button"
                                  onClick={() =>
                                    setReportDialogState({
                                      targetType: 'CHAT',
                                      targetUserId: item.senderId,
                                      title: `${item.senderName}님의 메시지를 신고할까요?`,
                                      description: '부적절한 채팅, 스팸, 사기 의심 메시지는 바로 접수할 수 있습니다.',
                                    })
                                  }
                                  className="btn-ghost px-3 py-2 text-xs"
                                  aria-label={`${item.senderName}님 메시지 신고`}
                                >
                                  <Flag size={14} />
                                  신고
                                </button>
                                <button
                                  type="button"
                                  onClick={() =>
                                    setBlockDialogState({
                                      targetUserId: item.senderId,
                                      targetUsername: item.senderName,
                                      title: `${item.senderName}님을 차단할까요?`,
                                      description: '차단 후에는 이 사용자와 같은 파티와 채팅 접근이 제한됩니다.',
                                    })
                                  }
                                  className="btn-ghost px-3 py-2 text-xs text-amber-800"
                                  aria-label={`${item.senderName}님 메시지 차단`}
                                >
                                  <UserX size={14} />
                                  차단
                                </button>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <form onSubmit={handleSendMessage} className="mt-4 flex gap-2">
              <textarea
                rows="2"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="input min-h-[68px] flex-1"
                placeholder="메시지를 입력하세요."
              />
              <button className="btn-primary h-[68px] px-4" disabled={!clientRef.current?.connected || !message.trim()}>
                <Send size={16} />
                전송
              </button>
            </form>
            <p className="mt-2 text-xs text-ink/45">
              {connectionState === 'reconnecting'
                ? '채팅 연결이 끊겨 다시 연결 중입니다.'
                : connectionState === 'live'
                  ? '실시간 채팅이 연결되어 있습니다.'
                  : '채팅 연결을 준비 중입니다.'}
            </p>
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-ink/75">
              <div className="flex items-center gap-2 font-semibold text-ink">
                <ShieldAlert size={16} className="text-amber-700" />
                안전 안내
              </div>
              <p className="mt-2">
                파티 운영 문제는 상단의 파티 신고로, 특정 메시지 문제는 각 메시지 아래의 신고 또는 차단으로 바로 접수할 수 있습니다.
              </p>
            </div>
          </>
        )}
      </div>

      <ReportDialog
        open={Boolean(reportDialogState)}
        title={reportDialogState?.title ?? ''}
        description={reportDialogState?.description ?? ''}
        targetType={reportDialogState?.targetType ?? 'PARTY'}
        submitting={safetyActionLoading === 'report'}
        onClose={() => {
          if (safetyActionLoading === 'report') {
            return;
          }
          setReportDialogState(null);
        }}
        onSubmit={handleSubmitReport}
      />
      <ConfirmDialog
        open={Boolean(blockDialogState)}
        title={blockDialogState?.title ?? ''}
        description={blockDialogState?.description ?? ''}
        confirmLabel="차단하기"
        confirmTone="danger"
        submitting={safetyActionLoading === 'block'}
        onClose={() => {
          if (safetyActionLoading === 'block') {
            return;
          }
          setBlockDialogState(null);
        }}
        onConfirm={handleConfirmBlock}
      />
    </div>
  );
}

export default Chat;
