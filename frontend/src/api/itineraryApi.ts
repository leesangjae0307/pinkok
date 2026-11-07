import api from './axios';
import { fileUrl } from './fileApi';
import { PlaceItem, TransitInfo } from '../data/mockData';

/** 서버가 내려주는 장소. 좌표는 실제 위경도(WGS84) 다. */
export interface Place {
  id: number;
  name: string;
  roadAddress: string | null;
  lotAddress: string | null;
  latitude: number;
  longitude: number;
  categoryName: string | null;
  phone: string | null;
  placeUrl: string | null;
}

export interface ItineraryDay {
  id: number;
  tripId: number;
  dayNumber: number;
  visitDate: string | null;
}

/** 지도에 꽂힌 핀 하나. dayId 가 null 이면 아직 날짜를 안 정한 핀이다. */
export interface ItineraryItem {
  id: number;
  tripId: number;
  dayId: number | null;
  dayNumber: number | null;
  place: Place;
  visitOrder: number | null;
  /** 'HH:mm:ss' */
  plannedArrivalTime: string | null;
  stayMinutes: number | null;
  /** CAR / WALK / BUS / TRAIN */
  transportMode: string | null;
  memo: string | null;
  addedBy: string | null;
  /** 팀원 기록 별점 평균. 기록이 없으면 null */
  rating: number | null;
  reviewsCount: number;
  thumbnailUrl: string | null;
  createdAt: string;
}

/**
 * 일자를 만든다. visitDate 는 선택이라 비워도 되고, 그러면 날짜 없이 N일차만 생긴다.
 *
 * 동선 최적화는 일자가 하나도 없으면 400 으로 거절한다. 핀을 날짜별로 나누려면
 * 먼저 여기서 일자를 만들어야 한다.
 */
export async function createItineraryDay(
  tripId: number,
  dayNumber: number,
  visitDate?: string,
): Promise<ItineraryDay> {
  const { data } = await api.post<ItineraryDay>('/itinerary-days', { tripId, dayNumber, visitDate });
  return data;
}

/** 일자를 지워도 그 핀들은 "날짜 미배정" 으로 남고 삭제되지 않는다. */
export async function deleteItineraryDay(dayId: number): Promise<void> {
  await api.delete(`/itinerary-days/${dayId}`);
}

export async function fetchItineraryDays(tripId: number): Promise<ItineraryDay[]> {
  const { data } = await api.get<ItineraryDay[]>('/itinerary-days', { params: { tripId } });
  return data;
}

/** 핀을 추가할 때 함께 보내는 장소. GET /places/search 결과를 그대로 넣으면 된다. */
export interface PlaceInput {
  kakaoPlaceId?: string | null;
  name: string;
  roadAddress?: string | null;
  lotAddress?: string | null;
  latitude: number;
  longitude: number;
  categoryGroupCode?: string | null;
  categoryName?: string | null;
  phone?: string | null;
  placeUrl?: string | null;
}

export interface ItineraryItemCreatePayload {
  tripId: number;
  /** 아직 날짜를 안 정했으면 비운다("미배정 핀"). */
  dayId?: number;
  place: PlaceInput;
  /** 'HH:mm' */
  plannedArrivalTime?: string;
  stayMinutes?: number;
  /** CAR / WALK / BUS / TRAIN */
  transportMode?: string;
  memo?: string;
}

/**
 * 지도에 핀을 추가한다.
 *
 * kakaoPlaceId 가 이미 저장된 장소와 같으면 서버가 그 장소를 재사용하고,
 * 아니면 보낸 정보로 새 장소를 만든다.
 */
export async function createItineraryItem(payload: ItineraryItemCreatePayload): Promise<ItineraryItem> {
  const { data } = await api.post<ItineraryItem>('/itinerary-items', payload);
  return data;
}

/**
 * 핀을 다른 일자로 옮긴다. 옮기면 그 일자의 맨 뒤 순서가 된다.
 *
 * 주의: dayId 를 비우면 서버는 "바꾸지 말라"로 읽는다. 그래서 이 API 로는 핀을
 * 미배정으로 되돌릴 수 없다(그 일자를 통째로 지우는 수밖에 없다).
 */
