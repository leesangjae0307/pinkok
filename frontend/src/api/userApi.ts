import api from './axios';
import { Me } from './authApi';

/** 닉네임 변경. PATCH /users/me — 서버 규칙: 1~50자 */
export async function updateNickname(nickname: string): Promise<Me> {
  const { data } = await api.patch('/users/me', { nickname });
  return data;
}

/**
 * 프로필 사진 변경. POST /files 로 올리고 받은 url 을 보낸다.
 * 아바타와는 둘 중 하나만 쓰이므로 이걸 설정하면 아바타는 해제된다.
 */
export async function updateProfileImage(profileImageUrl: string): Promise<Me> {
  const { data } = await api.patch('/users/me/profile-image', { profileImageUrl });
  return data;
}

/**
 * 아바타 선택. avatarId 는 GET /avatars 가 주는 id 다.
 * 반대로 이걸 설정하면 올려둔 프로필 사진이 지워진다.
 */
export async function selectAvatar(avatarId: number): Promise<Me> {
  const { data } = await api.patch('/users/me/avatar', { avatarId });
  return data;
}
