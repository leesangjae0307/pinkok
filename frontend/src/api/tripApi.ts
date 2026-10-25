import api from './axios';
import { fileUrl } from './fileApi';
import { DEFAULT_COVERS } from '../assets/images/covers';
import { daysBetween, toYmd } from '../components/pixel/PixelCalendar';
import { AvatarSource, TripFolder } from '../data/mockData';

/** 서버가 내려주는 여행. 날짜는 'YYYY-MM-DD', createdAt 은 ISO 문자열이다. */
export interface Trip {
  id: number;
  title: string;
  region: string | null;
  startDate: string | null;
  endDate: string | null;
  /** SOLO / COUPLE / FAMILY / FRIEND */
  companionType: string | null;
  /** PLANNING / ONGOING / COMPLETED */
  status: string;
  coverImageUrl: string | null;
  isPublic: boolean;
  /** OWNER / MEMBER. 수정·삭제 버튼 노출 여부에 쓴다. */
  myRole: string;
  placesCount: number;
  createdAt: string;
}

export interface TripCreatePayload {
  title: string;
  region?: string;
  startDate?: string;
  endDate?: string;
  companionType?: string;
  isPublic?: boolean;
  /**
   * 함께 갈 팀원의 @아이디 목록. 초대장이 갈 뿐 바로 팀원이 되지는 않으므로
   * 만든 직후의 여행은 아직 나 혼자다.
   */
  inviteeUsernames?: string[];
}

/** 내가 팀원으로 속한 여행 목록 */
export async function fetchTrips(): Promise<Trip[]> {
  const { data } = await api.get<Trip[]>('/trips');
  return data;
}

/** 여행 상세. 팀원이 아니면 403 이다. */
export async function fetchTrip(tripId: number): Promise<Trip> {
  const { data } = await api.get<Trip>(`/trips/${tripId}`);
  return data;
}

/** 만든 사람은 자동으로 OWNER 가 된다. */
export async function createTrip(payload: TripCreatePayload): Promise<Trip> {
  const { data } = await api.post<Trip>('/trips', payload);
  return data;
}

export interface TripUpdatePayload {
  title?: string;
  region?: string;
  startDate?: string;
  endDate?: string;
  companionType?: string;
  /** PLANNING / ONGOING / COMPLETED */
  status?: string;
  coverImageUrl?: string;
  isPublic?: boolean;
}

/** 부분 수정. 보내지 않은 필드는 그대로 둔다. OWNER 만 가능하다. */
export async function updateTrip(tripId: number, payload: TripUpdatePayload): Promise<Trip> {
  const { data } = await api.patch<Trip>(`/trips/${tripId}`, payload);
  return data;
}

/** 소프트 삭제. OWNER 가 아니면 403 이다. */
export async function deleteTrip(tripId: number): Promise<void> {
  await api.delete(`/trips/${tripId}`);
}

const COMPANION_TAGS: Record<string, string> = {
  SOLO: '혼자',
  COUPLE: '커플',
  FAMILY: '가족',
  FRIEND: '우정여행',
};

/** 목업과 같은 '2026.10.12 - 10.15'. 끝 날짜는 연도를 빼고 월·일만 붙인다. */
function dateLabel(startDate: string | null, endDate: string | null): string {
  if (!startDate) return '날짜 미정';
  const start = startDate.replace(/-/g, '.');
  if (!endDate) return start;
  return `${start} - ${endDate.slice(5).replace('-', '.')}`;
}

function dDayLabel(startDate: string | null): string {
  if (!startDate) return 'D-?';
  const days = daysBetween(toYmd(new Date()), startDate);
  if (days > 0) return `D-${days}`;
  return days === 0 ? 'D-DAY' : `D+${-days}`;
}

/**
 * 서버 응답을 화면이 쓰는 TripFolder 로 바꾼다.
 *
 * 표지와 태그는 아직 POST /trips 에 실을 자리가 없다. 그래서 방금 만든 여행처럼
 * 사용자가 고른 값을 아는 경우에는 `local` 로 넘겨 그 값을 유지하고,
 * 목록을 다시 받아오면 기본 표지와 동행 유형으로 돌아간다.
 */
export function toTripFolder(
  trip: Trip,
  local?: { image?: AvatarSource; tag?: string },
): TripFolder {
  return {
    id: trip.id,
    title: trip.title,
    date: dateLabel(trip.startDate, trip.endDate),
    dDay: dDayLabel(trip.startDate),
    // 서버 주소는 `/files/...` 상대 경로라 그대로 주면 Image 가 못 불러온다.
    image: fileUrl(trip.coverImageUrl) ?? local?.image ?? DEFAULT_COVERS[0].src,
    placesCount: trip.placesCount,
    tag: local?.tag ?? COMPANION_TAGS[trip.companionType ?? ''] ?? '여행',
    isOwner: trip.myRole === 'OWNER',
  };
}
