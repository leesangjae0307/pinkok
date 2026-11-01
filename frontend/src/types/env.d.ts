/**
 * babel 의 react-native-dotenv 가 .env 를 '@env' 모듈로 바꿔준다.
 * 타입스크립트는 그 모듈을 모르니 여기서 알려준다.
 *
 * .env 가 없거나 값이 비면 undefined 로 들어오므로 쓰는 쪽에서 기본값을 둔다.
 */
declare module '@env' {
  export const API_BASE_URL: string | undefined;
  export const KAKAO_JS_KEY: string | undefined;
  export const KAKAO_SDK_ORIGIN: string | undefined;
}
