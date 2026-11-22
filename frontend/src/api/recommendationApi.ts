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
   * 아직 서버 명세에 없는 자리다. 스프링은 모르는 필드를 기본으로 버리므로
   * 보내도 탈이 없고, 서버가 받기 시작하면 그대로 쓰인다. 지금은 추천 결과가
   * 이 지역을 따르지 않는다.
   */
  region?: string;
}

/**
 * 화면의 인원 수를 서버의 동행 유형으로 옮긴다.
 *
 * 서버에는 사람 수를 받는 자리가 없고 동행 유형(SOLO/COUPLE/FAMILY/FRIEND)만
 * 있다. 그래서 1명은 혼자, 2명은 커플, 그보다 많으면 우정여행으로 본다.
 * 가족 여행은 이 화면에서 고를 수 없다.
 */
export function companionTypeOf(people: string): string {
  const n = Number(people);
  if (n <= 1) {return 'SOLO';}
  if (n === 2) {return 'COUPLE';}
  return 'FRIEND';
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
