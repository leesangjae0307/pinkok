import api from './axios';
import { fileUrl } from './fileApi';
import { DEFAULT_COVERS, defaultCoverSource } from '../assets/images/covers';
import { daysBetween, toYmd } from '../components/pixel/PixelCalendar';
import { AvatarSource, TripFolder } from '../data/mockData';
import type { TripMember } from './invitationApi';

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
  /** 해시태그. 서버는 # 없이 주므로 보여줄 때 앱이 붙인다. */
  tags: string[];
  /**
   * 함께 가는 사람들. 목록·상세·생성·수정 응답에 모두 들어 있다.
   * 아직 내려주지 않는 서버를 대비해 선택 필드로 둔다.
   */
  members?: TripMember[];
  /** 나를 포함한 전체 팀원 수 */
  memberCount?: number;
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
  /** 최대 10개, 정리 후 각 20자. # 없이 보낸다(서버가 앞의 # 는 떼어 준다). */
  tags?: string[];
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
  /** 통째로 교체된다. 빼면 그대로 두고, [] 를 보내면 전부 지운다. */
  tags?: string[];
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
export function tripDateLabel(startDate: string | null, endDate: string | null): string {
  if (!startDate) {return '날짜 미정';}
  const start = startDate.replace(/-/g, '.');
  if (!endDate) {return start;}
  return `${start} - ${endDate.slice(5).replace('-', '.')}`;
}

export function tripDDayLabel(startDate: string | null): string {
  if (!startDate) {return 'D-?';}
  const days = daysBetween(toYmd(new Date()), startDate);
  if (days > 0) {return `D-${days}`;}
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
    date: tripDateLabel(trip.startDate, trip.endDate),
    dDay: tripDDayLabel(trip.startDate),
    // 기본 표지는 'default:tokyo' 처럼 적혀 오므로 앱 안의 그림으로 바꾼다.
    // 올린 사진이면 서버 주소가 `/files/...` 상대 경로라 BASE_URL 을 붙여야 한다.
    image:
      defaultCoverSource(trip.coverImageUrl) ??
      fileUrl(trip.coverImageUrl) ??
      local?.image ??
      DEFAULT_COVERS[0].src,
    placesCount: trip.placesCount,
    tag: local?.tag ?? COMPANION_TAGS[trip.companionType ?? ''] ?? '여행',
    tags: trip.tags ?? [],
    isOwner: trip.myRole === 'OWNER',
    members: trip.members ?? [],
    // 팀원을 주지 않는 서버에서도 최소한 나는 있다. 0 명으로 보이지 않게 한다.
    memberCount: trip.memberCount ?? trip.members?.length ?? 1,
  };
}
