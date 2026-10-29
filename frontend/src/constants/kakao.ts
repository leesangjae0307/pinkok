/**
 * 카카오맵 JavaScript SDK 설정.
 *
 * 이 키는 클라이언트 키라서 앱 번들과 WebView HTML 에 그대로 들어간다. 숨길 수 없는
 * 값이므로 카카오 콘솔에서 **도메인 제한**으로 보호한다. 서버가 쓰는 REST API 키
 * (장소 검색용)와는 다른 키이고, 그쪽은 절대 앱에 넣지 않는다.
 */
export const KAKAO_JS_KEY = '064efc6fc209186234937c463fc004ea';

/**
 * WebView 가 HTML 을 띄울 때 쓸 출처(origin).
 *
 * JS SDK 는 호출한 도메인으로 키를 검증한다. 로컬 HTML 은 출처가 없어서 인증이 막히므로,
 * 카카오 개발자 콘솔의 **JavaScript 키 → JavaScript SDK 도메인**에 등록한 주소를 그대로
 * 여기에 적어야 한다. 등록한 주소와 한 글자라도 다르면 지도가 뜨지 않는다.
 */
export const KAKAO_SDK_ORIGIN = 'https://localhost:8080';
