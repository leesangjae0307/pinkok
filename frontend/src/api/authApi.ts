import api from './axios';
import { tokenStore } from '../services/tokenStore';

export interface Avatar {
  id: number;
  code: string;
  name: string;
  imageUrl: string;
  gender: string;
}

export interface Me {
  id: number;
  email: string;
  username: string;
  nickname: string;
  profileImageUrl: string | null;
  avatar: Avatar | null;
}

/**
 * 응답: { accessToken, refreshToken, expiresIn, tokenType, newUser, user }
 * user 는 Me 와 같은 모양이다. 토큰은 저장하고 사용자 정보는 호출부로 넘긴다.
 */
export async function login(email: string, password: string): Promise<Me> {
  const { data } = await api.post('/auth/login', { email, password });
  await tokenStore.save(data.accessToken, data.refreshToken);
  return data.user;
}

/**
 * 가입 전에 이메일이 비어 있는지 묻는다. 로그인 없이 부를 수 있다.
 *
 * 형식이 틀리면 400 이고, 분당 60회(IP 기준)를 넘기면 429 다. 둘 다 예외로 올라오니
 * 호출하는 쪽에서 "사용 가능" 으로 넘기지 않도록 주의한다.
 *
 * 여기서 비어 있다고 나와도 가입까지의 사이에 다른 사람이 먼저 쓸 수 있다.
 * 최종 판단은 POST /auth/signup 이 한다.
 */
export async function checkEmailAvailable(email: string): Promise<boolean> {
  const { data } = await api.get<{ available: boolean }>('/auth/check-email', {
    params: { email },
  });
  return data.available;
}

/** 아이디(@username)도 같은 방식이다. */
export async function checkUsernameAvailable(username: string): Promise<boolean> {
  const { data } = await api.get<{ available: boolean }>('/auth/check-username', {
    params: { username },
  });
  return data.available;
}

/**
 * 201 과 함께 생성된 사용자를 돌려주지만 토큰은 주지 않는다.
 * 그래서 가입 직후에도 login() 을 한 번 더 호출해야 세션이 선다.
 */
export async function signup(payload: {
  email: string;
  password: string;
  username: string;
  nickname: string;
  avatarId?: number;
}): Promise<Me> {
  const { data } = await api.post<Me>('/auth/signup', payload);
  return data;
}

/**
 * 저장된 토큰으로 현재 사용자를 다시 받아온다. 앱을 재실행하면 토큰만 남고
 * 사용자 정보는 없으므로 이걸로 복원한다.
 */
export async function fetchMe(): Promise<Me> {
  const { data } = await api.get<Me>('/users/me');
  return data;
}

export async function logout(): Promise<void> {
  try {
    await api.post('/auth/logout', { refreshToken: tokenStore.refreshToken });
  } finally {
    await tokenStore.clear();
  }
}

/** 로그인 없이 호출 가능 */
export async function fetchAvatars(): Promise<Avatar[]> {
  const { data } = await api.get<Avatar[]>('/avatars');
  return data;
}
