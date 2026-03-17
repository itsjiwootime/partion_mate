import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Pin, Send, ExternalLink, ArrowLeft, ShieldCheck } from 'lucide-react';
import { api } from '../api/client';
import { connectChatRoom } from '../utils/chatClient';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { EmptyState, LoadingState } from '../components/Feedback';
import { subscribeToPartyStream } from '../utils/partyRealtime';

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
      return;
    }

    let active = true;
    const fetchRoomDetail = async () => {
      try {
        setRoomLoading(true);
        setRoomError('');
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
    if (!isAuthed || !token || !selectedPartyId) {
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
        addToast(error.message || '채팅 연결 중 오류가 발생했습니다.', 'error');
      },
    });

    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [addToast, isAuthed, selectedPartyId, token, userName]);

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
        {selectedPartyId && roomError && (
          <div className="space-y-2">
            <p className="text-sm text-red-600">{roomError}</p>
            <button onClick={() => setRoomReloadToken((current) => current + 1)} className="btn-secondary px-4 py-2 text-sm">
              채팅방 다시 불러오기
            </button>
          </div>
        )}

        {selectedPartyId && roomDetail && !roomLoading && (
          <>
            <div className="flex items-center justify-between gap-3 border-b border-ink/10 pb-4">
              <div className="space-y-1">
                <button onClick={() => navigate('/chat')} className="btn-ghost px-0 text-xs lg:hidden">
                  <ArrowLeft size={14} /> 목록으로
                </button>
                <h2 className="text-lg font-semibold text-ink">{roomDetail.partyTitle}</h2>
                <p className="text-xs text-ink/50">{roomDetail.storeName}</p>
              </div>
              {roomDetail.openChatUrl && (
                <a
                  href={roomDetail.openChatUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn-secondary px-3 py-2 text-xs"
                >
                  <ExternalLink size={14} />
                  오픈채팅 열기
                </a>
              )}
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
          </>
        )}
      </div>
    </div>
  );
}

export default Chat;
