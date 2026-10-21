/**
 * 새 PinLog 만들기 1단계: 어떤 여행의 PinLog 를 만들지 고른다 (웹 isPinLogTripPickerOpen 이관)
 */
import React from 'react';
import { Image, Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ModalBackdrop } from '../../../components/pixel/Primitives';
import { TripFolder, toImageSource } from '../../../data/mockData';

export default function PinLogTripPickerModal({
  trips,
  onClose,
  onSelect,
}: {
  trips: TripFolder[];
  onClose: () => void;
  onSelect: (trip: TripFolder) => void;
}) {
  const insets = useSafeAreaInsets();
  return (
    <ModalBackdrop onRequestClose={onClose}>
      <View className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]" style={{ marginBottom: insets.bottom }}>
        <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
          <View className="flex-1 pr-2">
            <Text className="font-pixel text-[9px] text-[#075953]">STEP 1 · SELECT TRIP</Text>
            <Text className="mt-1 text-sm font-black text-slate-900">어떤 여행의 PinLog를 만들까요?</Text>
          </View>
          <Pressable
            onPress={onClose}
            accessibilityLabel="여행 선택 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
          >
            <Text className="text-xs font-black text-slate-800">✕</Text>
          </Pressable>
        </View>
        <ScrollView style={{ maxHeight: 420 }} contentContainerStyle={{ padding: 16, gap: 8 }}>
          {trips.length === 0 && (
            <Text className="py-6 text-center text-xs font-bold text-slate-500">아직 만든 여행이 없어요.</Text>
          )}
          {trips.map((trip) => (
            <Pressable
              key={trip.id}
              onPress={() => onSelect(trip)}
              className="flex-row items-center gap-3 rounded-2xl border-2 border-[#82F0DC] bg-white p-3 active:border-pixel-navy"
            >
              <View className="h-14 w-14 overflow-hidden rounded-xl">
                <Image source={toImageSource(trip.image)} resizeMode="cover" className="h-full w-full" />
              </View>
              <View className="min-w-0 flex-1">
                <Text className="font-pixel text-[8px] text-[#075953]">{trip.dDay}</Text>
                <Text numberOfLines={1} className="mt-1 text-xs font-black text-slate-900">
                  {trip.title}
                </Text>
                <Text className="mt-1 text-[9px] font-semibold text-slate-500">
                  {trip.date} · 장소 {trip.placesCount}개
                </Text>
              </View>
              <Text className="font-pixel text-sm text-[#075953]">›</Text>
            </Pressable>
          ))}
        </ScrollView>
      </View>
    </ModalBackdrop>
  );
}
