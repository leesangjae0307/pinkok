/**
 * PinKok 픽셀(8-bit) 테마 컬러 — v2 팔레트
 * 메인: 짙은 블루 민트, 보조: 딥 블루 / 옐로우 / 코랄
 */
export const COLORS = {
  // Brand
  PRIMARY: '#1C5B9B', // 메인 버튼, 활성 탭
  PRIMARY_DARK: '#123B64', // 버튼 눌림 효과·픽셀 그림자용 (PRIMARY를 어둡게)
  PRIMARY_LIGHT: '#7FD9C3', // 카드 배경, 하이라이트 칩

  SECONDARY: '#2006A6', // 강조 텍스트, 아이콘 라인
  SECONDARY_DARK: '#14036B', // SECONDARY를 어둡게 (그림자용)

  ACCENT_YELLOW: '#FFC97D', // 별점, 배지, 노란 포인트
  ACCENT_YELLOW_DARK: '#A58251',
  ACCENT_PINK: '#E56F73', // 하트/핀 강조 (point-coral과 동일 색)
  ACCENT_PINK_DARK: '#94484A',

  // Base
  INK: '#20202D', // 도트 테두리, 메인 텍스트
  WHITE: '#FFFFFF',
  BLACK: '#000000',

  BACKGROUND: '#74F2E6', // 화면 기본 배경 (스카이/민트)
  SURFACE: '#FFFFFF', // 카드/입력창 내부 흰색

  GRAY100: '#B6DD6F', // 비활성 입력창, 보조 배경 (mint-soft)
  GRAY300: '#DCE6E4',
  GRAY500: '#68788C', // 서브 텍스트, 설명글

  ERROR: '#E56F73', // 삭제 버튼 (point-coral과 동일 색)
  ERROR_DARK: '#94484A',
};
