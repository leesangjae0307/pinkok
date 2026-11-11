/**
 * 여행 지도 하단 조절형 바텀시트 (드래그 핸들 · 일자 필터 · 이동수단 타임라인)
 */
import React, { useMemo, useRef } from 'react';
import { ActivityIndicator, PanResponder, Pressable, ScrollView, Text, View } from 'react-native';
import Svg, { Line } from 'react-native-svg';

import { PlaceItem } from '../../../data/mockData';
import { AnimatedLoop, PixelPressable } from '../../../components/pixel/Primitives';

/**
 * 선택 상태에서만 주는 그림자.
 *
 * className 으로 shadow-* 를 붙였다 뗐다 하면 NativeWind 가 이미 올라간 컴포넌트를
 * "업그레이드"해야 한다고 보고 경고를 찍는데, 그 경고가 props 를 직렬화하다 무한
 * 재귀에 빠져 JS 스레드를 통째로 멈춘다. 그래서 그림자는 클래스 대신 style 로 준다.
 */
const SELECTED_SHADOW = {
  shadowColor: '#000',
  shadowOpacity: 0.12,
  shadowRadius: 4,
  shadowOffset: { width: 0, height: 2 },
  // elevation 은 쓰지 않는다. 안드로이드는 elevation 이 붙은 뷰를 따로 합성하는데,
  // 배경이 반투명하면 둥근 모서리를 무시한 네모가 카드 위에 덧그려진다.
  // 선택 상태는 테두리·배경·"선택됨" 배지로 이미 충분히 보인다.
} as const;

interface TripsBottomSheetProps {
  /** 바텀시트가 놓인 영역의 실제 높이(px) */
  containerHeight: number;
  sheetHeightPercent: number;
  onHeightChange: (height: number) => void;
  selectedDay: number | 'all';
  onSelectDay: (day: number | 'all') => void;
  /** 지금 고른 일자의 장소. 일자를 고르면 걸러진 목록이 온다. */
  places: PlaceItem[];
  /** 일자와 상관없는 전체 장소 수. "전체" 칩의 숫자로 쓴다. */
  totalCount: number;
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
  onAddDay: () => void;
  isAddingDay: boolean;
  /** 일자 칩을 길게 눌렀을 때. 그 일자의 핀은 지워지지 않고 미배정으로 돌아간다. */
  onDeleteDay: (day: number) => void;
  /** 장소의 일자를 고르는 창을 연다. */
  onAssignDay: (place: PlaceItem) => void;
  /** 일자 안에서 한 칸 위(-1)/아래(1)로 옮긴다. */
  onMovePlace: (placeId: number, direction: -1 | 1) => void;
  /** 여행할 시각을 고르는 창을 연다. */
  onEditTime: (place: PlaceItem) => void;
  /** 넣어둔 시각을 지운다. 시각이 있을 때만 불린다. */
  onClearTime: (place: PlaceItem) => void;
  /** 지도에서 장소를 아주 지운다. 확인은 부모가 받는다. */
  onDeletePlace: (place: PlaceItem) => void;
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
  totalCount,
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
  onAddDay,
  isAddingDay,
  onDeleteDay,
  onAssignDay,
  onMovePlace,
  onEditTime,
  onClearTime,
  onDeletePlace,
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
        {/*
          칩은 하나하나 shrink-0 이어야 한다. 가로 ScrollView 안이라도 기본값(shrink:1)이면
          공간이 모자랄 때 눌려서 글자가 잘린다("1day" 가 잘려 보이던 원인).
        */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} className="flex-1" contentContainerStyle={{ gap: 8, paddingVertical: 2 }}>
          <Pressable
            onPress={() => onSelectDay('all')}
            style={selectedDay === 'all' ? SELECTED_SHADOW : undefined}
            className={`shrink-0 flex-row items-center gap-1 rounded-full border-2 px-3 py-1.5 ${
              selectedDay === 'all' ? 'border-pixel-navy bg-mint-primary' : 'border-slate-200 bg-slate-100'
            }`}
          >
            <Text className={`text-xs font-black ${selectedDay === 'all' ? 'text-pixel-navy' : 'text-slate-600'}`}>전체</Text>
            <View className="rounded-full bg-white/70 px-1">
              <Text className="font-pixel text-[9px] text-slate-700">{totalCount}</Text>
            </View>
          </Pressable>

          {dayNumbers.map((d) => (
            <Pressable
              key={d}
              onPress={() => onSelectDay(d)}
              onLongPress={() => onDeleteDay(d)}
              accessibilityLabel={`${d}일차. 길게 누르면 삭제`}
              style={selectedDay === d ? SELECTED_SHADOW : undefined}
              className={`shrink-0 flex-row items-center gap-1 rounded-full border-2 px-3 py-1.5 ${
                selectedDay === d ? 'border-pixel-navy bg-mint-primary' : 'border-slate-200 bg-slate-100'
              }`}
            >
              <Text className={`font-pixel text-[10px] ${selectedDay === d ? 'text-pixel-navy' : 'text-slate-600'}`}>{d}day</Text>
              {selectedDay === d && (
                <Text className="text-[10px] font-black text-pixel-navy/50">✕</Text>
              )}
            </Pressable>
          ))}

          {/* 일자가 하나도 없으면 날짜별 보기도, 동선 최적화도 할 수 없다. 항상 맨 뒤에 둔다. */}
          <Pressable
            onPress={onAddDay}
            disabled={isAddingDay}
            accessibilityLabel="일자 추가"
            className="shrink-0 flex-row items-center gap-1 rounded-full border-2 border-dashed border-[#075953] bg-white px-3 py-1.5 active:scale-95"
          >
            {isAddingDay ? (
              <ActivityIndicator size="small" color="#075953" />
            ) : (
              <Text className="text-xs font-black text-[#075953]">+ 일자</Text>
            )}
          </Pressable>
        </ScrollView>

