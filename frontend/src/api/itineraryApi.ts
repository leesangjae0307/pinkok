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

export async function fetchItineraryDays(tripId: number): Promise<ItineraryDay[]> {
  const { data } = await api.get<ItineraryDay[]>('/itinerary-days', { params: { tripId } });
  return data;
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
  return items.map((item) => {
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
      order: item.visitOrder ?? 0,
      day: item.dayNumber ?? 0,
      time: timeLabel(item.plannedArrivalTime),
      transitToNext: transit && { ...transit, duration: '' },
      note: item.memo ?? undefined,
    };
  });
}
