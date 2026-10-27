/**
 * 여행 상세: 인터랙티브 지도 + 조절형 바텀시트
 * (지금은 웹 디자인과 같은 픽셀 일러스트 지도. 카카오맵 WebView 로 교체할 자리)
 */
import React, { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { PlaceItem } from '../../../data/mockData';
import { PixelBackArrow, PixelPin, PixelTripsIcon } from '../../../components/pixel/PixelIcons';
import { AnimatedLoop } from '../../../components/pixel/Primitives';
import TripsBottomSheet from '../components/TripsBottomSheet';
import { useApp } from '../AppContext';

/** 좌표(%) 중심에 맞춰 자기 크기의 절반만큼 이동하는 마커 */
function MapMarker({
  place,
  focused,
  width,
  height,
  onPress,
}: {
  place: PlaceItem;
  focused: boolean;
  width: number;
  height: number;
  onPress: () => void;
}) {
  const [size, setSize] = useState({ w: 0, h: 0 });
  const x = (place.lng / 100) * width;
  const y = (place.lat / 100) * height;
  return (
    <Pressable
      onPress={onPress}
      onLayout={(e) => setSize({ w: e.nativeEvent.layout.width, h: e.nativeEvent.layout.height })}
      style={{
        position: 'absolute',
        left: x - size.w / 2,
        top: y - size.h / 2,
        zIndex: focused ? 25 : 20,
        opacity: size.w ? 1 : 0,
        transform: [{ scale: focused ? 1.25 : 1 }],
      }}
    >
      <PixelPin
        size={focused ? 32 : 26}
        color={focused ? '#FF5A87' : '#075953'}
        label={`${place.order}. ${place.name.replace(/^\d+\.\s*/, '')}`}
        active={focused}
      />
    </Pressable>
  );
}

export default function TripMapView() {
  const {
    activeTrip,
    selectedDayFilter,
    setSelectedDayFilter,
    displayedMapPlaces,
    mapPlaces,
    isMapPlacesLoading,
    mapPlacesError,
    tripDayNumbers,
    reloadItinerary,
    handleAddDay,
    isAddingDay,
    selectedMapPin,
    setSelectedMapPin,
    setIsTripMapOpen,
    setInviteTrip,
    setIsAddPlaceOpen,
    setIsPinLogMakerOpen,
    setPinLogTrip,
    setIsPlaceDetailModalOpen,
    setStartPlaceCamera,
    placeRecords,
    tripsSheetHeight,
    setTripsSheetHeight,
    handleOptimizeRoute,
    isOptimizingRoute,
    showToast,
  } = useApp();
  const [layout, setLayout] = useState({ width: 0, height: 0 });

  const routePath =
    displayedMapPlaces.length >= 2
      ? displayedMapPlaces.reduce((acc, p, idx) => {
          const x = (p.lng / 100) * 400;
          const y = (p.lat / 100) * 650;
          return idx === 0 ? `M ${x},${y}` : `${acc} L ${x},${y}`;
        }, '')
      : '';

  const teamRecordCounts = placeRecords.reduce((acc, r) => {
    acc[r.pinId] = (acc[r.pinId] || 0) + 1;
    return acc;
  }, {} as { [key: number]: number });

  const focusX = ((selectedMapPin?.lng ?? 50) / 100) * layout.width;
  const focusY = ((selectedMapPin?.lat ?? 50) / 100) * layout.height;

  return (
    <View
      className="relative flex-1 overflow-hidden"
      onLayout={(e) => setLayout({ width: e.nativeEvent.layout.width, height: e.nativeEvent.layout.height })}
    >
      {/* 지도 배경 */}
      <View className="absolute inset-0 overflow-hidden bg-[#B4FAF0]/40">
        {layout.width > 0 && (
          <Svg width={layout.width} height={layout.height} viewBox="0 0 400 650" opacity={0.7}>
            {/* 바다 & 해안선 */}
            <Path d="M 0,0 L 400,0 L 400,100 Q 280,160 170,110 Q 60,70 0,140 Z" fill="#82F0DC" />
            {/* 도로 */}
            <Path d="M 20,120 Q 110,240 210,290 T 370,480" fill="none" stroke="#FFFFFF" strokeWidth={14} />
            <Path d="M 20,120 Q 110,240 210,290 T 370,480" fill="none" stroke="#50E6DC" strokeWidth={4} />
            <Path d="M 190,40 L 230,580" fill="none" stroke="#FFFFFF" strokeWidth={10} />
            <Path d="M 190,40 L 230,580" fill="none" stroke="#82F0DC" strokeWidth={3} />
            <Path d="M 40,320 Q 180,360 360,260" fill="none" stroke="#FFFFFF" strokeWidth={8} />
            {/* 장소 연결 점선 경로 */}
            {routePath ? (
              <Path d={routePath} fill="none" stroke="#FF5A87" strokeWidth={3.5} strokeDasharray="6 4" />
            ) : null}
          </Svg>
        )}

        {/* 포커스된 핀 레이더. 고른 핀이 지금 지도에 없으면(빈 일정 등) 그리지 않는다. */}
        {layout.width > 0 && displayedMapPlaces.some((p) => p.id === selectedMapPin?.id) && (
          <View
            pointerEvents="none"
            style={{ position: 'absolute', left: focusX - 24, top: focusY - 24, width: 48, height: 48, zIndex: 10 }}
          >
            <AnimatedLoop kind="ping" style={{ position: 'absolute', left: -8, top: -8 }}>
              <View className="h-16 w-16 rounded-full bg-[#FF5A87]/25" />
            </AnimatedLoop>
            <AnimatedLoop kind="spin">
              <View className="h-12 w-12 rounded-full border-2 border-dashed border-[#FF5A87]" />
            </AnimatedLoop>
          </View>
        )}

        {/* 픽셀 핀 */}
        {layout.width > 0 &&
          displayedMapPlaces.map((place) => (
            <MapMarker
              key={place.id}
              place={place}
              focused={selectedMapPin?.id === place.id}
              width={layout.width}
              height={layout.height}
              onPress={() => {
                setSelectedMapPin(place);
                showToast(`📍 [${place.name}] 마커로 이동했습니다.`);
              }}
            />
          ))}

        {/* 상단 플로팅 바 */}
        <View className="absolute left-3 right-3 top-3 z-20 flex-row items-center gap-2">
          <Pressable
            onPress={() => setIsTripMapOpen(false)}
            accessibilityLabel="내 여행 목록으로 돌아가기"
            className="h-9 w-9 items-center justify-center rounded-2xl border border-[#82F0DC] bg-white shadow-md active:scale-90"
          >
            <PixelBackArrow size={16} />
          </Pressable>
          <View className="flex-1 flex-row items-center justify-between rounded-2xl border border-[#82F0DC] bg-white/95 px-3.5 py-2 shadow-md">
            <View className="min-w-0 flex-1 flex-row items-center gap-2">
              <PixelTripsIcon size={20} active />
              <Text numberOfLines={1} className="flex-1 text-xs font-bold text-slate-800">
                {activeTrip?.title ?? "여행"}
              </Text>
            </View>
            <View className="rounded border border-[#50E6DC] bg-[#56EADD] px-1.5 py-0.5">
              <Text className="font-pixel text-[9px] text-slate-900">
                {selectedDayFilter === 'all' ? '전체' : `${selectedDayFilter}DAY`}
              </Text>
            </View>
          </View>
          <Pressable
            onPress={() => setIsAddPlaceOpen(true)}
            accessibilityLabel="장소 추가"
            className="rounded-2xl border-2 border-pixel-navy bg-white px-2.5 py-2 shadow-md active:translate-y-0.5"
          >
            <Text className="text-[10px] font-black text-[#075953]">장소 +</Text>
          </Pressable>
          <Pressable
            onPress={() => setInviteTrip(activeTrip)}
            className="rounded-2xl border-2 border-pixel-navy bg-white px-2.5 py-2 shadow-md active:translate-y-0.5"
          >
            <Text className="text-[10px] font-black text-[#075953]">팀원 +</Text>
          </Pressable>
          <Pressable
            onPress={() => {
              // PinLog 모달은 pinLogTrip 을 보고 그린다. 지도에서 바로 열 때는
              // 핀로그 탭을 거치지 않으므로 지금 보고 있는 여행으로 맞춰준다.
              if (activeTrip) setPinLogTrip(activeTrip);
              setIsPinLogMakerOpen(true);
            }}
            accessibilityLabel="팀원 영상으로 PinLog 만들기"
            className="flex-row items-center gap-1 rounded-2xl border-2 border-pixel-navy bg-mint-primary px-3 py-2 shadow-md active:translate-y-0.5 active:bg-mint-dark"
          >
            <Text className="text-xs font-black text-pixel-navy">🎬 PinLog</Text>
          </Pressable>
        </View>
      </View>

      {/* 조절형 바텀시트 */}
      <TripsBottomSheet
        containerHeight={layout.height}
        sheetHeightPercent={tripsSheetHeight}
        onHeightChange={setTripsSheetHeight}
        selectedDay={selectedDayFilter}
        onSelectDay={(day) => {
          setSelectedDayFilter(day);
          const matching = day === 'all' ? mapPlaces : mapPlaces.filter((p) => p.day === day);
          if (matching.length > 0) setSelectedMapPin(matching[0]);
        }}
        places={displayedMapPlaces}
        dayNumbers={tripDayNumbers}
        isLoading={isMapPlacesLoading}
        loadError={mapPlacesError}
        onRetry={reloadItinerary}
        onAddDay={handleAddDay}
        isAddingDay={isAddingDay}
        selectedPlaceId={selectedMapPin?.id ?? -1}
        onSelectPlace={(place) => {
          setSelectedMapPin(place);
          showToast(`📍 [${place.name}] 핀으로 카메라가 이동했습니다.`);
        }}
        onOpenTeamRecord={(place) => {
          setSelectedMapPin(place);
          setStartPlaceCamera(false);
          setIsPlaceDetailModalOpen(true);
        }}
        onOpenCamera={(place) => {
          setSelectedMapPin(place);
          setStartPlaceCamera(true);
          setIsPlaceDetailModalOpen(true);
        }}
        teamRecordCounts={teamRecordCounts}
        onOptimizeRoute={handleOptimizeRoute}
        isOptimizingRoute={isOptimizingRoute}
      />
    </View>
  );
}
