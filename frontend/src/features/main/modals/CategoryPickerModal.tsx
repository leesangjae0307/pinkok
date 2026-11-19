/**
 * 핀 하나의 장소 종류를 바꾸는 창.
 *
 * 같은 장소를 여러 여행에서 쓰더라도 지금 고른 핀만 바뀐다. 예를 들어 같은
 * 호텔을 한 여행에서는 "숙박", 다른 여행에서는 "관광명소"로 둘 수 있다.
 *
 * 사용자가 직접 바꾼 핀에만 "원래 분류로 되돌리기"가 보인다. 서버가 카카오에서
 * 받아온 기본 분류가 따로 있어서, 되돌리면 그 값으로 돌아간다.
 */
import React from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ModalBackdrop } from '../../../components/pixel/Primitives';
import { PlaceCategory } from '../../../api/placeApi';
import { PlaceItem } from '../../../data/mockData';

export default function CategoryPickerModal({
  place,
  categories,
  isLoading,
  onClose,
  onSelect,
}: {
  place: PlaceItem;
  categories: PlaceCategory[];
  isLoading: boolean;
  onClose: () => void;
  /** null 이면 장소의 원래 분류로 되돌린다. */
  onSelect: (code: string | null) => void;
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
            <Text className="font-pixel text-[9px] text-[#075953]">SELECT CATEGORY</Text>
            <Text numberOfLines={1} className="mt-1 text-sm font-black text-slate-900">
              {place.name}
            </Text>
          </View>
          <Pressable
            onPress={onClose}
            accessibilityLabel="장소 종류 선택 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
          >
            <Text className="text-xs font-black text-slate-800">✕</Text>
          </Pressable>
        </View>

        <ScrollView style={{ maxHeight: 420 }} contentContainerStyle={{ padding: 16, gap: 8 }}>
          {isLoading && categories.length === 0 ? (
            <View className="items-center py-8">
              <ActivityIndicator color="#075953" />
            </View>
          ) : null}

          {categories.map((c) => {
            const isCurrent = place.categoryCode === c.code;
            return (
              <Pressable
                key={c.code}
                onPress={() => onSelect(c.code)}
                disabled={isCurrent}
                className={`flex-row items-center justify-between rounded-2xl border-2 p-3 ${
                  isCurrent
                    ? 'border-pixel-navy bg-[#B4FAF0]'
                    : 'border-[#82F0DC] bg-white active:border-pixel-navy'
                }`}
              >
                <Text className="text-xs font-black text-slate-800">{c.label}</Text>
                {isCurrent ? (
                  <Text className="text-[10px] font-black text-rose-600">지금 이것</Text>
                ) : (
                  <Text className="font-pixel text-sm text-[#075953]">›</Text>
                )}
              </Pressable>
            );
          })}

          {/* 직접 바꾼 핀만 되돌릴 것이 있다. */}
          {place.categoryOverridden ? (
            <Pressable
              onPress={() => onSelect(null)}
              className="mt-1 flex-row items-center justify-between rounded-2xl border-2 border-dashed border-slate-400 bg-white p-3 active:bg-slate-50"
            >
              <Text className="text-xs font-black text-slate-600">원래 분류로 되돌리기</Text>
              <Text className="font-pixel text-sm text-slate-500">›</Text>
            </Pressable>
          ) : null}
        </ScrollView>
      </View>
    </ModalBackdrop>
  );
}
