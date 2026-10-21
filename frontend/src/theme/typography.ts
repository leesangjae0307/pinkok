/**
 * 화면은 NativeWind 클래스(`font-bold`, `font-pixel` …)로 폰트를 지정하고,
 * 여기 값들은 className 을 못 쓰는 자리(StyleSheet, 네이티브 옵션 등)를 위한 것이다.
 * 실제 파일과 클래스 대응표는 FONT_SETUP.md 에 있다.
 *
 * 두 폰트 모두 한글 글자가 없어서 한글은 OS 기본 폰트로 대체된다(웹 디자인과 동일).
 * 네이티브 폰트 링킹 전에는 fontFamily 가 무시되고 OS 기본 폰트로 보인다.
 */
export const FONT_FAMILY = {
  /** 본문·버튼 (웹의 기본 굵기) */
  BODY: 'PlusJakartaSans-Medium',
  BODY_SEMIBOLD: 'PlusJakartaSans-SemiBold',
  BODY_BOLD: 'PlusJakartaSans-Bold',
  /** 로고·강조 제목 (웹의 font-black 도 이 파일로 보인다) */
  BODY_EXTRABOLD: 'PlusJakartaSans-ExtraBold',
  /** 픽셀 배지·라벨 (웹의 font-pixel) */
  PIXEL: 'PressStart2P-Regular',
};

export const TYPOGRAPHY = {
  logo: 30,
  h1: 22,
  h2: 18,
  h3: 15,

  body: 14,
  caption: 12,
  tiny: 10,
};

export const LINE_HEIGHT = {
  logo: 38,
  h1: 30,
  h2: 26,
  h3: 22,
  body: 20,
  caption: 18,
  tiny: 14,
};
