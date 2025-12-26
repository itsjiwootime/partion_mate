let kakaoLoadingPromise = null;

export function loadKakaoMapsSdk(appKey) {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Kakao Maps SDK can only load in the browser.'));
  }

  if (window.kakao && window.kakao.maps) {
    return Promise.resolve(window.kakao);
  }

  if (kakaoLoadingPromise) {
    return kakaoLoadingPromise;
  }

  if (!appKey) {
    return Promise.reject(new Error('VITE_KAKAO_MAP_KEY is missing.'));
  }

  kakaoLoadingPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&libraries=services&autoload=false`;
    script.onload = () => {
      if (!window.kakao || !window.kakao.maps) {
        reject(new Error('Kakao Maps SDK failed to initialize.'));
        return;
      }
      window.kakao.maps.load(() => resolve(window.kakao));
    };
    script.onerror = () => reject(new Error('Failed to load Kakao Maps SDK.'));
    document.head.appendChild(script);
  });

  return kakaoLoadingPromise;
}
