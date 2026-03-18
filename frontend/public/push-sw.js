self.addEventListener('push', (event) => {
  const payload = parsePayload(event);
  const title = payload.title || '파티메이트 알림';
  const url = resolveTargetUrl(payload.url);

  event.waitUntil(
    self.registration.showNotification(title, {
      body: payload.body || '',
      tag: payload.tag || 'partition-mate-notification',
      data: { url },
    }),
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = resolveTargetUrl(event.notification.data?.url);
  event.waitUntil(focusOrOpen(url));
});

function parsePayload(event) {
  if (!event.data) {
    return {};
  }

  try {
    return event.data.json();
  } catch (error) {
    return {};
  }
}

function resolveTargetUrl(path) {
  const targetPath = typeof path === 'string' && path.trim() ? path : '/notifications';
  return new URL(targetPath, self.location.origin).toString();
}

async function focusOrOpen(targetUrl) {
  const clientList = await clients.matchAll({ type: 'window', includeUncontrolled: true });

  for (const client of clientList) {
    const clientUrl = new URL(client.url);
    if (clientUrl.origin !== self.location.origin) {
      continue;
    }

    if ('focus' in client) {
      await client.focus();
    }
    if ('navigate' in client) {
      await client.navigate(targetUrl);
    }
    return;
  }

  await clients.openWindow(targetUrl);
}
