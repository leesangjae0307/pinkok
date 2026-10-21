import React from 'react';
import { Image, Text, View } from 'react-native';

import { VlogClip } from '../../../data/mockData';
import { PixelPinLogIcon } from '../../../components/pixel/PixelIcons';
import { FadeIn, PixelPressable } from '../../../components/pixel/Primitives';
import { useApp } from '../AppContext';

function ClipCard({ clip, onPress, showDots }: { clip: VlogClip; onPress: () => void; showDots: boolean }) {
  return (
    <PixelPressable
      shadowColor="#56EADD"
      offset={4}
      radius={16}
      onPress={onPress}
      wrapperClassName="w-full"
      className="w-full rounded-2xl border-4 border-pixel-navy bg-slate-900 p-2"
    >
      <View className="mb-2 flex-row items-center justify-between px-1">
        <Text className="font-pixel text-[8px] text-[#56EADD]">PINKOK TAPE</Text>
        <Text className="font-pixel text-[8px] text-[#FFC837]">{clip.time}</Text>
      </View>
      <View className="relative aspect-video overflow-hidden rounded-lg border-2 border-slate-600 bg-black">
        <Image source={{ uri: clip.thumbnail }} resizeMode="cover" className="h-full w-full" />
        <View className="absolute inset-0 items-center justify-center bg-black/20">
          <View className="h-10 w-10 items-center justify-center rounded-full border-2 border-white bg-black/55">
            <Text className="text-sm text-white">▶</Text>
          </View>
        </View>
        <View className="absolute bottom-2 right-2 rounded bg-black/75 px-2 py-1">
          <Text className="font-pixel text-[8px] text-[#56EADD]">16:9 · 1.0s</Text>
        </View>
      </View>
      <View className="flex-row items-center justify-between px-1 pt-2">
        <Text className="text-xs font-black text-white">{clip.location}</Text>
        {showDots && (
          <View className="flex-row gap-1">
            <View className="h-2 w-2 rounded-sm bg-rose-500" />
            <View className="h-2 w-2 rounded-sm bg-[#56EADD]" />
            <View className="h-2 w-2 rounded-sm bg-[#FFC837]" />
          </View>
        )}
      </View>
    </PixelPressable>
  );
}

export default function PinLogTab() {
  const { vlogClips, setActiveClipModal, setIsPinLogTripPickerOpen } = useApp();

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      <View className="flex-row items-center pt-1">
        <View className="flex-row items-center gap-2">
          <View className="h-11 w-11 items-center justify-center">
            <PixelPinLogIcon size={34} active />
          </View>
          <View>
            <Text className="font-pixel text-xl tracking-tight text-[#0f6861]">PINLOG</Text>
            <Text className="text-[11px] font-semibold text-slate-600">소중한 순간을 담는 짧은 브이로그</Text>
          </View>
        </View>
      </View>

      <PixelPressable
        shadowColor="#075953"
        offset={3}
        radius={16}
        onPress={() => setIsPinLogTripPickerOpen(true)}
        wrapperClassName="w-full"
        className="h-11 w-full flex-row items-center justify-center gap-1.5 rounded-2xl border-2 border-pixel-navy bg-mint-primary"
      >
        <Text className="text-xs font-black text-pixel-navy">🎬</Text>
        <Text className="text-xs font-black text-pixel-navy">새 PinLog 만들기</Text>
      </PixelPressable>

      <View className="gap-4">
        <View className="gap-3">
          <View className="flex-row items-center justify-between px-1">
            <Text className="text-xs font-bold text-slate-800">
              📅 2026년 10월 12일 <Text className="font-normal text-[#075953]">(제주)</Text>
            </Text>
            <Text className="font-pixel text-[10px] text-[#075953]">4 CLIPS</Text>
          </View>
          <View className="gap-3">
            {vlogClips.slice(0, 4).map((clip) => (
              <ClipCard key={clip.id} clip={clip} onPress={() => setActiveClipModal(clip)} showDots />
            ))}
          </View>
        </View>

        <View className="gap-3 pt-2">
          <View className="flex-row items-center justify-between px-1">
            <Text className="text-xs font-bold text-slate-800">
              📅 2026년 8월 15일 <Text className="font-normal text-[#075953]">(도쿄)</Text>
            </Text>
            <Text className="font-pixel text-[10px] text-[#075953]">2 CLIPS</Text>
          </View>
          <View className="gap-3">
            {vlogClips.slice(4, 6).map((clip) => (
              <ClipCard key={clip.id} clip={clip} onPress={() => setActiveClipModal(clip)} showDots={false} />
            ))}
          </View>
        </View>
      </View>
    </FadeIn>
  );
}
