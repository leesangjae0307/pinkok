import api from './axios';
import { fileUrl } from './fileApi';

/** AI 가 글·링크·스크린샷에서 뽑아낸 장소 한 건 (아직 좌표는 없다). */
export interface ExtractedPlaceRaw {
  name: string;
  address: string | null;
  category: string | null;
  description: string | null;
}

/** PENDING → PROCESSING → SUCCESS / FAILED */
export interface AiRequest {
  id: number;
  tripId: number | null;
  /** LINK / TEXT / IMAGE */
  inputType: string;
  status: string;
  errorCode: string | null;
  retryCount: number | null;
  requestedAt: string;
  completedAt: string | null;
  /** SUCCESS 일 때만 온다. */
  title: string | null;
  places: ExtractedPlaceRaw[] | null;
}

export interface AiRequestCreatePayload {
  /** 나중에 여행을 골라도 되므로 선택 사항. */
  tripId?: number;
  /** LINK / TEXT / IMAGE */
  inputType: 'LINK' | 'TEXT' | 'IMAGE';
  sourceUrl?: string;
  sourceText?: string;
  /** POST /files 로 먼저 올린 뒤 받은 주소들. */
  imageUrls?: string[];
}

/** AI 가 뽑은 장소 후보. 좌표를 못 찾으면 mapped=false 라 핀으로 추가할 수 없다. */
export interface PlaceCandidate {
  id: number;
  aiRequestId: number;
  /** AI 가 추출한 원래 이름. 카카오맵에서 찾은 이름과 다를 수 있다. */
  rawName: string;
  rawAddress: string | null;
  categoryText: string | null;
  description: string | null;
  /** 0~1. 지도 검색 결과가 AI 이름과 얼마나 맞아떨어지는지 */
  confidence: number | null;
  mapped: boolean;
  needsReview: boolean;
  place: {
    id: number;
    name: string;
    roadAddress: string | null;
    lotAddress: string | null;
    latitude: number;
    longitude: number;
    categoryName: string | null;
  } | null;
  sourceImageUrl: string | null;
  /** 이미 여행에 추가한 적이 있는지 */
  selected: boolean;
  displayOrder: number | null;
}

/** 실제 추출은 뒤에서 돌아간다. 바로 202 로 끝나고 상태는 폴링으로 확인한다. */
export async function createAiRequest(payload: AiRequestCreatePayload): Promise<AiRequest> {
  const { data } = await api.post<AiRequest>('/ai-requests', payload);
  return data;
}

export async function fetchAiRequest(id: number): Promise<AiRequest> {
  const { data } = await api.get<AiRequest>(`/ai-requests/${id}`);
  return data;
}

/** 후보는 추출이 성공하면 서버가 자동으로 만들어 둔다. 보통은 이것만 부르면 된다. */
export async function fetchPlaceCandidates(aiRequestId: number): Promise<PlaceCandidate[]> {
  const { data } = await api.get<PlaceCandidate[]>('/place-candidates', { params: { aiRequestId } });
  return data;
}

/** 자동 생성이 실패해 목록이 비었을 때 재시도. 이미 있으면 그대로 돌려준다. */
export async function createPlaceCandidates(aiRequestId: number): Promise<PlaceCandidate[]> {
  const { data } = await api.post<PlaceCandidate[]>('/place-candidates', null, { params: { aiRequestId } });
  return data;
}

/** 체크한 후보를 여행 지도에 핀으로 추가한다 (날짜 미배정). */
export async function addCandidatesToTrip(
  tripId: number,
  candidateIds: number[],
): Promise<{ addedCount: number; skippedCount: number }> {
  const { data } = await api.post('/place-candidates/add-to-trip', { tripId, candidateIds });
  return data;
}

/** 스크린샷 후보에 붙은 원본 이미지 주소 (상대 경로를 절대 주소로). */
export function candidateImageUrl(candidate: PlaceCandidate): string | null {
  return fileUrl(candidate.sourceImageUrl);
}

/**
 * SUCCESS 나 FAILED 가 될 때까지 다시 조회한다.
 *
 * Gemini 호출이라 몇 초에서 수십 초까지 걸린다. 2초 간격에 최대 2분까지만 기다리고,
 * 그 안에 안 끝나면 마지막 상태를 그대로 돌려준다 — 화면이 영원히 매달려 있으면
 * 멈춘 것처럼 보이기 때문이다.
 */
export async function waitForAiRequest(id: number): Promise<AiRequest> {
  const INTERVAL_MS = 2000;
  const MAX_ATTEMPTS = 60;
  let latest = await fetchAiRequest(id);
  for (let i = 0; i < MAX_ATTEMPTS; i += 1) {
    if (latest.status === 'SUCCESS' || latest.status === 'FAILED') return latest;
    await new Promise((resolve) => setTimeout(resolve, INTERVAL_MS));
    latest = await fetchAiRequest(id);
  }
  return latest;
}
