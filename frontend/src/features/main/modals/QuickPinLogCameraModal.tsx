/**
 * 홈의 "핀로그 촬영" 카드로 여는 빠른 촬영 화면 (웹 QuickPinLogCameraModal.tsx 이관)
 *
 * 촬영 → 장소 지정 → 그 장소의 내 기록에 영상 추가.
 * 웹과 같이 촬영은 시뮬레이션이에요. 실제 카메라를 붙일 때는 촬영 버튼의
 * setIsRecording(true) 자리에서 녹화를 시작하고, 끝나면 POST /files 로 올린 뒤
 * 받은 URL 을 onUpload 의 clip.videoUrl 로 넘기면 돼요.
 */
import React, { useEffect, useState } from 'react';
import { Image, Modal, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PixelBackArrow, PixelPinLogIcon } from '../../../components/pixel/PixelIcons';
import { PixelPressable, PixelSelect } from '../../../components/pixel/Primitives';
import { PlaceItem, PlaceRecord, TripFolder, toImageSource } from '../../../data/mockData';

export type QuickClip = NonNullable<PlaceRecord['videoClip']>;

/** 웹과 같은 2.8초 촬영: 56ms 마다 2%씩 → 100% */
const TICK_MS = 56;
const STEP = 2;
const CLIP_SECONDS = 2.8;

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
  const [isRecording, setIsRecording] = useState(false);
  const [progress, setProgress] = useState(0);
  const [recorded, setRecorded] = useState(false);
  const [placeId, setPlaceId] = useState<number | null>(places[0]?.id ?? null);

  useEffect(() => {
    if (!isRecording) {
      return;
    }
    const timer = setInterval(() => {
      setProgress((current) => {
        const next = current + STEP;
        if (next >= 100) {
          clearInterval(timer);
          setIsRecording(false);
          setRecorded(true);
          return 100;
        }
        return next;
      });
    }, TICK_MS);
    return () => clearInterval(timer);
  }, [isRecording]);

  const selectedPlace = places.find((place) => place.id === placeId);
  const viewfinder = selectedPlace?.thumbnail ? { uri: selectedPlace.thumbnail } : toImageSource(trip.image);

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

        {/* 뷰파인더 */}
        <View className="relative flex-1 overflow-hidden bg-black">
          <Image source={viewfinder} resizeMode="cover" className="h-full w-full" />
          {/* 웹의 brightness-75 / brightness-110 대신 어두운 레이어로 표현 */}
          {!isRecording && <View pointerEvents="none" className="absolute inset-0 bg-black/25" />}

          {/* 가이드 프레임 + 십자선 */}
          <View pointerEvents="none" className="absolute inset-5 items-center justify-center border border-white/35">
            <View className="absolute h-px w-12 bg-white/70" />
            <View className="absolute h-12 w-px bg-white/70" />
          </View>

          <View className="absolute left-4 top-4 rounded-xl bg-black/60 px-3 py-2">
            <Text className="text-xs font-black text-white">
              {recorded ? '촬영 완료 · 장소를 지정해 주세요' : `${CLIP_SECONDS}초 자동 촬영`}
            </Text>
          </View>

          {(isRecording || recorded) && (
            <View className="absolute bottom-0 left-0 right-0 h-2 bg-black/60">
              <View className="h-full bg-rose-500" style={{ width: `${progress}%` }} />
            </View>
          )}
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
                onUpload(selectedPlace, {
                  id: Date.now(),
                  duration: CLIP_SECONDS,
                  thumbnail: selectedPlace.thumbnail,
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
                disabled={isRecording}
                onPress={() => {
                  setProgress(0);
                  setIsRecording(true);
                }}
                accessibilityLabel="핀로그 영상 촬영 시작"
                className={`h-20 w-20 items-center justify-center rounded-full border-4 border-white ${
                  isRecording ? 'bg-slate-600' : 'bg-rose-500'
                }`}
              >
                <View className="h-10 w-10 rounded-full border-2 border-white bg-rose-600" />
              </Pressable>
            </View>
          </View>
        )}
      </View>
    </Modal>
  );
}
