/** 네이티브 모듈은 Jest(노드) 환경에 없어서 테스트용 대역으로 바꿔요. */
require('react-native-gesture-handler/jestSetup');

jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);
