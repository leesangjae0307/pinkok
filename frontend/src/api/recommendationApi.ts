import api from './axios';
import { AiRequest } from './aiRequestApi';

/** SUGGESTED → ACCEPTED / REJECTED */
export interface Recommendation {
  id: number;
  tripId: number;
  name: string;
  address: string | null;
  category: string | null;
  /** 왜 추천했는지 */
  reason: string | null;
  status: string;
  /** 수락해서 좌표를 찾은 뒤에만 온다. */
  placeId: number | null;
  /** 수락했을 때 만들어진 일정 핀 id (수락 응답에서만) */
  itineraryItemId: number | null;
  createdAt: string;
}

export interface RecommendationCreatePayload {
  tripId: number;
  /** SOLO / COUPLE / FAMILY / FRIEND. 비우면 여행 → 내 기본 설정 순으로 찾는다. */
  companionType?: string;
  /** GET /travel-styles 의 code. 비우면 여행 → 내 취향 순으로 찾는다. */
  styleCodes?: string[];
  /** 1~10, 비우면 5 */
  count?: number;
  /**
   * 사용자가 직접 적은 여행 지역("부산 해운대", "전주 한옥마을").
   *
   * 정식 필드다. 보내면 그 지역이 우선이고, 비워서 보내지 않으면 서버가 여행에
   * 저장된 지역을 쓴다. 그래서 빈 문자열 대신 아예 빼고 보낸다.
   */
  region?: string;
}

/**
 * 서버가 받는 동행 유형. 화면의 선택지와 1:1 로 맞춘다.
 *
 * 전에는 화면에서 사람 수(1~4명+)를 고르게 하고 1명→SOLO, 2명→COUPLE,
 * 그 이상→FRIEND 로 옮겼다. 서버에는 사람 수를 받는 자리가 없는데다
 * 그렇게 옮기면 FAMILY 를 영영 고를 수 없어서, 유형을 그대로 고르게 한다.
 */
export const COMPANION_TYPES = [
  { value: 'SOLO', label: '혼자' },
  { value: 'COUPLE', label: '커플' },
  { value: 'FRIEND', label: '친구' },
  { value: 'FAMILY', label: '가족' },
] as const;

/**
 * 추천 요청. 바로 202 로 끝나고 실제 추천은 뒤에서 만들어진다.
 * 응답의 id 로 AI 요청 상태를 폴링하고, SUCCESS 가 되면 목록을 읽는다.
 */
export async function createRecommendation(payload: RecommendationCreatePayload): Promise<AiRequest> {
  const { data } = await api.post<AiRequest>('/recommendations', payload);
  return data;
}

/** 최신순. 수락·거절한 것도 함께 온다. */
export async function fetchRecommendations(tripId: number): Promise<Recommendation[]> {
  const { data } = await api.get<Recommendation[]>('/recommendations', { params: { tripId } });
  return data;
}

/** 지도에서 좌표를 찾아 일정에 핀으로 추가한다. 못 찾으면 422 다. */
export async function acceptRecommendation(id: number, dayId?: number): Promise<Recommendation> {
  const { data } = await api.post<Recommendation>(`/recommendations/${id}/accept`, dayId ? { dayId } : {});
  return data;
}

export async function rejectRecommendation(id: number): Promise<Recommendation> {
  const { data } = await api.post<Recommendation>(`/recommendations/${id}/reject`);
  return data;
}
