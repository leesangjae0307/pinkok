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
  /**
   * 후보 저장까지 끝났는지. 장소 추출이 SUCCESS 일 때만 오고 그 외에는 null 이다.
   *
   * - true  : 바로 GET /place-candidates 로 받아오면 된다(0곳이면 AI 가 못 찾은 것).
   * - false : 장소는 찾았는데 후보 저장이 실패했다(카카오맵 장애 등).
   *           POST /place-candidates 로 다시 만들어야 한다.
   */
  candidatesReady: boolean | null;
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

/** 실패한 요청을 서버가 처음부터 다시 돌린다. 스크린샷을 다시 올릴 필요는 없다. */
export async function retryAiRequest(id: number): Promise<AiRequest> {
  const { data } = await api.post<AiRequest>(`/ai-requests/${id}/retry`);
  return data;
}

/**
 * SUCCESS 나 FAILED 가 될 때까지 다시 조회한다.
 *
 * 서버가 늦어도 5분 안에 FAILED(GEMINI_TIMEOUT) 로 끝내준다. 그런데 서버가 아예
 * 꺼져 응답이 없으면 그 보장도 소용이 없으므로, 앱에서도 6분을 넘기면 포기한다.
 * 끝나지 않은 채 돌려준 상태는 부른 쪽에서 실패로 처리한다.
 */
export async function waitForAiRequest(id: number): Promise<AiRequest> {
  const INTERVAL_MS = 2000;
  const MAX_ATTEMPTS = 180;
  let latest = await fetchAiRequest(id);
  for (let i = 0; i < MAX_ATTEMPTS; i += 1) {
    if (latest.status === 'SUCCESS' || latest.status === 'FAILED') return latest;
    await new Promise((resolve) => setTimeout(resolve, INTERVAL_MS));
    latest = await fetchAiRequest(id);
  }
  return latest;
}

/**
 * 추출이 끝난 요청에서 후보 목록을 가져온다.
 *
 * 보통은 서버가 미리 만들어 둬서 그대로 받으면 되는데(candidatesReady=true),
 * 후보 저장이 실패했으면(false) 여기서 다시 만든다. SUCCESS 이전에 만들기를
 * 부르면 서버가 400 으로 거절하므로 이 함수는 끝난 요청에만 쓴다.
 */
export async function loadPlaceCandidates(request: AiRequest): Promise<PlaceCandidate[]> {
  if (request.candidatesReady === false) return createPlaceCandidates(request.id);
  return fetchPlaceCandidates(request.id);
}
