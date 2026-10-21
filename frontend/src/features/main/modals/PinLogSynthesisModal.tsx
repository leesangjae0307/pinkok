/**
 * PinLog 합성 모달: 장소별 클립 선택 → 생성 요청 → 폴링 → 완성 영상 재생
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Image, Modal, Pressable, ScrollView, Text, View } from 'react-native';
import Video from 'react-native-video';

import { AvatarSource, CURRENT_USER, PlaceItem, PlaceRecord, isImageAvatar, toImageSource } from '../../../data/mockData';
import { PixelBackArrow, PixelHotAirBalloon } from '../../../components/pixel/PixelIcons';
import { AnimatedLoop, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';

interface PinLogSynthesisModalProps {
  places: PlaceItem[];
  records: PlaceRecord[];
  currentUserAvatarUrl: number;
  /** 헤더에 보여줄 여행 이름 */
  tripTitle?: string;
  onClose: () => void;
  onSynthesizeComplete: (result: SynthesizedVlog) => void;
}

type PinLogStatus = 'IDLE' | 'QUEUED' | 'PROCESSING' | 'DONE' | 'FAILED';

interface ClipOption {
  diaryMediaId: number;
  placeId: number;
  thumbnailUrl: string;
  durationMs: number;
  author: { nickname: string; avatar: AvatarSource };
  mine: boolean;
}

export interface SynthesizedVlog {
  id: number;
  title: string;
  totalDuration: number;
  clipsCount: number;
  createdAt: string;
  videoUrl?: string;
  mine?: boolean;
  clips: {
    placeName: string;
    authorName: string;
    authorAvatar: AvatarSource;
    duration: number;
    thumbnail: string;
  }[];
}

