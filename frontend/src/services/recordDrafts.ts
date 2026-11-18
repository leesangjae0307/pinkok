import AsyncStorage from '@react-native-async-storage/async-storage';
import RNFS from 'react-native-fs';

import type { DiaryMediaPayload } from '../api/diaryApi';
import type { PlaceRecord } from '../data/mockData';

/**
 * 장소 기록 임시저장.
 *
 * 기록 저장은 작성 화면을 닫은 뒤 뒤에서 진행된다. 업로드나 저장이 실패하면 사용자가
 * 쓴 메모·별점·고른 사진이 사라지면 안 되므로, 요청을 보내기 **전에** 여기 적어 두고
 * 서버 저장이 성공한 뒤에만 지운다. 앱이 도중에 꺼져도 다음 실행 때 다시 시도할 수 있다.
 *
 * 키는 사용자 ID 와 핀 ID 를 함께 쓴다. 한 기기에서 계정을 바꿔도 남의 임시저장이 섞이지 않는다.
 */

const PREFIX = 'pinkok.recordDraft.';

const keyOf = (userId: number, pinId: number) => `${PREFIX}${userId}.${pinId}`;

export interface RecordDraft {
  userId: number;
  pinId: number;
  /** 작성하던 기록. 사진·영상 주소는 로컬 파일이거나 이미 서버에 있는 주소다. */
  record: PlaceRecord;
  /**
   * 이미 올린 로컬 파일 → 서버가 준 주소. 다시 시도할 때 같은 파일을 또 올리지 않는다.
   * 파일 하나가 올라갈 때마다 바로 적어 두므로 일부만 올라간 상태도 남는다.
   */
  uploaded: Record<string, DiaryMediaPayload>;
  /** saving: 저장 중 (앱이 그 사이 꺼졌으면 다음 실행 때 실패로 본다) / failed: 다시 시도를 기다린다 */
  status: 'saving' | 'failed';
  updatedAt: number;
}

export async function saveRecordDraft(draft: RecordDraft): Promise<void> {
  await AsyncStorage.setItem(keyOf(draft.userId, draft.pinId), JSON.stringify(draft));
}

export async function loadRecordDraft(userId: number, pinId: number): Promise<RecordDraft | null> {
  const raw = await AsyncStorage.getItem(keyOf(userId, pinId));
  if (!raw) {return null;}
  try {
    return JSON.parse(raw) as RecordDraft;
  } catch {
    // 깨진 값은 복구할 수 없으니 치운다.
    await AsyncStorage.removeItem(keyOf(userId, pinId));
    return null;
  }
}

/** 이 사용자의 임시저장을 모두 읽는다. 앱을 다시 켰을 때 실패 표시를 되살리는 데 쓴다. */
export async function loadRecordDrafts(userId: number): Promise<RecordDraft[]> {
  const mine = `${PREFIX}${userId}.`;
  const keys = (await AsyncStorage.getAllKeys()).filter((k) => k.startsWith(mine));
  if (keys.length === 0) {return [];}
  const entries = await AsyncStorage.multiGet(keys);
  const drafts: RecordDraft[] = [];
  const broken: string[] = [];
  entries.forEach(([key, raw]) => {
    if (!raw) {return;}
    try {
      drafts.push(JSON.parse(raw) as RecordDraft);
    } catch {
      broken.push(key);
    }
  });
  if (broken.length > 0) {await AsyncStorage.multiRemove(broken);}
  return drafts;
}

export async function removeRecordDraft(userId: number, pinId: number): Promise<void> {
  await AsyncStorage.removeItem(keyOf(userId, pinId));
}

/** 이미 서버에 있는 파일인지. 사진첩·카메라에서 막 고른 것은 file:// 나 content:// 로 온다. */
export const isRemoteUri = (uri: string) => /^https?:\/\//.test(uri);

/**
 * 로컬 파일이 아직 남아 있는지.
 *
 * 사진 선택기는 고른 파일을 앱 캐시에 복사해 file:// 로 넘겨준다. 캐시는 기기 용량이
 * 부족하면 OS 가 지울 수 있어서, 앱을 다시 켰을 때 없을 수 있다. content:// 처럼 여기서
 * 확인할 수 없는 주소는 있다고 보고, 실제로 없으면 업로드가 실패해 임시저장이 남는다.
 */
async function localFileExists(uri: string): Promise<boolean> {
  if (!uri.startsWith('file://')) {return true;}
  try {
    return await RNFS.exists(decodeURIComponent(uri.slice('file://'.length)));
  } catch {
    return false;
  }
}

/**
 * 임시저장에서 사라진 로컬 사진·영상을 뺀다. 이미 올라간 파일은 서버 주소가 있으므로 남긴다.
 * 빠진 개수를 함께 돌려줘서 사용자에게 다시 골라 달라고 알릴 수 있게 한다.
 */
export async function pruneMissingMedia(
  draft: RecordDraft,
): Promise<{ draft: RecordDraft; removed: number }> {
  const keep = async (uri: string) =>
    isRemoteUri(uri) || !!draft.uploaded[uri] || (await localFileExists(uri));

  const photoChecks = await Promise.all(draft.record.photos.map(keep));
  const photos = draft.record.photos.filter((_, i) => photoChecks[i]);
  let removed = draft.record.photos.length - photos.length;

  let videoClip = draft.record.videoClip;
  if (videoClip?.videoUrl && !(await keep(videoClip.videoUrl))) {
    videoClip = undefined;
    removed += 1;
  }
  if (removed === 0) {return { draft, removed };}
  return { draft: { ...draft, record: { ...draft.record, photos, videoClip } }, removed };
}
