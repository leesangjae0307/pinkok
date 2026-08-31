/**
 * 픽셀 폰트: Galmuri (SIL OFL 1.1 라이선스, 상업적 이용 무료)
 * https://github.com/quiple/galmuri
 *
 * 픽셀 폰트는 작은 크기에서 가독성이 떨어지기 때문에
 * - PIXEL 계열: 로고, 타이틀, 버튼, 배지처럼 "짧고 굵게 보여줄 텍스트"에만 사용
 * - BODY(시스템 기본 폰트): 다이어리 메모, 안내문 등 "길게 읽는 텍스트"에 사용
 * 두 가지를 구분해서 씁니다.
 *
 * 주의: 네이티브 폰트 링킹 전에는 fontFamily가 적용되지 않고
 * OS 기본 폰트로 표시됩니다 (아래 README_FONT.md 참고).
 */
export const FONT_FAMILY = {
  PIXEL: 'Galmuri11-Regular',
  PIXEL_BOLD: 'Galmuri11-Bold',
  PIXEL_SMALL: 'Galmuri7-Regular', // 배지·캡션처럼 아주 작은 텍스트용
  BODY: undefined, // 시스템 기본 폰트 (플랫폼별 San Francisco / Roboto)
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
