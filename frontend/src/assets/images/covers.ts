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

/**
 * 고른 기본 표지를 서버에 적어 두는 방법.
 *
 * 기본 표지는 앱 안에 들어 있는 그림이라 올릴 파일이 없다. 그래서 coverImageUrl
 * 에 'default:tokyo' 처럼 어느 그림인지만 적어 둔다. 이렇게 하지 않으면 서버에는
 * 표지가 비어 있는 것으로 남아, 목록을 다시 받아올 때마다 첫 번째 그림(제주)으로
 * 되돌아간다. 무엇을 고르든 제주가 되던 이유가 이것이다.
 */
const DEFAULT_COVER_PREFIX = 'default:';

/** 고른 그림을 서버에 적을 값으로. 기본 표지가 아니면 null. */
export function defaultCoverMarker(src: unknown): string | null {
  const found = DEFAULT_COVERS.find((c) => c.src === src);
  return found ? `${DEFAULT_COVER_PREFIX}${found.id}` : null;
}

/** 서버가 준 값이 기본 표지를 가리키면 그 그림을, 아니면 null. */
export function defaultCoverSource(url: string | null | undefined): number | null {
  if (!url || !url.startsWith(DEFAULT_COVER_PREFIX)) {return null;}
  const id = url.slice(DEFAULT_COVER_PREFIX.length);
  return DEFAULT_COVERS.find((c) => c.id === id)?.src ?? null;
}
