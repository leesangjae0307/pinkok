module.exports = {
  // nativewind/babel 안에 react-native-reanimated/plugin 이 이미 포함돼 있어서
  // 아래 plugins 에 reanimated 플러그인을 따로 넣지 않아요 (중복 시 빌드 오류).
  presets: ['module:@react-native/babel-preset', 'nativewind/babel'],
  plugins: [
    [
      // .env 값을 '@env' 모듈로 읽어온다. 키를 소스에 적어 두지 않기 위한 것이고,
      // 번들에는 그대로 박히므로 비밀이 되는 건 아니다(클라이언트 키 특성).
      'module:react-native-dotenv',
      {
        moduleName: '@env',
        path: '.env',
        safe: false,
        allowUndefined: true,
      },
    ],
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
