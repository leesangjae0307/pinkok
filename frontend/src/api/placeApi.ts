import api from './axios';

/**
 * 카카오맵 키워드 검색 결과 한 건.
 *
 * 서버의 PlaceInput 과 필드가 같아서, 사용자가 고른 값을 그대로
 * POST /itinerary-items 의 place 에 넣으면 된다.
 */
export interface PlaceSearchResult {
  /**
   * PLACE: 카카오맵에 등록된 장소. ADDRESS: 주소로만 찾은 위치(kakaoPlaceId 와 카테고리가 비어 있고,
   * 이름은 임시라서 사용자가 입력한 이름으로 바꿔 추가한다). 옛 서버는 값을 주지 않는다.
   */
  type?: 'PLACE' | 'ADDRESS';
  kakaoPlaceId: string | null;
  name: string;
  roadAddress: string | null;
  lotAddress: string | null;
  latitude: number;
  longitude: number;
  categoryGroupCode: string | null;
  categoryName: string | null;
  phone: string | null;
  placeUrl: string | null;
}

export interface PlaceSearchPage {
  places: PlaceSearchResult[];
  /** 다음 페이지(page+1)가 더 있는지 */
  hasMore: boolean;
}

/**
 * 로그인한 사용자면 누구나 쓸 수 있다. Gemini 가 아니라 카카오맵을 탄다.
 *
 * 첫 페이지는 키워드 결과 뒤에 주소 결과(type: 'ADDRESS')가 붙어서 온다. 겹치는
 * 주소는 서버가 걸러 주고, 2페이지부터는 키워드 결과만 온다. 그러니 앱에서 다시
 * 합치거나 거를 것은 없다.
 */
export async function searchPlaces(keyword: string, page?: number, size?: number): Promise<PlaceSearchPage> {
  const { data } = await api.get<PlaceSearchPage>('/places/search', {
    params: { keyword, page, size },
  });
  return data;
}

/** 장소 종류. code 는 장소를 추가할 때 categoryGroupCode 로 보낸다. */
export interface PlaceCategory {
  code: string;
  label: string;
  tasteLabel?: string;
  order: number;
}

/** GET /places/categories. 장소 종류 9개(맛집·카페 … 기타)를 순서대로 준다. */
export async function fetchPlaceCategories(): Promise<PlaceCategory[]> {
  const { data } = await api.get<PlaceCategory[]>('/places/categories');
  return [...data].sort((a, b) => a.order - b.order);
}

/** 목록에 보여줄 주소. 도로명이 없으면 지번으로 대신한다. */
export function placeAddress(place: PlaceSearchResult): string {
  return place.roadAddress ?? place.lotAddress ?? '';
}
