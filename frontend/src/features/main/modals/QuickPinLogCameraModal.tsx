/**
 * 홈의 "핀로그 촬영" 카드로 여는 빠른 촬영 화면 (웹 QuickPinLogCameraModal.tsx 이관)
 *
 * 촬영 → 장소 지정 → 그 장소의 내 기록에 영상 추가.
 * 촬영은 RecordClipModal 이 실제 카메라로 한다. 여기서는 찍어 온 로컬 파일을
 * 들고 있다가 장소를 고르면 넘긴다. 업로드는 기록을 저장할 때 한 번에 한다.
 */
import React, { useEffect, useState } from 'react';
import { Image, Modal, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PixelBackArrow, PixelPinLogIcon } from '../../../components/pixel/PixelIcons';
import { PixelPressable, PixelSelect } from '../../../components/pixel/Primitives';
import { PlaceItem, PlaceRecord, TripFolder, toImageSource } from '../../../data/mockData';
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
  const isRecording = false;
  const progress = recorded ? 100 : 0;

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
              {recorded ? '촬영 완료 · 장소를 지정해 주세요' : '2~5초로 짧게 찍어요'}
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
                clip &&
                onUpload(selectedPlace, {
                  id: Date.now(),
                  duration: clip.durationMs / 1000,
                  // 썸네일은 올린 뒤 서버가 첫 장면으로 만들어 준다. 장소 사진을
                  // 넣으면 영상과 다른 그림이 클립 표지로 남는다.
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
