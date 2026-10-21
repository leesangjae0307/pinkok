module.exports = {
  preset: 'react-native',
  // App.tsx 가 NativeWind 용 global.css 를 import 하는데 Jest 는 CSS 를 못 읽어요.
  moduleNameMapper: {
    '\\.css$': '<rootDir>/__tests__/setup/styleMock.js',
  },
  setupFiles: ['<rootDir>/__tests__/setup/jest.setup.js'],
  // 테스트가 아닌 보조 파일(목·스텁)은 테스트로 수집하지 않아요.
  testPathIgnorePatterns: ['<rootDir>/__tests__/setup/'],
  transformIgnorePatterns: [
    'node_modules/(?!(jest-)?@?react-native|@react-native-community|@react-navigation|nativewind|react-native-css-interop)',
  ],
};
