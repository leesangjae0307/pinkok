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
 * 회원 탈퇴. 되돌릴 수 없어서 서버가 비밀번호를 한 번 더 확인한다.
 *
 * 계정과 개인정보만 지우고 남긴 기록·영상은 남긴다 — 여행을 함께 쓴 팀원의 화면과
 * PinLog 가 깨지지 않게 하기 위해서다. 내가 만든 여행이 남아 있으면 409 로 거절한다.
 */
export async function deleteAccount(password: string): Promise<void> {
  await api.delete('/users/me', { data: { password } });
}

/**
 * 아바타 선택. avatarId 는 GET /avatars 가 주는 id 다.
 * 반대로 이걸 설정하면 올려둔 프로필 사진이 지워진다.
 */
export async function selectAvatar(avatarId: number): Promise<Me> {
  const { data } = await api.patch('/users/me/avatar', { avatarId });
  return data;
}
