/**
 * 여행 지도 하단 조절형 바텀시트 (드래그 핸들 · 일자 필터 · 이동수단 타임라인)
 */
import React, { useMemo, useRef } from 'react';
import { ActivityIndicator, Image, PanResponder, Pressable, ScrollView, Text, View } from 'react-native';
import Svg, { Line } from 'react-native-svg';

import { PlaceItem } from '../../../data/mockData';
import { AnimatedLoop, PixelPressable } from '../../../components/pixel/Primitives';

interface TripsBottomSheetProps {
  /** 바텀시트가 놓인 영역의 실제 높이(px) */
  containerHeight: number;
  sheetHeightPercent: number;
  onHeightChange: (height: number) => void;
  selectedDay: number | 'all';
  onSelectDay: (day: number | 'all') => void;
  places: PlaceItem[];
  selectedPlaceId: number;
  onSelectPlace: (place: PlaceItem) => void;
  onOpenTeamRecord: (place: PlaceItem) => void;
  /** 장소 상세를 카메라 화면으로 바로 연다 */
  onOpenCamera: (place: PlaceItem) => void;
  teamRecordCounts: { [pinId: number]: number };
  onOptimizeRoute: () => void;
  isOptimizingRoute: boolean;
  /** 이 여행에 실제로 있는 일자 번호. 비어 있으면 일자 칩을 숨긴다. */
  dayNumbers: number[];
  isLoading: boolean;
  loadError: string | null;
  onRetry: () => void;
}

function DashedVLine() {
  return (
    <View pointerEvents="none" style={{ position: 'absolute', left: 25, top: 0, bottom: 0, width: 2 }}>
      <Svg width={2} height="100%">
        <Line x1="1" y1="0" x2="1" y2="100%" stroke="rgba(45,212,191,0.8)" strokeWidth={2} strokeDasharray="4 3" />
      </Svg>
    </View>
  );
}

