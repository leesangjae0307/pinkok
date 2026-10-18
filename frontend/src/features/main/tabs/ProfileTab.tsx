import React from 'react';
import { Image, Pressable, Text, View } from 'react-native';

import { PixelProfileIcon } from '../../../components/pixel/PixelIcons';
import { FadeIn, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';
import { TRAVEL_TASTES, useApp } from '../AppContext';

const GALLERY = [
  {
    image: 'https://images.unsplash.com/photo-1548115184-bc6544d06a58?auto=format&fit=crop&w=300&q=80',
    title: '제주 3박4일 요약 핀로그',
    meta: '2026.09.01 • 5초',
  },
  {
    image: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=300&q=80',
    title: '도쿄 먹방 하이라이트',
    meta: '2026.08.15 • 8초',
  },
];

export default function ProfileTab() {
  const {
    setIsSettingsOpen,
    selectedProfileAvatar,
    setPendingProfileAvatar,
    setIsAvatarPickerOpen,
    setProfileDetailModal,
    setActiveTab,
  } = useApp();

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {/* 상단 바 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2">
          <PixelProfileIcon size={30} active />
          <Text className="text-sm font-black text-[#075953]">프로필</Text>
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
          onPress={() => {
            setPendingProfileAvatar(selectedProfileAvatar);
            setIsAvatarPickerOpen(true);
          }}
          className="relative h-20 w-20 overflow-hidden rounded-2xl border-2 border-[#075953] bg-[#B4FAF0]"
        >
          <Image source={selectedProfileAvatar.image} resizeMode="cover" className="h-full w-full" />
          <View className="absolute bottom-1 left-2 right-2 rounded-lg border border-[#075953] bg-white/90 px-2 py-0.5">
            <Text className="text-center text-[8px] font-black text-[#075953]">변경</Text>
          </View>
        </PixelPressable>
        <View className="min-w-0 flex-1">
          <View className="self-start">
            <ShadowBox
              shadowColor="#075953"
              offset={2}
              radius={8}
              className="rounded-lg border-2 border-[#075953] bg-[#56EADD] px-2 py-1"
            >
              <Text className="text-[10px] font-black text-[#075953]">🌊 바다뷰 수집가</Text>
            </ShadowBox>
          </View>
          <View className="mt-2 flex-row items-baseline gap-2">
            <Text className="text-lg font-black text-slate-900">김지은</Text>
            <Text className="font-pixel text-[9px] text-[#08736d]">@gamjieun</Text>
          </View>
          <Text className="mt-1 text-[11px] font-semibold leading-4 text-slate-500">
            바다와 골목 카페를 발견하면 가장 먼저 핀을 남겨요.
          </Text>
        </View>
      </View>

      {/* 통계 3박스 */}
      <View className="flex-row gap-2">
        {(
          [
            { key: 'visited', value: '87', label: '방문한 장소', color: 'text-[#075953]' },
            { key: 'saved', value: '156', label: '저장한 장소', color: 'text-[#FF5A87]' },
            { key: 'regions', value: '12', label: '다녀온 지역 수', color: 'text-violet-500' },
          ] as const
        ).map((s) => (
          <Pressable
            key={s.key}
            onPress={() => setProfileDetailModal(s.key)}
            className="flex-1 items-center rounded-2xl border border-[#82F0DC] bg-white p-3 active:scale-95"
          >
            <Text className={`font-pixel text-lg ${s.color}`}>{s.value}</Text>
            <Text className="mt-2 text-[10px] font-black text-slate-500">{s.label}</Text>
          </Pressable>
        ))}
      </View>

      {/* 여행 취향 픽셀 바 차트 */}
      <View className="rounded-3xl border-2 border-[#82F0DC] bg-white p-4 shadow-sm">
        <View className="mb-4 flex-row items-center justify-between">
          <View className="flex-row items-center gap-2">
            <View className="h-8 w-8 items-center justify-center rounded-xl border border-[#82F0DC] bg-[#B4FAF0]">
              <Text className="text-base text-slate-800">▥</Text>
            </View>
            <View>
              <Text className="text-sm font-black text-slate-900">내 여행 취향</Text>
              <Text className="text-[9px] font-semibold text-slate-400">저장·방문 장소 기반 분석</Text>
            </View>
          </View>
          <Text className="font-pixel text-[8px] text-[#075953]">PIXEL TASTE</Text>
        </View>

        <View className="gap-3">
          {TRAVEL_TASTES.map((taste) => (
            <View key={taste.label}>
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
      </View>

      {/* 핀로그 갤러리 */}
      <View className="gap-2">
        <View className="flex-row items-center justify-between px-1">
          <Text className="text-xs font-bold text-slate-800">내 핀로그 브이로그 모아보기 🎞️</Text>
          <Pressable onPress={() => setActiveTab('pinlog')}>
            <Text className="text-[10px] font-black text-[#075953]">전체보기 ›</Text>
          </Pressable>
        </View>

        <View className="flex-row gap-2">
          {GALLERY.map((g) => (
            <View key={g.title} className="flex-1 gap-1.5 rounded-2xl border border-slate-100 bg-white p-2">
              <View className="relative aspect-video overflow-hidden rounded-xl">
                <Image source={{ uri: g.image }} resizeMode="cover" className="h-full w-full" />
                <View className="absolute inset-0 items-center justify-center bg-black/30">
                  <Text className="text-xl text-white">▶</Text>
                </View>
              </View>
              <Text numberOfLines={1} className="text-[11px] font-bold text-slate-800">
                {g.title}
              </Text>
              <Text className="text-[9px] text-slate-400">{g.meta}</Text>
            </View>
          ))}
        </View>
      </View>
    </FadeIn>
  );
}
