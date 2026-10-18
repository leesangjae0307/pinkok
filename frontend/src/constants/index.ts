/**
 * 서버 travel_styles 코드. `GET /travel-styles` 응답과 같은 5개다.
 * 목록에 없는 코드를 보내면 서버가 400 을 주고 아무것도 저장하지 않으므로,
 * 스타일 선택지는 반드시 이 목록(또는 `GET /travel-styles` 결과)으로만 만든다.
 */
export const TRAVEL_STYLES = [
  { code: 'FOOD', label: '맛집' },
  { code: 'SIGHT', label: '관광' },
  { code: 'NATURE', label: '자연' },
  { code: 'SHOPPING', label: '쇼핑' },
  { code: 'CAFE', label: '카페' },
] as const;

export type TravelStyleCode = (typeof TRAVEL_STYLES)[number]['code'];

export const travelStyleLabel = (code: string): string =>
  TRAVEL_STYLES.find((style) => style.code === code)?.label ?? code;