export default function TripsBottomSheet({
  containerHeight,
  sheetHeightPercent,
  onHeightChange,
  selectedDay,
  onSelectDay,
  places,
  selectedPlaceId,
  onSelectPlace,
  onOpenTeamRecord,
  onOpenCamera,
  teamRecordCounts,
  onOptimizeRoute,
  isOptimizingRoute,
  dayNumbers,
  isLoading,
  loadError,
  onRetry,
}: TripsBottomSheetProps) {
  // PanResponder 콜백 안에서 최신 값을 읽기 위한 ref
  const heightRef = useRef(sheetHeightPercent);
  heightRef.current = sheetHeightPercent;
  const startHeightRef = useRef(sheetHeightPercent);
  const containerRef = useRef(containerHeight);
  containerRef.current = containerHeight;
  const onChangeRef = useRef(onHeightChange);
  onChangeRef.current = onHeightChange;

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        onPanResponderGrant: () => {
          startHeightRef.current = heightRef.current;
        },
        onPanResponderMove: (_, g) => {
          const base = containerRef.current || 800;
          const deltaPercent = (-g.dy / base) * 100; // 위로 끌면 +
          const next = Math.min(85, Math.max(22, startHeightRef.current + deltaPercent));
          onChangeRef.current(next);
        },
        onPanResponderRelease: () => {
          // 가까운 단계로 스냅: 접힘(25%) / 중간(50%) / 최대(78%)
          const h = heightRef.current;
          if (h < 36) onChangeRef.current(25);
          else if (h > 62) onChangeRef.current(78);
          else onChangeRef.current(50);
        },
        onPanResponderTerminationRequest: () => false,
      }),
    [],
  );

  return (
    <View
      className="absolute bottom-0 left-0 right-0 z-30 overflow-hidden rounded-t-[32px] border-x-3 border-t-3 border-pixel-navy bg-white"
      style={{
        height: `${sheetHeightPercent}%`,
        elevation: 16,
        shadowColor: '#000',
        shadowOpacity: 0.18,
        shadowRadius: 15,
        shadowOffset: { width: 0, height: -8 },
      }}
    >
      {/* 1. 드래그 핸들 */}
      <View {...panResponder.panHandlers} className="w-full items-center justify-center pb-2 pt-3">
        <View className="mb-1.5 h-1.5 w-14 rounded-full bg-slate-300" />
        <View className="flex-row items-center gap-1.5">
          <Text className="font-pixel text-[8px] text-teal-700">DRAG SHEET</Text>
          <Text className="text-[10px] font-bold text-slate-400">·</Text>
          <Text className="text-[10px] font-bold text-slate-400">
            {sheetHeightPercent <= 30
              ? '접힘 (올려서 일정 보기)'
              : sheetHeightPercent >= 70
                ? '최대 확장 (전체 일정 모드)'
                : '중간 (지도 & 일정 동시 보기)'}
          </Text>
        </View>
      </View>

      {/* 2. 일자 탭 */}
      <View className="flex-row items-center justify-between border-b border-slate-100 bg-white px-4 py-2">
        <ScrollView horizontal showsHorizontalScrollIndicator={false} className="flex-1" contentContainerStyle={{ gap: 8, paddingVertical: 2 }}>
          <Pressable
            onPress={() => onSelectDay('all')}
            className={`flex-row items-center gap-1 rounded-full border-2 px-3 py-1.5 ${
              selectedDay === 'all' ? 'border-pixel-navy bg-mint-primary shadow-sm' : 'border-slate-200 bg-slate-100'
            }`}
          >
            <Text className={`text-xs font-black ${selectedDay === 'all' ? 'text-pixel-navy' : 'text-slate-600'}`}>전체</Text>
            <View className="rounded-full bg-white/70 px-1">
              <Text className="font-pixel text-[9px] text-slate-700">{places.length}</Text>
            </View>
          </Pressable>

          {dayNumbers.map((d) => (
            <Pressable
              key={d}
              onPress={() => onSelectDay(d)}
              className={`flex-row items-center gap-1 rounded-full border-2 px-3 py-1.5 ${
                selectedDay === d ? 'border-pixel-navy bg-mint-primary shadow-sm' : 'border-slate-200 bg-slate-100'
              }`}
            >
              <Text className={`font-pixel text-[10px] ${selectedDay === d ? 'text-pixel-navy' : 'text-slate-600'}`}>{d}day</Text>
            </Pressable>
          ))}
        </ScrollView>

        <View className="flex-row items-center gap-1 pl-2">
          <Pressable
            onPress={onOptimizeRoute}
            disabled={isOptimizingRoute}
            className={`h-8 items-center justify-center rounded-lg border-2 border-pixel-navy bg-[#56EADD] px-2 ${
              isOptimizingRoute ? 'opacity-60' : ''
            }`}
          >
            <Text className="text-[9px] font-black text-pixel-navy">
              {isOptimizingRoute ? '최적화 중...' : '🪄 AI 동선 최적화'}
            </Text>
          </Pressable>
          <Pressable
            onPress={() => onHeightChange(sheetHeightPercent > 50 ? 25 : 78)}
            accessibilityLabel="높이 전환"
            className="h-7 w-7 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 active:bg-slate-100"
          >
            <Text className="text-xs font-black text-slate-600">{sheetHeightPercent > 50 ? '▼' : '▲'}</Text>
          </Pressable>
        </View>
      </View>

      {/* 3. 장소 타임라인 */}
      <ScrollView className="flex-1" contentContainerStyle={{ paddingHorizontal: 16, paddingVertical: 12, gap: 8 }} showsVerticalScrollIndicator={false}>
        {isLoading ? (
          <View className="items-center rounded-2xl border-2 border-dashed border-slate-200 p-8">
            <ActivityIndicator color="#075953" />
          </View>
        ) : loadError ? (
          <View className="items-center gap-3 rounded-2xl border-2 border-dashed border-slate-200 p-8">
            <Text className="text-center text-xs text-slate-500">{loadError}</Text>
            <Pressable onPress={onRetry} className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95">
              <Text className="text-xs font-black text-[#075953]">다시 시도</Text>
            </Pressable>
          </View>
        ) : places.length === 0 ? (
          <View className="rounded-2xl border-2 border-dashed border-slate-200 p-8">
            <Text className="text-center text-xs text-slate-400">해당 일자에 등록된 장소가 없습니다.</Text>
          </View>
        ) : (
          places.map((place) => {
            const isSelected = selectedPlaceId === place.id;
            const recordsCount = teamRecordCounts[place.id] || 0;

            return (
              <React.Fragment key={place.id}>
                <Pressable
                  onPress={() => onSelectPlace(place)}
                  className={`flex-row items-center justify-between gap-3 rounded-2xl border-2 p-3 ${
                    isSelected ? 'border-pixel-navy bg-[#B4FAF0]/70 shadow-md' : 'border-slate-200 bg-white'
                  }`}
                >
                  <View className="min-w-0 flex-1 flex-row items-center gap-3">
                    {/* 순번 픽셀 뱃지 */}
                    {isSelected ? (
                      <AnimatedLoop kind="bounceSmall">
                        <View className="h-7 w-7 items-center justify-center rounded-full border-2 border-pixel-navy bg-[#FF5A87]">
                          <Text className="font-pixel text-xs text-white">{place.order}</Text>
                        </View>
                      </AnimatedLoop>
                    ) : (
                      <View className="h-7 w-7 items-center justify-center rounded-full border-2 border-pixel-navy bg-pixel-navy">
                        <Text className="font-pixel text-xs text-white">{place.order}</Text>
                      </View>
                    )}

                    {/* 동그란 썸네일 */}
                    <View className="h-13 w-13 overflow-hidden rounded-full border-2 border-pixel-navy bg-white p-0.5">
                      <Image source={{ uri: place.thumbnail }} resizeMode="cover" className="h-full w-full rounded-full" />
                    </View>

                    {/* 장소명 + 태그 */}
                    <View className="min-w-0 flex-1">
                      <Text className="text-xs font-extrabold leading-4 text-slate-900">{place.name}</Text>
                      <View className="mt-1.5 flex-row flex-wrap items-center gap-1.5">
                        <Text className="font-pixel text-[9px] text-amber-500">★ {place.rating}</Text>
                        <Text className="text-[10px] text-slate-300">·</Text>
                        <View className="rounded bg-[#B4FAF0] px-1.5 py-0.5">
                          <Text className="text-[9px] font-bold text-teal-800">{place.day}day</Text>
                        </View>
                        {place.time ? (
                          <View className="rounded border border-slate-300 bg-slate-100 px-1.5 py-0.5">
                            <Text className="font-pixel text-[8px] text-slate-600">{place.time}</Text>
                          </View>
                        ) : null}
                        {isSelected && (
                          <View className="rounded border border-rose-200 bg-rose-50 px-1.5 py-0.5">
                            <Text className="text-[8px] font-black text-rose-600">선택됨 ✓</Text>
                          </View>
                        )}
                      </View>
                    </View>
                  </View>

                  {/* 팀 기록 · 바로 촬영 */}
                  <View className="items-end gap-1">
                    <Pressable
                      onPress={() => onOpenTeamRecord(place)}
                      className="flex-row items-center gap-1 rounded-xl border border-teal-600 bg-white px-2.5 py-1.5 active:translate-y-0.5 active:bg-teal-50"
                    >
                      <Text className="text-[10px] font-black text-teal-900">👥 기록</Text>
                      {recordsCount > 0 && (
                        <View className="rounded-full bg-teal-700 px-1">
                          <Text className="font-pixel text-[8px] text-white">{recordsCount}</Text>
                        </View>
                      )}
                    </Pressable>
                    <PixelPressable
                      shadowColor="#075953"
                      offset={2}
                      radius={12}
                      accessibilityLabel={`${place.name}에서 바로 촬영`}
                      className="rounded-xl border-2 border-pixel-navy bg-rose-500 px-2.5 py-1.5"
                      onPress={() => onOpenCamera(place)}
                    >
                      <Text className="text-[9px] font-black text-white">● 바로 촬영</Text>
                    </PixelPressable>
                  </View>
                </Pressable>

                {/* 장소 간 이동 수단 */}
                {place.transitToNext && (
                  <View className="relative my-0.5 flex-row items-center gap-2.5 py-1.5 pl-6">
                    <DashedVLine />
                    <View className="z-10 h-7 w-7 items-center justify-center rounded-full border-2 border-pixel-navy bg-white">
                      <Text className="text-sm">{place.transitToNext.type === 'car' ? '🚗' : '🚶'}</Text>
                    </View>
                    <View className="flex-row items-center gap-1.5 rounded-xl border border-teal-200 bg-white/95 px-2.5 py-1">
                      <Text className="text-[10px] font-black text-slate-800">{place.transitToNext.label}</Text>
                      <View className="rounded bg-[#B4FAF0] px-1.5">
                        <Text className="font-pixel text-[9px] text-[#075953]">{place.transitToNext.duration}</Text>
                      </View>
                      {place.transitToNext.distance ? (
                        <Text className="text-[9px] text-slate-400">({place.transitToNext.distance})</Text>
                      ) : null}
                    </View>
                  </View>
                )}
              </React.Fragment>
            );
          })
        )}
      </ScrollView>
    </View>
  );
}
