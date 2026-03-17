export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

let authToken = null;
let authFailureHandler = null;
let accessTokenRefreshHandler = null;
let refreshPromise = null;

class ApiError extends Error {
  constructor(message, { status, code, isAuthFailure } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.isAuthFailure = isAuthFailure;
  }
}

export const setAuthToken = (token) => {
  authToken = token;
};

export const setAuthFailureHandler = (handler) => {
  authFailureHandler = handler;
};

export const setAccessTokenRefreshHandler = (handler) => {
  accessTokenRefreshHandler = handler;
};

function buildHeaders(options = {}) {
  return {
    'Content-Type': 'application/json',
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    ...options.headers,
  };
}

async function parseErrorResponse(response) {
  const contentType = response.headers.get('content-type') || '';
  let message = `Request failed: ${response.status}`;
  let code = null;

  if (contentType.includes('application/json')) {
    try {
      const err = await response.json();
      message = err.message || message;
      code = err.code || null;
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

  return new ApiError(message, {
    status: response.status,
    code,
    isAuthFailure: response.status === 401,
  });
}

async function parseSuccessResponse(response) {
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  const text = await response.text();
  if (!text) return null;
  if (contentType.includes('application/json')) {
    return JSON.parse(text);
  }
  return text;
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = fetchJson('/api/auth/refresh', {
      method: 'POST',
      skipAuthHandling: true,
      skipTokenRefresh: true,
    })
      .then((response) => {
        const trimmed = response.accessToken?.trim() || null;
        setAuthToken(trimmed);
        if (typeof accessTokenRefreshHandler === 'function') {
          accessTokenRefreshHandler(response);
        }
        return response;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function fetchJson(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: buildHeaders(options),
    ...options,
  });

  if (!response.ok) {
    const error = await parseErrorResponse(response);

    if (
      response.status === 401 &&
      authToken &&
      !path.startsWith('/api/auth/') &&
      !options.skipTokenRefresh
    ) {
      try {
        await refreshAccessToken();
        return fetchJson(path, {
          ...options,
          skipTokenRefresh: true,
        });
      } catch (refreshError) {
        const finalError = refreshError instanceof ApiError ? refreshError : error;

        if (
          !path.startsWith('/api/auth/') &&
          !options.skipAuthHandling &&
          typeof authFailureHandler === 'function'
        ) {
          authFailureHandler(finalError);
        }

        throw finalError;
      }
    }

    if (
      response.status === 401 &&
      !path.startsWith('/api/auth/') &&
      !options.skipAuthHandling &&
      typeof authFailureHandler === 'function'
    ) {
      authFailureHandler(error);
    }

    throw error;
  }

  return parseSuccessResponse(response);
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

  refreshSession: () =>
    fetchJson('/api/auth/refresh', {
      method: 'POST',
      skipAuthHandling: true,
      skipTokenRefresh: true,
    }),

  logout: () =>
    fetchJson('/api/auth/logout', {
      method: 'POST',
      skipAuthHandling: true,
      skipTokenRefresh: true,
    }),

  checkUsername: (username) => fetchJson(`/api/auth/check-username?username=${encodeURIComponent(username)}`),
  checkEmail: (email) => fetchJson(`/api/auth/check-email?email=${encodeURIComponent(email)}`),

  getNearbyStores: ({ latitude, longitude }, options = {}) =>
    fetchJson(`/api/stores/nearby?latitude=${latitude}&longitude=${longitude}`, options),

  getStoreDetail: (storeId) => fetchJson(`/api/stores/${storeId}`),

  getStoreParties: (storeId) => fetchJson(`/api/stores/${storeId}/parties`),
  getAllParties: () => fetchJson('/party/all'),

  getPartyDetail: (partyId) => fetchJson(`/party/${partyId}`),

  getMyParties: () => fetchJson('/api/users/me/parties'),
  getMyNotifications: () => fetchJson('/api/users/me/notifications'),
  getMyChatRooms: () => fetchJson('/api/chat/rooms'),
  getChatRoomDetail: (partyId) => fetchJson(`/api/chat/rooms/${partyId}`),
  markChatRoomRead: (partyId) =>
    fetchJson(`/api/chat/rooms/${partyId}/read`, {
      method: 'POST',
    }),
  updatePinnedNotice: ({ partyId, pinnedNotice }) =>
    fetchJson(`/api/chat/rooms/${partyId}/notice`, {
      method: 'PUT',
      body: JSON.stringify({ pinnedNotice }),
    }),

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

  submitReview: ({ partyId, targetUserId, rating, comment }) =>
    fetchJson(`/party/${partyId}/reviews`, {
      method: 'POST',
      body: JSON.stringify({ targetUserId, rating, comment }),
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
