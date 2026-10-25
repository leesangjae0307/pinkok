import React from 'react';
import { Image, Pressable, Text, View } from 'react-native';

import { PixelBellIcon, PixelExtractIcon, PixelHotAirBalloon, PixelPinLogIcon } from '../../../components/pixel/PixelIcons';
import { AnimatedLoop, FadeIn, LinearGradient, PixelPressable } from '../../../components/pixel/Primitives';
import { toImageSource } from '../../../data/mockData';
import { useApp } from '../AppContext';

export default function HomeTab() {
  const {
    trips,
    unreadNotificationCount,
    setIsNotificationOpen,
    openTrip,
    setActiveTab,
    openQuickPinLogCamera,
  } = useApp();
  const firstTrip = trips[0];

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {/* 헤더: 열기구 핀 로고 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2.5">
          <PixelHotAirBalloon size={38} active />
          <View>
            <View className="flex-row items-center gap-1.5">
              <Text className="font-pixel text-xl tracking-tight text-[#0f6861]">PINKOK</Text>
              <AnimatedLoop kind="ping">
                <View className="h-2 w-2 rounded-full bg-[#FF5A87]" />
              </AnimatedLoop>
            </View>
            <Text className="-mt-0.5 text-[11px] font-semibold text-slate-600">내 손안의 픽셀 여행 스크랩북</Text>
          </View>
        </View>

        {/* 알림 배지 */}
        <Pressable
          onPress={() => setIsNotificationOpen(true)}
          accessibilityLabel={`알림함 열기, 읽지 않은 알림 ${unreadNotificationCount}개`}
          className="relative h-10 w-10 items-center justify-center rounded-2xl border border-[#82F0DC]/80 bg-white shadow-sm active:bg-[#82F0DC]/20"
        >
          <PixelBellIcon size={23} active={unreadNotificationCount > 0} />
          {unreadNotificationCount > 0 && (
            <View className="absolute -right-1 -top-1 h-5 min-w-5 items-center justify-center rounded-md border-2 border-white bg-[#FF5A87] px-1">
              <Text className="font-pixel text-[8px] text-white">
                {unreadNotificationCount > 9 ? '9+' : unreadNotificationCount}
              </Text>
            </View>
          )}
        </Pressable>
      </View>

      {/* 다가오는 여행 대시보드 */}
      {firstTrip && (
        <PixelPressable
          shadowColor="#56EADD"
          offset={5}
          radius={30}
          pressEffect="none"
          wrapperClassName="w-full"
          className="w-full overflow-hidden rounded-[30px] border-3 border-pixel-navy bg-slate-900"
          onPress={() => {
            setActiveTab('trips');
            openTrip(firstTrip);
          }}
        >
          <Image source={toImageSource(firstTrip.image)} resizeMode="cover" className="h-52 w-full" />
          <LinearGradient
            colors={['rgba(15,23,42,0)', 'rgba(15,23,42,0.25)', '#020617']}
            start={{ x: 0, y: 0 }}
            end={{ x: 0, y: 1 }}
            style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}
          />
          <View className="absolute bottom-0 left-0 right-0 p-4">
            <View className="mb-2 flex-row items-center justify-between">
              <View className="rounded-lg border border-[#56EADD] bg-slate-950/80 px-2 py-1">
                <Text className="font-pixel text-[9px] text-[#56EADD]">다가오는 여행 · {firstTrip.dDay}</Text>
              </View>
              <Text className="text-[10px] font-black text-[#FFC837]">오늘의 여행 대시보드</Text>
            </View>
            <Text className="text-lg font-black text-white">{firstTrip.title}</Text>
            <View className="mt-2 flex-row items-center justify-between">
              <Text className="text-[10px] font-semibold text-slate-200">📅 {firstTrip.date}</Text>
              <Text className="text-[10px] font-semibold text-slate-200">📍 {firstTrip.placesCount}개 핀 →</Text>
            </View>
          </View>
        </PixelPressable>
      )}

      {/* 빠른 AI 추출 */}
      <Pressable onPress={() => setActiveTab('extract')} className="active:opacity-90">
        <LinearGradient
          colors={['#56EADD', '#B4FAF0']}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 0 }}
          style={{
            width: '100%',
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderRadius: 24,
            borderWidth: 2,
            borderColor: '#075953',
            padding: 16,
          }}
        >
          <View className="flex-1">
            <Text className="font-pixel text-[9px] text-[#075953]">QUICK AI EXTRACT</Text>
            <Text className="mt-1 text-sm font-black text-slate-900">링크 한 번으로 여행 장소 찾기</Text>
            <Text className="mt-1 text-[10px] font-semibold text-[#075953]">유튜브·인스타그램 링크를 바로 분석해요.</Text>
          </View>
          <PixelExtractIcon size={48} active />
        </LinearGradient>
      </Pressable>

      {/* 빠른 핀로그 촬영: D-Day 가 가장 가까운 여행의 카메라를 바로 연다 */}
      <PixelPressable
        shadowColor="#FF5A87"
        offset={4}
        radius={24}
        wrapperClassName="w-full"
        className="w-full flex-row items-center justify-between rounded-3xl border-2 border-pixel-navy bg-slate-900 p-4"
        accessibilityLabel="핀로그 빠른 촬영 열기"
        onPress={openQuickPinLogCamera}
      >
        <View className="flex-1">
          <Text className="font-pixel text-[9px] text-[#56EADD]">QUICK RECORD</Text>
          <Text className="mt-1 text-sm font-black text-white">핀로그 촬영</Text>
          <Text className="mt-1 text-[10px] font-semibold text-slate-300">가장 가까운 D-Day 여행 카메라를 바로 열어요.</Text>
        </View>
        <PixelPinLogIcon size={48} active />
      </PixelPressable>

      {/* AI 팁 */}
      <View className="flex-row items-center gap-3 rounded-2xl border border-[#82F0DC] bg-white/90 p-3">
        <View className="h-10 w-10 items-center justify-center rounded-xl bg-[#56EADD]/30">
          <Text className="text-lg">💡</Text>
        </View>
        <View className="flex-1">
          <Text className="font-pixel text-[9px] text-[#075953]">PINKOK AI TIP</Text>
          <Text className="mt-1 text-xs leading-snug text-slate-700">
            유튜브나 인스타그램 링크를 복사해 추출 탭에 붙여넣으면 AI가 여행 장소를 한눈에 찾아줘요!
          </Text>
        </View>
      </View>
    </FadeIn>
  );
}
