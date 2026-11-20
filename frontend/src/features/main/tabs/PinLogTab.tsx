/**
 * 핀로그 탭: 여행별 폴더 → 그 여행의 핀로그 목록.
 *
 * 데이터는 GET /pinlogs?mine=true&status=ALL(내가 참여한 모든 여행의 PinLog)를 여행(tripId)별로
 * 묶은 것이다. 만드는 중이거나 실패한 것도 함께 와서, 카드가 상태를 대신 보여 준다.
 * 폴더 안에서 "선택"을 누르면 장소 추출처럼 여러 개를 골라 지울 수 있다.
 * 삭제는 내가 만든 PinLog 만 되고(서버 규칙), 여행과 원본 기록은 지워지지 않는다.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, Image, Pressable, Text, View } from 'react-native';

import {
  isPinlogFailed,
  isPinlogPending,
  pinlogThumbnailUrl,
  pinlogVideoUrl,
  retryPinlog,
  type Pinlog,
} from '../../../api/pinlogApi';
import { serverDateLabel } from '../../../api/serverTime';
import { PixelBackArrow, PixelPinLogIcon } from '../../../components/pixel/PixelIcons';
import { FadeIn, PixelPressable } from '../../../components/pixel/Primitives';
import { TripFolder, VlogClip, toImageSource } from '../../../data/mockData';
import { useApp } from '../AppContext';

function ClipCard({
  clip,
  onPress,
  selection,
  progress,
}: {
  clip: VlogClip;
  onPress: () => void;
  /** 선택 모드일 때만 준다. disabled 는 내가 만든 게 아니라 지울 수 없는 것 */
  selection?: { checked: boolean; disabled: boolean };
  /**
   * 아직 완성되지 않은 PinLog 일 때만 준다. 완성본이면 없다.
   * failed 면 reason 에 서버가 알려준 이유가, onRetry 에 다시 시도가 들어 있다.
   */
  progress?: { failed: boolean; reason: string | null; onRetry?: () => void };
}) {
  return (
    <PixelPressable
      shadowColor="#56EADD"
      offset={4}
      radius={16}
      onPress={onPress}
      wrapperClassName="w-full"
      className="w-full rounded-2xl border-4 border-pixel-navy bg-slate-900 p-2"
    >
      <View className="mb-2 flex-row items-center justify-between px-1">
        <Text className="font-pixel text-[8px] text-[#56EADD]">PINKOK TAPE</Text>
        <Text className="font-pixel text-[8px] text-[#FFC837]">{clip.time}</Text>
      </View>
      <View className="relative aspect-video overflow-hidden rounded-lg border-2 border-slate-600 bg-black">
        {clip.thumbnail ? (
          <Image source={{ uri: clip.thumbnail }} resizeMode="cover" className="h-full w-full" />
        ) : null}

        {/* 완성본만 재생할 수 있다. 나머지는 지금 어떤 상태인지 대신 보여 준다. */}
        {!progress ? (
          <View className="absolute inset-0 items-center justify-center bg-black/20">
            <View className="h-10 w-10 items-center justify-center rounded-full border-2 border-white bg-black/55">
              <Text className="text-sm text-white">▶</Text>
            </View>
          </View>
        ) : progress.failed ? (
          <View className="absolute inset-0 items-center justify-center gap-2 bg-black/70 px-4">
            <Text className="text-center text-xs font-black text-rose-300">만들지 못했어요</Text>
            {progress.reason ? (
              <Text numberOfLines={2} className="text-center text-[10px] text-slate-300">
                {progress.reason}
              </Text>
            ) : null}
            {progress.onRetry ? (
              <Pressable
                onPress={progress.onRetry}
                accessibilityLabel="PinLog 다시 만들기"
                className="rounded-lg border-2 border-white bg-black/60 px-3 py-1 active:opacity-70"
              >
                <Text className="text-[11px] font-black text-white">다시 시도</Text>
              </Pressable>
            ) : null}
          </View>
        ) : (
          <View className="absolute inset-0 items-center justify-center gap-2 bg-black/70">
            <ActivityIndicator color="#56EADD" />
            <Text className="text-[11px] font-black text-[#56EADD]">만드는 중…</Text>
          </View>
        )}
        {selection && (
          <View
            className={`absolute right-2 top-2 h-7 w-7 items-center justify-center rounded-md border-2 ${
              selection.checked ? 'border-[#075953] bg-[#56EADD]' : 'border-white bg-black/40'
            }`}
          >
            <Text className={`font-black ${selection.checked ? 'text-[#075953]' : 'text-transparent'}`}>✓</Text>
          </View>
        )}
        {selection?.disabled && (
          <View className="absolute left-2 top-2 rounded bg-black/70 px-2 py-0.5">
            <Text className="text-[9px] font-bold text-white">내가 만든 것만 삭제 가능</Text>
          </View>
        )}
      </View>
      <View className="flex-row items-center justify-between px-1 pt-2">
        <Text numberOfLines={1} className="flex-1 pr-2 text-xs font-black text-white">
          {clip.location}
        </Text>
        <View className="flex-row gap-1">
          <View className="h-2 w-2 rounded-sm bg-rose-500" />
          <View className="h-2 w-2 rounded-sm bg-[#56EADD]" />
          <View className="h-2 w-2 rounded-sm bg-[#FFC837]" />
        </View>
      </View>
    </PixelPressable>
  );
}

