/**
 * 프로필 화면이 보여주는 "내 여행 기록" 집계.
 *
 * 사용자 단위로 합쳐주는 API 가 없어서(/users/me 는 닉네임·아바타만 준다) 내가
 * 속한 여행을 하나씩 돌며 모은다. 여행 수만큼 요청이 늘어나므로 프로필 탭을 열 때
 * 한 번만 부르고 결과를 들고 있는다.
 *
 * 집계 기준은 다음과 같다.
 * - 저장한 장소: 내가 속한 여행의 일정에 담긴 핀 전부
 * - 방문한 장소: 그중 내가 기록(다이어리)을 남긴 핀
 * - 다녀온 지역: 방문한 장소 주소의 첫 번째 행정구역(시·도)
 * - 여행 취향: 저장한 장소의 카테고리 분포
 * - 핀로그: 내가 만든 완성본(DONE)
 */
import { fetchTripDiaries } from '../../api/diaryApi';
import { fetchItineraryItems, type ItineraryItem } from '../../api/itineraryApi';
import { fetchPinlogs, pinlogThumbnailUrl, pinlogVideoUrl } from '../../api/pinlogApi';

export interface ProfilePlace {
  id: number;
  name: string;
  address: string;
  category: string;
  tripTitle: string;
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
  savedPlaces: ProfilePlace[];
  visitedPlaces: ProfilePlace[];
  regions: string[];
  tastes: TasteSlice[];
  pinlogs: ProfilePinlog[];
}

export const EMPTY_PROFILE_STATS: ProfileStats = {
  savedPlaces: [],
  visitedPlaces: [],
  regions: [],
  tastes: [],
  pinlogs: [],
};

/**
 * 카카오 카테고리 이름을 화면에 쓰는 취향 묶음으로 바꾼다.
 * 카테고리는 '음식점 > 한식 > 국밥' 처럼 '>' 로 이어진 문자열이라 포함 여부로 가른다.
 */
const TASTE_BUCKETS: { label: string; icon: string; color: string; match: (c: string) => boolean }[] = [
  { label: '맛집', icon: '🍜', color: 'bg-[#56EADD]', match: (c) => c.includes('음식점') },
  { label: '카페', icon: '☕', color: 'bg-[#82F0DC]', match: (c) => c.includes('카페') },
  {
    label: '자연',
    icon: '🌲',
    color: 'bg-emerald-300',
    match: (c) => c.includes('관광') || c.includes('공원') || c.includes('산') || c.includes('해수욕장'),
  },
  {
    label: '문화',
    icon: '🎨',
    color: 'bg-violet-300',
    match: (c) => c.includes('문화') || c.includes('박물관') || c.includes('미술'),
  },
  { label: '숙소', icon: '🏨', color: 'bg-amber-300', match: (c) => c.includes('숙박') },
  { label: '쇼핑', icon: '🛍️', color: 'bg-rose-300', match: (c) => c.includes('쇼핑') || c.includes('마트') },
];

const ETC_BUCKET = { label: '기타', icon: '📍', color: 'bg-slate-300' };

/** '부산 해운대구 …' → '부산'. 주소가 없으면 센지 않는다. */
function regionOf(address: string): string | null {
  const first = address.trim().split(/\s+/)[0];
  return first && first.length >= 2 ? first : null;
}

function toProfilePlace(item: ItineraryItem, tripTitle: string): ProfilePlace {
  return {
    id: item.id,
    name: item.place.name,
    address: item.place.roadAddress ?? item.place.lotAddress ?? '',
    category: item.place.categoryName ?? '',
    tripTitle,
  };
}

/** 저장한 장소의 카테고리 분포를 백분율로 바꾼다. 합이 100 이 되게 가장 큰 쪽에서 맞춘다. */
function toTastes(places: ProfilePlace[]): TasteSlice[] {
  if (places.length === 0) return [];

  const counts = new Map<string, number>();
  places.forEach((p) => {
    const bucket = TASTE_BUCKETS.find((b) => b.match(p.category))?.label ?? ETC_BUCKET.label;
    counts.set(bucket, (counts.get(bucket) ?? 0) + 1);
  });

  const meta = (label: string) => TASTE_BUCKETS.find((b) => b.label === label) ?? ETC_BUCKET;
  const slices = [...counts.entries()]
    .map(([label, count]) => ({
      ...meta(label),
      label,
      percent: Math.round((count / places.length) * 100),
    }))
    .sort((a, b) => b.percent - a.percent)
    .map(({ label, icon, color, percent }) => ({ label, icon, color, percent }));

  // 반올림 때문에 합이 100 에서 어긋나면 가장 큰 조각으로 흡수시킨다.
  const drift = 100 - slices.reduce((sum, s) => sum + s.percent, 0);
  if (drift !== 0 && slices.length > 0) slices[0].percent += drift;
  return slices;
}

/**
 * 내가 속한 여행 전부를 훑어 프로필 집계를 만든다.
 * 한 여행에서 실패해도 나머지는 살리려고 여행 단위로 감싼다.
 */
export async function loadProfileStats(
  trips: { id: number; title: string }[],
  myUserId: number,
): Promise<ProfileStats> {
  const perTrip = await Promise.all(
    trips.map(async (trip) => {
      try {
        const [items, diaries, pinlogs] = await Promise.all([
          fetchItineraryItems(trip.id),
          fetchTripDiaries(trip.id).catch(() => []),
          fetchPinlogs(trip.id).catch(() => []),
        ]);
        return { trip, items, diaries, pinlogs };
      } catch {
        return { trip, items: [], diaries: [], pinlogs: [] };
      }
    }),
  );

  const savedPlaces: ProfilePlace[] = [];
  const visitedPlaces: ProfilePlace[] = [];
  const pinlogList: ProfilePinlog[] = [];

  perTrip.forEach(({ trip, items, diaries, pinlogs }) => {
    // 서버가 mine 을 주지만, 오래된 응답을 대비해 작성자 id 로도 한 번 더 거른다.
    const myPinIds = new Set(
      diaries.filter((d) => d.mine || d.author.userId === myUserId).map((d) => d.itineraryItemId),
    );

    items.forEach((item) => {
      const place = toProfilePlace(item, trip.title);
      savedPlaces.push(place);
      if (myPinIds.has(item.id)) visitedPlaces.push(place);
    });

    pinlogs
      .filter((p) => p.status === 'DONE' && p.mine)
      .forEach((p) => {
        pinlogList.push({
          id: p.id,
          tripId: p.tripId,
          title: p.title ?? `${trip.title} PinLog`,
          thumbnail: pinlogThumbnailUrl(p) ?? '',
          videoUrl: pinlogVideoUrl(p) ?? '',
          meta: `${p.createdAt.slice(0, 10).replace(/-/g, '.')} • ${
            p.durationMs ? `${(p.durationMs / 1000).toFixed(1)}초` : '—'
          }`,
        });
      });
  });

  const regions = [...new Set(visitedPlaces.map((p) => regionOf(p.address)).filter((r): r is string => !!r))];

  return {
    savedPlaces,
    visitedPlaces,
    regions,
    tastes: toTastes(savedPlaces),
    pinlogs: pinlogList,
  };
}
