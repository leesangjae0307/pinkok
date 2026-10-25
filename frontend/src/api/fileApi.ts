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
 * POST /files. 한 번에 1~10개까지 올린다.
 *
 * multipart 라 Content-Type 을 직접 지정하지 않는다. 경계 문자열(boundary)은
 * fetch/axios 가 FormData 를 보고 직접 붙이는데, 여기서 헤더를 덮어쓰면 그게 빠져
 * 서버가 본문을 못 읽는다.
 */
export async function uploadFiles(files: PickedFile[]): Promise<UploadedFile[]> {
  const form = new FormData();
  files.forEach((file, index) => {
    form.append('file', {
      uri: file.uri,
      name: file.fileName ?? `upload-${Date.now()}-${index}.jpg`,
      type: file.type ?? 'image/jpeg',
    } as unknown as Blob);
  });
  const { data } = await api.post<UploadedFile[]>('/files', form);
  return data;
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
  if (!url) return null;
  if (/^https?:\/\//.test(url)) return url;
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;
}
