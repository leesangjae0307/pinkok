/**
 * 다른 사람의 아바타를 그릴 때 쓰는 변환기.
 *
 * 왜 필요한가: 서버는 남의 아바타를 코드가 아니라 imageUrl(`/avatars/F1.png` 같은
 * 상대 경로) 로만 준다. 내 아바타는 코드가 있어서 앱 에셋으로 바로 그려지는데,
 * 남의 것은 그 주소를 그대로 Image 에 넘기게 되어 서버에 그 파일이 없거나 주소가
 * 상대 경로이면 얼굴이 비어 보였다("내 계정에서는 보이는데 다른 계정에서는 안 보임").
 *
 * 그래서 GET /avatars 를 한 번 받아 imageUrl → 코드 표를 만들어 두고, 남의
 * avatarImageUrl 도 앱에 있는 같은 그림으로 바꿔 그린다. 표를 못 받았을 때는 파일
 * 이름에서 코드(F1 … M3)를 읽어 보고, 그것도 안 되면 절대 주소로 떨어뜨린다.
 */
import api from './axios';
import { fileUrl } from './fileApi';
import { AVATAR_IMAGE_BY_CODE } from '../assets/images/avatars';
import type { AvatarSource } from '../data/mockData';

/** 아바타가 없는 사람에게 쓰는 기본 얼굴 */
export const FALLBACK_AVATAR = '🙂';

interface ServerAvatar {
  id: number;
  code: string;
  imageUrl: string;
  name: string;
  gender: string;
}

/** imageUrl(앞뒤 공백·호스트 제거) → 아바타 코드 */
let codeByUrl: Record<string, string> = {};

/** 주소 비교용으로 호스트와 질의 문자열을 떼고 소문자로 맞춘다. */
function normalizeUrl(url: string): string {
  const withoutQuery = url.split('?')[0];
  const withoutHost = withoutQuery.replace(/^https?:\/\/[^/]+/i, '');
  return withoutHost.toLowerCase();
}

/**
 * 아바타 목록을 한 번 받아 표를 채운다. 실패해도 파일 이름으로 읽는 길이 남아 있어
 * 그냥 넘어간다(로그인 직후 한 번만 부른다).
 */
export async function loadAvatarCatalog(): Promise<void> {
  try {
    const { data } = await api.get<ServerAvatar[]>('/avatars');
    const next: Record<string, string> = {};
    data.forEach((a) => {
      if (a.imageUrl && a.code) next[normalizeUrl(a.imageUrl)] = a.code;
    });
    codeByUrl = next;
  } catch {
    // 표가 없으면 avatarCodeFromUrl 이 파일 이름으로 읽는다.
  }
}

/** 아바타 주소에서 서버 코드를 알아낸다. 모르면 null. */
export function avatarCodeFromUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  const known = codeByUrl[normalizeUrl(url)];
  if (known) return known;
  // 표를 못 받았을 때. `/avatars/f1.png`, `avatar_F1_v2.png` 모두 F1 로 읽는다.
  const match = /(?:^|[^a-z0-9])([fm][123])(?:[^a-z0-9]|$)/i.exec(normalizeUrl(url));
  return match ? match[1].toUpperCase() : null;
}

/**
 * 서버가 준 사람 정보를 Image 에 넘길 수 있는 값으로 바꾼다.
 *
 * 1) 아바타 코드를 알면 앱 에셋(number) — 네트워크가 필요 없어 가장 확실하다.
 * 2) 직접 올린 프로필 사진이면 절대 주소.
 * 3) 아무것도 없으면 이모지.
 */
export function personAvatarSource(
  person:
    | {
        avatarImageUrl?: string | null;
        profileImageUrl?: string | null;
      }
    | null
    | undefined,
): AvatarSource {
  const local = AVATAR_IMAGE_BY_CODE[avatarCodeFromUrl(person?.avatarImageUrl) ?? ''];
  if (local) return local;
  return fileUrl(person?.profileImageUrl) ?? fileUrl(person?.avatarImageUrl) ?? FALLBACK_AVATAR;
}
