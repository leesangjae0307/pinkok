/**
 * 장소를 몇 일차에 둘지 고르는 창.
 *
 * 미배정으로 되돌리는 선택지는 없다. 서버의 핀 수정 API 는 dayId 를 비우면
 * "바꾸지 말라"로 읽어서, 이미 배정된 핀을 날짜 없는 상태로 되돌릴 방법이 없다.
 * (그 일자를 통째로 지우면 그 일자의 핀들이 미배정으로 돌아간다.)
 */
import React from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ModalBackdrop } from '../../../components/pixel/Primitives';
import { PlaceItem } from '../../../data/mockData';

export default function DayPickerModal({
  place,
  dayNumbers,
  onClose,
  onSelect,
}: {
  place: PlaceItem;
  dayNumbers: number[];
  onClose: () => void;
  onSelect: (day: number) => void;
}) {
  const insets = useSafeAreaInsets();
  return (
    <ModalBackdrop onRequestClose={onClose}>
      <View
        className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]"
        style={{ marginBottom: insets.bottom }}
      >
        <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
          <View className="flex-1 pr-2">
            <Text className="font-pixel text-[9px] text-[#075953]">SELECT DAY</Text>
            <Text numberOfLines={1} className="mt-1 text-sm font-black text-slate-900">
              {place.name}
            </Text>
          </View>
          <Pressable
            onPress={onClose}
            accessibilityLabel="일자 선택 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
          >
            <Text className="text-xs font-black text-slate-800">✕</Text>
          </Pressable>
        </View>

        <ScrollView style={{ maxHeight: 360 }} contentContainerStyle={{ padding: 16, gap: 8 }}>
          {dayNumbers.length === 0 && (
            <Text className="py-6 text-center text-xs font-bold text-slate-500">
              아직 만든 일자가 없어요. 먼저 "+ 일자"로 일자를 만들어 주세요.
            </Text>
          )}
          {dayNumbers.map((d) => {
            const isCurrent = place.day === d;
            return (
              <Pressable
                key={d}
                onPress={() => onSelect(d)}
                disabled={isCurrent}
                className={`flex-row items-center justify-between rounded-2xl border-2 p-3 ${
                  isCurrent ? 'border-pixel-navy bg-[#B4FAF0]' : 'border-[#82F0DC] bg-white active:border-pixel-navy'
                }`}
              >
                <Text className="font-pixel text-xs text-[#075953]">{d}day</Text>
                {isCurrent ? (
                  <Text className="text-[10px] font-black text-rose-600">지금 여기</Text>
                ) : (
                  <Text className="font-pixel text-sm text-[#075953]">›</Text>
                )}
              </Pressable>
            );
          })}
        </ScrollView>
      </View>
    </ModalBackdrop>
  );
}
