/**
 * 프로필 화면이 보여주는 "내 여행 기록" 집계.
 *
 * 전에는 내가 속한 여행을 하나씩 돌며 일정·기록·PinLog 를 따로 받아 앱에서 셌다.
 * 여행 수만큼 요청이 늘어서 느렸는데, 서버가 GET /users/me/stats 로 한 번에
 * 주게 되어 그대로 받아 쓴다. 세는 기준은 그때와 같다 — 방문은 내가 기록을 남긴
 * 핀, 저장은 일정에 담긴 핀이다. 정렬도 서버가 해서 보낸다.
 */
import { fetchMyPinlogs, pinlogThumbnailUrl, pinlogVideoUrl } from '../../api/pinlogApi';
import { fetchMyStats, type StatPlace } from '../../api/userApi';

/** 화면에 한 줄로 보여줄 장소. 방문·저장이 같은 모양이다. */
export interface ProfilePlace {
  /** 한 핀이 여러 여행에 있을 수 있어 여행까지 묶어 키로 쓴다. */
  key: string;
  name: string;
  tripTitle: string;
  /** 방문이면 기록한 날짜, 저장이면 주소. 없으면 빈 문자열. */
  detail: string;
}

export interface ProfilePinlog {
  id: number;
  tripId: number;
  title: string;
  thumbnail: string;
  videoUrl: string;
  meta: string;
}

export interface TasteSlice {
  label: string;
  icon: string;
  percent: number;
  color: string;
}

export interface ProfileStats {
  visitedCount: number;
  savedCount: number;
  regionCount: number;
  visited: ProfilePlace[];
  saved: ProfilePlace[];
  regions: { name: string; tripCount: number }[];
  tastes: TasteSlice[];
  pinlogs: ProfilePinlog[];
}

export const EMPTY_PROFILE_STATS: ProfileStats = {
  visitedCount: 0,
  savedCount: 0,
  regionCount: 0,
  visited: [],
  saved: [],
  regions: [],
  tastes: [],
  pinlogs: [],
};

/** 취향 코드마다 쓸 그림과 색. 이름은 서버가 보내준 것을 그대로 쓴다. */
const TASTE_LOOK: Record<string, { icon: string; color: string }> = {
  FOOD: { icon: '🍜', color: 'bg-[#56EADD]' },
  CAFE: { icon: '☕', color: 'bg-[#82F0DC]' },
  NATURE: { icon: '🌲', color: 'bg-emerald-300' },
  SIGHT: { icon: '🎎', color: 'bg-violet-300' },
  SHOPPING: { icon: '🛍️', color: 'bg-rose-300' },
};

const DEFAULT_LOOK = { icon: '📍', color: 'bg-slate-300' };

/** '2026-11-11T10:00:00' → '2026.11.11'. 값이 없으면 빈 문자열. */
function dateLabel(value: string | null): string {
  if (!value) return '';
  return value.slice(0, 10).replace(/-/g, '.');
}

function toProfilePlace(item: StatPlace, kind: 'visited' | 'saved'): ProfilePlace {
  return {
    key: `${item.tripId}-${item.itineraryItemId}`,
    name: item.placeName,
    tripTitle: item.tripTitle,
    detail:
      kind === 'visited'
        ? dateLabel(item.recordedAt)
        : item.roadAddress ?? item.lotAddress ?? item.categoryName ?? '',
  };
}

/**
 * 프로필 집계를 받아온다.
 *
 * 통계와 PinLog 목록은 서로 기다릴 이유가 없어 함께 보낸다. PinLog 쪽이 실패해도
 * 숫자는 보여줄 수 있게 따로 삼킨다.
 */
export async function loadProfileStats(): Promise<ProfileStats> {
  const [stats, pinlogs] = await Promise.all([fetchMyStats(), fetchMyPinlogs().catch(() => [])]);

  return {
    visitedCount: stats.visitedCount,
    savedCount: stats.savedCount,
    regionCount: stats.regionCount,
    visited: stats.visited.map((v) => toProfilePlace(v, 'visited')),
    saved: stats.saved.map((v) => toProfilePlace(v, 'saved')),
    regions: stats.regions,
    tastes: stats.tastes.map((t) => ({
      label: t.name,
      percent: t.percent,
      ...(TASTE_LOOK[t.code] ?? DEFAULT_LOOK),
    })),
    // 서버가 완성본만 최신순으로 주므로 앱에서 따로 거르지 않는다.
    pinlogs: pinlogs.map((p) => ({
      id: p.id,
      tripId: p.tripId,
      title: p.title ?? 'PinLog',
      thumbnail: pinlogThumbnailUrl(p) ?? '',
      videoUrl: pinlogVideoUrl(p) ?? '',
      meta: `${dateLabel(p.createdAt)} • ${
        p.durationMs ? `${(p.durationMs / 1000).toFixed(1)}초` : '—'
      }`,
    })),
  };
}
