/**
 * 완성된 PinLog 영상을 기기 갤러리에 저장한다.
 *
 * 영상은 서버에 있고 CameraRoll 은 로컬 파일만 받으므로, 먼저 캐시로 받은 뒤 저장한다.
 * 받는 곳은 `GET /pinlogs/{id}/download` 이고 Authorization 헤더가 필요하다.
 * (재생용 videoUrl 은 로그인 없이 열리지만, 내려받기는 팀원만 할 수 있다.)
 *
 * 권한은 안드로이드 버전마다 다르다.
 * - 13(API 33) 이상: 사진·영상 접근이 READ_MEDIA_* 로 쪼개졌다. 저장만 할 때는
 *   없어도 되는 경우가 많지만, 저장한 뒤 다시 읽으려면 READ_MEDIA_VIDEO 가 필요하다.
 * - 10(API 29)~12: Scoped Storage 라 저장에 권한이 필요 없다.
 * - 9(API 28) 이하: WRITE_EXTERNAL_STORAGE 를 받아야 한다.
 */
import { PermissionsAndroid, Platform } from 'react-native';
import { CameraRoll } from '@react-native-camera-roll/camera-roll';
import ReactNativeBlobUtil from 'react-native-blob-util';

import { BASE_URL } from '../api/axios';
import { tokenStore } from './tokenStore';

export class GalleryPermissionError extends Error {}

/** 저장에 필요한 권한을 받는다. 받을 필요가 없는 버전이면 그냥 true. */
async function ensurePermission(): Promise<boolean> {
  if (Platform.OS !== 'android') return true;

  if (Platform.Version >= 33) {
    const result = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.READ_MEDIA_VIDEO,
      {
        title: '갤러리 접근 권한',
        message: '저장한 PinLog 를 갤러리에서 바로 볼 수 있게 권한이 필요해요.',
        buttonPositive: '확인',
      },
    );
    return result === PermissionsAndroid.RESULTS.GRANTED;
  }

  if (Platform.Version <= 28) {
    const result = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
      {
        title: '저장 권한',
        message: 'PinLog 영상을 갤러리에 저장하려면 권한이 필요해요.',
        buttonPositive: '확인',
      },
    );
    return result === PermissionsAndroid.RESULTS.GRANTED;
  }

  // 29~32 는 저장에 권한이 필요 없다.
  return true;
}

/**
 * PinLog 영상을 갤러리의 PinKok 앨범에 저장한다.
 * 권한이 없으면 GalleryPermissionError 를 던진다.
 *
 * @param onProgress 0~1. Content-Length 가 없으면 불리지 않는다.
 */
export async function savePinlogToGallery(
  pinlogId: number,
  onProgress?: (ratio: number) => void,
): Promise<void> {
  const granted = await ensurePermission();
  if (!granted) throw new GalleryPermissionError('갤러리 접근 권한이 없어요.');

  const token = tokenStore.accessToken;
  // 받은 파일 이름이 그대로 갤러리에 남으므로, 캐시 경로를 직접 정해 이름을 붙여준다.
  // (fileCache 에 맡기면 ReactNativeBlobUtilTmp_<uuid>.mp4 로 저장된다.)
  const cachePath = `${ReactNativeBlobUtil.fs.dirs.CacheDir}/pinkok-${Date.now()}.mp4`;
  const task = ReactNativeBlobUtil.config({ path: cachePath }).fetch(
    'GET',
    `${BASE_URL}/pinlogs/${pinlogId}/download`,
    token ? { Authorization: `Bearer ${token}` } : undefined,
  );

  if (onProgress) {
    task.progress((received, total) => {
      const got = Number(received);
      const all = Number(total);
      if (all > 0) onProgress(Math.min(1, got / all));
    });
  }

  const res = await task;
  const status = res.info().status;
  if (status >= 400) {
    // 받다 만 파일을 남겨두지 않는다.
    res.flush();
    throw new Error(
      status === 404
        ? '아직 영상이 준비되지 않았어요.'
        : status === 403
          ? '이 여행의 팀원만 내려받을 수 있어요.'
          : `영상을 내려받지 못했어요 (${status}).`,
    );
  }

  try {
    await CameraRoll.saveAsset(`file://${res.path()}`, { type: 'video', album: 'PinKok' });
  } finally {
    // 갤러리로 복사됐으니 캐시본은 남겨둘 이유가 없다.
    res.flush();
  }
}
