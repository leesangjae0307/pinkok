/**
 * 새 여행 만들기 (웹 CreateTripScreen.tsx 이관)
 *
 * 표지 · 제목 · 지역 · 대표 이모지 · 날짜 · 팀원 · 테마 태그를 받아 onCreate 로 넘긴다.
 * 웹과 다른 점
 * - 날짜: <input type="date"> 대신 PixelCalendar 모달
 * - 표지: 📷 촬영 / 🖼️ 갤러리가 실제 카메라·사진첩을 연다 (react-native-image-picker)
 * - "N박 M일" 을 고정 문구 대신 선택한 날짜로 계산
 */
import React, { useState } from 'react';
import { ActivityIndicator, Alert, Image, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { ImagePickerResponse, launchCamera, launchImageLibrary } from 'react-native-image-picker';

import { uploadFile } from '../../../api/fileApi';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { DEFAULT_COVERS } from '../../../assets/images/covers';
import PixelCalendar, { addDays, nightsLabel, toYmd } from '../../../components/pixel/PixelCalendar';
import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { LinearGradient, PixelPressable, ShadowBox } from '../../../components/pixel/Primitives';
import { AvatarSource, isImageAvatar, toImageSource } from '../../../data/mockData';

export interface InvitedTripMember {
  nickname: string;
  /** 화면에 @ 로 보이는 로그인 아이디 */
  loginId: string;
  avatar: AvatarSource;
}

export interface NewTripPayload {
  title: string;
  location: string;
  emoji: string;
  /** 'YYYY-MM-DD' */
  startDate: string;
  endDate: string;
  coverImage: AvatarSource;
  /** 직접 올린 표지의 서버 주소. 기본 픽셀 표지를 고르면 없다. */
  coverImageUrl?: string;
  tags: string[];
}

const BASE_EMOJIS = ['🌴', '🌊', '🗼', '✈️', '🍜', '☕', '🌲', '📷'];
const BASE_TAGS = ['힐링', '맛집탐방', '카페투어', '자연', '쇼핑', '액티비티'];
const TITLE_MAX = 20;

const formatDate = (date: string) => date.replace(/-/g, '.');

function MemberAvatar({ value }: { value: AvatarSource }) {
  return isImageAvatar(value) ? (
    <Image source={toImageSource(value)} resizeMode="cover" className="h-full w-full" />
  ) : (
    <Text className="text-xl">{value}</Text>
  );
}

/** 탭형 토글 버튼 (기본 픽셀 이미지 / 직접 사진, 이모지, 태그 공통 스타일) */
function chipClass(selected: boolean) {
  return selected ? 'border-[#075953] bg-[#56EADD]' : 'border-slate-200 bg-slate-50';
}

export default function CreateTripScreen({
  hostName,
  hostAvatar,
  invitedMembers,
  onBack,
  onOpenInvite,
  onRemoveMember,
  onCreate,
  isCreating = false,
}: {
  hostName: string;
  hostAvatar: AvatarSource;
  invitedMembers: InvitedTripMember[];
  onBack: () => void;
  onOpenInvite: () => void;
  onRemoveMember: (loginId: string) => void;
  onCreate: (payload: NewTripPayload) => void;
  /** 서버에 만드는 중. 버튼을 잠가 두 번 눌리지 않게 한다. */
  isCreating?: boolean;
}) {
  const insets = useSafeAreaInsets();
  const [title, setTitle] = useState('');
  const [location, setLocation] = useState('');
  const [emoji, setEmoji] = useState(BASE_EMOJIS[0]);
  const [customEmoji, setCustomEmoji] = useState('');
  const [emojis, setEmojis] = useState(BASE_EMOJIS);
  const [coverImage, setCoverImage] = useState<AvatarSource>(DEFAULT_COVERS[0].src);
  const [coverTab, setCoverTab] = useState<'default' | 'upload'>('default');
  /** POST /files 가 돌려준 주소. 기본 픽셀 표지를 쓰면 비어 있다. */
  const [coverImageUrl, setCoverImageUrl] = useState<string | undefined>(undefined);
  const [isUploadingCover, setIsUploadingCover] = useState(false);
  // 기본값: 일주일 뒤 출발 3박 4일
  const [startDate, setStartDate] = useState(() => addDays(toYmd(new Date()), 7));
  const [endDate, setEndDate] = useState(() => addDays(toYmd(new Date()), 10));
  const [calendarOpen, setCalendarOpen] = useState(false);
  const [tags, setTags] = useState(BASE_TAGS);
  const [selectedTags, setSelectedTags] = useState<string[]>([BASE_TAGS[0]]);
  const [customTag, setCustomTag] = useState('');
  const [focused, setFocused] = useState<'title' | 'location' | 'emoji' | 'tag' | null>(null);

  const canCreate = title.trim().length > 0 && location.trim().length > 0;

  const addCustomEmoji = () => {
    const value = customEmoji.trim();
    if (!value) {
      return;
    }
    if (!emojis.includes(value)) {
      setEmojis((current) => [...current, value]);
    }
    setEmoji(value);
    setCustomEmoji('');
  };

  const addCustomTag = () => {
    const value = customTag.trim().replace(/^#/, '');
    if (!value) {
      return;
    }
    if (!tags.includes(value)) {
      setTags((current) => [...current, value]);
    }
    if (!selectedTags.includes(value)) {
      setSelectedTags((current) => [...current, value]);
    }
    setCustomTag('');
  };

  /**
   * 고른 사진을 바로 올려 둔다. 생성 버튼을 누를 때 한꺼번에 올리면 그만큼 기다려야 하고,
   * 업로드가 실패했을 때 이미 만들어진 여행만 남는 어정쩡한 상태가 된다.
   */
  const applyPickedCover = async (res: ImagePickerResponse) => {
    if (res.didCancel) {
      return;
    }
    const asset = res.assets?.[0];
    if (res.errorCode || !asset?.uri) {
      Alert.alert('PINKOK', res.errorMessage ?? '사진을 불러오지 못했어요.');
      return;
    }
    setCoverTab('upload');
    setCoverImage(asset.uri);
    setIsUploadingCover(true);
    try {
      const uploaded = await uploadFile({ uri: asset.uri, fileName: asset.fileName, type: asset.type });
      setCoverImageUrl(uploaded.url);
    } catch {
      setCoverImageUrl(undefined);
      Alert.alert('PINKOK', '사진을 올리지 못했어요. 기본 표지로 만들어집니다.');
    } finally {
      setIsUploadingCover(false);
    }
  };

  const handleCreate = () =>
    onCreate({
      title: title.trim(),
      location: location.trim(),
      emoji,
      startDate,
      endDate,
      coverImage,
      coverImageUrl: coverTab === 'upload' ? coverImageUrl : undefined,
      tags: selectedTags,
    });

  const inputBorder = (key: typeof focused) => (focused === key ? 'border-[#56EADD]' : 'border-slate-200');

  return (
    <View className="flex-1 bg-[#B4FAF0]">
      {/* 헤더 (웹의 sticky header 와 같이 스크롤 밖에 고정) */}
      <View className="flex-row items-center border-b-2 border-[#82F0DC] bg-white/95 px-4 py-3">
        <View className="w-14">
          <Pressable
            onPress={onBack}
            accessibilityLabel="새 여행 만들기 취소"
            className="h-10 w-10 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5"
          >
            <PixelBackArrow size={16} />
          </Pressable>
        </View>
        <Text className="flex-1 text-center font-pixel text-[11px] text-[#075953]">새 여행 만들기</Text>
        <View className="w-14">
          <PixelPressable
            shadowColor="#075953"
            offset={2}
            radius={12}
            disabled={!canCreate || isCreating || isUploadingCover}
            accessibilityLabel="여행 생성"
            className="h-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
            disabledClassName="h-9 items-center justify-center rounded-xl border-2 border-slate-300 bg-slate-200"
            onPress={handleCreate}
          >
            {isCreating ? (
              <ActivityIndicator size="small" color="#075953" />
            ) : (
              <Text className={`text-[10px] font-black ${canCreate ? 'text-pixel-navy' : 'text-slate-400'}`}>생성</Text>
            )}
          </PixelPressable>
        </View>
      </View>

      <ScrollView
        className="flex-1"
        contentContainerStyle={{ padding: 16, paddingBottom: insets.bottom + 32, gap: 16 }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* 표지 */}
        <ShadowBox
          shadowColor="#56EADD"
          offset={4}
          radius={24}
          className="overflow-hidden rounded-3xl border-3 border-pixel-navy bg-white"
        >
          <View className="relative h-48 overflow-hidden bg-slate-900">
            <Image source={toImageSource(coverImage)} resizeMode="cover" className="h-full w-full" />
            <LinearGradient
              colors={['rgba(0,0,0,0)', 'rgba(0,0,0,0)', 'rgba(0,0,0,0.65)']}
              start={{ x: 0, y: 0 }}
              end={{ x: 0, y: 1 }}
              className="absolute inset-0"
            />
            <Text className="absolute bottom-3 left-3 text-4xl">{emoji}</Text>
            <View className="absolute bottom-3 right-3 flex-row gap-2">
              <Pressable
                onPress={() => launchCamera({ mediaType: 'photo', saveToPhotos: false }).then(applyPickedCover)}
                className="rounded-xl border border-white/50 bg-black/55 px-2.5 py-2 active:opacity-70"
              >
                <Text className="text-[10px] font-black text-white">📷 촬영</Text>
              </Pressable>
              <Pressable
                onPress={() => launchImageLibrary({ mediaType: 'photo', selectionLimit: 1 }).then(applyPickedCover)}
                className="rounded-xl border border-white/50 bg-black/55 px-2.5 py-2 active:opacity-70"
              >
                <Text className="text-[10px] font-black text-white">🖼️ 갤러리</Text>
              </Pressable>
            </View>
          </View>

          <View className="p-3">
            <View className="flex-row gap-2">
              {(
                [
                  ['default', '기본 픽셀 이미지'],
                  ['upload', '직접 사진'],
                ] as const
              ).map(([key, label]) => (
                <Pressable
                  key={key}
                  onPress={() => setCoverTab(key)}
                  className={`h-9 flex-1 items-center justify-center rounded-xl border-2 ${chipClass(coverTab === key)}`}
                >
                  <Text className={`text-[10px] font-black ${coverTab === key ? 'text-[#075953]' : 'text-slate-500'}`}>
                    {label}
                  </Text>
                </Pressable>
              ))}
            </View>

            {coverTab === 'default' ? (
              <ScrollView horizontal showsHorizontalScrollIndicator={false} className="mt-3" contentContainerClassName="gap-2 pb-1">
                {DEFAULT_COVERS.map((cover) => {
                  const selected = coverImage === cover.src;
                  return (
                    <Pressable
                      key={cover.id}
                      onPress={() => setCoverImage(cover.src)}
                      accessibilityLabel={`${cover.label} 픽셀 표지`}
                      className={`w-28 overflow-hidden rounded-xl border-3 bg-white ${
                        selected ? 'border-[#56EADD]' : 'border-white'
                      }`}
                    >
                      <View className="w-full" style={{ aspectRatio: 16 / 9 }}>
                        <Image source={cover.src} resizeMode="cover" className="h-full w-full" />
                      </View>
                    </Pressable>
                  );
                })}
              </ScrollView>
            ) : (
              <Text className="mt-3 text-center text-[10px] font-semibold text-slate-500">
                위의 📷 촬영 · 🖼️ 갤러리로 표지 사진을 골라주세요.
              </Text>
            )}
          </View>
        </ShadowBox>

        {/* 제목 · 지역 · 이모지 */}
        <View className="gap-4 rounded-3xl border border-[#82F0DC] bg-white p-4 shadow-sm">
          <View>
            <View className="flex-row items-center justify-between">
              <Text className="text-xs font-black text-slate-700">여행 제목</Text>
              <Text className="font-pixel text-[8px] text-slate-400">
                {title.length}/{TITLE_MAX}
              </Text>
            </View>
            <TextInput
              value={title}
              maxLength={TITLE_MAX}
              onChangeText={setTitle}
              onFocus={() => setFocused('title')}
              onBlur={() => setFocused(null)}
              placeholder="제주도 감성 카페 & 오름 투어"
              placeholderTextColor="#94A3B8"
              className={`mt-2 h-12 rounded-xl border-2 px-3 text-sm font-bold text-slate-900 ${inputBorder('title')}`}
            />
          </View>

          <View>
            <Text className="text-xs font-black text-slate-700">여행지 (지역)</Text>
            <View className={`mt-2 h-12 flex-row items-center gap-2 rounded-xl border-2 px-3 ${inputBorder('location')}`}>
              <Text>🔍</Text>
              <TextInput
                value={location}
                onChangeText={setLocation}
                onFocus={() => setFocused('location')}
                onBlur={() => setFocused(null)}
                placeholder="부산, 제주, 도쿄 등 지역 입력"
                placeholderTextColor="#94A3B8"
                className="h-full flex-1 p-0 text-sm font-bold text-slate-900"
              />
            </View>
          </View>

          <View>
            <Text className="text-xs font-black text-slate-700">대표 픽셀 이모지</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} className="mt-2" contentContainerClassName="gap-2 pb-1">
              {emojis.map((item) =>
                emoji === item ? (
                  <ShadowBox
                    key={item}
                    shadowColor="#075953"
                    offset={2}
                    radius={12}
                    className="h-11 w-11 items-center justify-center rounded-xl border-2 border-[#075953] bg-[#56EADD]"
                  >
                    <Text className="text-xl">{item}</Text>
                  </ShadowBox>
                ) : (
                  <Pressable
                    key={item}
                    onPress={() => setEmoji(item)}
                    className="h-11 w-11 items-center justify-center rounded-xl border-2 border-slate-200 bg-slate-50"
                  >
                    <Text className="text-xl">{item}</Text>
                  </Pressable>
                ),
              )}
            </ScrollView>
            <View className="mt-2 flex-row gap-2">
              <TextInput
                value={customEmoji}
                maxLength={4}
                onChangeText={setCustomEmoji}
                onSubmitEditing={addCustomEmoji}
                onFocus={() => setFocused('emoji')}
                onBlur={() => setFocused(null)}
                returnKeyType="done"
                placeholder="다른 이모지 입력"
                placeholderTextColor="#94A3B8"
                className={`h-10 flex-1 rounded-xl border-2 px-3 text-xs font-bold text-slate-900 ${inputBorder('emoji')}`}
              />
              <Pressable
                onPress={addCustomEmoji}
                className="items-center justify-center rounded-xl border-2 border-[#075953] bg-[#B4FAF0] px-3 active:opacity-70"
              >
                <Text className="text-[10px] font-black text-[#075953]">추가</Text>
              </Pressable>
            </View>
          </View>
        </View>

        {/* 날짜 */}
        <View className="rounded-3xl border border-[#82F0DC] bg-white p-4 shadow-sm">
          <Text className="text-xs font-black text-slate-700">여행 날짜</Text>
          <Pressable
            onPress={() => setCalendarOpen(true)}
            className="mt-2 h-12 flex-row items-center justify-between rounded-xl border-2 border-slate-200 px-3 active:bg-slate-50"
          >
            <Text className="text-[11px] font-bold text-slate-700">
              📅 {formatDate(startDate)} ~ {formatDate(endDate)}
            </Text>
            <Text className="font-pixel text-[8px] text-[#075953]">{nightsLabel(startDate, endDate)} ›</Text>
          </Pressable>
        </View>

        {/* 팀원 */}
        <View className="rounded-3xl border border-[#82F0DC] bg-white p-4 shadow-sm">
          <View className="flex-row items-center justify-between">
            <Text className="text-xs font-black text-slate-700">함께 가는 팀원</Text>
            <Pressable
              onPress={onOpenInvite}
              className="rounded-xl border-2 border-[#075953] bg-[#56EADD] px-3 py-1.5 active:opacity-80"
            >
              <Text className="text-[10px] font-black text-[#075953]">+ 팀원 초대</Text>
            </Pressable>
          </View>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} className="mt-3" contentContainerClassName="gap-2 pb-1">
            <View className="flex-row items-center gap-2 rounded-xl border-2 border-[#56EADD] bg-[#B4FAF0]/45 p-2">
              <View className="h-9 w-9 items-center justify-center overflow-hidden rounded-lg border border-white">
                <MemberAvatar value={hostAvatar} />
              </View>
              <View>
                <Text className="text-[10px] font-black text-slate-800">{hostName}</Text>
                <Text className="font-pixel text-[7px] text-[#075953]">HOST</Text>
              </View>
            </View>
            {invitedMembers.map((member) => (
              <View
                key={member.loginId}
                className="flex-row items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 p-2"
              >
                <View className="h-9 w-9 items-center justify-center overflow-hidden rounded-lg bg-white">
                  <MemberAvatar value={member.avatar} />
                </View>
                <Text className="text-[10px] font-black text-slate-700">{member.nickname}</Text>
                <Pressable
                  onPress={() => onRemoveMember(member.loginId)}
                  hitSlop={8}
                  accessibilityLabel={`${member.nickname} 삭제`}
                >
                  <Text className="text-[10px] font-black text-slate-400">✕</Text>
                </Pressable>
              </View>
            ))}
          </ScrollView>
        </View>

        {/* 테마 태그 */}
        <View className="rounded-3xl border border-[#82F0DC] bg-white p-4 shadow-sm">
          <Text className="text-xs font-black text-slate-700">여행 테마 & 스타일</Text>
          <Text className="mt-1 text-[9px] font-semibold text-slate-400">
            선택한 태그는 AI 장소 추천과 동선 최적화에 반영돼요.
          </Text>
          <View className="mt-3 flex-row flex-wrap gap-2">
            {tags.map((tag) => {
              const selected = selectedTags.includes(tag);
              return (
                <Pressable
                  key={tag}
                  onPress={() =>
                    setSelectedTags((current) => (selected ? current.filter((t) => t !== tag) : [...current, tag]))
                  }
                  className={`rounded-xl border-2 px-2.5 py-1.5 ${chipClass(selected)}`}
                >
                  <Text className={`text-[10px] font-black ${selected ? 'text-[#075953]' : 'text-slate-500'}`}>#{tag}</Text>
                </Pressable>
              );
            })}
          </View>
          <View className="mt-3 flex-row gap-2">
            <TextInput
              value={customTag}
              maxLength={12}
              onChangeText={setCustomTag}
              onSubmitEditing={addCustomTag}
              onFocus={() => setFocused('tag')}
              onBlur={() => setFocused(null)}
              returnKeyType="done"
              placeholder="#나만의 해시태그"
              placeholderTextColor="#94A3B8"
              className={`h-10 flex-1 rounded-xl border-2 px-3 text-xs font-bold text-slate-900 ${inputBorder('tag')}`}
            />
            <Pressable
              onPress={addCustomTag}
              className="items-center justify-center rounded-xl border-2 border-[#075953] bg-[#B4FAF0] px-3 active:opacity-70"
            >
              <Text className="text-[10px] font-black text-[#075953]">추가</Text>
            </Pressable>
          </View>
        </View>
      </ScrollView>

      {calendarOpen && (
        <PixelCalendar
          initialStart={startDate}
          initialEnd={endDate}
          onCancel={() => setCalendarOpen(false)}
          onApply={(start, end) => {
            setStartDate(start);
            setEndDate(end);
            setCalendarOpen(false);
          }}
        />
      )}
    </View>
  );
}
