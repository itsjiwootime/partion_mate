export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

let authToken = null;

export const setAuthToken = (token) => {
  authToken = token;
};

async function fetchJson(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const contentType = response.headers.get('content-type') || '';
    let message = `Request failed: ${response.status}`;
    if (contentType.includes('application/json')) {
      try {
        const err = await response.json();
        message = err.message || message;
      } catch {
        // ignore json parse error
      }
    } else {
      try {
        message = await response.text();
      } catch {
        // ignore
      }
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  const text = await response.text();
  if (!text) return null;
  if (contentType.includes('application/json')) {
    return JSON.parse(text);
  }
  return text;
}

export const api = {
  signup: (payload) =>
    fetchJson('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  login: (payload) =>
    fetchJson('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  checkUsername: (username) => fetchJson(`/api/auth/check-username?username=${encodeURIComponent(username)}`),
  checkEmail: (email) => fetchJson(`/api/auth/check-email?email=${encodeURIComponent(email)}`),

  getNearbyStores: ({ latitude, longitude }) =>
    fetchJson(`/api/stores/nearby?latitude=${latitude}&longitude=${longitude}`),

  getStoreDetail: (storeId) => fetchJson(`/api/stores/${storeId}`),

  getStoreParties: (storeId) => fetchJson(`/api/stores/${storeId}/parties`),
  getAllParties: () => fetchJson('/party/all'),

  getPartyDetail: (partyId) => fetchJson(`/party/${partyId}`),

  getMyParties: () => fetchJson('/api/users/me/parties'),
  getMyNotifications: () => fetchJson('/api/users/me/notifications'),

  joinParty: ({ partyId, quantity = 1 }) =>
    fetchJson(`/party/${partyId}/join`, {
      method: 'POST',
      body: JSON.stringify({ memberRequestQuantity: quantity }),
    }),

  cancelJoin: (partyId) =>
    fetchJson(`/party/${partyId}/join`, {
      method: 'DELETE',
    }),

  confirmSettlement: ({ partyId, actualTotalPrice, receiptNote }) =>
    fetchJson(`/party/${partyId}/settlement`, {
      method: 'PUT',
      body: JSON.stringify({ actualTotalPrice, receiptNote }),
    }),

  confirmPickupSchedule: ({ partyId, pickupPlace, pickupTime }) =>
    fetchJson(`/party/${partyId}/pickup`, {
      method: 'PUT',
      body: JSON.stringify({ pickupPlace, pickupTime }),
    }),

  acknowledgePickup: (partyId) =>
    fetchJson(`/party/${partyId}/pickup/acknowledge`, {
      method: 'POST',
    }),

  updatePaymentStatus: ({ partyId, memberId, paymentStatus }) =>
    fetchJson(`/party/${partyId}/members/${memberId}/payment`, {
      method: 'PUT',
      body: JSON.stringify({ paymentStatus }),
    }),

  updateTradeStatus: ({ partyId, memberId, tradeStatus }) =>
    fetchJson(`/party/${partyId}/members/${memberId}/trade-status`, {
      method: 'PUT',
      body: JSON.stringify({ tradeStatus }),
    }),

  createParty: (payload) =>
    fetchJson('/party', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  getMe: () => fetchJson('/api/users/me'),
  handleError: (err) => {
    throw err;
  },
};
