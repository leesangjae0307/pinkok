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

/** 핀 삭제. */
export async function deleteItineraryItem(itemId: number): Promise<void> {
  await api.delete(`/itinerary-items/${itemId}`);
}

export async function fetchItineraryItems(tripId: number): Promise<ItineraryItem[]> {
  const { data } = await api.get<ItineraryItem[]>('/itinerary-items', { params: { tripId } });
  return data;
}

/** 썸네일이 없는 핀에 쓰는 기본 이미지 */
const FALLBACK_THUMBNAIL =
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=400&q=80';

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
 * 지도는 실제 지도가 아니라 일러스트 캔버스이고, PlaceItem 의 lat/lng 는
 * 그 캔버스의 백분율(0~100) 이다. 그래서 서버의 진짜 위경도를 그대로 쓸 수 없다.
 *
 * 핀 전체를 감싸는 사각형을 구해 10~90% 안으로 펴 넣는다. 서로의 위치 관계는
 * 유지되고 가장자리에 붙어 잘리지도 않는다. 핀이 하나뿐이거나 모두 같은 지점이면
 * 가운데(50%)에 둔다. 위도는 북쪽이 위라서 y 를 뒤집는다.
 */
function projectToCanvas(items: ItineraryItem[]): Map<number, { lat: number; lng: number }> {
  const MIN = 10;
  const SPAN = 80;
  const lats = items.map((i) => i.place.latitude);
  const lngs = items.map((i) => i.place.longitude);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);
  const latRange = maxLat - minLat;
  const lngRange = maxLng - minLng;

  const projected = new Map<number, { lat: number; lng: number }>();
  for (const item of items) {
    const x = lngRange === 0 ? 50 : MIN + ((item.place.longitude - minLng) / lngRange) * SPAN;
    // 위도가 클수록(북쪽) 화면 위쪽이어야 하므로 뒤집는다.
    const y = latRange === 0 ? 50 : MIN + ((maxLat - item.place.latitude) / latRange) * SPAN;
    projected.set(item.id, { lat: y, lng: x });
  }
  return projected;
}

/**
 * 서버 핀을 지도·바텀시트가 쓰는 PlaceItem 으로 바꾼다.
 *
 * 이동 수단은 서버가 종류만 주고 소요 시간·거리는 주지 않아서 라벨만 채운다.
 * 날짜를 안 정한 핀(dayNumber 가 null)은 0 으로 두어 day 필터에 걸리지 않게 한다.
 */
export function toPlaceItems(items: ItineraryItem[]): PlaceItem[] {
  const projected = projectToCanvas(items);
  return items.map((item, index) => {
    const transit = item.transportMode ? TRANSIT_BY_MODE[item.transportMode] : undefined;
    const point = projected.get(item.id) ?? { lat: 50, lng: 50 };
    return {
      id: item.id,
      name: item.place.name,
      category: item.place.categoryName ?? '장소',
      rating: item.rating ?? 0,
      reviewsCount: item.reviewsCount,
      address: item.place.roadAddress ?? item.place.lotAddress ?? '',
      thumbnail: fileUrl(item.thumbnailUrl) ?? FALLBACK_THUMBNAIL,
      lat: point.lat,
      lng: point.lng,
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
