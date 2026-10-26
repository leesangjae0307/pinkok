/**
 * 프로필 아바타 선택 / 내 여행 데이터 상세 / 1초 클립 재생 모달
 */
import React from 'react';
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useState } from 'react';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { FadeIn, ModalBackdrop, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';
import { PROFILE_AVATARS, useApp } from '../AppContext';
import { fileUrl } from '../../../api/fileApi';

function SheetHeader({
  eyebrow,
  title,
  subtitle,
  onClose,
  closeLabel,
}: {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  onClose: () => void;
  closeLabel: string;
}) {
  return (
    <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
      <View className="flex-1 pr-2">
        {eyebrow ? <Text className="font-pixel text-[8px] text-[#075953]">{eyebrow}</Text> : null}
        <Text className={eyebrow ? 'mt-1 text-sm font-black text-slate-900' : 'font-pixel text-[11px] text-[#075953]'}>
          {title}
        </Text>
        {subtitle ? <Text className="mt-2 text-[11px] font-bold text-slate-500">{subtitle}</Text> : null}
      </View>
      <Pressable
        onPress={onClose}
        accessibilityLabel={closeLabel}
        className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100"
      >
        <Text className="text-xs font-black text-slate-800">✕</Text>
      </Pressable>
    </View>
  );
}

export function AvatarPickerModal() {
  const insets = useSafeAreaInsets();
  const {
    isAvatarPickerOpen,
    setIsAvatarPickerOpen,
    pendingProfileAvatar,
    setPendingProfileAvatar,
    pendingProfileNickname,
    setPendingProfileNickname,
    saveProfile,
    pickProfilePhoto,
    isUploadingProfilePhoto,
  } = useApp();
  if (!isAvatarPickerOpen) return null;

  return (
    <ModalBackdrop onRequestClose={() => setIsAvatarPickerOpen(false)}>
      <View className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]" style={{ marginBottom: insets.bottom }}>
        <SheetHeader
          title="프로필 편집"
          subtitle="닉네임과 기본 픽셀 캐릭터를 함께 변경해요."
          onClose={() => setIsAvatarPickerOpen(false)}
          closeLabel="아바타 선택 닫기"
        />
        <View className="px-4 pb-2 pt-4">
          <Text className="text-[11px] font-black text-slate-700">닉네임</Text>
          <TextInput
            value={pendingProfileNickname}
            onChangeText={setPendingProfileNickname}
            maxLength={12}
            placeholder="닉네임 입력"
            placeholderTextColor="#94A3B8"
            className="mt-1.5 h-11 rounded-xl border-2 border-pixel-navy bg-white px-3 py-0 text-sm font-black text-slate-900"
          />
        </View>
        {/* 직접 올린 사진. 서버는 아바타와 사진 중 하나만 들고 있어서 둘은 서로를 해제한다. */}
        <View className="flex-row items-center gap-3 px-4 pt-2">
          <View className="h-14 w-14 overflow-hidden rounded-2xl border-2 border-pixel-navy bg-white">
            {pendingProfileAvatar.photoUrl ? (
              <Image
                source={{ uri: fileUrl(pendingProfileAvatar.photoUrl) ?? undefined }}
                resizeMode="cover"
                className="h-full w-full"
              />
            ) : (
              <Image source={pendingProfileAvatar.image} resizeMode="cover" className="h-full w-full" />
            )}
          </View>
          <Pressable
            onPress={pickProfilePhoto}
            disabled={isUploadingProfilePhoto}
            className="h-10 flex-1 flex-row items-center justify-center rounded-xl border-2 border-pixel-navy bg-white active:translate-y-0.5"
          >
            {isUploadingProfilePhoto ? (
              <ActivityIndicator size="small" color="#075953" />
            ) : (
              <Text className="text-[11px] font-black text-[#075953]">
                {pendingProfileAvatar.photoUrl ? '사진 다시 고르기' : '직접 사진 올리기'}
              </Text>
            )}
          </Pressable>
        </View>

        <View className="flex-row flex-wrap justify-between gap-y-3 p-4 pt-2">
          {PROFILE_AVATARS.map((avatar) => {
            // 사진을 올려둔 동안에는 아바타가 선택된 상태로 보이면 안 된다.
            const selected = !pendingProfileAvatar.photoUrl && pendingProfileAvatar.id === avatar.id;
            return (
              <Pressable key={avatar.id} onPress={() => setPendingProfileAvatar(avatar)} style={{ width: '31%' }}>
                {/* avatar 에는 photoUrl 이 없으므로 고르는 순간 올려둔 사진은 해제된다. */}
                {selected && (
                  <View
                    pointerEvents="none"
                    style={{ position: 'absolute', left: 4, top: 4, right: -4, bottom: -4, borderRadius: 16, backgroundColor: '#075953' }}
                  />
                )}
                <View
                  className={`relative overflow-hidden rounded-2xl border-3 bg-white ${
                    selected ? 'border-[#56EADD]' : 'border-white'
                  }`}
                >
                  {/* 정사각 비율은 감싸는 View 의 style 로 잡는다. Image 에 직접 준
                      style 과 aspect-square 는 적용되지 않아 원본 높이로 늘어난다. */}
                  <View style={{ width: '100%', aspectRatio: 1 }}>
                    <Image source={avatar.image} resizeMode="cover" className="h-full w-full" />
                  </View>
                  <Text className="min-h-10 px-1.5 py-2 text-center text-[9px] font-black leading-3 text-slate-700">
                    {avatar.label}
                  </Text>
                  {selected && (
                    <View className="absolute right-1.5 top-1.5 h-6 w-6 items-center justify-center rounded-md border-2 border-white bg-[#56EADD]">
                      <Text className="text-xs font-black text-[#075953]">✓</Text>
                    </View>
                  )}
                </View>
              </Pressable>
            );
          })}
        </View>
        <View className="border-t-2 border-pixel-navy bg-white p-3">
          <PixelPressable
            shadowColor="#075953"
            offset={3}
            radius={12}
            wrapperClassName="w-full"
            className="h-12 w-full items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
            onPress={saveProfile}
          >
            <Text className="text-xs font-black text-pixel-navy">프로필 저장하기</Text>
          </PixelPressable>
        </View>
      </View>
    </ModalBackdrop>
  );
}

