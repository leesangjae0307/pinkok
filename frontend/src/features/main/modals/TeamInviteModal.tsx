import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Image, Pressable, ScrollView, Text, TextInput, View, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { ModalBackdrop, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';
import { AvatarSource, isImageAvatar, toImageSource } from '../../../data/mockData';
import { errorMessage } from '../../../api/axios';
import { fileUrl } from '../../../api/fileApi';
import {
  fetchTripMembers,
  leaveTrip,
  removeTripMember,
  searchUsers,
  type UserSearchResult,
} from '../../../api/invitationApi';

interface TeamInviteModalProps {
  tripTitle: string;
  /** 팀원 목록을 받아올 여행. 새 여행을 만드는 중이면 없다. */
  tripId?: number;
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
  /** 서버 사용자 id. 내보내기에 쓴다. 나 자신이면 없다. */
  userId?: number;
  nickname: string;
  avatar: AvatarSource;
}

const ME_ID = 'me';

export default function TeamInviteModal({
  tripTitle,
  tripId,
  currentAvatarUrl,
  currentNickname = '나',
  onClose,
  onInvite,
  showTeamManagement = false,
}: TeamInviteModalProps) {
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const [query, setQuery] = useState('');
  const [invitedLoginIds, setInvitedLoginIds] = useState<string[]>([]);
  const [results, setResults] = useState<UserSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const [members, setMembers] = useState<CrewMember[]>([]);
  const [isMembersLoading, setIsMembersLoading] = useState(false);
  const [leaderId, setLeaderId] = useState(ME_ID);
  const [actionMemberId, setActionMemberId] = useState<string | null>(null);
  const canManage = leaderId === ME_ID;

  /** 팀원 목록. 새 여행을 만드는 중(tripId 없음)이면 아직 나뿐이다. */
  useEffect(() => {
    if (!tripId) {
      setMembers([{ id: ME_ID, nickname: currentNickname, avatar: currentAvatarUrl }]);
      return;
    }
    let cancelled = false;
    setIsMembersLoading(true);
    fetchTripMembers(tripId)
      .then((list) => {
        if (cancelled) return;
        setMembers(
          list.map((m) => ({
            id: m.me ? ME_ID : String(m.userId),
            userId: m.userId,
            nickname: m.me ? currentNickname : m.nickname,
            avatar: m.me
              ? currentAvatarUrl
              : fileUrl(m.avatarImageUrl ?? m.profileImageUrl) ?? '🙂',
          })),
        );
        const owner = list.find((m) => m.role === 'OWNER');
        setLeaderId(owner ? (owner.me ? ME_ID : String(owner.userId)) : ME_ID);
      })
      .catch(() => {
        if (!cancelled) setMembers([{ id: ME_ID, nickname: currentNickname, avatar: currentAvatarUrl }]);
      })
      .finally(() => {
        if (!cancelled) setIsMembersLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [tripId, currentNickname, currentAvatarUrl]);

  /**
   * 사람 검색. 서버 규칙이 2글자 이상이라 그 전에는 부르지 않는다.
   * 타자마다 요청하면 금방 쌓이므로 300ms 쉰 뒤에 보낸다.
   */
  useEffect(() => {
    const keyword = query.trim();
    if (keyword.length < 2) {
      setResults([]);
      setSearchError(null);
      return;
    }
    let cancelled = false;
    setIsSearching(true);
    const timer = setTimeout(() => {
      searchUsers(keyword, tripId)
        .then((found) => {
          if (!cancelled) {
            setResults(found);
            setSearchError(null);
          }
        })
        .catch((e) => {
          if (!cancelled) {
            setResults([]);
            setSearchError(errorMessage(e, '검색에 실패했어요.'));
          }
        })
        .finally(() => {
          if (!cancelled) setIsSearching(false);
        });
    }, 300);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query, tripId]);

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
                {isMembersLoading && (
                  <View className="items-center rounded-2xl border border-[#82F0DC] bg-white p-5">
                    <ActivityIndicator color="#075953" />
                  </View>
                )}
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
                        {/* 팀장은 먼저 팀장을 넘겨야 나갈 수 있어서 버튼을 주지 않는다. */}
                        {member.id === ME_ID && !isLeader && tripId && (
                          <Pressable
                            onPress={async () => {
                              const previous = members;
                              setMembers((cur) => cur.filter((m) => m.id !== ME_ID));
                              try {
                                await leaveTrip(tripId);
                                onClose();
                              } catch (e) {
                                setMembers(previous);
                                Alert.alert('PINKOK', errorMessage(e, '여행에서 나가지 못했어요.'));
                              }
                            }}
                            className="h-8 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 px-2.5 active:bg-rose-100"
                          >
                            <Text className="text-[9px] font-black text-rose-600">나가기</Text>
                          </Pressable>
                        )}
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
                            onPress={async () => {
                              setActionMemberId(null);
                              if (!tripId || member.userId === undefined) return;
                              const previous = members;
                              setMembers((cur) => cur.filter((m) => m.id !== member.id));
                              try {
                                await removeTripMember(tripId, member.userId);
                              } catch (e) {
                                setMembers(previous);
                                Alert.alert('PINKOK', errorMessage(e, '팀원을 내보내지 못했어요.'));
                              }
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
            {isSearching ? (
              <View className="items-center rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
                <ActivityIndicator color="#075953" />
              </View>
            ) : searchError ? (
              <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
                <Text className="text-center text-xs font-bold text-slate-500">{searchError}</Text>
              </View>
            ) : results.length > 0 ? (
              results.map((user) => {
                // 이미 팀원이거나 답을 기다리는 중이면 다시 초대할 수 없다.
                const invited = invitedLoginIds.includes(user.username) || user.alreadyInvited;
                const disabled = invited || user.alreadyMember;
                const avatar = fileUrl(user.avatarImageUrl ?? user.profileImageUrl) ?? '🙂';
                return (
                  <View key={user.username} className="flex-row items-center gap-3 rounded-2xl border border-[#82F0DC] bg-white p-3 shadow-sm">
                    <ShadowBox
                      shadowColor="#56EADD"
                      offset={2}
                      radius={12}
                      className="h-12 w-12 items-center justify-center overflow-hidden rounded-xl border-2 border-pixel-navy bg-[#B4FAF0]"
                    >
                      {isImageAvatar(avatar) ? (
                        <Image source={toImageSource(avatar)} resizeMode="cover" className="h-full w-full" />
                      ) : (
                        <Text className="text-xl">{avatar}</Text>
                      )}
                    </ShadowBox>
                    <View className="min-w-0 flex-1">
                      <View className="flex-row items-center gap-2">
                        <Text className="text-sm font-black text-slate-900">{user.nickname}</Text>
                        <Text numberOfLines={1} className="flex-shrink font-pixel text-[8px] text-[#075953]">
                          @{user.username}
                        </Text>
                      </View>
                      <Text className="mt-1 text-[10px] font-semibold text-slate-500">
                        {user.alreadyMember ? '이미 팀원이에요' : user.alreadyInvited ? '초대에 답을 기다리는 중' : '초대할 수 있어요'}
                      </Text>
                    </View>
                    <PixelPressable
                      shadowColor="#075953"
                      offset={2}
                      radius={12}
                      disabled={disabled}
                      onPress={() => {
                        setInvitedLoginIds((cur) => [...cur, user.username]);
                        onInvite(user.nickname, user.username, avatar);
                      }}
                      className="h-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD] px-3"
                      disabledClassName="h-9 items-center justify-center rounded-xl border-2 border-slate-300 bg-slate-100 px-3"
                    >
                      <Text className={`text-[10px] font-black ${disabled ? 'text-slate-400' : 'text-pixel-navy'}`}>
                        {user.alreadyMember ? '팀원' : invited ? '초대됨' : '초대'}
                      </Text>
                    </PixelPressable>
                  </View>
                );
              })
            ) : (
              <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
                <Text className="text-center text-xs font-bold text-slate-400">
                  {query.trim().length < 2 ? '아이디나 닉네임을 2글자 이상 입력해 주세요.' : '검색 결과가 없습니다.'}
                </Text>
              </View>
            )}
          </View>
        </ScrollView>
      </View>
    </ModalBackdrop>
  );
}
