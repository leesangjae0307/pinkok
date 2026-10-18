import React from 'react';
import { Pressable, Text, View } from 'react-native';

import { PixelBackArrow, PixelKakaoIcon, PixelToggleSwitch } from '../../../components/pixel/PixelIcons';
import { FadeIn } from '../../../components/pixel/Primitives';
import { useApp } from '../AppContext';

function Emoji({ children }: { children: string }) {
  return (
    <View className="rounded-xl bg-[#B4FAF0]/50 p-1.5">
      <Text className="text-base">{children}</Text>
    </View>
  );
}

function GroupTitle({ children }: { children: string }) {
  return <Text className="px-1 font-pixel text-[11px] text-[#075953]">{children}</Text>;
}

function Chevron() {
  return <Text className="font-pixel text-[10px] text-slate-400">›</Text>;
}

export default function SettingsScreen() {
  const {
    setIsSettingsOpen,
    showToast,
    pushNotification,
    setPushNotification,
    marketingNotification,
    setMarketingNotification,
    cacheSize,
    handleClearCache,
    handleLogout,
  } = useApp();

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {/* 상단 바 */}
      <View className="flex-row items-center justify-between border-b border-[#82F0DC]/80 pb-1 pt-1">
        <Pressable
          onPress={() => setIsSettingsOpen(false)}
          className="flex-row items-center gap-1 rounded-2xl border border-[#82F0DC] bg-white p-2 active:scale-90"
        >
          <PixelBackArrow size={16} />
        </Pressable>
        <Text className="text-base font-extrabold tracking-tight text-slate-800">환경설정</Text>
        <View className="h-9 w-9" />
      </View>

      {/* 계정 */}
      <View className="gap-1.5">
        <GroupTitle>ACCOUNT</GroupTitle>
        <View className="gap-3 rounded-3xl border border-[#82F0DC] bg-white p-3.5 shadow-sm">
          <View className="flex-row items-center justify-between border-b border-slate-100 pb-2.5">
            <View className="flex-row items-center gap-2">
              <Emoji>👤</Emoji>
              <View>
                <Text className="text-xs font-bold text-slate-800">계정 정보</Text>
                <Text className="text-[10px] text-slate-400">pinkok_user@kakao.com</Text>
              </View>
            </View>
            <View className="flex-row items-center gap-1.5 rounded-xl border border-amber-200 bg-amber-50 px-2 py-1">
              <PixelKakaoIcon size={14} />
              <Text className="text-[10px] font-bold text-slate-700">카카오 연동</Text>
            </View>
          </View>

          <Pressable
            onPress={() => showToast('프로필 및 닉네임 수정 화면으로 이동합니다.')}
            className="flex-row items-center justify-between pt-0.5 active:opacity-80"
          >
            <View className="flex-row items-center gap-2">
              <Emoji>✏️</Emoji>
              <Text className="text-xs font-bold text-slate-800">닉네임 및 기본 프로필 변경</Text>
            </View>
            <Chevron />
          </Pressable>
        </View>
      </View>

      {/* 알림 */}
      <View className="gap-1.5">
        <GroupTitle>NOTIFICATIONS</GroupTitle>
        <View className="gap-3 rounded-3xl border border-[#82F0DC] bg-white p-3.5 shadow-sm">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-2">
              <Emoji>🔔</Emoji>
              <View>
                <Text className="text-xs font-bold text-slate-800">푸시 알림</Text>
                <Text className="text-[10px] text-slate-400">여행 일정 및 AI 장소 추출 알림</Text>
              </View>
            </View>
            <PixelToggleSwitch
              checked={pushNotification}
              onChange={() => {
                setPushNotification(!pushNotification);
                showToast(`푸시 알림이 ${!pushNotification ? '켜졌습니다' : '꺼졌습니다'}.`);
              }}
            />
          </View>

          <View className="flex-row items-center justify-between border-t border-slate-100 pt-2">
            <View className="flex-row items-center gap-2">
              <Emoji>🎁</Emoji>
              <View>
                <Text className="text-xs font-bold text-slate-800">이벤트 및 혜택 알림</Text>
                <Text className="text-[10px] text-slate-400">맞춤 여행지 추천 정보 수신</Text>
              </View>
            </View>
            <PixelToggleSwitch
              checked={marketingNotification}
              onChange={() => {
                setMarketingNotification(!marketingNotification);
                showToast(`이벤트 알림이 ${!marketingNotification ? '켜졌습니다' : '꺼졌습니다'}.`);
              }}
            />
          </View>
        </View>
      </View>

      {/* 저장공간 */}
      <View className="gap-1.5">
        <GroupTitle>STORAGE</GroupTitle>
        <View className="rounded-3xl border border-[#82F0DC] bg-white p-3.5 shadow-sm">
          <Pressable onPress={handleClearCache} className="flex-row items-center justify-between active:opacity-80">
            <View className="flex-row items-center gap-2">
              <Emoji>🗑️</Emoji>
              <View>
                <Text className="text-xs font-bold text-slate-800">임시저장 데이터 삭제</Text>
                <Text className="text-[10px] text-slate-400">캐시 및 AI 장소 추출 데이터 정리</Text>
              </View>
            </View>
            <View className="rounded-lg bg-slate-100 px-2 py-1">
              <Text className="font-pixel text-[10px] text-slate-600">{cacheSize}</Text>
            </View>
          </Pressable>
        </View>
      </View>

      {/* 정보 및 지원 */}
      <View className="gap-1.5">
        <GroupTitle>SUPPORT & INFO</GroupTitle>
        <View className="gap-3 rounded-3xl border border-[#82F0DC] bg-white p-3.5 shadow-sm">
          <Pressable
            onPress={() => showToast('고객센터 및 FAQ로 연결됩니다.')}
            className="flex-row items-center justify-between border-b border-slate-100 pb-2 active:opacity-80"
          >
            <View className="flex-row items-center gap-2">
              <Emoji>❓</Emoji>
              <Text className="text-xs font-bold text-slate-800">고객센터 및 자주 묻는 질문</Text>
            </View>
            <Chevron />
          </Pressable>

          <Pressable
            onPress={() => showToast('이용약관 및 개인정보 처리방침')}
            className="flex-row items-center justify-between border-b border-slate-100 pb-2 active:opacity-80"
          >
            <View className="flex-row items-center gap-2">
              <Emoji>📄</Emoji>
              <Text className="text-xs font-bold text-slate-800">약관 및 정책</Text>
            </View>
            <Chevron />
          </Pressable>

          <View className="flex-row items-center justify-between pt-0.5">
            <View className="flex-row items-center gap-2">
              <Emoji>ℹ️</Emoji>
              <Text className="text-xs font-bold text-slate-800">앱 버전</Text>
            </View>
            <View className="rounded-md bg-[#B4FAF0] px-2 py-0.5">
              <Text className="font-pixel text-[10px] text-[#075953]">v1.0.0 (최신버전)</Text>
            </View>
          </View>
        </View>
      </View>

      {/* 로그아웃 */}
      <View className="items-center pb-6 pt-3">
        <Pressable
          onPress={handleLogout}
          className="flex-row items-center gap-1.5 rounded-2xl border border-[#82F0DC] bg-white/80 px-6 py-2.5 active:scale-95 active:bg-white"
        >
          <Text className="text-xs font-bold text-slate-600">🚪 로그아웃</Text>
        </Pressable>
      </View>
    </FadeIn>
  );
}