        <View className="flex-row items-center gap-1 pl-2">
          <Pressable
            onPress={onOptimizeRoute}
            disabled={isOptimizingRoute}
            // 조건부 className 으로 스타일 구조(opacity 유무)를 바꾸면 NativeWind 가
            // 그 렌더를 날려버린다. 흐리게 하는 건 style 로 준다.
            style={{ opacity: isOptimizingRoute ? 0.6 : 1 }}
            className="h-8 items-center justify-center rounded-lg border-2 border-pixel-navy bg-[#56EADD] px-2"
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
          places.map((place, index) => {
            const isSelected = selectedPlaceId === place.id;
            const recordsCount = teamRecordCounts[place.id] || 0;

            return (
              <React.Fragment key={place.id}>
                <Pressable
                  onPress={() => onSelectPlace(place)}
                  style={isSelected ? SELECTED_SHADOW : undefined}
                  className={`flex-row items-center justify-between gap-3 rounded-2xl border-2 p-3 ${
                    isSelected ? 'border-pixel-navy bg-[#B4FAF0]' : 'border-slate-200 bg-white'
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

                    {/* 장소명 · 주소 · 시각 · 배지 (썸네일을 뺀 만큼 글자 영역을 넓게 쓴다) */}
                    <View className="min-w-0 flex-1">
                      <Text numberOfLines={1} className="text-xs font-extrabold leading-4 text-slate-900">
                        {place.name}
                      </Text>
                      {place.address ? (
                        <Text numberOfLines={1} className="mt-0.5 text-[10px] font-semibold text-slate-500">
                          {place.address}
                        </Text>
                      ) : null}

                      {/* 여행할 시각. 누르면 고치고, 길게 누르면 지운다. */}
                      <Pressable
                        onPress={() => onEditTime(place)}
                        onLongPress={place.time ? () => onClearTime(place) : undefined}
                        accessibilityLabel={`${place.name} 시각 ${
                          place.time ? `${place.time}. 길게 누르면 지움` : '추가'
                        }`}
                        className="mt-1 flex-row items-center gap-1 self-start"
                      >
                        <Text className="text-[10px]">🕑</Text>
                        <Text
                          className={`font-pixel text-[10px] ${place.time ? 'text-[#075953]' : 'text-slate-400'}`}
                        >
                          {place.time ?? '시각 추가'}
                        </Text>
                      </Pressable>

                      <View className="mt-1.5 flex-row flex-wrap items-center gap-1.5">
                        <Text className="font-pixel text-[9px] text-amber-500">★ {place.rating}</Text>
                        <Text className="text-[10px] text-slate-300">·</Text>
                        {/* 날짜를 안 정한 핀은 day 가 0 이라 "0day" 로 보이면 안 된다. */}
                        <Pressable
                          onPress={() => onAssignDay(place)}
                          accessibilityLabel={`${place.name} 일자 지정`}
                          className={`rounded px-1.5 py-0.5 ${place.day > 0 ? 'bg-[#B4FAF0]' : 'border border-dashed border-slate-400 bg-white'}`}
                        >
                          <Text className="text-[9px] font-bold text-teal-800">
                            {place.day > 0 ? `${place.day}day` : '일자 지정'}
                          </Text>
                        </Pressable>
                        {isSelected && (
                          <View className="rounded border border-rose-200 bg-rose-50 px-1.5 py-0.5">
                            <Text className="text-[8px] font-black text-rose-600">선택됨 ✓</Text>
                          </View>
                        )}
                      </View>
                    </View>
                  </View>

                  {/* 일자 안에서만 순서를 바꿀 수 있다. 전체에는 기준이 되는 일자가 없다. */}
                  {selectedDay !== 'all' && (
                    <View className="gap-1">
                      <Pressable
                        onPress={() => onMovePlace(place.id, -1)}
                        disabled={index === 0}
                        accessibilityLabel={`${place.name} 위로`}
                        style={{
                          backgroundColor: index === 0 ? '#F1F5F9' : '#fff',
                          opacity: index === 0 ? 0.4 : 1,
                        }}
                        className="h-7 w-7 items-center justify-center rounded-lg border-2 border-pixel-navy"
                      >
                        <Text className="text-[10px] font-black text-pixel-navy">▲</Text>
                      </Pressable>
                      <Pressable
                        onPress={() => onMovePlace(place.id, 1)}
                        disabled={index === places.length - 1}
                        accessibilityLabel={`${place.name} 아래로`}
                        style={{
                          backgroundColor: index === places.length - 1 ? '#F1F5F9' : '#fff',
                          opacity: index === places.length - 1 ? 0.4 : 1,
                        }}
                        className="h-7 w-7 items-center justify-center rounded-lg border-2 border-pixel-navy"
                      >
                        <Text className="text-[10px] font-black text-pixel-navy">▼</Text>
                      </Pressable>
                    </View>
                  )}

                  {/* 팀 기록 · 바로 촬영 */}
                  <View className="items-end gap-1">
                    <View className="flex-row items-center gap-1">
                      {/* 장소 삭제. 되돌릴 수 없어서 누르면 확인을 한 번 받는다. */}
                      <Pressable
                        onPress={() => onDeletePlace(place)}
                        accessibilityLabel={`${place.name} 삭제`}
                        hitSlop={6}
                        className="h-7 w-7 items-center justify-center rounded-xl border border-rose-300 bg-white active:bg-rose-50"
                      >
                        <Text className="text-[11px] font-black text-rose-500">✕</Text>
                      </Pressable>
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
                    </View>
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
