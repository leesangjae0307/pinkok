import React from 'react';
import { Pressable, Text, View } from 'react-native';

import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { FadeIn } from '../../../components/pixel/Primitives';
import { useApp } from '../AppContext';

export default function NotificationScreen() {
  const { notifications, unreadNotificationCount, setIsNotificationOpen, handleNotificationSelect } = useApp();

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 32 }}>
      {/* 헤더 */}
      <View className="flex-row items-center border-b border-[#82F0DC] pb-3 pt-1">
        <Pressable
          onPress={() => setIsNotificationOpen(false)}
          accessibilityLabel="홈으로 돌아가기"
          className="h-10 w-10 items-center justify-center rounded-2xl border border-[#82F0DC] bg-white active:scale-90"
        >
          <PixelBackArrow size={17} />
        </Pressable>
        <View className="flex-1 items-center">
          <Text className="text-base font-extrabold text-slate-900">알림</Text>
          <Text className="font-pixel text-[8px] text-[#0f6861]">NOTIFICATION</Text>
        </View>
        <View className="w-10 items-end">
          <View className="h-8 min-w-8 items-center justify-center rounded-xl bg-[#56EADD] px-2">
            <Text className="font-pixel text-[9px] text-[#075953]">{unreadNotificationCount}</Text>
          </View>
        </View>
      </View>

      <View className="mt-4 gap-3">
        {notifications.map((n) => (
          <Pressable
            key={n.id}
            onPress={() => handleNotificationSelect(n)}
            className={`relative w-full rounded-3xl border p-4 active:scale-[0.98] ${
              n.read ? 'border-slate-200 bg-white/55 opacity-70' : 'border-[#56EADD] bg-white shadow-md'
            }`}
          >
            {!n.read && (
              <View
                className="absolute left-2 h-2.5 w-2.5 rounded-sm border border-[#075953] bg-[#FF5A87]"
                style={{ top: '50%', marginTop: -5 }}
              />
            )}
            <View className="flex-row items-start gap-3 pl-1">
              <View
                className={`h-11 w-11 items-center justify-center rounded-2xl border ${
                  n.type === 'TRIP_INVITED' ? 'border-[#82F0DC] bg-[#B4FAF0]' : 'border-amber-200 bg-amber-50'
                }`}
              >
                <Text className="text-lg">
                  {n.type === 'TRIP_INVITED' ? '✉️' : n.type === 'TRIP_PLACE_ADDED' ? '📍' : '🎬'}
                </Text>
              </View>
              <View className="min-w-0 flex-1">
                <View className="mb-1 flex-row items-center justify-between gap-2">
                  <Text className="font-pixel text-[8px] text-[#075953]">
                    {n.type === 'TRIP_INVITED' ? 'TRIP INVITE' : n.type === 'TRIP_PLACE_ADDED' ? 'PLACE UPDATE' : 'PINLOG REMINDER'}
                  </Text>
                  <Text className="text-[10px] font-semibold text-slate-400">{n.createdAt}</Text>
                </View>
                <Text className="text-xs font-bold leading-5 text-slate-800">{n.message}</Text>
                <View className="mt-2 flex-row items-center gap-1">
                  <Text className="text-[10px] font-extrabold text-[#08736d]">
                    {n.type === 'TRIP_INVITED'
                      ? 'Trips에서 초대 확인하기'
                      : n.type === 'TRIP_PLACE_ADDED'
                        ? '여행 지도에서 확인하기'
                        : 'PinLog 클립 선택하기'}
                  </Text>
                  <Text className="font-pixel text-[10px] text-[#08736d]">→</Text>
                </View>
              </View>
            </View>
          </Pressable>
        ))}
      </View>

      <View className="mt-5 rounded-2xl border border-[#82F0DC] bg-white/65 px-4 py-3">
        <Text className="text-center text-[10px] font-semibold text-slate-500">최근 30일 동안 받은 알림을 보여드려요.</Text>
      </View>
    </FadeIn>
  );
}
