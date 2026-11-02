/**
 * 완성된 PinLog 영상을 기기 갤러리에 저장한다.
 *
 * 영상은 서버에 있어서 주소만 들고 있다. CameraRoll 은 로컬 파일만 받으므로 먼저
 * 캐시로 내려받은 뒤 저장한다.
 *
 * 권한은 안드로이드 버전마다 다르다.
 * - 13(API 33) 이상: 사진·영상 접근이 READ_MEDIA_* 로 쪼개졌다. 저장만 할 때는
 *   따로 권한을 받지 않아도 되지만, 저장한 뒤 다시 읽으려면 READ_MEDIA_VIDEO 가 필요하다.
 * - 10(API 29)~12: Scoped Storage 라 저장에 권한이 필요 없다.
 * - 9(API 28) 이하: WRITE_EXTERNAL_STORAGE 를 받아야 한다.
 */
import { PermissionsAndroid, Platform } from 'react-native';
import { CameraRoll } from '@react-native-camera-roll/camera-roll';
import RNFS from 'react-native-fs';

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

export class GalleryPermissionError extends Error {}

/**
 * 영상을 갤러리에 저장한다. 권한이 없으면 GalleryPermissionError 를 던진다.
 *
 * @param url 서버 주소 또는 이미 로컬인 file:// 주소
 * @param fileName 갤러리에 남길 이름 (확장자 없이)
 */
export async function saveVideoToGallery(url: string, fileName: string): Promise<void> {
  const granted = await ensurePermission();
  if (!granted) throw new GalleryPermissionError('갤러리 접근 권한이 없어요.');

  let localPath = url;
  let downloaded: string | null = null;

  if (!url.startsWith('file://')) {
    downloaded = `${RNFS.CachesDirectoryPath}/${fileName}.mp4`;
    const { statusCode } = await RNFS.downloadFile({ fromUrl: url, toFile: downloaded }).promise;
    if (statusCode && statusCode >= 400) {
      throw new Error(`영상을 내려받지 못했어요 (${statusCode}).`);
    }
    localPath = `file://${downloaded}`;
  }

  try {
    await CameraRoll.saveAsset(localPath, { type: 'video', album: 'PinKok' });
  } finally {
    // 갤러리에 복사됐으니 캐시본은 남겨둘 이유가 없다. 실패해도 저장 자체는 끝난 거라 무시한다.
    if (downloaded) await RNFS.unlink(downloaded).catch(() => {});
  }
}