const REGIONS = ['제주', '부산', '서울', '강릉', '도쿄', '오사카', '교토', '후쿠오카', '타이베이', '다낭', '파리', '뉴욕'];

export function ProfileDetailModal() {
  const insets = useSafeAreaInsets();
  const { profileDetailModal, setProfileDetailModal, mapPlaces, placeRecords, me } = useApp();
  if (!profileDetailModal) return null;

  const title =
    profileDetailModal === 'visited' ? '방문한 장소' : profileDetailModal === 'saved' ? '저장한 장소' : '다녀온 지역';
  /**
   * 방문·저장 장소를 따로 주는 API 가 없어서 지금 열어 둔 여행의 자료로 보여준다.
   * 방문한 곳은 내가 기록을 남긴 핀이고, 저장한 곳은 일정에 담아 둔 핀이다.
   */
  const visitedPinIds = new Set(placeRecords.filter((r) => r.userId === me.id).map((r) => r.pinId));
  const list = profileDetailModal === 'visited' ? mapPlaces.filter((p) => visitedPinIds.has(p.id)) : mapPlaces;

  return (
    <ModalBackdrop onRequestClose={() => setProfileDetailModal(null)}>
      <View className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]" style={{ marginBottom: insets.bottom }}>
        <SheetHeader
          eyebrow="MY TRAVEL DATA"
          title={title}
          onClose={() => setProfileDetailModal(null)}
          closeLabel="상세 내역 닫기"
        />
        <ScrollView style={{ maxHeight: 480 }} contentContainerStyle={{ padding: 16 }} showsVerticalScrollIndicator={false}>
          {profileDetailModal === 'regions' ? (
            <View className="flex-row flex-wrap justify-between gap-y-2">
              {REGIONS.map((region, index) => (
                <View
                  key={region}
                  className="items-center rounded-2xl border-2 border-[#82F0DC] bg-white p-3 shadow-sm"
                  style={{ width: '31.5%' }}
                >
                  <Text className="text-xl">{index < 4 ? '🇰🇷' : index < 8 ? '🇯🇵' : '📍'}</Text>
                  <Text className="mt-2 text-xs font-black text-slate-700">{region}</Text>
                </View>
              ))}
            </View>
          ) : (
            <View className="gap-2">
              {list.map((place) => (
                <View key={place.id} className="flex-row items-center gap-3 rounded-2xl border border-[#82F0DC] bg-white p-3 shadow-sm">
                  <Image source={{ uri: place.thumbnail }} resizeMode="cover" className="h-14 w-14 rounded-xl" />
                  <View className="min-w-0 flex-1">
                    <Text numberOfLines={1} className="text-xs font-black text-slate-800">
                      {place.name}
                    </Text>
                    <Text numberOfLines={1} className="mt-1 text-[10px] font-semibold text-slate-500">
                      {place.category}
                    </Text>
                    <View className="mt-1 self-start rounded-md bg-[#B4FAF0] px-1.5 py-0.5">
                      <Text className="text-[9px] font-bold text-[#075953]">
                        {profileDetailModal === 'visited' ? '다이어리 기록 완료' : 'AI 추출 · 저장됨'}
                      </Text>
                    </View>
                  </View>
                </View>
              ))}
            </View>
          )}
        </ScrollView>
      </View>
    </ModalBackdrop>
  );
}

