/**
 * 여행 상세: 인터랙티브 지도 + 조절형 바텀시트
 * 지도는 카카오맵 JavaScript SDK 를 WebView 로 띄운다.
 */
import React, { useMemo, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import KakaoMapView, { type MapMarker } from '../../../components/map/KakaoMapView';
import TripsBottomSheet from '../components/TripsBottomSheet';
import { useApp } from '../AppContext';


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
    handleDeleteDay,
    setAssigningPlace,
    handleMovePlace,
    setEditingTimePlace,
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

  const teamRecordCounts = placeRecords.reduce((acc, r) => {
    acc[r.pinId] = (acc[r.pinId] || 0) + 1;
    return acc;
  }, {} as { [key: number]: number });

  /** 지도에 넘길 마커. 라벨이 길면 말풍선이 화면을 덮어서 줄인다. */
  const mapMarkers: MapMarker[] = useMemo(
    () =>
      displayedMapPlaces.map((p) => ({
        id: p.id,
        label: p.name.length > 10 ? `${p.name.slice(0, 10)}…` : p.name,
        latitude: p.lat,
        longitude: p.lng,
        order: p.order,
      })),
    [displayedMapPlaces],
  );

  return (
    <View
      className="relative flex-1 overflow-hidden"
      onLayout={(e) => setLayout({ width: e.nativeEvent.layout.width, height: e.nativeEvent.layout.height })}
    >
      {/* 카카오맵 */}
      <KakaoMapView
        markers={mapMarkers}
        selectedId={selectedMapPin?.id ?? null}
        bottomInset={Math.round((layout.height * tripsSheetHeight) / 100)}
        // 상단 플로팅 바(top-3 + 버튼 높이)에 마커 말풍선이 가리지 않게 한다.
        topInset={72}
      />

      {/* 지도 위에 띄우는 조작부. 지도 제스처를 막지 않도록 빈 곳은 터치를 흘려보낸다. */}
      <View className="absolute bottom-0 left-0 right-0 top-0" pointerEvents="box-none">
        {/* 상단 플로팅 바 */}
        <View className="absolute left-3 right-3 top-3 z-20 flex-row items-center gap-2">
          <Pressable
            onPress={() => setIsTripMapOpen(false)}
            accessibilityLabel="내 여행 목록으로 돌아가기"
            className="h-9 w-9 items-center justify-center rounded-2xl border border-[#82F0DC] bg-white shadow-md active:scale-90"
          >
            <PixelBackArrow size={16} />
          </Pressable>
          {/*
            이 알약은 고정폭 버튼 4개(뒤로·장소·팀원·PinLog)와 한 줄을 나눠 써서 남는 폭이
            아주 좁다. 아이콘까지 넣으면 제목이 0 으로 눌리고도 모자라 아이콘이 배지 뒤로
            밀려 깨졌다(RN 은 flexShrink 기본값이 0 이라 shrink 유틸로는 해결되지 않는다).
            그래서 장식인 아이콘을 빼고 정보인 제목과 배지만 남긴다.
          */}
          <View className="min-w-0 flex-1 flex-row items-center justify-between rounded-2xl border border-[#82F0DC] bg-white/95 px-3 py-2 shadow-md">
            <Text numberOfLines={1} className="min-w-0 flex-1 text-xs font-bold text-slate-800">
              {activeTrip?.title ?? '여행'}
            </Text>
            <View className="ml-1.5 rounded border border-[#50E6DC] bg-[#56EADD] px-1.5 py-0.5">
              <Text className="font-pixel text-[9px] text-slate-900">
                {selectedDayFilter === 'all' ? '전체' : `${selectedDayFilter}D`}
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
        totalCount={mapPlaces.length}
        dayNumbers={tripDayNumbers}
        isLoading={isMapPlacesLoading}
        loadError={mapPlacesError}
        onRetry={reloadItinerary}
        onAddDay={handleAddDay}
        isAddingDay={isAddingDay}
        onDeleteDay={handleDeleteDay}
        onAssignDay={setAssigningPlace}
        onMovePlace={handleMovePlace}
        onEditTime={setEditingTimePlace}
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
