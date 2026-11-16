/** 네이티브 모듈은 Jest(노드) 환경에 없어서 테스트용 대역으로 바꿔요. */
require('react-native-gesture-handler/jestSetup');

jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

/**
 * Firebase 는 네이티브 모듈을 바로 찾아서, 모킹하지 않으면 import 단계에서 터져요.
 * 푸시는 화면 렌더와 상관없으니 쓰는 함수만 빈 껍데기로 둡니다.
 */
jest.mock('@react-native-firebase/messaging', () => {
  const messaging = () => ({
    requestPermission: jest.fn().mockResolvedValue(1),
    hasPermission: jest.fn().mockResolvedValue(1),
    getToken: jest.fn().mockResolvedValue('test-token'),
    deleteToken: jest.fn().mockResolvedValue(undefined),
    onMessage: jest.fn(() => jest.fn()),
    onNotificationOpenedApp: jest.fn(() => jest.fn()),
    getInitialNotification: jest.fn().mockResolvedValue(null),
    setBackgroundMessageHandler: jest.fn(),
  });
  messaging.AuthorizationStatus = { AUTHORIZED: 1, PROVISIONAL: 2, DENIED: 0 };
  return { __esModule: true, default: messaging };
});

jest.mock('@react-native-firebase/app', () => ({
  __esModule: true,
  default: { apps: [], app: jest.fn() },
}));

/** 지도는 WebView 로 띄워요. 노드에는 그 네이티브 뷰가 없어 빈 View 로 둡니다. */
jest.mock('react-native-webview', () => {
  const { View } = require('react-native');
  return { __esModule: true, WebView: View, default: View };
});

/** 카메라·영상도 화면 렌더에만 필요해서 빈 View 로 바꿔요. */
jest.mock('react-native-video', () => {
  const { View } = require('react-native');
  return { __esModule: true, default: View };
});

jest.mock('react-native-vision-camera', () => {
  const { View } = require('react-native');
  return {
    __esModule: true,
    Camera: View,
    useCameraDevice: () => null,
    useCameraFormat: () => undefined,
    useCameraPermission: () => ({ hasPermission: true, requestPermission: jest.fn() }),
    useMicrophonePermission: () => ({ hasPermission: true, requestPermission: jest.fn() }),
  };
});

jest.mock('react-native-image-picker', () => ({
  __esModule: true,
  launchImageLibrary: jest.fn().mockResolvedValue({ didCancel: true }),
  launchCamera: jest.fn().mockResolvedValue({ didCancel: true }),
}));

jest.mock('react-native-blob-util', () => ({
  __esModule: true,
  default: {
    config: jest.fn(() => ({ fetch: jest.fn().mockResolvedValue({ path: () => '' }) })),
    fs: { dirs: { CacheDir: '/tmp' }, unlink: jest.fn() },
  },
}));

/** 갤러리 저장도 네이티브라 대역으로 둡니다. */
jest.mock('@react-native-camera-roll/camera-roll', () => ({
  __esModule: true,
  CameraRoll: { saveAsset: jest.fn().mockResolvedValue({}) },
}));
