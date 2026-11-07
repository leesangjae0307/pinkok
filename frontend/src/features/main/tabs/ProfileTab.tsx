/**
 * 프로필 탭.
 *
 * 숫자·테마·브이로그는 모두 내 여행에서 센 값이다(AppContext 의 profileStats).
 * 전에는 87/156/12 같은 고정 숫자와 샘플 사진이 박혀 있어서, 계정이 비어 있어도
 * 남의 기록처럼 보였다.
 */
import React from 'react';
import { ActivityIndicator, Image, Pressable, Text, View } from 'react-native';

import { PixelProfileIcon } from '../../../components/pixel/PixelIcons';
import { FadeIn, PixelPressable } from '../../../components/pixel/Primitives';
import { profileAvatarSource, useApp } from '../AppContext';
import { toImageSource } from '../../../data/mockData';

export default function ProfileTab() {
  const {
    me,
    setIsSettingsOpen,
    selectedProfileAvatar,
    profileNickname,
    openProfileEditor,
    setProfileDetailModal,
    setActiveTab,
    setActiveClipModal,
    profileStats,
    isProfileStatsLoading,
    profileStatsError,
    reloadProfileStats,
  } = useApp();

  /**
   * 가입할 때 고른 아바타가 서버에 있으면 그걸 쓰고, 없으면 앱에서 고른 것을
   * 쓴다. 서버가 주지 않는 값을 지어내지 않으려고 코드로만 맞춘다.
   */
  // selectedProfileAvatar 는 로그인 사용자 값으로 시작하고 편집하면 바로 갱신되므로 이것만 보면 된다.
  const avatarImage = profileAvatarSource(selectedProfileAvatar);

  // 방문·저장은 같은 핀이 여러 번 들어올 일이 없지만(핀 하나 + 내 기록 하나), 혹시
  // 모를 중복은 키로 걸러 센다.
  const visitedCount = new Set(profileStats.visited.map((v) => v.key)).size;
  const savedCount = new Set(profileStats.saved.map((v) => v.key)).size;
  const regionCount = profileStats.regions.length;
  const gallery = profileStats.pinlogs.slice(0, 2);

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {/* 상단 바 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2">
          <PixelProfileIcon size={30} active />
          <Text className="font-pixel text-lg tracking-tight text-[#075953]">PROFILE</Text>
        </View>
        <Pressable
          onPress={() => setIsSettingsOpen(true)}
          accessibilityLabel="환경설정"
          className="h-10 w-10 items-center justify-center rounded-2xl border border-[#82F0DC] bg-white active:scale-90 active:bg-[#82F0DC]/30"
        >
          <Text className="text-xl">⚙️</Text>
        </Pressable>
      </View>

      {/* 프로필 카드 */}
      <View className="relative flex-row items-center gap-3 rounded-3xl border-2 border-[#82F0DC] bg-white p-4 shadow-sm">
        <PixelPressable
          shadowColor="#56EADD"
          offset={3}
          radius={16}
          pressEffect="scale"
          accessibilityLabel="프로필 아바타 변경"
          onPress={openProfileEditor}
          className="relative h-20 w-20 overflow-hidden rounded-2xl border-2 border-[#075953] bg-[#B4FAF0]"
        >
          <Image source={toImageSource(avatarImage)} resizeMode="cover" className="h-full w-full" />
          <View className="absolute bottom-1 left-2 right-2 rounded-lg border border-[#075953] bg-white/90 px-2 py-0.5">
            <Text className="text-center text-[8px] font-black text-[#075953]">변경</Text>
          </View>
        </PixelPressable>
        <View className="min-w-0 flex-1">
          <View className="flex-row items-baseline gap-2">
            <Text className="text-lg font-black text-slate-900" numberOfLines={1}>
              {profileNickname}
            </Text>
            <Text className="font-pixel text-[9px] text-[#08736d]" numberOfLines={1}>
              @{me.username}
            </Text>
          </View>
        </View>
      </View>

      {profileStatsError ? (
        <View className="items-center gap-2 rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/70 p-4">
          <Text className="text-center text-[11px] font-bold text-slate-500">{profileStatsError}</Text>
          <Pressable onPress={reloadProfileStats} className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95">
            <Text className="text-[11px] font-black text-[#075953]">다시 시도</Text>
          </Pressable>
        </View>
      ) : null}

      {/* 통계 3박스 — 내 여행에서 센 값 */}
      <View className="flex-row gap-2">
        {(
          [
            { key: 'visited', value: visitedCount, label: '방문한 장소', color: 'text-[#075953]' },
            { key: 'saved', value: savedCount, label: '저장한 장소', color: 'text-[#FF5A87]' },
            { key: 'regions', value: regionCount, label: '다녀온 지역 수', color: 'text-violet-500' },
          ] as const
        ).map((s) => (
          <Pressable
            key={s.key}
            onPress={() => setProfileDetailModal(s.key)}
            className="flex-1 items-center rounded-2xl border border-[#82F0DC] bg-white p-3 active:scale-95"
          >
            {isProfileStatsLoading ? (
              <View className="h-[21px] justify-center">
                <ActivityIndicator size="small" color="#075953" />
              </View>
            ) : (
              <Text className={`font-pixel text-lg ${s.color}`}>{s.value}</Text>
            )}
            <Text className="mt-2 text-[10px] font-black text-slate-500">{s.label}</Text>
          </Pressable>
        ))}
      </View>

      {/* 여행 취향 픽셀 바 차트 — 내 여행들이 고른 테마 비율 */}
      <View className="rounded-3xl border-2 border-[#82F0DC] bg-white p-4 shadow-sm">
        <View className="mb-4 flex-row items-center justify-between">
          <View className="flex-row items-center gap-2">
            <View className="h-8 w-8 items-center justify-center rounded-xl border border-[#82F0DC] bg-[#B4FAF0]">
              <Text className="text-base text-slate-800">▥</Text>
            </View>
            <View>
              <Text className="text-sm font-black text-slate-900">내 여행 취향</Text>
              {/* 어떤 자료로 센 값인지 그대로 적는다. */}
              <Text className="text-[9px] font-semibold text-slate-400">내 여행에서 고른 테마 기준</Text>
            </View>
          </View>
          <Text className="font-pixel text-[8px] text-[#075953]">PIXEL TASTE</Text>
        </View>

        {isProfileStatsLoading ? (
          <View className="items-center py-6">
            <ActivityIndicator color="#075953" />
          </View>
        ) : profileStats.tastes.length === 0 ? (
          <View className="rounded-2xl border-2 border-dashed border-slate-200 p-5">
            <Text className="text-center text-[11px] font-bold text-slate-400">
              아직 테마를 고른 여행이 없어요.{'\n'}여행을 만들 때 테마를 고르면 취향이 쌓여요.
            </Text>
          </View>
        ) : (
          <View className="gap-3">
            {profileStats.tastes.map((taste) => (
              <View key={taste.code}>
                <View className="mb-1.5 flex-row items-center justify-between">
                  <Text className="text-xs font-black text-slate-700">
                    {taste.icon} {taste.label}
                  </Text>
                  <Text className="font-pixel text-[9px] text-slate-600">{taste.percent}%</Text>
                </View>
                <View className="flex-row" style={{ gap: 2 }}>
                  {Array.from({ length: 20 }).map((_, index) => (
                    <View
                      key={index}
                      className={`h-3 flex-1 rounded-[2px] border border-slate-900/10 ${
                        index < Math.round(taste.percent / 5) ? taste.color : 'bg-slate-100'
                      }`}
                    />
                  ))}
                </View>
              </View>
            ))}
          </View>
        )}
      </View>

      {/* 핀로그 갤러리 — 내 여행에서 완성된 PinLog */}
      <View className="gap-2">
        <View className="flex-row items-center justify-between px-1">
          <Text className="text-xs font-bold text-slate-800">내 핀로그 브이로그 모아보기 🎞️</Text>
          <Pressable onPress={() => setActiveTab('pinlog')}>
            <Text className="text-[10px] font-black text-[#075953]">전체보기 ›</Text>
          </Pressable>
        </View>

        {isProfileStatsLoading ? (
          <View className="items-center rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-6">
            <ActivityIndicator color="#075953" />
          </View>
        ) : gallery.length === 0 ? (
          <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-6">
            <Text className="text-center text-[11px] font-bold text-slate-400">
              아직 완성된 PinLog 가 없어요.{'\n'}장소에 영상을 남기고 PinLog 를 만들어 보세요.
            </Text>
          </View>
        ) : (
          <View className="flex-row gap-2">
            {gallery.map((clip) => (
              <Pressable
                key={clip.id}
                onPress={() => setActiveClipModal(clip)}
                accessibilityLabel={`${clip.location} 재생`}
                className="flex-1 gap-1.5 rounded-2xl border border-slate-100 bg-white p-2 active:scale-95"
              >
                <View className="relative aspect-video overflow-hidden rounded-xl bg-black">
                  {clip.thumbnail ? (
                    <Image source={{ uri: clip.thumbnail }} resizeMode="cover" className="h-full w-full" />
                  ) : null}
                  <View className="absolute bottom-0 left-0 right-0 top-0 items-center justify-center bg-black/30">
                    <Text className="text-xl text-white">▶</Text>
                  </View>
                </View>
                <Text numberOfLines={1} className="text-[11px] font-bold text-slate-800">
                  {clip.location}
                </Text>
                <Text className="text-[9px] text-slate-400">{clip.time}</Text>
              </Pressable>
            ))}
            {/* 한 개뿐이면 카드가 가로를 다 먹지 않게 빈 칸을 둔다. */}
            {gallery.length === 1 && <View className="flex-1" />}
          </View>
        )}
      </View>
    </FadeIn>
  );
}
