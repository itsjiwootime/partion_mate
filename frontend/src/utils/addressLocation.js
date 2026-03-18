import { loadKakaoMapsSdk } from './kakaoMaps';
import { loadKakaoPostcode } from './kakaoPostcode';

export async function geocodeAddress(address, appKey) {
  const trimmedAddress = address?.trim();
  if (!trimmedAddress) {
    throw new Error('주소를 먼저 입력해 주세요.');
  }

  const kakao = await loadKakaoMapsSdk(appKey);
  const geocoder = new kakao.maps.services.Geocoder();

  return new Promise((resolve, reject) => {
    geocoder.addressSearch(trimmedAddress, (result, status) => {
      if (status !== kakao.maps.services.Status.OK || !result || result.length === 0) {
        reject(new Error('주소를 찾지 못했습니다. 정확한 주소를 입력해주세요.'));
        return;
      }

      const { y, x } = result[0];
      resolve({
        latitude: Number(y).toFixed(6),
        longitude: Number(x).toFixed(6),
      });
    });
  });
}

export async function searchAddressWithPostcode() {
  const daum = await loadKakaoPostcode();

  return new Promise((resolve, reject) => {
    let completed = false;

    new daum.Postcode({
      oncomplete: (data) => {
        completed = true;
        const selectedAddress = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;
        const nextAddress = selectedAddress || data.address;

        if (!nextAddress) {
          reject(new Error('주소를 확인해주세요.'));
          return;
        }

        resolve(nextAddress);
      },
      onclose: () => {
        if (!completed) {
          resolve(null);
        }
      },
    }).open();
  });
}
