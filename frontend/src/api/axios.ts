import axios, {AxiosError, InternalAxiosRequestConfig} from 'axios';
import {tokenStore} from '../services/tokenStore';

/**
 * 끝에 `/` 나 `/api` 를 붙이지 않는다. 서버에는 `/api` 접두사가 없어서 붙이면 전부 404 다.
 *
 * 개발 PC 에서 띄운 서버를 USB 로 연결된 실기기에서 쓰는 설정이다.
 * 기기의 8080 을 PC 로 넘겨줘야 하므로 앱을 켜기 전에 한 번 걸어둔다:
 *
 *     adb reverse tcp:8080 tcp:8080
 *
 * cloudflared 터널도 쓸 수 있지만 재시작할 때마다 주소가 바뀌어 매번 이 값을
 * 고쳐야 했다. 같은 PC 에서 돌릴 거면 이 쪽이 손이 덜 간다.
 *
 * 환경별 대안:
 * - 에뮬레이터: `http://10.0.2.2:8080`
 * - 같은 와이파이의 실기기: 서버 PC 의 LAN IP (예: `http://192.168.1.53:8080`)
 * - 서버가 다른 PC 에 있을 때: 그 PC 에서 띄운 터널 주소
 */
export const BASE_URL = 'http://localhost:8080';

const api = axios.create({baseURL: BASE_URL, timeout: 15000});

let onSessionExpired: (() => void) | undefined;

/** 토큰 갱신까지 실패했을 때 로그인 화면으로 돌려보내는 콜백을 등록한다. */
export const setSessionExpiredHandler = (fn: () => void) => {
  onSessionExpired = fn;
};

api.interceptors.request.use(config => {
  const token = tokenStore.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// refreshToken 은 1회용이라 동시에 401 이 여러 개 와도 갱신은 한 번만 해야 한다.
let refreshing: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const {data} = await axios.post(`${BASE_URL}/auth/refresh`, {
    refreshToken: tokenStore.refreshToken,
  });
  await tokenStore.save(data.accessToken, data.refreshToken);
  return data.accessToken;
}

api.interceptors.response.use(
  res => res,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & {_retried?: boolean})
      | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (
      error.response?.status !== 401 ||
      !original ||
      original._retried ||
      isAuthCall
    ) {
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

/**
 * 서버가 준 한국어 메시지를 꺼낸다. 토스트·Alert 에 그대로 쓸 수 있다.
 *
 * 검증 실패(400)면 `message` 는 "입력값이 올바르지 않습니다." 라는 총평이고
 * 어느 항목이 문제인지는 `errors` 맵에만 있다. 그것까지 붙여야 사용자가
 * 무엇을 고쳐야 할지 알 수 있다.
 */
export function errorMessage(
  e: unknown,
  fallback = '잠시 후 다시 시도해 주세요.',
): string {
  const data = (e as AxiosError<{message?: string; errors?: Record<string, string>}>)
    ?.response?.data;
  const fields = Object.values(data?.errors ?? {}).filter(Boolean);
  if (fields.length > 0) return fields.join('\n');
  return data?.message ?? fallback;
}

export default api;
