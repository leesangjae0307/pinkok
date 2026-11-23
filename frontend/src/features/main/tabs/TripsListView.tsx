import React from 'react';
import { ActivityIndicator, Image, Pressable, Text, TextInput, View } from 'react-native';

import { PixelExtractIcon, PixelHotAirBalloon, PixelTripsIcon } from '../../../components/pixel/PixelIcons';
import {
  FadeIn,
  LinearGradient,
  PixelPressable,
  PixelSelect,
  ShadowBox,
} from '../../../components/pixel/Primitives';
import { TRAVEL_STYLES } from '../../../constants';
import { isImageAvatar, toImageSource } from '../../../data/mockData';
import { profileAvatarSource, useApp } from '../AppContext';

/** 목업 date 문자열('2026.10.12 - 10.15')에서 일수를 계산한다. 서버 연동 시 startDate/endDate 로 대체. */
function tripDayCount(date: string): number {
  const nums = date.match(/\d+/g)?.map(Number) ?? [];
  if (nums.length < 5) {return 1;}
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
    openTrip,
    handleDeleteTrip,
    deletingTripId,
    setEditingTrip,
    isTripsLoading,
    tripsError,
    reloadTrips,
    setInviteTrip,
    setIsCreateTripOpen,
    selectedProfileAvatar,
    recommendationPeople,
    setRecommendationPeople,
    recommendationStyle,
    setRecommendationStyle,
    recommendationRegion,
    setRecommendationRegion,
    isRecommending,
    recommendationError,
    hasRequestedRecommendation,
    handleAiTripRecommendation,
    recommendations,
    handleAcceptRecommendation,
    handleRejectRecommendation,
  } = useApp();
  const [searchFocused, setSearchFocused] = React.useState(false);

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 20 }}>
      {/* 헤더 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2.5">
          <View className="h-10 w-10 items-center justify-center">
            <PixelTripsIcon size={30} active />
          </View>
          <View>
            <Text className="font-pixel text-xl tracking-tight text-[#0f6861]">TRIPS</Text>
            <Text className="-mt-1 text-[11px] font-semibold text-slate-600">초대받은 여행과 내 여행목록 관리</Text>
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

      {/* 우측 하단 + 버튼과 같은 생성 화면을 연다. 여행이 하나도 없을 때도 여기서 바로 시작한다. */}
      <PixelPressable
        shadowColor="#075953"
        offset={3}
        radius={16}
        accessibilityLabel="새 여행 만들기"
        onPress={() => setIsCreateTripOpen(true)}
        wrapperClassName="-mt-2 w-full"
        className="h-12 w-full flex-row items-center justify-center gap-2 rounded-2xl border-2 border-[#075953] bg-[#56EADD]"
      >
        <Text className="font-pixel text-base text-[#075953]">+</Text>
        <Text className="text-sm font-extrabold text-slate-900">새 여행 만들기</Text>
      </PixelPressable>

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
                  className="h-11 w-11 items-center justify-center overflow-hidden rounded-xl border-2 border-[#075953] bg-white"
                >
                  {/* 아바타는 그림일 수도 이모지일 수도 있다. 그림을 Text 로 그리면 주소가 그대로 찍힌다. */}
                  {isImageAvatar(inv.inviterAvatar) ? (
                    <Image source={toImageSource(inv.inviterAvatar)} resizeMode="cover" className="h-full w-full" />
                  ) : (
                    <Text className="text-xl">{inv.inviterAvatar}</Text>
                  )}
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
            const open = () => openTrip(trip);
            return (
              <View
                key={trip.id}
                className="w-full flex-row items-center gap-3 rounded-3xl border border-[#82F0DC] bg-white p-3 shadow-sm"
              >
                <Pressable
                  onPress={open}
                  className="relative h-20 w-20 overflow-hidden rounded-2xl border border-slate-100 active:scale-95"
                >
                  <Image source={toImageSource(trip.image)} resizeMode="cover" className="h-full w-full" />
                  <View className="absolute left-1 top-1 rounded-md bg-slate-900/80 px-1.5 py-0.5">
                    <Text className="font-pixel text-[8px] text-[#FFC837]">{trip.dDay}</Text>
                  </View>
                </Pressable>

                <View className="min-w-0 flex-1">
                  <Pressable onPress={open}>
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

                  {/* 해시태그. 카드가 길어지지 않게 세 개까지만 보여 준다. */}
                  {trip.tags.length > 0 && (
                    <View className="mt-1.5 flex-row flex-wrap items-center gap-1">
                      {trip.tags.slice(0, 3).map((tag) => (
                        <View key={tag} className="rounded-full bg-[#E8FFFB] px-2 py-0.5">
                          <Text className="text-[8px] font-bold text-teal-800">#{tag}</Text>
                        </View>
                      ))}
                      {trip.tags.length > 3 && (
                        <Text className="text-[8px] font-bold text-slate-400">
                          +{trip.tags.length - 3}
                        </Text>
                      )}
                    </View>
                  )}

                  <View className="mt-1.5 flex-row items-center justify-end gap-1.5">
                    {trip.isOwner && (
                      <Pressable
                        onPress={() => setEditingTrip(trip)}
                        accessibilityLabel={`${trip.title} 수정`}
                        className="h-[26px] w-[26px] items-center justify-center rounded-lg border border-[#82F0DC] bg-white active:scale-95"
                      >
                        <Text className="text-[11px]">✏️</Text>
                      </Pressable>
                    )}
                    {trip.isOwner && (
                      <Pressable
                        onPress={() => handleDeleteTrip(trip)}
                        disabled={deletingTripId === trip.id}
                        accessibilityLabel={`${trip.title} 삭제`}
                        className="h-[26px] w-[26px] items-center justify-center rounded-lg border border-rose-300 bg-rose-50 active:scale-95"
                      >
                        {deletingTripId === trip.id ? (
                          <ActivityIndicator size="small" color="#e11d48" />
                        ) : (
                          <Text className="text-[11px]">🗑️</Text>
                        )}
                      </Pressable>
                    )}
                    <Pressable
                      onPress={() => setInviteTrip(trip)}
                      accessibilityLabel={`${trip.title} 팀원 관리`}
                      className="flex-row items-center gap-1.5 rounded-lg border border-[#075953] bg-[#56EADD] px-2 py-1 active:scale-95"
                    >
                      {/*
                        내 얼굴만 보여 준다. 전에는 옆에 은발 버킷햇 아바타를 박아
                        두어, 그 사람이 팀원인 것처럼 모든 여행에 똑같이 나왔다.
                        여행 목록 응답에는 팀원이 들어 있지 않아 실제 얼굴을 알 수
                        없으므로, 모르는 것을 지어내는 대신 비운다.
                      */}
                      <Image
                        source={toImageSource(profileAvatarSource(selectedProfileAvatar))}
                        className="h-4 w-4 rounded border border-white"
                      />
                      <Text className="text-[8px] font-black text-[#075953]">팀원 추가·수정</Text>
                    </Pressable>
                  </View>
                </View>
              </View>
            );
          })}
          {isTripsLoading ? (
            <View className="items-center rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <ActivityIndicator color="#075953" />
            </View>
          ) : tripsError ? (
            <View className="items-center gap-3 rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <Text className="text-center text-xs font-bold text-slate-500">{tripsError}</Text>
              <Pressable onPress={reloadTrips} className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95">
                <Text className="text-xs font-black text-[#075953]">다시 시도</Text>
              </Pressable>
            </View>
          ) : filteredTrips.length === 0 ? (
            <View className="rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <Text className="text-center text-xs font-bold text-slate-400">
                {tripSearchQuery.trim()
                  ? `'${tripSearchQuery}'와 일치하는 여행이 없어요.`
                  : '아직 만든 여행이 없어요. 새 여행을 만들어 보세요.'}
              </Text>
            </View>
          ) : null}
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
          </View>

          {/* 지역은 고른 목록에 매이지 않게 직접 적는다. "부산 해운대" 처럼 세세하게도 적을 수 있다. */}
          <TextInput
            value={recommendationRegion}
            onChangeText={setRecommendationRegion}
            placeholder="여행할 지역을 입력해 주세요"
            placeholderTextColor="#5D9C96"
            returnKeyType="done"
            onSubmitEditing={handleAiTripRecommendation}
            accessibilityLabel="여행 지역"
            className="mt-2 h-10 rounded-xl border-2 border-[#075953] bg-white px-3 text-xs font-bold text-slate-900"
          />

          <Pressable
            onPress={handleAiTripRecommendation}
            disabled={isRecommending}
            // 못 누르는 상태를 className 으로 바꾸면 NativeWind 가 그 렌더를 날려버린다.
            style={{ opacity: isRecommending ? 0.6 : 1 }}
            className="mt-3 h-10 w-full flex-row items-center justify-center gap-2 rounded-xl border-2 border-pixel-navy bg-slate-900 active:translate-y-0.5"
          >
            {isRecommending ? <ActivityIndicator size="small" color="#56EADD" /> : null}
            <Text className="text-xs font-black text-[#56EADD]">
              {isRecommending ? 'AI 가 장소를 고르는 중…' : 'AI 추천 장소 추출하기'}
            </Text>
          </Pressable>

          {/* 돌아가는 중 */}
          {isRecommending ? (
            <View className="mt-3 items-center gap-2 rounded-2xl border-2 border-dashed border-[#075953] bg-white/70 p-5">
              <ActivityIndicator color="#075953" />
              <Text className="text-[10px] font-bold text-[#075953]">
                취향에 맞는 장소를 찾고 있어요. 잠시만요.
              </Text>
            </View>
          ) : null}

          {/* 실패. 다시 누르면 그대로 재시도된다. */}
          {!isRecommending && recommendationError ? (
            <View className="mt-3 gap-2 rounded-2xl border-2 border-rose-300 bg-white p-4">
              <Text className="text-[11px] font-bold leading-4 text-rose-700">{recommendationError}</Text>
              <Pressable
                onPress={handleAiTripRecommendation}
                className="self-start rounded-lg border-2 border-[#075953] bg-[#56EADD] px-3 py-1.5 active:scale-95"
              >
                <Text className="text-[10px] font-black text-[#075953]">다시 시도</Text>
              </Pressable>
            </View>
          ) : null}

          {/* 돌렸는데 건질 게 없었던 경우. 처음 화면과 구분한다. */}
          {!isRecommending && !recommendationError && hasRequestedRecommendation && recommendations.length === 0 ? (
            <View className="mt-3 rounded-2xl border-2 border-dashed border-[#075953] bg-white/70 p-5">
              <Text className="text-center text-[10px] font-bold text-[#075953]">
                추천할 만한 장소를 찾지 못했어요. 지역이나 취향을 바꿔 다시 해보세요.
              </Text>
            </View>
          ) : null}

          {/* 추천 결과. 수락하면 지금 보고 있는 여행의 일정에 핀으로 들어간다. */}
          {!isRecommending && recommendations.length > 0 && (
            <View className="mt-3 gap-2">
              {recommendations.map((rec) => (
                <View key={rec.id} className="rounded-2xl border-2 border-[#075953] bg-white p-3">
                  <Text className="text-xs font-black text-slate-900">{rec.name}</Text>
                  {rec.address ? (
                    <Text numberOfLines={1} className="mt-0.5 text-[9px] font-bold text-slate-500">
                      📍 {rec.address}
                    </Text>
                  ) : null}
                  {rec.reason ? (
                    <Text className="mt-1 text-[10px] font-semibold leading-4 text-slate-600">{rec.reason}</Text>
                  ) : null}
                  <View className="mt-2 flex-row justify-end gap-1.5">
                    <Pressable
                      onPress={() => handleRejectRecommendation(rec.id)}
                      className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 active:scale-95"
                    >
                      <Text className="text-[9px] font-black text-slate-500">거절</Text>
                    </Pressable>
                    <Pressable
                      onPress={() => handleAcceptRecommendation(rec.id)}
                      className="rounded-lg border border-[#075953] bg-[#56EADD] px-2.5 py-1.5 active:scale-95"
                    >
                      <Text className="text-[9px] font-black text-[#075953]">일정에 추가</Text>
                    </Pressable>
                  </View>
                </View>
              ))}
            </View>
          )}
        </LinearGradient>
      </ShadowBox>
    </FadeIn>
  );
}
