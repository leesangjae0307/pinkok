module.exports = {
  // nativewind/babel 안에 react-native-reanimated/plugin 이 이미 포함돼 있어서
  // 아래 plugins 에 reanimated 플러그인을 따로 넣지 않아요 (중복 시 빌드 오류).
  presets: ['module:@react-native/babel-preset', 'nativewind/babel'],
  plugins: [
    [
      'module-resolver',
      {
        root: ['./src'],
        alias: {
          '@': './src',
        },
      },
    ],
  ],
};
