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
  /**
   * 사진인지 영상인지. 이름도 타입도 확장자도 없을 때(카메라가 준 content:// 등)
   * 무엇으로 가정할지 정한다. 비우면 사진으로 본다.
   */
  mediaHint?: 'IMAGE' | 'VIDEO';
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
  if (!sub) return 'bin';
  // image/jpeg → jpg, video/quicktime → mov 처럼 흔한 것만 따로 맞춘다.
  if (sub === 'jpeg') return 'jpg';
  if (sub === 'quicktime') return 'mov';
  return sub.split(';')[0];
}

/**
 * 보낼 파일 이름을 정한다.
 *
 * 서버는 이름의 확장자로 사진·영상을 가르기 때문에 실제 확장자를 지켜야 한다.
 * 갤러리에서 고른 파일은 image-picker 가 `file://.../rn_image_picker_...png` 처럼
 * 확장자가 있는 임시 파일로 주므로 그 이름을 그대로 쓴다. 이름도 확장자도 없을
 * 때만(촬영 직후의 content:// 등) mime 타입으로 지어 붙인다.
 */
function fileNameFor(file: PickedFile, type: string, index: number): string {
  if (file.fileName) return file.fileName;
  const base = uriBaseName(file.uri);
  // 아는 확장자일 때만 그 이름을 쓴다. `.tmp` 같은 건 서버가 미디어로 받아주지 않는다.
  const ext = extensionIn(base);
  if (ext && TYPE_BY_EXTENSION[ext]) return base;
  return `upload-${Date.now()}-${index}.${extensionOf(type)}`;
}

/** `file:///a/b/c.png?x=1` → `c.png` */
function uriBaseName(uri: string): string {
  return uri.split('?')[0].split('/').pop() ?? '';
}

/** 파일 이름 끝의 확장자(점 없이, 소문자). 없으면 null. */
function extensionIn(name: string): string | null {
  const match = /\.([a-z0-9]{2,5})$/i.exec(name);
  return match ? match[1].toLowerCase() : null;
}

/** 확장자로 추측하는 mime 타입. 서버·RN 둘 다 이 타입을 보고 판단한다. */
const TYPE_BY_EXTENSION: Record<string, string> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  heic: 'image/heic',
  gif: 'image/gif',
  mp4: 'video/mp4',
  mov: 'video/quicktime',
  m4v: 'video/x-m4v',
  '3gp': 'video/3gpp',
};

/**
 * 보낼 mime 타입을 정한다.
 *
 * 이름과 타입이 어긋나면(이름은 .png 인데 타입은 jpeg) 서버가 거절할 수 있어서
 * 둘을 같은 근거로 맞춘다. 확장자를 알면 그게 가장 믿을 만하고, 그다음이 고른
 * 쪽에서 알려준 타입, 마지막이 사진·영상 힌트다.
 */
function typeFor(file: PickedFile): string {
  const ext = extensionIn(file.fileName ?? uriBaseName(file.uri));
  const byExtension = ext ? TYPE_BY_EXTENSION[ext] : undefined;
  // 확장자를 알면 그게 가장 믿을 만하다. 호출부가 넘긴 타입은 추측일 수 있다
  // (예: 'video/mp4' 로 넘겼지만 실제로는 .mov 를 고른 경우).
  if (byExtension) return byExtension;
  if (file.type) return file.type;
  return file.mediaHint === 'VIDEO' ? 'video/mp4' : 'image/jpeg';
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
    const type = typeFor(file);
    form.append('file', {
      uri: file.uri,
      name: fileNameFor(file, type, index),
      type,
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
