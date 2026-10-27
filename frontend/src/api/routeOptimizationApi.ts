import api from './axios';

export interface RouteOptimizationDay {
  dayNumber: number | null;
  dayId: number | null;
  /** 바뀐 뒤의 방문 순서대로 */
  items: {
    itineraryItemId: number;
    visitOrder: number;
    placeName: string;
    /** 다음 장소까지 */
    distanceM: number | null;
    durationMin: number | null;
    transportMode: string | null;
  }[];
}

export interface RouteOptimization {
  id: number;
  tripId: number;
  /** 날짜를 다시 나눴는지 */
  redistribute: boolean;
  totalDistanceM: number;
  totalDurationMin: number;
  /** 최적화 전 거리. 얼마나 줄었는지 보여줄 때 쓴다. */
  beforeDistanceM: number;
  applied: boolean;
  createdAt: string;
  days: RouteOptimizationDay[];
  /** 좌표가 없어 건드리지 못한 핀 */
  untouchedItemIds: number[];
}

/**
 * 제안을 만든다. 계산이라 바로 결과가 온다 — 이 단계에서 일정은 아직 바뀌지 않는다.
 * 실제로 반영하려면 applyRouteOptimization 을 불러야 한다.
 */
export async function createRouteOptimization(
  tripId: number,
  redistribute = true,
): Promise<RouteOptimization> {
  const { data } = await api.post<RouteOptimization>('/route-optimizations', { tripId, redistribute });
  return data;
}

export async function fetchRouteOptimizations(tripId: number): Promise<RouteOptimization[]> {
  const { data } = await api.get<RouteOptimization[]>('/route-optimizations', { params: { tripId } });
  return data;
}

/** 제안을 일정에 반영한다. 핀의 날짜·순서·이동수단이 바뀐다. */
export async function applyRouteOptimization(id: number): Promise<RouteOptimization> {
  const { data } = await api.post<RouteOptimization>(`/route-optimizations/${id}/apply`);
  return data;
}

/** '2.4km 단축' 처럼 보여줄 값. 늘어났으면 음수가 나온다. */
export function savedDistanceM(result: RouteOptimization): number {
  return result.beforeDistanceM - result.totalDistanceM;
}
