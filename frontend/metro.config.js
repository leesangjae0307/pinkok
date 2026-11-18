const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const {withNativeWind} = require('nativewind/metro');

// Element Inspector 나 빨간 에러 화면에서 소스 위치를 누르면 그 파일의 그 줄을
// 에디터로 열어준다. RN 은 process.env.REACT_EDITOR 를 보고 에디터를 고르는데
// (@react-native-community/cli-tools/launchEditor), 값이 없으면 아무것도 열리지
// 않는다. 여기서 기본값만 깔아 두고, 다른 에디터를 쓰는 사람이 셸에서
// REACT_EDITOR 를 잡아 두면 그 값이 그대로 쓰인다.
if (!process.env.REACT_EDITOR) {
  process.env.REACT_EDITOR = 'code';
}

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {};

// inlineRem: 16 → 웹(Figma Make)과 같은 1rem = 16px 기준으로 크기를 맞춰요.
module.exports = withNativeWind(mergeConfig(getDefaultConfig(__dirname), config), {
  input: './global.css',
  inlineRem: 16,
});
