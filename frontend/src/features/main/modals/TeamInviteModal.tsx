import React, { useMemo, useState } from 'react';
import { Image, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { ModalBackdrop, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';

interface TeamInviteModalProps {
  tripTitle: string;
  currentAvatarUrl: number;
  onClose: () => void;
  /** loginId 는 숫자 PK 가 아니라 화면에 @ 로 보이는 로그인 아이디다. */
  onInvite: (nickname: string, loginId: string) => void;
}

const TEAM_SEARCH_RESULTS = [
  { loginId: 'gamjieun', nickname: '지은', avatar: '🎀', role: '감성 여행가' },
  { loginId: 'kimjieun2', nickname: '지은', avatar: '📷', role: '사진 기록가' },
  { loginId: 'jieun1234', nickname: '지은', avatar: '🧢', role: '맛집 탐험가' },
  { loginId: 'jeju_mint', nickname: '민지', avatar: '🌿', role: '제주 로컬러' },
];

export default function TeamInviteModal({ tripTitle, currentAvatarUrl, onClose, onInvite }: TeamInviteModalProps) {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState('지은');
  const [invitedLoginIds, setInvitedLoginIds] = useState<string[]>([]);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    return TEAM_SEARCH_RESULTS.filter((m) => m.nickname.toLowerCase().includes(q) || m.loginId.toLowerCase().includes(q));
  }, [query]);

  return (
    <ModalBackdrop onRequestClose={onClose}>
      <View
        className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]"
        style={{ marginBottom: insets.bottom }}
      >
        <View className="flex-row items-center border-b-2 border-pixel-navy bg-white px-4 py-3">
          <Pressable
            onPress={onClose}
            accessibilityLabel="팀원 초대 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
          >
            <PixelBackArrow size={15} />
          </Pressable>
          <View className="flex-1 items-center">
            <Text className="font-pixel text-[9px] text-[#075953]">TEAM INVITE</Text>
            <Text className="mt-1 text-sm font-black text-slate-900">팀원 검색 및 초대</Text>
          </View>
          <View className="w-9" />
        </View>

        <View className="gap-4 p-4">
          <View className="rounded-2xl border border-[#82F0DC] bg-white/80 px-3 py-2">
            <Text className="text-[11px] font-bold text-[#075953]">'{tripTitle}'에 함께할 팀원을 찾아보세요.</Text>
          </View>

          <ShadowBox
            shadowColor="#56EADD"
            offset={3}
            radius={16}
            className="h-12 flex-row items-center gap-2 rounded-2xl border-2 border-pixel-navy bg-white px-3"
          >
            <Text>🔍</Text>
            <TextInput
              value={query}
              onChangeText={setQuery}
              placeholder="닉네임 또는 @아이디 검색"
              placeholderTextColor="#94A3B8"
              autoCapitalize="none"
              className="flex-1 p-0 text-sm font-bold text-slate-800"
            />
          </ShadowBox>

          <ScrollView style={{ maxHeight: 320 }} contentContainerStyle={{ gap: 8 }} keyboardShouldPersistTaps="handled">
            {results.length > 0 ? (
              results.map((member) => {
                const invited = invitedLoginIds.includes(member.loginId);
                return (
                  <View key={member.loginId} className="flex-row items-center gap-3 rounded-2xl border border-[#82F0DC] bg-white p-3 shadow-sm">
                    <ShadowBox
                      shadowColor="#56EADD"
                      offset={2}
                      radius={12}
                      className="h-12 w-12 items-center justify-center overflow-hidden rounded-xl border-2 border-pixel-navy bg-[#B4FAF0]"
                    >
                      {member.loginId === 'gamjieun' ? (
                        <Image source={currentAvatarUrl} resizeMode="cover" className="h-full w-full" />
                      ) : (
                        <Text className="text-xl">{member.avatar}</Text>
                      )}
                    </ShadowBox>
                    <View className="min-w-0 flex-1">
                      <View className="flex-row items-center gap-2">
                        <Text className="text-sm font-black text-slate-900">{member.nickname}</Text>
                        <Text numberOfLines={1} className="flex-shrink font-pixel text-[8px] text-[#075953]">
                          @{member.loginId}
                        </Text>
                      </View>
                      <Text className="mt-1 text-[10px] font-semibold text-slate-500">{member.role}</Text>
                    </View>
                    <PixelPressable
                      shadowColor="#075953"
                      offset={2}
                      radius={12}
                      disabled={invited}
                      onPress={() => {
                        setInvitedLoginIds((cur) => [...cur, member.loginId]);
                        onInvite(member.nickname, member.loginId);
                      }}
                      className="h-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD] px-3"
                      disabledClassName="h-9 items-center justify-center rounded-xl border-2 border-slate-300 bg-slate-100 px-3"
                    >
                      <Text className={`text-[10px] font-black ${invited ? 'text-slate-400' : 'text-pixel-navy'}`}>
                        {invited ? '초대됨' : '초대'}
                      </Text>
                    </PixelPressable>
                  </View>
                );
              })
            ) : (
              <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
                <Text className="text-center text-xs font-bold text-slate-400">검색 결과가 없습니다.</Text>
              </View>
            )}
          </ScrollView>
        </View>
      </View>
    </ModalBackdrop>
  );
}