export default function PinLogSynthesisModal({
  places,
  records,
  currentUserAvatarUrl,
  tripTitle,
  onClose,
  onSynthesizeComplete,
}: PinLogSynthesisModalProps) {
  const orderedPlaces = useMemo(() => [...places].sort((a, b) => a.order - b.order), [places]);

  const clipOptions = useMemo<ClipOption[]>(
    () =>
      records.flatMap((record) =>
        record.videoClip
          ? [
              {
                diaryMediaId: record.videoClip.id,
                placeId: record.pinId,
                thumbnailUrl: record.videoClip.thumbnail,
                durationMs: Math.round(record.videoClip.duration * 1000),
                author: {
                  nickname: record.authorName,
                  avatar: record.userId === CURRENT_USER.id ? currentUserAvatarUrl : record.authorAvatar,
                },
                mine: record.userId === CURRENT_USER.id,
              },
            ]
          : [],
      ),
    [records, currentUserAvatarUrl],
  );

  const [selectedMedia, setSelectedMedia] = useState<Record<string, number>>(() => {
    const initial: Record<string, number> = {};
    orderedPlaces.forEach((place) => {
      const options = clipOptions.filter((o) => o.placeId === place.id);
      const preferred = options.find((o) => o.mine) || options[0];
      if (preferred) initial[place.id] = preferred.diaryMediaId;
    });
    return initial;
  });
  const [status, setStatus] = useState<PinLogStatus>('IDLE');
  const [pollCount, setPollCount] = useState(0);
  const [pinLogId, setPinLogId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [result, setResult] = useState<SynthesizedVlog | null>(null);

  const selectedOptions = orderedPlaces
    .map((place) => {
      const option = clipOptions.find((o) => o.diaryMediaId === selectedMedia[place.id]);
      return option ? { place, option } : null;
    })
    .filter(Boolean) as { place: PlaceItem; option: ClipOption }[];

  const totalDurationMs = selectedOptions.reduce((t, i) => t + i.option.durationMs, 0);

  const finishGeneration = () => {
    const completed: SynthesizedVlog = {
      id: pinLogId ?? Date.now(),
      title: '제주 시그니처 가로형 PinLog',
      totalDuration: Number((totalDurationMs / 1000).toFixed(1)),
      clipsCount: selectedOptions.length,
      createdAt: '방금 전',
      videoUrl: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
      mine: true,
      clips: selectedOptions.map(({ place, option }) => ({
        placeName: place.name,
        authorName: option.author.nickname,
        authorAvatar: option.author.avatar,
        duration: option.durationMs / 1000,
        thumbnail: option.thumbnailUrl,
      })),
    };
    setResult(completed);
    setStatus('DONE');
    onSynthesizeComplete(completed);
  };

  // 합성이 끝날 때까지 2.4초마다 상태를 다시 확인한다.
  useEffect(() => {
    if (status !== 'QUEUED' && status !== 'PROCESSING') return;
    const timer = setTimeout(() => {
      if (status === 'QUEUED') {
        setStatus('PROCESSING');
        setPollCount(1);
        return;
      }
      if (pollCount < 2) {
        setPollCount((c) => c + 1);
        return;
      }
      finishGeneration();
    }, 2400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, pollCount]);

  const handleCreatePinLog = () => {
    if (selectedOptions.length === 0) {
      setErrorMessage('선택된 클립이 없어 PinLog를 생성할 수 없습니다.');
      setStatus('FAILED');
      return;
    }
    setPinLogId(Date.now());
    setPollCount(0);
    setStatus('QUEUED');
  };

  const handleDelete = () => {
    if (!result?.mine) return;
    setResult(null);
    setStatus('IDLE');
    setPinLogId(null);
  };

  return (
    <Modal transparent visible animationType="fade" onRequestClose={onClose} statusBarTranslucent>
      <View className="flex-1 items-center justify-center bg-black/75 p-3">
        <View
          className="w-full max-w-md overflow-hidden rounded-3xl border-4 border-pixel-navy bg-[#B4FAF0]"
          style={{ height: '92%', maxHeight: 820 }}
        >
          {/* 헤더 */}
          <View className="flex-row items-center border-b-2 border-pixel-navy bg-white px-4 py-3">
            <Pressable
              onPress={onClose}
              accessibilityLabel="PinLog 만들기 닫기"
              className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
            >
              <PixelBackArrow size={15} />
            </Pressable>
            <View className="flex-1 flex-row items-center justify-center gap-2">
              <PixelHotAirBalloon size={30} active />
              <View className="items-center">
                <Text className="text-sm font-black text-pixel-navy">Pinlog 영상 생성</Text>
                {tripTitle ? (
                  <Text numberOfLines={1} className="mt-0.5 max-w-56 font-pixel text-[7px] text-[#075953]">
                    {tripTitle}
                  </Text>
                ) : null}
              </View>
            </View>
            <View className="w-9" />
          </View>

          <ScrollView className="flex-1" contentContainerStyle={{ padding: 16, flexGrow: 1 }} showsVerticalScrollIndicator={false}>
            {status === 'DONE' && result ? (
              <View className="gap-4">
                <View className="relative rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-md">
                  {result.mine && (
                    <Pressable
                      onPress={handleDelete}
                      className="absolute right-3 top-3 z-10 rounded-lg border border-rose-300 bg-rose-50 px-2 py-1"
                    >
                      <Text className="font-pixel text-[8px] text-rose-600">삭제</Text>
                    </Pressable>
                  )}
                  <View className="self-start rounded-lg bg-[#56EADD] px-2 py-1">
                    <Text className="font-pixel text-[9px] text-[#075953]">DONE</Text>
                  </View>
                  <Text className="mt-2 pr-12 text-sm font-black text-slate-900">{result.title}</Text>
                  <Text className="mt-1 text-[10px] font-semibold text-slate-500">
                    1280×720 · {result.clipsCount}개 클립 · {result.totalDuration}초
                  </Text>
                </View>

                <ShadowBox shadowColor="#56EADD" offset={5} radius={16} className="rounded-2xl border-4 border-pixel-navy bg-slate-950 p-2">
                  <View className="mb-2 flex-row items-center justify-between px-1">
                    <Text className="font-pixel text-[8px] text-[#56EADD]">PINKOK VIDEO TAPE</Text>
                    <Text className="font-pixel text-[8px] text-[#FFC837]">1280 × 720</Text>
                  </View>
                  <View className="aspect-video w-full overflow-hidden rounded-lg border-2 border-slate-600 bg-black">
                    {result.videoUrl ? (
                      <Video
                        source={{ uri: result.videoUrl }}
                        poster={result.clips[0] ? { source: { uri: result.clips[0].thumbnail }, resizeMode: 'cover' } : undefined}
                        controls
                        paused
                        resizeMode="cover"
                        style={{ width: '100%', height: '100%' }}
                      />
                    ) : null}
                  </View>
                  <View className="mt-2 flex-row items-center justify-between px-1">
                    <View className="flex-row gap-1">
                      <View className="h-2 w-2 rounded-sm bg-rose-500" />
                      <View className="h-2 w-2 rounded-sm bg-[#56EADD]" />
                      <View className="h-2 w-2 rounded-sm bg-[#FFC837]" />
                    </View>
                    <View className="h-2 w-16 rounded-sm bg-slate-700" />
                  </View>
                </ShadowBox>

                <View className="flex-row flex-wrap justify-between gap-y-2">
                  {result.clips.map((clip, index) => (
                    <View
                      key={`${clip.placeName}-${index}`}
                      className="overflow-hidden rounded-xl border border-[#82F0DC] bg-white"
                      style={{ width: '31.5%' }}
                    >
                      {/* 비율은 감싸는 View 의 style 로 잡는다. Image 의 aspect-* 클래스는
                          적용되지 않아 높이가 잡히지 않는다(아바타에서 확인). */}
                      <View style={{ width: '100%', aspectRatio: 16 / 9 }}>
                        <Image source={{ uri: clip.thumbnail }} resizeMode="cover" className="h-full w-full" />
                      </View>
                      <View className="p-2">
                        <Text numberOfLines={1} className="text-[9px] font-black text-slate-800">
                          {index + 1}. {clip.placeName}
                        </Text>
                        <Text className="mt-1 text-[8px] text-slate-500">{clip.authorName}</Text>
                      </View>
                    </View>
                  ))}
                </View>
              </View>
            ) : status === 'QUEUED' || status === 'PROCESSING' ? (
              <View className="min-h-[580px] flex-1 items-center justify-center rounded-3xl border-3 border-pixel-navy bg-slate-950 p-6">
                <View className="relative">
                  <ShadowBox
                    shadowColor="#FF5A87"
                    offset={6}
                    radius={24}
                    className="h-28 w-28 items-center justify-center rounded-3xl border-4 border-[#56EADD] bg-[#B4FAF0]"
                  >
                    <PixelHotAirBalloon size={70} active />
                  </ShadowBox>
                  <View className="absolute -right-3 -top-3 h-8 w-8 items-center justify-center rounded-lg border-2 border-white bg-rose-500">
                    <Text className="font-pixel text-[9px] text-white">{pollCount + 1}</Text>
                  </View>
                </View>
                <Text className="mt-6 font-pixel text-[10px] text-[#56EADD]">
                  {status === 'QUEUED' ? 'QUEUED...' : 'PROCESSING...'}
                </Text>
                <Text className="mt-3 text-center text-base font-black text-white">레트로 렌더 머신이 가로 영상을 만들고 있어요</Text>
                <Text className="mt-2 text-center text-xs leading-5 text-slate-400">
                  영상 장면을 순서대로 연결하고 있어요.{'\n'}잠시만 기다리면 자동으로 재생됩니다.
                </Text>
                <View className="mt-6 flex-row gap-2">
                  {[0, 1, 2, 3, 4, 5].map((step) =>
                    step <= pollCount + 1 ? (
                      <AnimatedLoop key={step} kind="pulse">
                        <View className="h-4 w-4 rounded-sm border border-white/30 bg-[#56EADD]" />
                      </AnimatedLoop>
                    ) : (
                      <View key={step} className="h-4 w-4 rounded-sm border border-white/30 bg-slate-700" />
                    ),
                  )}
                </View>
              </View>
            ) : status === 'FAILED' ? (
              <View className="items-center rounded-3xl border-3 border-rose-500 bg-white p-6 shadow-xl">
                <View className="h-20 w-20 items-center justify-center rounded-2xl bg-rose-100">
                  <Text className="text-3xl text-rose-600">!</Text>
                </View>
                <Text className="mt-4 font-pixel text-[10px] text-rose-600">FAILED</Text>
                <Text className="mt-2 text-base font-black text-slate-900">PinLog 생성에 실패했어요</Text>
                <View className="mt-2 w-full rounded-xl bg-rose-50 p-3">
                  <Text className="text-center text-xs font-bold text-rose-700">
                    {errorMessage || '영상 렌더링 서버에서 오류가 발생했습니다.'}
                  </Text>
                </View>
                <Pressable
                  onPress={() => setStatus('IDLE')}
                  className="mt-4 h-11 w-full items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
                >
                  <Text className="text-xs font-black text-slate-900">클립 다시 선택하기</Text>
                </Pressable>
              </View>
            ) : (
              <View className="gap-4">
                <View className="rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-sm">
                  <View className="flex-row items-center justify-between">
                    <Text className="text-xs font-black text-[#075953]">장소별 클립 선택</Text>
                    <View className="rounded-lg bg-rose-100 px-2 py-1">
                      <Text className="text-[10px] font-black text-rose-700">{(totalDurationMs / 1000).toFixed(1)}초</Text>
                    </View>
                  </View>
                  <Text className="mt-2 text-xs font-semibold leading-5 text-slate-600">
                    장소마다 영상 하나를 선택하세요. 선택 순서대로 가로형 PinLog가 생성됩니다.
                  </Text>
                </View>

                {orderedPlaces.map((place, index) => {
                  const options = clipOptions.filter((o) => o.placeId === place.id);
                  return (
                    <View key={place.id} className="rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-sm">
                      <View className="flex-row items-center gap-2">
                        <View className="h-6 w-6 items-center justify-center rounded-md bg-pixel-navy">
                          <Text className="font-pixel text-[9px] text-white">{index + 1}</Text>
                        </View>
                        <Text className="text-xs font-black text-slate-900">{place.name}</Text>
                      </View>

                      <View className="mt-3">
                        {options.length > 0 ? (
                          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
                            {options.map((option) => {
                              const selected = selectedMedia[place.id] === option.diaryMediaId;
                              return (
                                <Pressable
                                  key={option.diaryMediaId}
                                  onPress={() => setSelectedMedia((cur) => ({ ...cur, [place.id]: option.diaryMediaId }))}
                                  className={`w-32 overflow-hidden rounded-xl border-2 ${
                                    selected ? 'border-pixel-navy bg-[#B4FAF0]' : 'border-slate-200 bg-slate-50'
                                  }`}
                                >
                                  <View className="relative">
                                    {/* 비율은 감싸는 View 의 style 로 잡는다(위와 같은 이유). */}
                                    <View style={{ width: '100%', aspectRatio: 16 / 9 }}>
                                      <Image source={{ uri: option.thumbnailUrl }} resizeMode="cover" className="h-full w-full" />
                                    </View>
                                    <View className="absolute bottom-1 right-1 rounded bg-black/75 px-1">
                                      <Text className="font-pixel text-[7px] text-white">{(option.durationMs / 1000).toFixed(1)}s</Text>
                                    </View>
                                    {option.mine && (
                                      <View className="absolute left-1 top-1 rounded bg-[#56EADD] px-1.5 py-0.5">
                                        <Text className="text-[8px] font-black text-[#075953]">내 영상</Text>
                                      </View>
                                    )}
                                  </View>
                                  <View className="flex-row items-center gap-1 px-2.5 py-2">
                                    {isImageAvatar(option.author.avatar) ? (
                                      <Image source={toImageSource(option.author.avatar)} resizeMode="cover" className="h-6 w-6 rounded-md" />
                                    ) : (
                                      <Text>{option.author.avatar}</Text>
                                    )}
                                    <Text className="text-[10px] font-black text-slate-800">{option.author.nickname}</Text>
                                  </View>
                                </Pressable>
                              );
                            })}
                          </ScrollView>
                        ) : (
                          <View className="w-full rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 p-4">
                            <Text className="text-center text-[10px] font-bold text-slate-400">선택 가능한 영상이 없습니다.</Text>
                          </View>
                        )}
                      </View>
                    </View>
                  );
                })}
              </View>
            )}
          </ScrollView>

          {status === 'IDLE' && (
            <View className="border-t-2 border-pixel-navy bg-white p-3">
              <PixelPressable
                shadowColor="#075953"
                offset={3}
                radius={12}
                onPress={handleCreatePinLog}
                wrapperClassName="w-full"
                className="h-12 w-full items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
              >
                <Text className="text-xs font-black text-pixel-navy">PinLog 생성 요청 · {selectedOptions.length}개 클립</Text>
              </PixelPressable>
            </View>
          )}
        </View>
      </View>
    </Modal>
  );
}
