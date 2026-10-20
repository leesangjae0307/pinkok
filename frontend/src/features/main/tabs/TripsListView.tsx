import React from 'react';
import { Image, Pressable, Text, TextInput, View } from 'react-native';

import { PixelExtractIcon, PixelHotAirBalloon, PixelTripsIcon } from '../../../components/pixel/PixelIcons';
import {
  FadeIn,
  LinearGradient,
  PixelPressable,
  PixelSelect,
  ShadowBox,
} from '../../../components/pixel/Primitives';
import { TRAVEL_STYLES } from '../../../constants';
import { avatarSilverBucket, useApp } from '../AppContext';

/** 목업 date 문자열('2026.10.12 - 10.15')에서 일수를 계산한다. 서버 연동 시 startDate/endDate 로 대체. */
function tripDayCount(date: string): number {
  const nums = date.match(/\d+/g)?.map(Number) ?? [];
  if (nums.length < 5) return 1;
  const [year, startMonth, startDay, endMonth, endDay] = nums;
  const diff = new Date(year, endMonth - 1, endDay).getTime() - new Date(year, startMonth - 1, startDay).getTime();
  return Math.round(diff / 86400000) + 1;
}

export default function TripsListView() {
  const {
    trips,
    filteredTrips,
    tripSearchQuery,
    setTripSearchQuery,
    tripInvitations,
    focusedInvitationId,
    handleAcceptInvitation,
    handleRejectInvitation,
    setActiveTrip,
    setIsTripMapOpen,
    setInviteTrip,
    selectedProfileAvatar,
    recommendationPeople,
    setRecommendationPeople,
    recommendationStyle,
    setRecommendationStyle,
    recommendationRegion,
    setRecommendationRegion,
    handleAiTripRecommendation,
  } = useApp();
  const [searchFocused, setSearchFocused] = React.useState(false);

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 20 }}>
      {/* 헤더 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2.5">
          <View className="h-10 w-10 items-center justify-center">
            <PixelTripsIcon size={34} active />
          </View>
          <View>
            <Text className="font-pixel text-lg text-[#0f6861]">MY TRIPS</Text>
            <Text className="text-[11px] font-semibold text-slate-600">초대받은 여행과 내 여행을 관리해요</Text>
          </View>
        </View>
        <View className="rounded-xl border border-[#82F0DC] bg-white px-2.5 py-1">
          <Text className="font-pixel text-[9px] text-[#075953]">{trips.length} TRIPS</Text>
        </View>
      </View>

      {/* 검색 */}
      <View
        className={`h-12 flex-row items-center gap-2 rounded-2xl border-2 bg-white px-3 shadow-sm ${
          searchFocused ? 'border-[#075953]' : 'border-[#82F0DC]'
        }`}
      >
        <Text>🔍</Text>
        <TextInput
          value={tripSearchQuery}
          onChangeText={setTripSearchQuery}
          onFocus={() => setSearchFocused(true)}
          onBlur={() => setSearchFocused(false)}
          placeholder="여행 제목으로 검색 ex) 부산, 제주도"
          placeholderTextColor="#94A3B8"
          className="flex-1 p-0 text-xs font-bold text-slate-800"
        />
        {tripSearchQuery ? (
          <Pressable onPress={() => setTripSearchQuery('')} accessibilityLabel="여행 검색어 지우기">
            <Text className="text-xs font-black text-slate-400">✕</Text>
          </Pressable>
        ) : null}
      </View>

      {tripInvitations.length > 0 && (
        <View
          className={`gap-2 rounded-3xl bg-white/90 p-3.5 shadow-md ${
            focusedInvitationId ? 'border-4 border-[#FF5A87]' : 'border-2 border-[#56EADD]'
          }`}
        >
          <View className="flex-row items-center justify-between px-1">
            <View className="flex-row items-center gap-2">
              <View className="h-2.5 w-2.5 rounded-sm bg-[#FF5A87]" />
              <Text className="text-sm font-extrabold text-slate-900">받은 초대</Text>
            </View>
            <View className="rounded-lg bg-[#B4FAF0] px-2 py-1">
              <Text className="font-pixel text-[8px] text-[#075953]">{tripInvitations.length} NEW</Text>
            </View>
          </View>

          {tripInvitations.map((inv) => (
            <View key={inv.id} className="rounded-2xl border border-[#82F0DC] bg-[#B4FAF0]/45 p-3">
              <View className="flex-row items-center gap-3">
                <ShadowBox
                  shadowColor="#56EADD"
                  offset={2}
                  radius={12}
                  className="h-11 w-11 items-center justify-center rounded-xl border-2 border-[#075953] bg-white"
                >
                  <Text className="text-xl">{inv.inviterAvatar}</Text>
                </ShadowBox>
                <View className="min-w-0 flex-1">
                  <Text numberOfLines={1} className="text-xs font-extrabold text-slate-800">
                    <Text className="text-[#08736d]">'{inv.inviterName}'</Text>님의 <Text>'{inv.tripTitle}'</Text>
                  </Text>
                  <Text className="mt-1 text-[10px] font-semibold text-slate-500">{inv.date} · 함께 여행을 계획해요</Text>
                </View>
              </View>
              <View className="mt-3 flex-row gap-2">
                <PixelPressable
                  shadowColor="#075953"
                  offset={2}
                  radius={12}
                  wrapperClassName="flex-1"
                  className="h-10 items-center justify-center rounded-xl border-2 border-[#075953] bg-[#56EADD]"
                  onPress={() => handleAcceptInvitation(inv)}
                >
                  <Text className="font-pixel text-[9px] text-slate-900">수락</Text>
                </PixelPressable>
                <PixelPressable
                  shadowColor="#94A3B8"
                  offset={2}
                  radius={12}
                  wrapperClassName="flex-1"
                  className="h-10 items-center justify-center rounded-xl border-2 border-slate-400 bg-slate-100"
                  onPress={() => handleRejectInvitation(inv)}
                >
                  <Text className="font-pixel text-[9px] text-slate-600">거절</Text>
                </PixelPressable>
              </View>
            </View>
          ))}
        </View>
      )}

      <View className="gap-3">
        <View className="flex-row items-center justify-between px-1">
          <View className="flex-row items-center gap-2">
            <PixelHotAirBalloon size={18} active />
            <Text className="text-sm font-extrabold text-slate-900">내 여행</Text>
          </View>
          <Text className="text-[10px] font-bold text-slate-500">{filteredTrips.length}개의 여행</Text>
        </View>

        <View className="gap-3">
          {filteredTrips.map((trip) => {
            const days = `${tripDayCount(trip.date)} days`;
            const openTrip = () => {
              setActiveTrip(trip);
              setIsTripMapOpen(true);
            };
            return (
              <View
                key={trip.id}
                className="w-full flex-row items-center gap-3 rounded-3xl border border-[#82F0DC] bg-white p-3 shadow-sm"
              >
                <Pressable
                  onPress={openTrip}
                  className="relative h-20 w-20 overflow-hidden rounded-2xl border border-slate-100 active:scale-95"
                >
                  <Image source={{ uri: trip.image }} resizeMode="cover" className="h-full w-full" />
                  <View className="absolute left-1 top-1 rounded-md bg-slate-900/80 px-1.5 py-0.5">
                    <Text className="font-pixel text-[8px] text-[#FFC837]">{trip.dDay}</Text>
                  </View>
                </Pressable>

                <View className="min-w-0 flex-1">
                  <Pressable onPress={openTrip}>
                    <Text numberOfLines={1} className="text-sm font-black text-slate-900">
                      {trip.title}
                    </Text>
                  </Pressable>

                  <View className="mt-1.5 flex-row items-center gap-1 overflow-hidden">
                    <View className="flex-shrink rounded-lg bg-[#B4FAF0] px-1.5 py-1">
                      <Text numberOfLines={1} className="text-[8px] font-bold text-[#075953]">
                        📅 {trip.date}
                      </Text>
                    </View>
                    <Text className="text-[8px] text-slate-300">·</Text>
                    <Text className="text-[8px] font-bold text-slate-500">📍 장소 {trip.placesCount}개</Text>
                    <Text className="text-[8px] text-slate-300">·</Text>
                    <Text className="text-[8px] font-bold text-slate-500">⏱️ {days}</Text>
                  </View>

                  <View className="mt-1.5 flex-row justify-end">
                    <Pressable
                      onPress={() => setInviteTrip(trip)}
                      accessibilityLabel={`${trip.title} 팀원 관리`}
                      className="flex-row items-center gap-1.5 rounded-lg border border-[#075953] bg-[#56EADD] px-2 py-1 active:scale-95"
                    >
                      <View className="flex-row items-center">
                        <Image source={selectedProfileAvatar.image} className="h-4 w-4 rounded border border-white" />
                        <Image source={avatarSilverBucket} className="-ml-1 h-4 w-4 rounded border border-white" />
                      </View>
                      <Text className="text-[8px] font-black text-[#075953]">팀원 추가·수정</Text>
                    </Pressable>
                  </View>
                </View>
              </View>
            );
          })}
          {filteredTrips.length === 0 && (
            <View className="rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <Text className="text-center text-xs font-bold text-slate-400">
                '{tripSearchQuery}'와 일치하는 여행이 없어요.
              </Text>
            </View>
          )}
        </View>
      </View>

      {/* 맞춤형 AI 여행지 추천 */}
      <ShadowBox shadowColor="#075953" offset={4} radius={24} className="overflow-hidden rounded-3xl border-2 border-[#075953]">
        <LinearGradient colors={['#56EADD', '#B4FAF0']} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={{ padding: 16 }}>
          <View className="flex-row items-center justify-between">
            <View>
              <Text className="font-pixel text-[9px] text-[#075953]">PINKOK AI TRIP</Text>
              <Text className="mt-1 text-sm font-black text-slate-900">맞춤형 AI 여행지 추천</Text>
            </View>
            <PixelExtractIcon size={42} active />
          </View>
          <Text className="mt-1 text-[10px] font-semibold text-[#075953]">
            인원과 취향을 고르면 어울리는 장소를 한 번에 찾아드려요.
          </Text>
          <View className="mt-3 flex-row gap-2">
            <PixelSelect
              className="flex-1"
              accessibilityLabel="여행 인원"
              value={recommendationPeople}
              onChange={setRecommendationPeople}
              options={[
                { value: '1', label: '1명' },
                { value: '2', label: '2명' },
                { value: '3', label: '3명' },
                { value: '4', label: '4명+' },
              ]}
            />
            <PixelSelect
              className="flex-1"
              accessibilityLabel="여행 스타일"
              value={recommendationStyle}
              onChange={setRecommendationStyle}
              options={TRAVEL_STYLES.map((s) => ({ value: s.code, label: s.label }))}
            />
            <PixelSelect
              className="flex-1"
              accessibilityLabel="여행 지역"
              value={recommendationRegion}
              onChange={setRecommendationRegion}
              options={['제주', '부산', '서울', '강릉'].map((v) => ({ value: v, label: v }))}
            />
          </View>
          <Pressable
            onPress={handleAiTripRecommendation}
            className="mt-3 h-10 w-full items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-900 active:translate-y-0.5"
          >
            <Text className="text-xs font-black text-[#56EADD]">AI 추천 장소 추출하기</Text>
          </Pressable>
        </LinearGradient>
      </ShadowBox>
    </FadeIn>
  );
}