/** 서버의 PinLog 를 재생 모달이 쓰는 모양으로 바꾼다. */
function toClip(p: Pinlog): VlogClip {
  return {
    id: p.id,
    time: serverDateLabel(p.createdAt),
    location: p.title ?? 'PinLog',
    thumbnail: pinlogThumbnailUrl(p) ?? pinlogVideoUrl(p) ?? '',
    videoUrl: pinlogVideoUrl(p) ?? undefined,
    selected: false,
  };
}

/** 여행 한 개의 폴더 카드. 대표 썸네일은 가장 최근 PinLog 의 것을 쓴다. */
function FolderCard({
  title,
  cover,
  count,
  onPress,
}: {
  title: string;
  cover: string | TripFolder['image'] | null;
  count: number;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityLabel={`${title} 폴더, PinLog ${count}개`}
      className="overflow-hidden rounded-2xl border-2 border-pixel-navy bg-white active:opacity-80"
      style={{ width: '48.5%' }}
    >
      <View className="aspect-video w-full items-center justify-center bg-[#B4FAF0]/60">
        {cover ? (
          <Image
            source={typeof cover === 'string' ? { uri: cover } : toImageSource(cover)}
            resizeMode="cover"
            className="h-full w-full"
          />
        ) : (
          <Text className="text-3xl">🎞️</Text>
        )}
        <View className="absolute bottom-1.5 right-1.5 rounded-lg bg-black/70 px-1.5 py-0.5">
          <Text className="font-pixel text-[9px] text-white">{count}</Text>
        </View>
      </View>
      <View className="p-2.5">
        <Text numberOfLines={1} className="text-xs font-black text-slate-900">
          📁 {title}
        </Text>
        <Text className="mt-0.5 text-[10px] font-semibold text-slate-500">
          {count > 0 ? `PinLog ${count}개` : '아직 없어요'}
        </Text>
      </View>
    </Pressable>
  );
}

