/**
 * 새 여행 만들기의 기본 픽셀 표지 4종.
 * 웹 디자인의 cover-pixel-*.svg 를 4배(1280×720) PNG 로 변환한 것이다.
 * 모두 사각형으로만 그려진 픽셀 그림이라 변환해도 경계가 그대로 유지된다.
 */
import coverJeju from './cover-pixel-jeju.png';
import coverTokyo from './cover-pixel-tokyo.png';
import coverBusan from './cover-pixel-busan.png';
import coverGangneung from './cover-pixel-gangneung.png';

export const DEFAULT_COVERS = [
  { id: 'jeju', label: '제주', src: coverJeju },
  { id: 'tokyo', label: '도쿄', src: coverTokyo },
  { id: 'busan', label: '부산', src: coverBusan },
  { id: 'gangneung', label: '강릉', src: coverGangneung },
] as const;
