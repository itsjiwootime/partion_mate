import { API_BASE } from '../api/client';

function buildStreamUrl({ storeId, partyId } = {}) {
  const url = new URL('/party/stream', API_BASE);
  if (storeId) {
    url.searchParams.set('storeId', String(storeId));
  }
  if (partyId) {
    url.searchParams.set('partyId', String(partyId));
  }
  return url.toString();
}

export function subscribeToPartyStream({
  storeId,
  partyId,
  onConnected,
  onPartyUpdated,
  onReconnectStateChange,
  onFallback,
} = {}) {
  let eventSource = null;
  let reconnectTimer = null;
  let reconnectAttempts = 0;
  let closed = false;

  const clearReconnect = () => {
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  const scheduleReconnect = () => {
    reconnectAttempts += 1;
    const delay = Math.min(5000, reconnectAttempts * 1000);
    onReconnectStateChange?.('reconnecting');
    reconnectTimer = window.setTimeout(connect, delay);
  };

  const connect = () => {
    if (closed) return;

    clearReconnect();
    eventSource = new EventSource(buildStreamUrl({ storeId, partyId }));

    eventSource.addEventListener('connected', (event) => {
      reconnectAttempts = 0;
      onReconnectStateChange?.('live');
      onConnected?.(JSON.parse(event.data));
    });

    eventSource.addEventListener('party-updated', (event) => {
      onPartyUpdated?.(JSON.parse(event.data));
    });

    eventSource.onerror = () => {
      if (eventSource) {
        eventSource.close();
      }
      if (closed) return;
      onFallback?.();
      scheduleReconnect();
    };
  };

  connect();

  return () => {
    closed = true;
    clearReconnect();
    if (eventSource) {
      eventSource.close();
    }
  };
}
