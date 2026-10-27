import api from './axios';
import { fileUrl } from './fileApi';
import { PlaceRecord } from '../data/mockData';

export interface DiaryAuthor {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  avatarImageUrl: string | null;
}

export interface DiaryMedia {
  id: number;
  /** PHOTO / VIDEO */
  mediaType: string;
  fileUrl: string;
  thumbnailUrl: string | null;
  durationMs: number | null;
}

/** 핀 하나에 남긴 기록. 목록 화면이 바로 그릴 수 있도록 핀·작성자 정보를 함께 준다. */
export interface Diary {
  id: number;
  itineraryItemId: number;
  tripId: number;
  dayNumber: number | null;
  visitOrder: number | null;
  placeName: string;
  author: DiaryAuthor;
  /** 내가 쓴 기록인지. 수정·삭제 버튼 노출에 쓴다. */
  mine: boolean;
  content: string | null;
  rating: number | null;
  media: DiaryMedia[];
  createdAt: string;
  updatedAt: string;
}

/** 기록에 붙일 파일. POST /files 로 먼저 올리고 받은 url 을 넣는다. */
export interface DiaryMediaPayload {
  fileUrl: string;
  /** 영상 길이(ms). 사진이면 비운다. */
  durationMs?: number;
}

export interface DiaryCreatePayload {
  itineraryItemId: number;
  content?: string;
  /** 1~5. 안 매겼으면 비운다. */
  rating?: number;
  media?: DiaryMediaPayload[];
}

/** 여행 전체 기록을 동선 순서로. 팀원 것까지 모두 온다. */
export async function fetchTripDiaries(tripId: number): Promise<Diary[]> {
  const { data } = await api.get<Diary[]>('/diaries', { params: { tripId } });
  return data;
}

/** 핀 하나에 달린 팀원들의 기록. */
export async function fetchPlaceDiaries(itineraryItemId: number): Promise<Diary[]> {
  const { data } = await api.get<Diary[]>('/diaries', { params: { itineraryItemId } });
  return data;
}

/** 같은 핀에 내 기록이 이미 있으면 409 가 온다. */
export async function createDiary(payload: DiaryCreatePayload): Promise<Diary> {
  const { data } = await api.post<Diary>('/diaries', payload);
  return data;
}

/** media 를 보내면 첨부가 그 목록으로 통째로 바뀐다. */
export async function updateDiary(
  diaryId: number,
  payload: Omit<DiaryCreatePayload, 'itineraryItemId'>,
): Promise<Diary> {
  const { data } = await api.patch<Diary>(`/diaries/${diaryId}`, payload);
  return data;
}

/** 붙어 있던 사진·영상 파일도 같이 지워진다. */
export async function deleteDiary(diaryId: number): Promise<void> {
  await api.delete(`/diaries/${diaryId}`);
}

/** '2026.10.13' */
function dateLabel(iso: string): string {
  return iso.slice(0, 10).replace(/-/g, '.');
}

/**
 * 서버 기록을 화면이 쓰는 PlaceRecord 로 바꾼다.
 *
 * 화면은 영상을 한 개만 다루므로 media 에서 첫 번째 VIDEO 만 꺼내 쓰고, 나머지 사진은
 * photos 로 모은다. 서버는 길이를 ms 로 주는데 화면은 초 단위로 보여준다.
 */
export function toPlaceRecord(diary: Diary): PlaceRecord {
  const video = diary.media.find((m) => m.mediaType === 'VIDEO');
  const photos = diary.media
    .filter((m) => m.mediaType === 'PHOTO')
    .map((m) => fileUrl(m.fileUrl))
    .filter((url): url is string => url !== null);

  return {
    id: diary.id,
    pinId: diary.itineraryItemId,
    userId: diary.author.userId,
    authorName: diary.author.nickname,
    authorAvatar:
      fileUrl(diary.author.avatarImageUrl ?? diary.author.profileImageUrl) ?? '🙂',
    createdAt: dateLabel(diary.createdAt),
    rating: diary.rating ?? 0,
    memo: diary.content ?? '',
    photos,
    videoClip: video
      ? {
          id: video.id,
          duration: Math.round(((video.durationMs ?? 0) / 1000) * 10) / 10,
          thumbnail: fileUrl(video.thumbnailUrl) ?? fileUrl(video.fileUrl) ?? '',
          videoUrl: fileUrl(video.fileUrl) ?? undefined,
          createdAt: dateLabel(diary.createdAt),
        }
      : undefined,
  };
}