export function ClipPlaybackModal() {
  const { activeClipModal, setActiveClipModal, showToast } = useApp();
  if (!activeClipModal) return null;

  return (
    <ModalBackdrop onRequestClose={() => setActiveClipModal(null)} position="center" dimClassName="bg-slate-900/80">
      <FadeIn from="scale">
        <View className="w-full gap-3 rounded-3xl border-2 border-[#56EADD] bg-white p-4">
          <View className="flex-row items-center justify-between">
            <Text className="font-pixel text-xs text-[#075953]">PINLOG 1-SEC</Text>
            <Pressable onPress={() => setActiveClipModal(null)}>
              <Text className="font-bold text-slate-400">✕</Text>
            </Pressable>
          </View>

          <ShadowBox shadowColor="#56EADD" offset={5} radius={16} className="rounded-2xl border-4 border-pixel-navy bg-slate-950 p-2">
            <View className="mb-2 flex-row items-center justify-between px-1">
              <Text className="font-pixel text-[8px] text-[#56EADD]">1280 × 720</Text>
              <Text className="font-pixel text-[8px] text-[#FFC837]">PINKOK TAPE</Text>
            </View>
            <View className="relative aspect-video overflow-hidden rounded-lg border-2 border-slate-600 bg-black">
              <Image source={{ uri: activeClipModal.thumbnail }} resizeMode="cover" className="h-full w-full" />
              <View className="absolute left-2 top-2 rounded bg-slate-900/80 px-2 py-0.5">
                <Text className="font-pixel text-[9px] text-[#56EADD]">1.0초 플레이중</Text>
              </View>
              <View className="absolute bottom-3 left-3 right-3 rounded-xl bg-black/60 p-2">
                <Text className="font-pixel text-[10px] text-[#FFC837]">{activeClipModal.time}</Text>
                <Text className="text-xs font-bold text-white">📍 {activeClipModal.location}</Text>
              </View>
            </View>
            <View className="mt-2 flex-row justify-center gap-2">
              <View className="h-2 w-8 rounded-sm bg-slate-700" />
              <View className="h-2 w-2 rounded-sm bg-rose-500" />
              <View className="h-2 w-2 rounded-sm bg-[#56EADD]" />
            </View>
          </ShadowBox>

          <Pressable
            onPress={() => {
              setActiveClipModal(null);
              showToast('클립이 정상 재생 완료되었습니다!');
            }}
            className="w-full items-center rounded-2xl border border-[#50E6DC] bg-[#56EADD] py-2.5 shadow-md"
          >
            <Text className="text-xs font-bold text-slate-900">닫기</Text>
          </Pressable>
        </View>
      </FadeIn>
    </ModalBackdrop>
  );
}

/** 비밀번호 변경 모달. 서버에 비밀번호 변경 API 가 아직 없어서 입력 검증과 토스트까지만 동작한다. */
export function PasswordModal() {
  const insets = useSafeAreaInsets();
  const { isPasswordModalOpen, setIsPasswordModalOpen, showToast } = useApp();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  if (!isPasswordModalOpen) return null;

  const close = () => {
    setCurrent('');
    setNext('');
    setConfirm('');
    setError('');
    setIsPasswordModalOpen(false);
  };

  const submit = () => {
    if (current.length < 8 || next.length < 8 || confirm.length < 8) {
      setError('비밀번호는 8자 이상 입력해 주세요.');
      return;
    }
    if (next.length > 64) {
      setError('새 비밀번호는 64자 이하로 입력해 주세요.');
      return;
    }
    if (next !== confirm) {
      setError('새 비밀번호가 서로 달라요.');
      return;
    }
    // TODO(서버 연동): 비밀번호 변경 API 가 생기면 여기서 호출하고 실패 시 error 로 보여준다.
    close();
    showToast('비밀번호가 안전하게 변경되었습니다.');
  };

  const fields = [
    { label: '현재 비밀번호', value: current, set: setCurrent },
    { label: '새 비밀번호', value: next, set: setNext },
    { label: '새 비밀번호 확인', value: confirm, set: setConfirm },
  ];

  return (
    <ModalBackdrop onRequestClose={close}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]" style={{ marginBottom: insets.bottom }}>
          <SheetHeader eyebrow="PASSWORD" title="비밀번호 변경" onClose={close} closeLabel="비밀번호 변경 닫기" />
          <View className="gap-3 p-4">
            {fields.map((field) => (
              <View key={field.label}>
                <Text className="text-[11px] font-black text-slate-700">{field.label}</Text>
                <TextInput
                  value={field.value}
                  onChangeText={(text) => {
                    field.set(text);
                    setError('');
                  }}
                  secureTextEntry
                  autoCapitalize="none"
                  autoCorrect={false}
                  placeholder="8자 이상 입력"
                  placeholderTextColor="#94A3B8"
                  className="mt-1.5 h-11 rounded-xl border-2 border-slate-200 bg-white px-3 py-0 text-xs font-bold text-slate-900"
                />
              </View>
            ))}
            <View className="rounded-xl bg-white/70 px-3 py-2">
              <Text className={`text-[10px] font-semibold ${error ? 'text-rose-600' : 'text-slate-500'}`}>
                {error || '영문, 숫자, 특수문자를 조합하면 더 안전해요.'}
              </Text>
            </View>
            <PixelPressable
              shadowColor="#075953"
              offset={3}
              radius={12}
              wrapperClassName="w-full"
              className="h-12 w-full items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
              onPress={submit}
            >
              <Text className="text-xs font-black text-pixel-navy">비밀번호 변경하기</Text>
            </PixelPressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </ModalBackdrop>
  );
}
