/**
 * 홈의 "핀로그 촬영" 카드로 여는 빠른 촬영 화면 (웹 QuickPinLogCameraModal.tsx 이관)
 *
 * 촬영 → 장소 지정 → 그 장소의 내 기록에 영상 추가.
 * 촬영은 RecordClipModal 이 실제 카메라로 한다. 여기서는 찍어 온 로컬 파일을
 * 들고 있다가 장소를 고르면 넘긴다. 업로드는 기록을 저장할 때 한 번에 한다.
 */
import React, { useEffect, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PixelBackArrow, PixelPinLogIcon } from '../../../components/pixel/PixelIcons';
import { PixelPressable, PixelSelect } from '../../../components/pixel/Primitives';
import { PlaceItem, PlaceRecord, TripFolder } from '../../../data/mockData';
import Video from 'react-native-video';
import RecordClipModal, { type RecordedClip } from './RecordClipModal';

export type QuickClip = NonNullable<PlaceRecord['videoClip']>;


export default function QuickPinLogCameraModal({
  trip,
  places,
  onClose,
  onUpload,
}: {
  trip: TripFolder;
  places: PlaceItem[];
  onClose: () => void;
  onUpload: (place: PlaceItem, clip: QuickClip) => void;
}) {
  const insets = useSafeAreaInsets();
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  /** 실제로 찍은 클립. 장소를 고르면 이 값을 그대로 올린다. */
  const [clip, setClip] = useState<RecordedClip | null>(null);
  const [placeId, setPlaceId] = useState<number | null>(places[0]?.id ?? null);

  /**
   * 이 창을 열 때 그 여행의 일정을 받아오므로, 열리는 순간에는 places 가 비어 있다가
   * 조금 뒤에 채워진다. 처음 한 번만 잡는 초기값으로 두면 선택이 null 로 남아
   * "이 장소에 영상 추가하기" 가 영영 눌리지 않는다.
   */
  useEffect(() => {
    if (placeId == null && places.length > 0) setPlaceId(places[0].id);
  }, [placeId, places]);

  const recorded = clip !== null;
  /** 찍어 온 클립을 미리보기에서 재생 중인지. */
  const [isPlaying, setIsPlaying] = useState(false);

  const selectedPlace = places.find((place) => place.id === placeId);

  return (
    <Modal visible animationType="slide" onRequestClose={onClose} statusBarTranslucent>
      <View className="flex-1 bg-slate-950" style={{ paddingTop: insets.top }}>
        {/* 상단 바 */}
        <View className="flex-row items-center justify-between border-b border-white/15 px-4 py-3">
          <Pressable
            onPress={onClose}
            accessibilityLabel="빠른 촬영 닫기"
            className="h-10 w-10 items-center justify-center rounded-xl border border-white/20 bg-white/10 active:opacity-70"
          >
            <PixelBackArrow size={16} color="#FFFFFF" />
          </Pressable>
          <View className="flex-1 items-center px-2">
            <Text className="font-pixel text-[9px] text-[#56EADD]">QUICK RECORD</Text>
            <Text className="mt-1 text-sm font-black text-white" numberOfLines={1}>
              {trip.title}
            </Text>
          </View>
          <PixelPinLogIcon size={34} active />
        </View>

        {/*
          촬영은 아래 버튼을 누르면 열리는 RecordClipModal 이 한다. 그래서 여기는
          카메라 화면이 아니다. 전에는 장소 사진을 깔고 가이드 프레임과 십자선을
          얹어 뷰파인더인 척했는데, 실제로 보이는 장면과 아무 상관이 없는 그림이라
          걷어냈다. 찍기 전에는 안내만, 찍은 뒤에는 그 영상을 그대로 보여준다.
        */}
        <View className="relative flex-1 overflow-hidden bg-black">
          {recorded && clip ? (
            <>
              <Pressable
                onPress={() => setIsPlaying((p) => !p)}
                accessibilityLabel={isPlaying ? '영상 일시정지' : '영상 재생'}
                className="h-full w-full"
              >
                {/* Video 는 네이티브 뷰라 터치를 먹는다. 감싼 Pressable 로 흘려보낸다. */}
                <View pointerEvents="none" style={StyleSheet.absoluteFill}>
                  <Video
                    source={{ uri: clip.uri }}
                    paused={!isPlaying}
                    repeat
                    muted={!isPlaying}
                    resizeMode="contain"
                    style={{ width: '100%', height: '100%' }}
                  />
                </View>
                {!isPlaying && (
                  <View
                    pointerEvents="none"
                    style={[StyleSheet.absoluteFill, { alignItems: 'center', justifyContent: 'center' }]}
                  >
                    <View className="h-16 w-16 items-center justify-center rounded-full border-2 border-white/80 bg-black/50">
                      <Text className="text-xl text-white">▶</Text>
                    </View>
                  </View>
                )}
              </Pressable>

              <View className="absolute bottom-4 right-4 rounded-lg bg-black/75 px-2 py-1">
                <Text className="font-pixel text-[9px] text-white">
                  {(clip.durationMs / 1000).toFixed(1)}s
                </Text>
              </View>
            </>
          ) : (
            <View className="flex-1 items-center justify-center gap-3 px-10">
              <PixelPinLogIcon size={52} active />
              <Text className="text-center text-sm font-black text-white">
                아래 버튼을 누르면 카메라가 열려요
              </Text>
              <Text className="text-center text-[11px] font-semibold text-white/60">
                최대 2초로 짧게 찍으면 돼요.{'\n'}가로로 들고 찍으면 PinLog 에 그대로 쓰여요.
              </Text>
            </View>
          )}

          <View className="absolute left-4 top-4 rounded-xl bg-black/60 px-3 py-2">
            <Text className="text-xs font-black text-white">
              {recorded ? '촬영 완료 · 장소를 지정해 주세요' : '아직 찍은 영상이 없어요'}
            </Text>
          </View>
        </View>

        {/* 하단 컨트롤 */}
        {recorded ? (
          <View className="gap-3 border-t border-white/10 bg-slate-950 p-4" style={{ paddingBottom: insets.bottom + 16 }}>
            <View>
              <Text className="mb-2 text-xs font-black text-[#56EADD]">어떤 장소의 영상인가요?</Text>
              <PixelSelect
                size="lg"
                accessibilityLabel="영상을 추가할 장소"
                value={placeId != null ? String(placeId) : ''}
                options={places.map((place) => ({ value: String(place.id), label: `${place.order}. ${place.name}` }))}
                onChange={(value) => setPlaceId(Number(value))}
              />
            </View>
            <PixelPressable
              shadowColor="#FF5A87"
              radius={12}
              disabled={!selectedPlace}
              className="h-12 w-full items-center justify-center rounded-xl border-2 border-white bg-[#56EADD]"
              disabledClassName="h-12 w-full items-center justify-center rounded-xl border-2 border-white bg-slate-600"
              onPress={() =>
                selectedPlace &&
                clip &&
                onUpload(selectedPlace, {
                  id: Date.now(),
                  duration: clip.durationMs / 1000,
                  // 썸네일은 올린 뒤 서버가 실제 첫 프레임으로 만들어 준다.
                  // 장소 사진을 넣어두면 영상과 상관없는 그림이 잠깐 보인다.
                  thumbnail: '',
                  videoUrl: clip.uri,
                  createdAt: '방금 전',
                })
              }
            >
              <Text className="text-xs font-black text-pixel-navy">이 장소에 영상 추가하기</Text>
            </PixelPressable>
          </View>
        ) : (
          <View className="items-center justify-center bg-slate-950 px-5 pt-5" style={{ paddingBottom: insets.bottom + 20 }}>
            {/* 웹의 0 0 0 6px 링 그림자는 바깥 테두리 View 로 표현 */}
            <View className="rounded-full bg-white/10 p-1.5">
              <Pressable
                onPress={() => setIsCameraOpen(true)}
                accessibilityLabel="핀로그 영상 촬영 시작"
                className="h-20 w-20 items-center justify-center rounded-full border-4 border-white bg-rose-500"
              >
                <View className="h-10 w-10 rounded-full border-2 border-white bg-rose-600" />
              </Pressable>
            </View>
          </View>
        )}
      </View>

      {isCameraOpen && (
        <RecordClipModal
          title={trip.title}
          onClose={() => setIsCameraOpen(false)}
          onRecorded={(recordedClip) => {
            setClip(recordedClip);
            setIsCameraOpen(false);
          }}
        />
      )}
    </Modal>
  );
}
