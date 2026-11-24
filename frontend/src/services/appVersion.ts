/**
 * 설치된 앱의 버전. android/app/build.gradle 의 versionName/versionCode 를
 * 그대로 읽는다(네이티브 AppVersion 모듈).
 *
 * 화면에 글자로 적어 두지 않는 이유: 배포 번호를 올릴 때 같이 고치는 것을
 * 잊으면 실제 설치본과 어긋난다. 실제로 1.0.1 을 설치하고도 설정 화면에는
 * v1.0.0 이 떠 있었다.
 *
 * iOS 에는 아직 같은 모듈이 없다. 모듈이 없으면 빈 문자열이라, 화면은 버전을
 * 아예 숨기는 쪽을 고른다. 모르는 값을 지어내지 않는다.
 */
import { NativeModules } from 'react-native';

const native = NativeModules.AppVersion as
  | { versionName?: string; versionCode?: number }
  | undefined;

/** 예: '1.0.1'. 알 수 없으면 빈 문자열. */
export const APP_VERSION_NAME = native?.versionName ?? '';

/** 예: 5. 알 수 없으면 0. */
export const APP_VERSION_CODE = native?.versionCode ?? 0;

