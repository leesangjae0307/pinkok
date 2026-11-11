/**
 * 폰 푸시 알림 (FCM).
 *
 * 앱 안 알림함(GET /notifications)과는 별개다. 알림함은 앱을 켜야 보이고,
 * 이건 앱이 꺼져 있어도 폰에 뜬다. 보내는 쪽은 서버이고 앱이 하는 일은 셋뿐이다.
 *   1. 이 기기의 토큰을 받아 서버에 등록한다(로그인할 때).
 *   2. 토큰이 바뀌면 다시 등록한다.
 *   3. 로그아웃하면 해제해서 남의 알림이 이 기기로 오지 않게 한다.
 *
 * 서버에 푸시 토큰 API 가 아직 없을 수 있다. 푸시는 없어도 앱을 쓰는 데 지장이
 * 없으므로 등록이 실패해도 조용히 넘어간다 — 그것 때문에 로그인이 막히면 안 된다.
 */
import { PermissionsAndroid, Platform } from 'react-native';
import messaging from '@react-native-firebase/messaging';

import { registerPushToken, unregisterPushToken } from '../api/userApi';

/** 알림을 눌렀을 때 서버가 함께 보내주는 값. 알림함 응답과 같은 이름을 쓴다. */
export interface PushTarget {
  /** TRIP / PINLOG / AI_REQUEST / INVITATION */
  targetType: string;
  targetId: number | null;
}

/**
 * 알림 권한을 받는다.
 *
 * 안드로이드 13(API 33)부터는 POST_NOTIFICATIONS 를 물어봐야 알림이 뜬다.
 * 그 아래 버전은 설치만 하면 되므로 묻지 않는다.
 */
export async function ensureNotificationPermission(): Promise<boolean> {
  if (Platform.OS !== 'android') return true;
  if (Platform.Version < 33) return true;

  const already = await PermissionsAndroid.check(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
  );
  if (already) return true;

  const result = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
    {
      title: '알림 권한',
      message: '여행 초대와 PinLog 완성 소식을 폰 알림으로 받아보세요.',
      buttonPositive: '확인',
    },
  );
  return result === PermissionsAndroid.RESULTS.GRANTED;
}

/** 지금 이 기기의 토큰. 권한이 없으면 null. */
async function currentToken(): Promise<string | null> {
  try {
    const granted = await ensureNotificationPermission();
    if (!granted) return null;
    const token = await messaging().getToken();
    // Firebase 콘솔에서 이 기기로 테스트 알림을 쏘려면 토큰이 필요하다.
    // 기기를 가리키는 값이라 개발 빌드에서만 찍는다.
    if (__DEV__) console.log('[FCM] token:', token);
    return token;
  } catch {
    // Firebase 설정(google-services.json)이 없는 빌드에서도 앱은 돌아가야 한다.
    return null;
  }
}

/**
 * 로그인한 뒤 이 기기를 푸시 대상으로 등록한다.
 * 토큰이 갱신되면 다시 등록하도록 구독해 두고, 그 구독을 끊는 함수를 돌려준다.
 */
export async function startPush(): Promise<() => void> {
  const token = await currentToken();
  if (token) {
    try {
      await registerPushToken(token);
    } catch {
      // 서버에 API 가 아직 없거나 잠깐 실패한 것. 푸시만 안 올 뿐이다.
    }
  }

  return messaging().onTokenRefresh(async (next) => {
    try {
      await registerPushToken(next);
    } catch {
      // 위와 같다.
    }
  });
}

/** 로그아웃할 때 해제한다. 안 하면 다음에 로그인한 사람에게 남의 알림이 간다. */
export async function stopPush(): Promise<void> {
  try {
    const token = await messaging().getToken();
    await unregisterPushToken(token);
  } catch {
    // 등록이 안 돼 있었거나 서버가 모르는 토큰이면 할 일이 없다.
  }
}

/** 알림 데이터에서 어디로 갈지 읽는다. 값이 없거나 모양이 다르면 null. */
function toTarget(data?: Record<string, string | object>): PushTarget | null {
  const type = data?.targetType;
  if (typeof type !== 'string' || type.length === 0) return null;
  const rawId = data?.targetId;
  const id = typeof rawId === 'string' ? Number(rawId) : NaN;
  return { targetType: type, targetId: Number.isFinite(id) ? id : null };
}

/**
 * 알림을 눌러 앱이 열렸을 때 어디로 갈지 알려준다.
 *
 * 두 경우를 함께 본다.
 *   - 앱이 백그라운드에 있다가 알림으로 올라온 경우 (onNotificationOpenedApp)
 *   - 앱이 아예 꺼져 있다가 알림으로 시작된 경우 (getInitialNotification)
 *
 * 구독을 끊는 함수를 돌려준다.
 */
export function onNotificationOpened(handler: (target: PushTarget) => void): () => void {
  let cancelled = false;

  messaging()
    .getInitialNotification()
    .then((message) => {
      if (cancelled || !message) return;
      const target = toTarget(message.data);
      if (target) handler(target);
    })
    .catch(() => {
      // 알림으로 연 게 아니면 아무 일도 없다.
    });

  const unsubscribe = messaging().onNotificationOpenedApp((message) => {
    const target = toTarget(message.data);
    if (target) handler(target);
  });

  return () => {
    cancelled = true;
    unsubscribe();
  };
}
