import { Client } from '@stomp/stompjs';
import { API_BASE } from '../api/client';

function resolveBrokerUrl() {
  if (API_BASE.startsWith('https://')) {
    return `${API_BASE.replace('https://', 'wss://')}/ws-chat`;
  }
  if (API_BASE.startsWith('http://')) {
    return `${API_BASE.replace('http://', 'ws://')}/ws-chat`;
  }
  return `${window.location.protocol === 'https:' ? 'wss://' : 'ws://'}${window.location.host}/ws-chat`;
}

export function connectChatRoom({ token, partyId, onConnected, onStateChange, onMessage, onError }) {
  const client = new Client({
    brokerURL: resolveBrokerUrl(),
    reconnectDelay: 1500,
    debug: () => {},
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  client.onConnect = () => {
    onStateChange?.('live');
    client.subscribe(`/topic/chat/${partyId}`, (frame) => {
      try {
        onMessage?.(JSON.parse(frame.body));
      } catch (error) {
        onError?.(error);
      }
    });
    onConnected?.(client);
  };

  client.onWebSocketClose = () => {
    onStateChange?.('reconnecting');
  };

  client.onWebSocketError = () => {
    onStateChange?.('error');
  };

  client.onStompError = (frame) => {
    onError?.(new Error(frame.headers.message || '채팅 연결 중 오류가 발생했습니다.'));
  };

  client.activate();

  return client;
}
