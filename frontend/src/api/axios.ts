import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { tokenStore } from '../services/tokenStore';

/**
 * 서버에는 `/api` 접두사가 없다. 붙이면 모든 호출이 404 가 된다.
 *
 * Android 에서 localhost 를 쓰려면 `adb reverse tcp:8080 tcp:8080` 이 걸려 있어야 한다
 * (USB 실기기·에뮬레이터 모두 됨). 안 걸었다면 에뮬레이터는 `http://10.0.2.2:8080`,
 * 같은 와이파이의 실기기는 서버 PC 의 LAN IP 를 쓴다.
 */
export const BASE_URL = 'http://localhost:8080';

const api = axios.create({ baseURL: BASE_URL, timeout: 15000 });

let onSessionExpired: (() => void) | undefined;

/** 토큰 갱신까지 실패했을 때 로그인 화면으로 돌려보내는 콜백을 등록한다. */
export const setSessionExpiredHandler = (fn: () => void) => {
  onSessionExpired = fn;
};

api.interceptors.request.use((config) => {
  const token = tokenStore.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// refreshToken 은 1회용이라 동시에 401 이 여러 개 와도 갱신은 한 번만 해야 한다.
let refreshing: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const { data } = await axios.post(`${BASE_URL}/auth/refresh`, {
    refreshToken: tokenStore.refreshToken,
  });
  await tokenStore.save(data.accessToken, data.refreshToken);
  return data.accessToken;
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (error.response?.status !== 401 || !original || original._retried || isAuthCall) {
      return Promise.reject(error);
    }
    original._retried = true;
    try {
      refreshing =
        refreshing ??
        refreshAccessToken().finally(() => {
          refreshing = null;
        });
      original.headers.Authorization = `Bearer ${await refreshing}`;
      return api(original);
    } catch (e) {
      await tokenStore.clear();
      onSessionExpired?.();
      return Promise.reject(e);
    }
  },
);

/** 서버가 준 한국어 메시지를 꺼낸다. 토스트에 그대로 쓸 수 있다. */
export function errorMessage(e: unknown, fallback = '잠시 후 다시 시도해 주세요.'): string {
  const data = (e as AxiosError<{ message?: string }>)?.response?.data;
  return data?.message ?? fallback;
}

export default api;
