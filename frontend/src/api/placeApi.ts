import api from './axios';

/**
 * 카카오맵 키워드 검색 결과 한 건.
 *
 * 서버의 PlaceInput 과 필드가 같아서, 사용자가 고른 값을 그대로
 * POST /itinerary-items 의 place 에 넣으면 된다.
 */
export interface PlaceSearchResult {
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

/** 로그인한 사용자면 누구나 쓸 수 있다. Gemini 가 아니라 카카오맵을 탄다. */
export async function searchPlaces(keyword: string, page?: number, size?: number): Promise<PlaceSearchPage> {
  const { data } = await api.get<PlaceSearchPage>('/places/search', {
    params: { keyword, page, size },
  });
  return data;
}

/** 목록에 보여줄 주소. 도로명이 없으면 지번으로 대신한다. */
export function placeAddress(place: PlaceSearchResult): string {
  return place.roadAddress ?? place.lotAddress ?? '';
}
