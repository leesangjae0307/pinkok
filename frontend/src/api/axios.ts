import axios, {AxiosError, InternalAxiosRequestConfig} from 'axios';
import {API_BASE_URL} from '@env';
import {tokenStore} from '../services/tokenStore';

/**
 * 서버 주소. 값은 저장소에 없는 .env 에 둔다(`.env.example` 을 복사해서 채운다).
 * 끝에 `/` 나 `/api` 를 붙이지 않는다. 서버에는 `/api` 접두사가 없어서 붙이면 전부 404 다.
 *
 * cloudflared 임시 터널을 쓰면 **터널을 재시작할 때마다 주소가 바뀐다**. 연결이 안 되면
 * 먼저 그 주소가 아직 살아 있는지 확인한다.
 *
 * 환경별 대안:
 * - 서버를 같은 PC 에서 돌릴 때: `adb reverse tcp:8080 tcp:8080` 걸고 `http://localhost:8080`
 *   (주소가 바뀌지 않아 .env 를 고칠 일이 없다)
 * - 에뮬레이터: `http://10.0.2.2:8080`
 * - 같은 와이파이의 실기기: 서버 PC 의 LAN IP (예: `http://192.168.1.53:8080`)
 */
export const BASE_URL = API_BASE_URL ?? 'http://localhost:8080';

const api = axios.create({baseURL: BASE_URL, timeout: 15000});

let onSessionExpired: (() => void) | undefined;

/** 토큰 갱신까지 실패했을 때 로그인 화면으로 돌려보내는 콜백을 등록한다. */
export const setSessionExpiredHandler = (fn: () => void) => {
  onSessionExpired = fn;
};

api.interceptors.request.use(config => {
  const token = tokenStore.accessToken;
  if (token) {config.headers.Authorization = `Bearer ${token}`;}
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

/** 요청이 서버에 닿지 못했는지(비행기 모드, 와이파이 끊김, 서버가 꺼져 있음). */
export function isNetworkError(e: unknown): boolean {
  const err = e as AxiosError;
  return !!err?.isAxiosError && !err.response && !isTimeoutError(e);
}

/** 서버가 제한 시간(15초) 안에 답하지 않았는지. */
export function isTimeoutError(e: unknown): boolean {
  const err = e as AxiosError;
  if (!err?.isAxiosError || err.response) {return false;}
  return err.code === 'ECONNABORTED' || err.code === 'ETIMEDOUT' || /timeout/i.test(err.message ?? '');
}

/**
 * 서버가 준 한국어 메시지를 꺼낸다. 토스트·Alert 에 그대로 쓸 수 있다.
 *
 * 검증 실패(400)면 `message` 는 "입력값이 올바르지 않습니다." 라는 총평이고
 * 어느 항목이 문제인지는 `errors` 맵에만 있다. 그것까지 붙여야 사용자가
 * 무엇을 고쳐야 할지 알 수 있다.
 *
 * 서버에 닿지도 못한 오류에는 fallback 을 쓰지 않는다. 호출하는 쪽의 fallback 은
 * "이메일 또는 비밀번호를 확인해 주세요" 처럼 그 요청이 거절당했다는 뜻이라,
 * 인터넷이 끊겼을 때 그대로 보여 주면 사용자가 제 계정을 의심하게 된다.
 */
export function errorMessage(
  e: unknown,
  fallback = '잠시 후 다시 시도해 주세요.',
): string {
  if (isTimeoutError(e)) {return '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.';}
  if (isNetworkError(e)) {return '인터넷 연결을 확인해 주세요.';}

  const err = e as AxiosError<{message?: string; errors?: Record<string, string>}>;
  // 5xx 는 서버가 주는 문구가 "Internal Server Error" 라 사용자에게 도움이 안 된다.
  if ((err?.response?.status ?? 0) >= 500) {
    return '일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
  }

  const data = err?.response?.data;
  const fields = Object.values(data?.errors ?? {}).filter(Boolean);
  if (fields.length > 0) {return fields.join('\n');}
  return data?.message ?? fallback;
}

export default api;
