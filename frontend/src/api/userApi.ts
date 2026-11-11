import api from './axios';
import { Me } from './authApi';

/** 프로필 집계에 쓰는 장소 한 줄. 방문·저장이 같은 모양으로 온다. */
export interface StatPlace {
  itineraryItemId: number;
  tripId: number;
  tripTitle: string;
  placeName: string;
  roadAddress: string | null;
  lotAddress: string | null;
  categoryName: string | null;
  /** 방문 목록에만 들어 있다. 내가 그 핀에 기록을 남긴 시각. */
  recordedAt: string | null;
}

export interface MyStats {
  visitedCount: number;
  savedCount: number;
  regionCount: number;
  /** 최신순 */
  visited: StatPlace[];
  /** 최신순 */
  saved: StatPlace[];
  /** 여행 수가 많은 순 → 가나다순 */
  regions: { name: string; tripCount: number }[];
  /** 많은 순. code 는 TRAVEL_STYLES 와 같은 값이다. */
  tastes: { code: string; name: string; count: number; percent: number }[];
}

/**
 * 내 여행 기록 집계. 방문·저장 장소, 다녀온 지역, 취향을 한 번에 준다.
 * 세는 기준과 정렬은 서버가 정한다. 지역을 비워둔 여행과 지운 여행은 빠진다.
 */
export async function fetchMyStats(): Promise<MyStats> {
  const { data } = await api.get<MyStats>('/users/me/stats');
  return data;
}

/**
 * 이 기기의 FCM 토큰을 서버에 등록한다. 로그인했을 때와 토큰이 갱신될 때 부른다.
 *
 * 서버가 아직 이 API 를 만들기 전이라 404 가 올 수 있다. 푸시는 없어도 앱을 쓰는 데
 * 지장이 없으므로 부르는 쪽에서 실패를 삼킨다.
 */
export async function registerPushToken(token: string): Promise<void> {
  await api.post('/users/me/push-tokens', { token, platform: 'ANDROID' });
}

/** 로그아웃할 때 이 기기로 더는 푸시가 가지 않게 한다. */
export async function unregisterPushToken(token: string): Promise<void> {
  await api.delete('/users/me/push-tokens', { data: { token } });
}

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
 * 회원 탈퇴. 되돌릴 수 없어서 서버가 비밀번호를 한 번 더 확인한다(틀리면 401).
 *
 * 내가 팀장이던 여행은 남은 팀원에게 넘어가고, 혼자 쓰던 여행은 함께 지워진다.
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
