import api from './axios';

export interface RegisterRequest {
  email: string;
  passwordHash: string;
  nickname: string;
}

export interface UserResponse {
  id: number;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  provider: string | null;
  createdAt: string;
}

// POST /users/register (backend UserController)
export const register = async (body: RegisterRequest) => {
  const {data} = await api.post<UserResponse>('/users/register', body);
  return data;
};
