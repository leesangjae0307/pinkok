import api from './axios';

/** 서버가 정한 여행 취향. 지금은 FOOD / SIGHT / NATURE / SHOPPING / CAFE 다섯 개다. */
export interface TravelStyle {
  id: number;
  code: string;
  name: string;
}

/** 로그인 없이도 부를 수 있다. 선택지를 서버 기준으로 맞출 때 쓴다. */
export async function fetchTravelStyles(): Promise<TravelStyle[]> {
  const { data } = await api.get<TravelStyle[]>('/travel-styles');
  return data;
}

export async function fetchTripStyles(tripId: number): Promise<TravelStyle[]> {
  const { data } = await api.get<TravelStyle[]>('/trip-travel-styles', { params: { tripId } });
  return data;
}

/**
 * 이 여행의 취향을 통째로 바꾼다. 보낸 것만 남고 나머지는 해제되며, 빈 배열이면 전부 해제된다.
 * 목록에 없는 코드가 섞이면 서버가 400 을 주고 아무것도 저장하지 않는다.
 */
export async function replaceTripStyles(tripId: number, styleCodes: string[]): Promise<TravelStyle[]> {
  const { data } = await api.put<TravelStyle[]>('/trip-travel-styles', { tripId, styleCodes });
  return data;
}

export async function fetchMyStyles(): Promise<TravelStyle[]> {
  const { data } = await api.get<TravelStyle[]>('/user-travel-styles');
  return data;
}

export async function replaceMyStyles(styleCodes: string[]): Promise<TravelStyle[]> {
  const { data } = await api.put<TravelStyle[]>('/user-travel-styles', { styleCodes });
  return data;
}
