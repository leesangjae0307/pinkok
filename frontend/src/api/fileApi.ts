import type { AxiosError } from 'axios';

import api, { BASE_URL } from './axios';

/** 올린 파일 한 건. 사진은 줄인 이미지, 영상은 첫 장면이 thumbnailUrl 로 온다. */
export interface UploadedFile {
  url: string;
  thumbnailUrl: string | null;
  /** IMAGE / VIDEO */
  mediaType: string;
  size: number;
  width: number | null;
  height: number | null;
  durationMs: number | null;
}

/** 갤러리·카메라가 돌려준 파일 하나. react-native-image-picker 의 Asset 과 같은 모양이다. */
export interface PickedFile {
  uri: string;
  fileName?: string | null;
  type?: string | null;
}

/**
 * mime 타입에서 확장자를 뽑는다.
 *
 * 서버는 파일 이름의 확장자로 사진인지 영상인지 가른다. 이름이 없을 때 늘 .jpg 를
 * 붙이면 영상도 사진으로 보고 10MB 제한에 걸려 400 으로 거절당한다
 * (촬영한 영상이 기록에 안 붙던 원인).
 */
function extensionOf(type: string): string {
  const sub = type.split('/')[1];
  if (!sub) {return 'bin';}
  // image/jpeg → jpg, video/quicktime → mov 처럼 흔한 것만 따로 맞춘다.
  if (sub === 'jpeg') {return 'jpg';}
  if (sub === 'quicktime') {return 'mov';}
  return sub.split(';')[0];
}

/**
 * 업로드 한 건의 제한 시간. 일반 API 는 15초(axios.ts)인데, 영상(최대 20MB)을 느린
 * 회선에서 올리면 그 안에 끝나지 않는다. 시간이 다 되면 서버는 받았더라도 앱은
 * 실패로 보고 뒤이은 기록 저장을 보내지 않으므로, 업로드에만 넉넉하게 준다.
 */
export const UPLOAD_TIMEOUT_MS = 120000;

/**
 * 동시에 올리는 파일 수. 한 요청에 여러 장을 담으면 Cloud Run 의 요청 본문 상한(32MB)에
 * 걸려 서버에 닿기도 전에 거절되므로, 파일마다 따로 보내되 회선을 나눠 쓰도록 2개로 묶는다.
 */
const UPLOAD_CONCURRENCY = 2;

/** 파일 하나를 올린 결과. 하나가 실패해도 나머지 결과는 살린다. */
export type UploadOutcome = { ok: true; file: UploadedFile } | { ok: false; error: unknown };

/**
 * 응답을 아예 못 받은 경우(연결 끊김·시간 초과)만 네트워크 오류로 본다.
 * 서버가 4xx/5xx 로 답했다면 같은 요청을 다시 보내도 결과가 같다.
 */
export function isNetworkError(e: unknown): boolean {
  const err = e as AxiosError | undefined;
  return !!err && !err.response;
}

/**
 * POST /files 로 파일 하나를 올린다. 서버는 여러 개를 받는 엔드포인트라 응답이 배열로 온다.
 *
 * multipart 라 Content-Type 을 직접 지정하지 않는다. 경계 문자열(boundary)은
 * fetch/axios 가 FormData 를 보고 직접 붙이는데, 여기서 헤더를 덮어쓰면 그게 빠져
 * 서버가 본문을 못 읽는다.
 *
 * 네트워크 오류일 때만 한 번 다시 보낸다. 업로드는 다시 보내도 기록이 겹치지 않는다
 * (최악의 경우 서버에 쓰이지 않는 파일이 하나 남는다).
 */
async function postOne(file: PickedFile, index: number): Promise<UploadedFile> {
  const send = async () => {
    const type = file.type ?? 'image/jpeg';
    const form = new FormData();
    form.append('file', {
      uri: file.uri,
      name: file.fileName ?? `upload-${Date.now()}-${index}.${extensionOf(type)}`,
      type,
    } as unknown as Blob);
    const { data } = await api.post<UploadedFile[]>('/files', form, { timeout: UPLOAD_TIMEOUT_MS });
    if (!data?.[0]) {throw new Error('업로드 응답이 비어 있어요.');}
    return data[0];
  };
  try {
    return await send();
  } catch (e) {
    if (!isNetworkError(e)) {throw e;}
    return send();
  }
}

/**
 * 파일을 하나씩 따로 올린다. 동시에 UPLOAD_CONCURRENCY 개까지만 보낸다.
 *
 * 하나가 실패해도 나머지는 끝까지 올리고, 결과를 입력과 같은 순서로 돌려준다.
 * onSettled 는 파일 하나가 끝날 때마다 불린다 — 진행률 표시와 "이미 올린 파일" 기록에 쓴다.
 */
export async function uploadEach(
  files: PickedFile[],
  onSettled?: (index: number, outcome: UploadOutcome) => void,
): Promise<UploadOutcome[]> {
  const results: UploadOutcome[] = new Array(files.length);
  let next = 0;
  const worker = async () => {
    while (next < files.length) {
      const i = next++;
      try {
        results[i] = { ok: true, file: await postOne(files[i], i) };
      } catch (error) {
        results[i] = { ok: false, error };
      }
      try {
        onSettled?.(i, results[i]);
      } catch {
        // 진행률 갱신이 실패해도 업로드는 계속한다.
      }
    }
  };
  const workers = Math.min(UPLOAD_CONCURRENCY, files.length);
  await Promise.all(Array.from({ length: workers }, worker));
  return results;
}

/**
 * 여러 파일을 올린다. 하나라도 실패하면 그 오류를 던진다 (기존 호출부와 같은 동작).
 * 안에서는 파일마다 따로 요청한다.
 */
export async function uploadFiles(files: PickedFile[]): Promise<UploadedFile[]> {
  const results = await uploadEach(files);
  const uploaded: UploadedFile[] = [];
  for (const r of results) {
    if (!r.ok) {throw r.error;}
    uploaded.push(r.file);
  }
  return uploaded;
}

/** 한 장만 올릴 때. */
export async function uploadFile(file: PickedFile): Promise<UploadedFile> {
  const [uploaded] = await uploadFiles([file]);
  return uploaded;
}

/**
 * 서버가 주는 주소는 `/files/2026/09/abc.jpg` 같은 상대 경로다.
 * Image 에 그대로 주면 불러오지 못하므로 BASE_URL 을 붙인다.
 */
export function fileUrl(url: string | null | undefined): string | null {
  if (!url) {return null;}
  if (/^https?:\/\//.test(url)) {return url;}
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;
}