export async function moveItineraryItemToDay(itemId: number, dayId: number): Promise<ItineraryItem> {
  const { data } = await api.patch<ItineraryItem>(`/itinerary-items/${itemId}`, { dayId });
  return data;
}

/**
 * 핀의 여행할 시각을 정한다. time 은 'HH:mm'.
 *
 * 주의: 비워서 보내면 서버가 "바꾸지 말라"로 읽는다. 이 API 로는 한 번 넣은 시각을
 * 지울 수 없다.
 */
export async function updateItineraryItemTime(itemId: number, time: string): Promise<ItineraryItem> {
  const { data } = await api.patch<ItineraryItem>(`/itinerary-items/${itemId}`, {
    plannedArrivalTime: time,
  });
  return data;
}

/** 한 일자 안의 핀 순서를 통째로 다시 정한다. itemIds 순서가 곧 방문 순서다. */
export async function reorderItineraryItems(dayId: number, itemIds: number[]): Promise<ItineraryItem[]> {
  const { data } = await api.patch<ItineraryItem[]>('/itinerary-items/order', { dayId, itemIds });
  return data;
}

/** 핀 삭제. */
export async function deleteItineraryItem(itemId: number): Promise<void> {
  await api.delete(`/itinerary-items/${itemId}`);
}

export async function fetchItineraryItems(tripId: number): Promise<ItineraryItem[]> {
  const { data } = await api.get<ItineraryItem[]>('/itinerary-items', { params: { tripId } });
  return data;
}

const TRANSIT_BY_MODE: Record<string, { type: TransitInfo['type']; label: string }> = {
  CAR: { type: 'car', label: '차량 이동' },
  WALK: { type: 'walk', label: '도보 이동' },
  BUS: { type: 'bus', label: '버스 이동' },
  TRAIN: { type: 'bus', label: '기차 이동' },
};

/** 'HH:mm:ss' → 'HH:mm'. 서버가 초를 빼고 줄 때도 있어 그대로 흘려보낸다. */
function timeLabel(value: string | null): string | undefined {
  if (!value) return undefined;
  return value.slice(0, 5);
}


/**
 * 서버 핀을 지도·바텀시트가 쓰는 PlaceItem 으로 바꾼다.
 *
 * lat/lng 는 실제 위경도(WGS84)를 그대로 넘긴다. 예전 일러스트 지도는 캔버스
 * 백분율을 썼고 그래서 투영이 필요했지만, 카카오맵으로 바뀌면서 그 변환이 없어졌다.
 *
 * 이동 수단은 서버가 종류만 주고 소요 시간·거리는 주지 않아서 라벨만 채운다.
 * 날짜를 안 정한 핀(dayNumber 가 null)은 0 으로 두어 day 필터에 걸리지 않게 한다.
 */
export function toPlaceItems(items: ItineraryItem[]): PlaceItem[] {
  return items.map((item, index) => {
    const transit = item.transportMode ? TRANSIT_BY_MODE[item.transportMode] : undefined;
    return {
      id: item.id,
      name: item.place.name,
      category: item.place.categoryName ?? '장소',
      rating: item.rating ?? 0,
      reviewsCount: item.reviewsCount,
      address: item.place.roadAddress ?? item.place.lotAddress ?? '',
      // 서버가 핀 사진을 주지 않으면 비워 둔다. 전에는 아무 샘플 사진을 넣어서
      // 장소와 상관없는 그림이 정보처럼 보였다.
      thumbnail: fileUrl(item.thumbnailUrl) ?? '',
      lat: item.place.latitude,
      lng: item.place.longitude,
      // 날짜를 안 정한 핀은 visitOrder 가 없다. 화면에는 순번이 꼭 필요하므로
      // 받은 순서대로 1 부터 매긴다. 0 으로 두면 지도와 목록에 "0." 이 찍힌다.
      order: item.visitOrder ?? index + 1,
      day: item.dayNumber ?? 0,
      time: timeLabel(item.plannedArrivalTime),
      transitToNext: transit && { ...transit, duration: '' },
      note: item.memo ?? undefined,
    };
  });
}
