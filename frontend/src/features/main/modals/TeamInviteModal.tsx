import React, { useMemo, useState } from 'react';
import { Image, Pressable, ScrollView, Text, TextInput, View, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { ModalBackdrop, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';
import { AvatarSource, isImageAvatar, toImageSource } from '../../../data/mockData';

interface TeamInviteModalProps {
  tripTitle: string;
  currentAvatarUrl: AvatarSource;
  /** 팀원 관리 목록에 "(나)" 로 보일 내 닉네임 */
  currentNickname?: string;
  onClose: () => void;
  /** loginId 는 숫자 PK 가 아니라 화면에 @ 로 보이는 로그인 아이디다. */
  onInvite: (nickname: string, loginId: string, avatar: AvatarSource) => void;
  /** true 면 위쪽에 현재 참여자 관리(팀장 위임 · 내보내기)를 보여준다 */
  showTeamManagement?: boolean;
}

interface CrewMember {
  id: string;
  nickname: string;
  avatar: AvatarSource;
}

const ME_ID = 'me';

const TEAM_SEARCH_RESULTS = [
  { loginId: 'gamjieun', nickname: '지은', avatar: '🎀', role: '감성 여행가' },
  { loginId: 'kimjieun2', nickname: '지은', avatar: '📷', role: '사진 기록가' },
  { loginId: 'jieun1234', nickname: '지은', avatar: '🧢', role: '맛집 탐험가' },
  { loginId: 'jeju_mint', nickname: '민지', avatar: '🌿', role: '제주 로컬러' },
];

export default function TeamInviteModal({
  tripTitle,
  currentAvatarUrl,
  currentNickname = '나',
  onClose,
  onInvite,
  showTeamManagement = false,
}: TeamInviteModalProps) {
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const [query, setQuery] = useState('지은');
  const [invitedLoginIds, setInvitedLoginIds] = useState<string[]>([]);

  // TODO(서버 연동): GET /trips/{id}/members 로 교체. 지금은 웹 목업과 같은 고정 참여자.
  const [members, setMembers] = useState<CrewMember[]>([
    { id: ME_ID, nickname: currentNickname, avatar: currentAvatarUrl },
    { id: 'minji', nickname: '민지', avatar: '🌿' },
    { id: 'seojun', nickname: '서준', avatar: '📷' },
  ]);
  const [leaderId, setLeaderId] = useState(ME_ID);
  const [actionMemberId, setActionMemberId] = useState<string | null>(null);
  const canManage = leaderId === ME_ID;

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
            accessibilityLabel="팀원 창 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
          >
            <PixelBackArrow size={15} />
          </Pressable>
          <View className="flex-1 items-center">
            <Text className="font-pixel text-[9px] text-[#075953]">{showTeamManagement ? 'TEAM MANAGE' : 'TEAM INVITE'}</Text>
            <Text className="mt-1 text-sm font-black text-slate-900">
              {showTeamManagement ? '팀원 관리' : '팀원 검색 및 초대'}
            </Text>
          </View>
          <View className="w-9" />
        </View>

        <ScrollView
          style={{ maxHeight: windowHeight * 0.78 }}
          contentContainerStyle={{ padding: 16, gap: 16 }}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {showTeamManagement && (
            <ShadowBox
              shadowColor="#56EADD"
              offset={3}
              radius={24}
              className="rounded-3xl border-2 border-pixel-navy bg-white p-3"
            >
              <View className="mb-3 flex-row items-center justify-between">
                <View>
                  <Text className="font-pixel text-[8px] text-[#075953]">CURRENT CREW</Text>
                  <Text className="mt-1 text-xs font-black text-slate-900">현재 참여자 {members.length}명</Text>
                </View>
                <View className="rounded-lg bg-[#B4FAF0] px-2 py-1">
                  <Text className="text-[9px] font-black text-[#075953]">{canManage ? '내가 팀장' : '팀원'}</Text>
                </View>
              </View>

              <View className="gap-2">
                {members.map((member) => {
                  const isLeader = leaderId === member.id;
                  const menuOpen = actionMemberId === member.id;
                  return (
                    <View key={member.id} className="rounded-2xl border border-[#82F0DC] bg-slate-50 p-2.5">
                      <View className="flex-row items-center gap-3">
                        <View className="h-10 w-10 items-center justify-center overflow-hidden rounded-xl border-2 border-pixel-navy bg-[#B4FAF0]">
                          {isImageAvatar(member.avatar) ? (
                            <Image source={toImageSource(member.avatar)} resizeMode="cover" className="h-full w-full" />
                          ) : (
                            <Text className="text-lg">{member.avatar}</Text>
                          )}
                        </View>
                        <View className="min-w-0 flex-1">
                          <View className="flex-row items-center gap-1.5">
                            <Text className="text-xs font-black text-slate-900">{member.nickname}</Text>
                            {member.id === ME_ID && <Text className="text-[9px] font-bold text-slate-400">(나)</Text>}
                          </View>
                          <View
                            className={`mt-1 self-start rounded-md px-1.5 py-0.5 ${isLeader ? 'bg-[#FFC837]' : 'bg-slate-200'}`}
                          >
                            <Text className={`text-[8px] font-black ${isLeader ? 'text-slate-900' : 'text-slate-500'}`}>
                              {isLeader ? '팀장' : '팀원'}
                            </Text>
                          </View>
                        </View>
                        {canManage && member.id !== ME_ID && (
                          <Pressable
                            onPress={() => setActionMemberId(menuOpen ? null : member.id)}
                            accessibilityLabel={`${member.nickname} 관리`}
                            className="h-8 w-8 items-center justify-center rounded-lg border border-slate-300 bg-white active:bg-slate-100"
                          >
                            <Text className="font-black text-slate-600">···</Text>
                          </Pressable>
                        )}
                      </View>

                      {/* 웹은 오른쪽에 뜨는 작은 메뉴지만, 스크롤 영역 안에서는 다른 행에 가려질 수 있어 행 아래에 펼친다. */}
                      {menuOpen && (
                        <View className="mt-2 flex-row gap-2">
                          <Pressable
                            onPress={() => {
                              setLeaderId(member.id);
                              setActionMemberId(null);
                            }}
                            className="h-9 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy bg-white active:bg-slate-50"
                          >
                            <Text className="text-[10px] font-black text-[#075953]">팀장 위임</Text>
                          </Pressable>
                          <Pressable
                            onPress={() => {
                              setMembers((cur) => cur.filter((m) => m.id !== member.id));
                              setActionMemberId(null);
                            }}
                            className="h-9 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy bg-white active:bg-slate-50"
                          >
                            <Text className="text-[10px] font-black text-rose-600">팀에서 내보내기</Text>
                          </Pressable>
                        </View>
                      )}
                    </View>
                  );
                })}
              </View>
            </ShadowBox>
          )}

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

          <View className="gap-2">
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
                        <Image source={toImageSource(currentAvatarUrl)} resizeMode="cover" className="h-full w-full" />
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
                        onInvite(
                          member.nickname,
                          member.loginId,
                          member.loginId === 'gamjieun' ? currentAvatarUrl : member.avatar,
                        );
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
          </View>
        </ScrollView>
      </View>
    </ModalBackdrop>
  );
}