export default function PinLogTab() {
  const {
    trips,
    myPinlogs,
    isMyPinlogsLoading,
    myPinlogsError,
    reloadMyPinlogs,
    deleteMyPinlogs,
    pinlogFolderTripId,
    setPinlogFolderTripId,
    setActiveClipModal,
    setIsPinLogTripPickerOpen,
    setPinLogTrip,
    setIsPinLogMakerOpen,
    showToast,
  } = useApp();

  const [selectMode, setSelectMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  /** 여행별로 묶는다. 최신 PinLog 가 있는 여행이 앞에 오고, 아직 없는 여행은 뒤에 온다. */
  const folders = useMemo(() => {
    const byTrip = new Map<number, Pinlog[]>();
    myPinlogs.forEach((p) => byTrip.set(p.tripId, [...(byTrip.get(p.tripId) ?? []), p]));
    const latest = (list: Pinlog[]) => Math.max(...list.map((p) => Date.parse(p.createdAt) || 0));
    const rows = trips.map((trip) => ({
      tripId: trip.id,
      trip: trip as TripFolder | undefined,
      title: trip.title,
      pinlogs: (byTrip.get(trip.id) ?? []).sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)),
    }));
    // 여행 목록에 없는 여행의 PinLog 도 누락하지 않는다.
    byTrip.forEach((list, tripId) => {
      if (!trips.some((t) => t.id === tripId)) {
        rows.push({
          tripId,
          trip: undefined,
          title: `여행 #${tripId}`,
          pinlogs: [...list].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)),
        });
      }
    });
    return rows.sort((a, b) => {
      if ((a.pinlogs.length > 0) !== (b.pinlogs.length > 0)) {return a.pinlogs.length > 0 ? -1 : 1;}
      return a.pinlogs.length > 0 && b.pinlogs.length > 0 ? latest(b.pinlogs) - latest(a.pinlogs) : 0;
    });
  }, [myPinlogs, trips]);

  const folder = pinlogFolderTripId !== null ? folders.find((f) => f.tripId === pinlogFolderTripId) : undefined;

  const exitSelectMode = () => {
    setSelectMode(false);
    setSelectedIds([]);
  };
  const openFolder = (tripId: number | null) => {
    exitSelectMode();
    setPinlogFolderTripId(tripId);
  };

  const deletable = folder ? folder.pinlogs.filter((p) => p.mine) : [];
  const allSelected = deletable.length > 0 && deletable.every((p) => selectedIds.includes(p.id));

  const toggle = (id: number) =>
    setSelectedIds((cur) => (cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]));

  /**
   * 만드는 중인 게 있으면 주기적으로 목록을 다시 받는다.
   *
   * 서버가 완성을 알려주지 않으므로 앱이 물어보는 수밖에 없다. 다 끝나면 조건이
   * 거짓이 되어 타이머가 걷힌다. 탭을 떠나면 이 화면이 사라지면서 같이 걷힌다.
   */
  const hasPending = myPinlogs.some(isPinlogPending);
  useEffect(() => {
    if (!hasPending) {return;}
    const timer = setInterval(() => { reloadMyPinlogs(); }, 5000);
    return () => clearInterval(timer);
  }, [hasPending, reloadMyPinlogs]);

  /** 실패한 PinLog 를 서버에 다시 걸어 둔다. 성공하면 다시 "만드는 중"이 된다. */
  const retry = async (pinlogId: number) => {
    try {
      await retryPinlog(pinlogId);
      await reloadMyPinlogs();
    } catch {
      showToast('다시 시도하지 못했어요. 잠시 후 다시 해주세요.');
    }
  };

  const confirmDelete = () => {
    if (selectedIds.length === 0 || isDeleting) {return;}
    Alert.alert(
      `PinLog ${selectedIds.length}개를 삭제할까요?`,
      '완성된 영상이 지워지고 되돌릴 수 없어요.\n여행과 원본 기록(사진·영상)은 그대로 남아요.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: async () => {
            setIsDeleting(true);
            const ids = selectedIds;
            const failed = await deleteMyPinlogs(ids);
            setIsDeleting(false);
            if (failed.length === 0) {
              showToast(`PinLog ${ids.length}개를 삭제했어요.`);
              exitSelectMode();
              return;
            }
            // 일부만 실패하면 지워진 것은 목록에서 이미 빠졌다. 실패한 것만 선택으로 남긴다.
            setSelectedIds(failed);
            const names = myPinlogs
              .filter((p) => failed.includes(p.id))
              .map((p) => p.title ?? 'PinLog')
              .join(', ');
            Alert.alert(
              `${failed.length}개는 삭제하지 못했어요`,
              `${ids.length - failed.length}개는 삭제했어요.\n삭제하지 못한 PinLog: ${names}\n다시 시도해 주세요.`,
            );
          },
        },
      ],
    );
  };

  const header = (
    <View className="flex-row items-center pt-1">
      <View className="flex-row items-center gap-2">
        <View className="h-11 w-11 items-center justify-center">
          <PixelPinLogIcon size={34} active />
        </View>
        <View>
          <Text className="font-pixel text-xl tracking-tight text-[#0f6861]">PINLOG</Text>
          <Text className="text-[11px] font-semibold text-slate-600">소중한 순간을 담는 짧은 브이로그</Text>
        </View>
      </View>
    </View>
  );

  /* ------------------------------ 폴더 안: PinLog 목록 ------------------------------ */
  if (pinlogFolderTripId !== null) {
    const title = folder?.title ?? '여행';
    const pinlogs = folder?.pinlogs ?? [];
    return (
      <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 14 }}>
        <View className="flex-row items-center gap-2 pt-1">
          <Pressable
            onPress={() => openFolder(null)}
            accessibilityLabel="폴더 목록으로 돌아가기"
            className="h-10 w-10 items-center justify-center rounded-xl border-2 border-pixel-navy bg-white active:translate-y-0.5"
          >
            <PixelBackArrow size={16} />
          </Pressable>
          <View className="min-w-0 flex-1">
            <Text numberOfLines={1} className="text-base font-black text-slate-900">
              📁 {title}
            </Text>
            <Text className="text-[11px] font-semibold text-slate-500">PinLog {pinlogs.length}개</Text>
          </View>
          {pinlogs.length > 0 && (
            <Pressable
              onPress={() => (selectMode ? exitSelectMode() : setSelectMode(true))}
              accessibilityLabel={selectMode ? '선택 취소' : '선택'}
              className="rounded-xl border-2 border-[#075953] bg-[#B4FAF0] px-3 py-1.5 active:bg-[#56EADD]"
            >
              <Text className="text-[11px] font-black text-[#075953]">{selectMode ? '취소' : '선택'}</Text>
            </Pressable>
          )}
        </View>

        {/* 선택 모드: 선택 개수 + 전체 선택/해제 (장소 추출과 같은 방식) */}
        {selectMode && (
          <View className="flex-row items-center justify-between px-1">
            <View className="rounded-xl border border-[#82F0DC] bg-white px-2.5 py-1">
              <Text className="text-[10px] font-bold text-[#075953]">{selectedIds.length}개 선택</Text>
            </View>
            <Pressable
              onPress={() => setSelectedIds(allSelected ? [] : deletable.map((p) => p.id))}
              disabled={deletable.length === 0}
              accessibilityLabel={allSelected ? '전체 선택 해제' : '전체 선택'}
              className="rounded-xl border-2 border-[#075953] bg-[#B4FAF0] px-2.5 py-1 active:bg-[#56EADD]"
            >
              <Text className="text-[10px] font-black text-[#075953]">
                {allSelected ? '전체 선택 해제' : '전체 선택'}
              </Text>
            </Pressable>
          </View>
        )}

        {isMyPinlogsLoading && myPinlogs.length === 0 ? (
          <View className="items-center rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
            <ActivityIndicator color="#075953" />
          </View>
        ) : pinlogs.length === 0 ? (
          <View className="items-center gap-3 rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
            <Text className="text-center text-xs font-bold text-slate-400">
              이 여행에는 아직 만든 PinLog 가 없어요.
            </Text>
            {folder?.trip ? (
              <Pressable
                onPress={() => {
                  setPinLogTrip(folder.trip as TripFolder);
                  setIsPinLogMakerOpen(true);
                }}
                className="rounded-xl border-2 border-pixel-navy bg-mint-primary px-4 py-2"
              >
                <Text className="text-xs font-black text-pixel-navy">🎬 이 여행으로 만들기</Text>
              </Pressable>
            ) : null}
          </View>
        ) : (
          <View className="gap-3">
            {pinlogs.map((p) => {
              const clip = toClip(p);
              const pending = isPinlogPending(p);
              const failed = isPinlogFailed(p);
              return (
                <ClipCard
                  key={p.id}
                  clip={clip}
                  selection={
                    selectMode ? { checked: selectedIds.includes(p.id), disabled: !p.mine } : undefined
                  }
                  progress={
                    pending || failed
                      ? {
                          failed,
                          reason: p.errorMessage,
                          // 선택 모드에서는 카드가 고르는 용도라 버튼을 숨긴다.
                          onRetry: failed && p.mine && !selectMode ? () => retry(p.id) : undefined,
                        }
                      : undefined
                  }
                  onPress={() => {
                    if (selectMode) {
                      if (p.mine) {
                        toggle(p.id);
                      } else {
                        showToast('내가 만든 PinLog 만 삭제할 수 있어요.');
                      }
                    } else if (pending) {
                      showToast('아직 만드는 중이에요. 다 되면 바로 보여드릴게요.');
                    } else if (!failed) {
                      setActiveClipModal(clip);
                    }
                  }}
                />
              );
            })}
          </View>
        )}

        {selectMode && (
          <PixelPressable
            shadowColor="#9F1239"
            offset={3}
            radius={16}
            disabled={selectedIds.length === 0 || isDeleting}
            onPress={confirmDelete}
            wrapperClassName="w-full"
            className="h-12 w-full flex-row items-center justify-center gap-2 rounded-2xl border-2 border-pixel-navy bg-rose-500"
            disabledClassName="h-12 w-full items-center justify-center rounded-2xl border-2 border-slate-300 bg-slate-200"
          >
            {isDeleting ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text className={`text-sm font-extrabold ${selectedIds.length === 0 ? 'text-slate-400' : 'text-white'}`}>
                🗑 {selectedIds.length}개 삭제
              </Text>
            )}
          </PixelPressable>
        )}
      </FadeIn>
    );
  }

  /* ------------------------------ 폴더 목록 ------------------------------ */
  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {header}

      <PixelPressable
        shadowColor="#075953"
        offset={3}
        radius={16}
        onPress={() => setIsPinLogTripPickerOpen(true)}
        wrapperClassName="w-full"
        className="h-11 w-full flex-row items-center justify-center gap-1.5 rounded-2xl border-2 border-pixel-navy bg-mint-primary"
      >
        <Text className="text-xs font-black text-pixel-navy">🎬</Text>
        <Text className="text-xs font-black text-pixel-navy">새 PinLog 만들기</Text>
      </PixelPressable>

      <View className="gap-3">
        <View className="flex-row items-center justify-between px-1">
          <Text className="text-xs font-bold text-slate-800">📁 여행별 PinLog</Text>
          <Text className="font-pixel text-[10px] text-[#075953]">{myPinlogs.length} CLIPS</Text>
        </View>

        {isMyPinlogsLoading && myPinlogs.length === 0 ? (
          <View className="items-center rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
            <ActivityIndicator color="#075953" />
          </View>
        ) : myPinlogsError && myPinlogs.length === 0 ? (
          <View className="items-center gap-3 rounded-3xl border-2 border-dashed border-rose-200 bg-white/70 p-8">
            <Text className="text-center text-xs font-bold text-slate-500">{myPinlogsError}</Text>
            <Pressable onPress={reloadMyPinlogs} className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95">
              <Text className="text-xs font-black text-[#075953]">다시 시도</Text>
            </Pressable>
          </View>
        ) : folders.length === 0 ? (
          <View className="rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
            <Text className="text-center text-xs font-bold text-slate-400">
              아직 여행이 없어요. 여행을 만들고 기록한 영상으로 PinLog 를 만들어 보세요.
            </Text>
          </View>
        ) : (
          <View className="flex-row flex-wrap justify-between gap-y-3">
            {folders.map((f) => {
              const first = f.pinlogs[0];
              return (
                <FolderCard
                  key={f.tripId}
                  title={f.title}
                  cover={(first && (pinlogThumbnailUrl(first) ?? null)) || f.trip?.image || null}
                  count={f.pinlogs.length}
                  onPress={() => openFolder(f.tripId)}
                />
              );
            })}
          </View>
        )}
      </View>
    </FadeIn>
  );
}
