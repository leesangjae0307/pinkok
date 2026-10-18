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

export async function login(email: string, password: string): Promise<Me> {
  const { data } = await api.post('/auth/login', { email, password });
  await tokenStore.save(data.accessToken, data.refreshToken);
  return data.user;
}

/** 201 만 주고 토큰은 안 준다. 이어서 login() 을 호출해야 한다. */
export async function signup(payload: {
  email: string;
  password: string;
  username: string;
  nickname: string;
  avatarId?: number;
}): Promise<void> {
  await api.post('/auth/signup', payload);
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
