const SERVICE_WORKER_PATH = '/push-sw.js';

export function isWebPushSupported() {
  return (
    typeof window !== 'undefined' &&
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  );
}

export function getNotificationPermissionState() {
  if (typeof Notification === 'undefined') {
    return 'unsupported';
  }
  return Notification.permission;
}

export async function registerPushServiceWorker() {
  if (!isWebPushSupported()) {
    return null;
  }

  return navigator.serviceWorker.register(SERVICE_WORKER_PATH);
}

export async function getCurrentPushSubscription() {
  if (!isWebPushSupported()) {
    return null;
  }

  const registration = await registerPushServiceWorker();
  return registration?.pushManager.getSubscription() ?? null;
}

export async function subscribeToWebPush(publicKey) {
  if (!publicKey) {
    throw new Error('브라우저 푸시 공개 키가 설정되지 않았습니다.');
  }

  if (!isWebPushSupported()) {
    throw new Error('현재 브라우저는 웹 푸시를 지원하지 않습니다.');
  }

  const registration = await registerPushServiceWorker();
  const existingSubscription = await registration.pushManager.getSubscription();
  if (existingSubscription) {
    return existingSubscription;
  }

  const permission =
    Notification.permission === 'granted'
      ? 'granted'
      : await Notification.requestPermission();

  if (permission !== 'granted') {
    throw new Error('브라우저 알림 권한이 허용되지 않았습니다.');
  }

  return registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(publicKey),
  });
}

export async function unsubscribeFromWebPush() {
  const subscription = await getCurrentPushSubscription();
  if (!subscription) {
    return null;
  }

  const endpoint = subscription.endpoint;
  await subscription.unsubscribe();
  return endpoint;
}

export function serializePushSubscription(subscription) {
  const payload = subscription.toJSON();
  return {
    endpoint: subscription.endpoint,
    keys: {
      p256dh: payload.keys?.p256dh ?? '',
      auth: payload.keys?.auth ?? '',
    },
    userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : '',
  };
}

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const normalized = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(normalized);
  const outputArray = new Uint8Array(rawData.length);

  for (let index = 0; index < rawData.length; index += 1) {
    outputArray[index] = rawData.charCodeAt(index);
  }

  return outputArray;
}
