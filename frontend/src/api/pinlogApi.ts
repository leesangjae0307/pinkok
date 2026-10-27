import api from './axios';
import { fileUrl } from './fileApi';
import { DiaryAuthor } from './diaryApi';

/** 핀 하나에서 고를 수 있는 클립. 같은 장소에 팀원 여러 명의 영상이 있을 수 있다. */
export interface ClipOption {
  itineraryItemId: number;
  dayNumber: number | null;
  visitOrder: number | null;
  placeName: string;
  options: {
    diaryMediaId: number;
    /** PHOTO / VIDEO */
    mediaType: string;
    fileUrl: string;
    thumbnailUrl: string | null;
    durationMs: number | null;
    author: DiaryAuthor;
    /** 내가 찍은 것인지 */
    mine: boolean;
  }[];
}

export interface PinlogClip {
  diaryMediaId: number;
  clipOrder: number;
}

/** QUEUED → PROCESSING → DONE 으로 바뀐다. FAILED 면 errorMessage 에 이유가 있다. */
export interface Pinlog {
  id: number;
  tripId: number;
  title: string | null;
  status: string;
  videoUrl: string | null;
  /** 갤러리에 저장할 때 쓸 파일 이름. 완성됐을 때만 온다. */
  downloadFileName: string | null;
  thumbnailUrl: string | null;
  durationMs: number | null;
  resolution: string | null;
  errorMessage: string | null;
  templateCode: string | null;
  styleCode: string | null;
  creatorNickname: string;
  /** 내가 만든 것인지. 삭제 버튼 노출에 쓴다. */
  mine: boolean;
  createdAt: string;
  completedAt: string | null;
}

export interface PinlogCreatePayload {
  tripId: number;
  title?: string;
  /** TRAVEL_DIARY / CINEMATIC / EMOTIONAL */
  templateCode?: string;
  /** BRIGHT / CALM / RETRO */
  styleCode?: string;
  /** 비워두면 서버가 동선 순서로 자동 선택한다. */
  clips?: PinlogClip[];
  /** 자동 선택에 사진까지 포함할지. 기본은 영상만. */
  includePhotos?: boolean;
}

export async function fetchClipOptions(tripId: number, includePhotos = false): Promise<ClipOption[]> {
  const { data } = await api.get<ClipOption[]>('/pinlogs/clip-options', {
    params: { tripId, includePhotos },
  });
  return data;
}

export async function fetchPinlogs(tripId: number): Promise<Pinlog[]> {
  const { data } = await api.get<Pinlog[]>('/pinlogs', { params: { tripId } });
  return data;
}

export async function fetchPinlog(pinlogId: number): Promise<Pinlog> {
  const { data } = await api.get<Pinlog>(`/pinlogs/${pinlogId}`);
  return data;
}

/** 합성은 오래 걸려서 202 로 바로 끝난다. 완성 여부는 waitForPinlog 로 확인한다. */
export async function createPinlog(payload: PinlogCreatePayload): Promise<Pinlog> {
  const { data } = await api.post<Pinlog>('/pinlogs', payload);
  return data;
}

/** 만든 사람만 지울 수 있다. 합성된 영상 파일도 같이 지워진다. */
export async function deletePinlog(pinlogId: number): Promise<void> {
  await api.delete(`/pinlogs/${pinlogId}`);
}

/**
 * DONE 이나 FAILED 가 될 때까지 다시 조회한다.
 *
 * 서버가 완성을 알려주지 않으므로 앱이 주기적으로 물어보는 수밖에 없다. 2초 간격에
 * 최대 2분까지만 기다리고, 그 안에 안 끝나면 마지막 상태를 그대로 돌려준다 — 영원히
 * 매달려 있으면 화면이 멈춘 것처럼 보이기 때문이다. 못 받은 결과는 목록을 다시
 * 열었을 때 완성된 상태로 보인다.
 */
export async function waitForPinlog(
  pinlogId: number,
  onProgress?: (pinlog: Pinlog) => void,
): Promise<Pinlog> {
  const INTERVAL_MS = 2000;
  const MAX_ATTEMPTS = 60;
  let latest = await fetchPinlog(pinlogId);
  for (let i = 0; i < MAX_ATTEMPTS; i += 1) {
    if (latest.status === 'DONE' || latest.status === 'FAILED') return latest;
    await new Promise((resolve) => setTimeout(resolve, INTERVAL_MS));
    latest = await fetchPinlog(pinlogId);
    onProgress?.(latest);
  }
  return latest;
}

/** 재생·썸네일에 쓸 절대 주소. 서버는 `/files/...` 상대 경로를 준다. */
export function pinlogVideoUrl(pinlog: Pinlog): string | null {
  return fileUrl(pinlog.videoUrl);
}

export function pinlogThumbnailUrl(pinlog: Pinlog): string | null {
  return fileUrl(pinlog.thumbnailUrl);
}
