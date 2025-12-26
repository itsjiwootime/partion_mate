let postcodeLoadingPromise = null;

export function loadKakaoPostcode() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Kakao Postcode can only load in the browser.'));
  }

  if (window.daum && window.daum.Postcode) {
    return Promise.resolve(window.daum);
  }

  if (postcodeLoadingPromise) {
    return postcodeLoadingPromise;
  }

  postcodeLoadingPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.async = true;
    script.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
    script.onload = () => {
      if (!window.daum || !window.daum.Postcode) {
        reject(new Error('Kakao Postcode failed to initialize.'));
        return;
      }
      resolve(window.daum);
    };
    script.onerror = () => reject(new Error('Failed to load Kakao Postcode.'));
    document.head.appendChild(script);
  });

  return postcodeLoadingPromise;
}
