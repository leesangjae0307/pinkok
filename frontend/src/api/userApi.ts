import api from './axios';
import { Me } from './authApi';

/** 닉네임 변경. PATCH /users/me — 서버 규칙: 1~50자 */
export async function updateNickname(nickname: string): Promise<Me> {
  const { data } = await api.patch('/users/me', { nickname });
  return data;
}
