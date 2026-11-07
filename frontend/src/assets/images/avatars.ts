/**
 * 픽셀 아바타 6종.
 *
 * 서버도 같은 그림을 들고 있지만(GET /avatars 의 imageUrl), 앱에 넣어 둔 것을 쓰면
 * 네트워크 없이 바로 그릴 수 있고 서버 파일이 없어도 얼굴이 비지 않는다.
 * 그래서 아바타는 "서버 코드 → 앱 에셋" 으로 바꿔서 쓴다.
 */
import avatarBookWinter from './avatar-book-winter.png';
import avatarSilverBucket from './avatar-silver-bucket.png';
import avatarPinkCamera from './avatar-pink-camera.png';
import avatarMapPhone from './avatar-map-phone.png';
import avatarHeadphoneWink from './avatar-headphone-wink.png';
import avatarSunglasses from './avatar-sunglasses.png';

export interface AvatarAsset {
  id: string;
  /** require() 결과(number). Image 에 그대로 넘길 수 있다. */
  image: number;
  label: string;
  /** 서버 아바타 코드 (F1~F3 / M1~M3) */
  code: string;
}

export const AVATAR_ASSETS: AvatarAsset[] = [
  { id: 'book-winter', image: avatarBookWinter, label: '책 읽는 겨울 여행가', code: 'F1' },
  { id: 'silver-bucket', image: avatarSilverBucket, label: '은발 버킷햇 여행가', code: 'F2' },
  { id: 'pink-camera', image: avatarPinkCamera, label: '핑크 카메라 여행가', code: 'F3' },
  { id: 'map-phone', image: avatarMapPhone, label: '지도 보는 도시 여행가', code: 'M1' },
  { id: 'headphone-wink', image: avatarHeadphoneWink, label: '윙크 헤드폰 여행가', code: 'M2' },
  { id: 'sunglasses', image: avatarSunglasses, label: '선글라스 로컬 여행가', code: 'M3' },
];

/** 서버 아바타 코드 → 앱 에셋. 모르는 코드면 undefined. */
export const AVATAR_IMAGE_BY_CODE: Record<string, number | undefined> = AVATAR_ASSETS.reduce(
  (acc, a) => {
    acc[a.code] = a.image;
    return acc;
  },
  {} as Record<string, number | undefined>,
);

export { avatarBookWinter, avatarSilverBucket, avatarPinkCamera, avatarMapPhone, avatarHeadphoneWink, avatarSunglasses };
