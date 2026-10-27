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
}

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
