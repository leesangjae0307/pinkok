/**
 * 장소별 팀 다이어리 모달 (1인 1기록 규칙 · 앱 내 2~3초 촬영 · 팀원 피드)
 */
import React, { useEffect, useState } from 'react';
import { Alert, Image, Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';

import { AvatarSource, PlaceItem, PlaceRecord, isImageAvatar, toImageSource } from '../../../data/mockData';
import RecordClipModal from './RecordClipModal';
import ClipPreviewModal, { type ClipPreview } from './ClipPreviewModal';
import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { FadeIn } from '../../../components/pixel/Primitives';

interface PlaceDetailModalProps {
  place: PlaceItem;
  records: PlaceRecord[];
  onClose: () => void;
  onSaveRecord: (record: PlaceRecord) => void;
  onDeleteRecord: (recordId: number) => void;
  onOpenPinLogMaker: () => void;
  currentUserAvatarUrl: AvatarSource;
  /** 로그인한 사용자. 내 기록인지 가릴 때 쓴다. */
  currentUserId: number;
  currentUserName: string;
  /** true 면 기록 편집 + 카메라 화면으로 바로 연다 (지도 바텀시트의 "바로 촬영") */
  startInCamera?: boolean;
}

/** 서버에 이미 올라간 주소인지. file://·content:// 는 아직 안 올린 로컬 파일이다. */
const isRemoteUrl = (url: string) => /^https?:\/\//.test(url);

/** POST /files 가 한 번에 받는 수. 저장할 때 새 사진을 한 번에 올리므로 여기서 막는다. */
const MAX_PHOTOS = 10;

export default function PlaceDetailModal({
  place,
  records,
  onClose,
  onSaveRecord,
  onDeleteRecord,
  onOpenPinLogMaker,
  currentUserAvatarUrl,
  currentUserId,
  currentUserName,
  startInCamera = false,
}: PlaceDetailModalProps) {
  const myExistingRecord = records.find((r) => r.pinId === place.id && r.userId === currentUserId);

  const [isEditing, setIsEditing] = useState(startInCamera);
  const [rating, setRating] = useState(myExistingRecord?.rating || 5);
  const [memoText, setMemoText] = useState(myExistingRecord?.memo || '');
  const [photos, setPhotos] = useState<string[]>(myExistingRecord?.photos || []);
  const [videoClip, setVideoClip] = useState<PlaceRecord['videoClip'] | undefined>(myExistingRecord?.videoClip);
  const [memoFocused, setMemoFocused] = useState(false);

  // 촬영
  const [isCameraActive, setIsCameraActive] = useState(startInCamera);

  useEffect(() => {
    if (myExistingRecord) {
      setRating(myExistingRecord.rating);
      setMemoText(myExistingRecord.memo);
      setPhotos(myExistingRecord.photos);
      setVideoClip(myExistingRecord.videoClip);
    } else {
      setRating(5);
      setMemoText('');
      setPhotos([]);
      setVideoClip(undefined);
    }
  }, [myExistingRecord, place.id]);


  /** 미리보기로 재생 중인 클립. null 이면 닫혀 있다. */
  const [previewClip, setPreviewClip] = useState<ClipPreview | null>(null);

  /**
   * 갤러리에서 사진을 고른다.
   *
   * 고른 파일의 로컬 주소(file:// 또는 content://)를 그대로 photos 에 담아 둔다.
   * 업로드는 기록을 저장할 때 uploadRecordMedia 가 한 번에 한다 — 쓰다 만 기록의
   * 사진이 서버에 쌓이지 않게 하려는 것이다.
   */
  const handlePickPhotos = async () => {
    try {
      const room = MAX_PHOTOS - photos.length;
      if (room <= 0) {
        Alert.alert('PINKOK', `사진은 ${MAX_PHOTOS}장까지 넣을 수 있어요.`);
        return;
      }
      const res = await launchImageLibrary({ mediaType: 'photo', selectionLimit: room });
      if (res.didCancel) return;
      const picked = (res.assets ?? [])
        .map((asset) => asset.uri)
        .filter((uri): uri is string => !!uri)
        .slice(0, room);
      if (picked.length === 0) {
        if (res.errorCode) Alert.alert('PINKOK', '갤러리를 열지 못했어요. 사진 접근 권한을 확인해 주세요.');
        return;
      }
      setPhotos((prev) => [...prev, ...picked].slice(0, MAX_PHOTOS));
    } catch {
      Alert.alert('PINKOK', '갤러리를 열지 못했어요. 잠시 후 다시 시도해 주세요.');
    }
  };

  /** 갤러리에서 PinLog 용 영상을 고른다. 합성에 쓰이므로 장소마다 한 개만 둔다. */
  const handlePickVideo = async () => {
    try {
      const res = await launchImageLibrary({ mediaType: 'video', selectionLimit: 1 });
      const asset = res.assets?.[0];
      if (res.didCancel || !asset?.uri) {
        if (!res.didCancel && res.errorCode) {
          Alert.alert('PINKOK', '갤러리를 열지 못했어요. 사진 접근 권한을 확인해 주세요.');
        }
        return;
      }
      setVideoClip({
        id: Date.now(),
        // 서버가 길이를 모르는 영상도 있어서 0 이면 저장할 때 실제 값으로 채워진다.
        duration: asset.duration ? Math.round(asset.duration * 10) / 10 : 0,
        // 썸네일은 올린 뒤 서버가 첫 장면으로 만들어 준다. 지금은 비워 둔다.
        thumbnail: '',
        videoUrl: asset.uri,
        createdAt: '방금 전',
      });
    } catch {
      Alert.alert('PINKOK', '갤러리를 열지 못했어요. 잠시 후 다시 시도해 주세요.');
    }
  };

  const handleSave = () => {
    if (!memoText.trim()) {
      Alert.alert('PINKOK', '메모 내용을 입력해주세요.');
      return;
    }
    const record: PlaceRecord = {
      id: myExistingRecord ? myExistingRecord.id : Date.now(),
      pinId: place.id,
      userId: currentUserId,
      authorName: currentUserName,
      authorAvatar: currentUserAvatarUrl,
      createdAt: '15:20',
      rating,
      memo: memoText,
      photos,
      videoClip,
    };
    onSaveRecord(record);
    setIsEditing(false);
  };

  const placeRecords = records.filter((r) => r.pinId === place.id);

  /* ------------------------------ 전체화면 카메라 ------------------------------ */
  if (isCameraActive) {
    return (
      <RecordClipModal
        title={place.name}
        onClose={() => setIsCameraActive(false)}
        onRecorded={(clip) => {
          // 업로드는 기록을 저장할 때 한 번에 한다(uploadRecordMedia 가 file:// 를 올린다).
          setVideoClip({
            id: Date.now(),
            duration: clip.durationMs / 1000,
            // 썸네일은 올린 뒤 서버가 첫 장면으로 만들어 준다.
            thumbnail: '',
            videoUrl: clip.uri,
            createdAt: '방금 전',
          });
          setIsCameraActive(false);
        }}
      />
    );
  }

  /* --------------------------------- 기본 모달 -------------------------------- */
  return (
    <Modal transparent visible animationType="fade" onRequestClose={onClose} statusBarTranslucent>
      <View className="flex-1 items-center justify-center bg-black/60 p-3">
        <View
          className="relative w-full max-w-md overflow-hidden rounded-3xl border-4 border-pixel-navy bg-[#bff2e1]"
          style={{ height: '92%', maxHeight: 800 }}
        >
          {/* 헤더 */}
          <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
            <Pressable
              onPress={onClose}
              className="h-8 w-8 items-center justify-center rounded-lg border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5 active:bg-slate-200"
            >
              <PixelBackArrow size={14} />
            </Pressable>

            <View className="flex-1 items-center px-2">
              <Text className="font-pixel text-[9px] tracking-wider text-teal-800">TEAM DIARY</Text>
              <Text numberOfLines={1} className="text-sm font-extrabold text-pixel-navy">
                📍 {place.name}
              </Text>
            </View>

            <Pressable
              onPress={onOpenPinLogMaker}
              accessibilityLabel="팀원 영상으로 PinLog 합성"
              className="flex-row items-center gap-1 rounded-xl border-2 border-pixel-navy bg-mint-primary px-2.5 py-1 active:translate-y-0.5 active:bg-teal-400"
            >
              <Text className="text-[10px] font-black text-pixel-navy">🎬</Text>
            </Pressable>
          </View>

          {/* 본문 */}
          <ScrollView className="flex-1" contentContainerStyle={{ padding: 16, gap: 16 }} keyboardShouldPersistTaps="handled">
            {/* 장소 요약 */}
            {/*
              썸네일은 뺀다. 서버가 핀 사진을 주지 않아 늘 같은 샘플 사진이 들어갔고,
              장소와 아무 상관 없는 그림이 정보처럼 보였다. 그 자리는 글자에 쓴다.
            */}
            <View className="flex-row items-center gap-3 rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-md">
              <View className="min-w-0 flex-1">
                <View className="self-start rounded-md bg-[#B4FAF0] px-2 py-0.5">
                  <Text className="text-[10px] font-bold text-teal-800">{place.category}</Text>
                </View>
                <Text numberOfLines={1} className="mt-0.5 text-sm font-black text-slate-800">
                  {place.name}
                </Text>
                <Text numberOfLines={1} className="text-[10px] text-slate-500">
                  {place.address}
                </Text>
                <View className="mt-1 flex-row items-center gap-2">
                  <Text className="font-pixel text-[9px] text-amber-500">★ {place.rating}</Text>
                  <Text className="text-[10px] font-bold text-slate-400">·</Text>
                  <View className="rounded bg-teal-50 px-1.5">
                    <Text className="text-[10px] font-bold text-teal-900">팀 기록 {placeRecords.length}개</Text>
                  </View>
                </View>
              </View>
            </View>

            {/* 1인 1기록 규칙 안내 */}
            <View className="rounded-2xl border-2 border-teal-700/40 bg-white/90 p-3">
              <View className="flex-row items-start gap-2">
                <View className="rounded-lg bg-amber-100 p-1">
                  <Text className="text-base">⚖️</Text>
                </View>
                <View className="flex-1 gap-0.5">
                  <Text className="text-xs font-black text-teal-900">1인 1기록 제약 규칙 (1 User = 1 Record)</Text>
                  <Text className="text-[11px] leading-snug text-slate-600">
                    [같은 핀 + 같은 유저] 당 <Text className="font-bold">오직 1개의 기록</Text>만 허용됩니다. 작성 후에는
                    자동으로 <Text className="font-bold">[수정]</Text> 모드로 전환됩니다.
                  </Text>
                  <View className="flex-row flex-wrap items-center gap-2 pt-1">
                    <Text className="text-[10px] font-bold text-teal-800">📝 메모 1개 (별점 포함)</Text>
                    <Text className="text-[10px] font-bold text-teal-800">·</Text>
                    <Text className="text-[10px] font-bold text-teal-800">🖼️ 사진 복수</Text>
                    <Text className="text-[10px] font-bold text-teal-800">·</Text>
                    <Text className="text-[10px] font-bold text-teal-800">🎥 2~3s 영상 최대 1개</Text>
                  </View>
                </View>
              </View>
            </View>

            {/* 내 기록 영역 */}
            <View className="flex-row items-center justify-between px-1">
              <View className="flex-row items-center gap-1">
                <Text className="text-xs font-black tracking-tight text-pixel-navy">내 기록 영역</Text>
                {myExistingRecord ? (
                  <View className="rounded bg-emerald-100 px-1.5 py-0.5">
                    <Text className="text-[9px] font-bold text-emerald-800">기록 완료됨 (수정 가능)</Text>
                  </View>
                ) : (
                  <View className="rounded bg-amber-100 px-1.5 py-0.5">
                    <Text className="text-[9px] font-bold text-amber-800">미작성 (신규 가능)</Text>
                  </View>
                )}
              </View>

              {!isEditing && (
                <Pressable
                  onPress={() => setIsEditing(true)}
                  className="flex-row items-center gap-1 rounded-xl border-2 border-pixel-navy bg-white px-3 py-1 active:translate-y-0.5 active:bg-teal-50"
                >
                  <Text className="text-xs font-black text-pixel-navy">{myExistingRecord ? '✏️ 내 기록 수정' : '+ 새 글 쓰기'}</Text>
                </Pressable>
              )}
            </View>

            {/* 작성/수정 폼 */}
            {isEditing && (
              <FadeIn from="up">
                <View className="gap-4 rounded-3xl border-3 border-pixel-navy bg-white p-4 shadow-xl">
                  <View className="flex-row items-center justify-between border-b border-slate-100 pb-2">
                    <View className="flex-row items-center gap-1.5">
                      <Text className="text-sm">✍️</Text>
                      <Text className="text-xs font-extrabold text-pixel-navy">
                        {myExistingRecord ? '기존 기록 수정하기' : '새 장소 기록 작성'}
                      </Text>
                    </View>
                    <Pressable onPress={() => setIsEditing(false)}>
                      <Text className="text-xs font-bold text-slate-400">취소</Text>
                    </Pressable>
                  </View>

                  {/* 별점 */}
                  <View>
                    <Text className="mb-1 text-xs font-black text-slate-700">별점 선택</Text>
                    <View className="flex-row items-center gap-2">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <Pressable key={star} onPress={() => setRating(star)} className="active:scale-125">
                          <Text className="text-2xl text-amber-400">{star <= rating ? '⭐' : '☆'}</Text>
                        </Pressable>
                      ))}
                      <Text className="ml-2 font-pixel text-xs text-amber-500">{rating}.0점</Text>
                    </View>
                  </View>

                  {/* 메모 */}
                  <View>
                    <Text className="mb-1 text-xs font-black text-slate-700">장소 메모 (유저당 1개)</Text>
                    <TextInput
                      multiline
                      numberOfLines={3}
                      value={memoText}
                      onChangeText={setMemoText}
                      onFocus={() => setMemoFocused(true)}
                      onBlur={() => setMemoFocused(false)}
                      placeholder="장소의 분위기, 꿀팁, 느낌을 솔직하게 기록해보세요."
                      placeholderTextColor="#94A3B8"
                      textAlignVertical="top"
                      className={`min-h-[76px] w-full rounded-xl border-2 p-2.5 text-xs font-semibold text-slate-800 ${
                        memoFocused ? 'border-mint-primary' : 'border-slate-200'
                      }`}
                    />
                  </View>

                  {/* 사진 */}
                  <View>
                    <View className="mb-1.5 flex-row items-center justify-between">
                      <Text className="text-xs font-black text-slate-700">
                        사진 첨부 ({photos.length}/{MAX_PHOTOS})
                      </Text>
                      <Pressable
                        onPress={handlePickPhotos}
                        accessibilityLabel="갤러리에서 사진 고르기"
                        className="rounded-lg border border-teal-300 bg-[#B4FAF0] px-2 py-0.5 active:bg-teal-200"
                      >
                        <Text className="text-[10px] font-extrabold text-teal-900">+ 갤러리에서 추가</Text>
                      </Pressable>
                    </View>
                    {photos.length === 0 ? (
                      <Pressable
                        onPress={handlePickPhotos}
                        accessibilityLabel="갤러리에서 사진 고르기"
                        className="w-full items-center justify-center rounded-xl border-2 border-dashed border-slate-300 py-4"
                      >
                        <Text className="text-xs text-slate-400">📷 갤러리에서 사진 고르기</Text>
                      </Pressable>
                    ) : (
                      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
                        {photos.map((url, idx) => (
                          <View key={`${url}-${idx}`} className="relative h-16 w-16 overflow-hidden rounded-xl border border-slate-200">
                            <Image source={{ uri: url }} resizeMode="cover" className="h-full w-full" />
                            <Pressable
                              onPress={() => setPhotos((prev) => prev.filter((_, i) => i !== idx))}
                              className="absolute right-0.5 top-0.5 h-4 w-4 items-center justify-center rounded-full bg-black/70"
                            >
                              <Text className="text-[9px] text-white">✕</Text>
                            </Pressable>
                          </View>
                        ))}
                      </ScrollView>
                    )}
                  </View>

                  {/* PinLog용 영상 */}
                  <View>
                    <View className="mb-1.5 flex-row items-center justify-between">
                      <View className="flex-row items-center gap-1">
                        <Text className="text-xs font-black text-slate-700">PinLog용 2~3초 영상</Text>
                        <View className="rounded bg-rose-100 px-1">
                          <Text className="font-pixel text-[8px] text-rose-700">최대 1개</Text>
                        </View>
                      </View>
                      <View className="flex-row gap-1">
                        <Pressable
                          onPress={() => setIsCameraActive(true)}
                          className="rounded-lg border border-rose-700 bg-rose-500 px-2 py-1 active:translate-y-0.5 active:bg-rose-600"
                        >
                          <Text className="text-[10px] font-black text-white">🔴 앱 내 바로 촬영</Text>
                        </Pressable>
                        <Pressable
                          onPress={handlePickVideo}
                          accessibilityLabel="갤러리에서 영상 고르기"
                          className="rounded-lg border border-slate-300 bg-slate-100 px-2 py-1 active:bg-slate-200"
                        >
                          <Text className="text-[10px] font-bold text-slate-700">갤러리</Text>
                        </Pressable>
                      </View>
                    </View>

                    {videoClip && (
                      <View className="relative flex-row items-center gap-3 overflow-hidden rounded-2xl border-2 border-pixel-navy bg-slate-900 p-2.5">
                        {/* 저장 전에도 방금 찍은 영상을 확인할 수 있어야 한다. 눌러서 재생한다. */}
                        <Pressable
                          onPress={() =>
                            videoClip.videoUrl &&
                            setPreviewClip({
                              videoUrl: videoClip.videoUrl,
                              title: place.name,
                              authorName: currentUserName,
                              duration: videoClip.duration || undefined,
                              local: !isRemoteUrl(videoClip.videoUrl),
                            })
                          }
                          disabled={!videoClip.videoUrl}
                          accessibilityLabel="내 영상 재생"
                          className="relative h-16 w-16 overflow-hidden rounded-xl border border-white/20 bg-black active:opacity-80"
                        >
                          {videoClip.thumbnail ? (
                            <Image source={{ uri: videoClip.thumbnail }} resizeMode="cover" className="h-full w-full" />
                          ) : null}
                          <View className="absolute bottom-0 left-0 right-0 top-0 items-center justify-center bg-black/30">
                            <Text className="text-base text-white">▶</Text>
                          </View>
                        </Pressable>
                        <View className="min-w-0 flex-1">
                          <View className="flex-row items-center gap-1.5">
                            {videoClip.duration > 0 && (
                              <View className="rounded bg-rose-500 px-1.5 py-0.5">
                                <Text className="font-pixel text-[9px] text-white">{videoClip.duration}s</Text>
                              </View>
                            )}
                            <Text numberOfLines={1} className="text-xs font-extrabold text-white">
                              PinLog 전용 영상
                            </Text>
                          </View>
                          <Text className="mt-0.5 text-[10px] text-slate-300">
                            {videoClip.videoUrl ? '눌러서 미리보기 · 1장소 1영상으로 쓰여요' : '합성 시 1장소 1영상으로 쓰여요'}
                          </Text>
                        </View>
                        <Pressable onPress={() => setVideoClip(undefined)} className="p-1">
                          <Text className="text-xs text-slate-400">✕ 삭제</Text>
                        </Pressable>
                      </View>
                    )}
                  </View>

                  {/* 저장 */}
                  <View className="flex-row gap-2 pt-2">
                    <Pressable
                      onPress={handleSave}
                      className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy bg-mint-primary shadow-md active:translate-y-0.5 active:bg-mint-dark"
                    >
                      <Text className="text-xs font-black text-pixel-navy">
                        {myExistingRecord ? '기록 저장 완료 ✓' : '기록 등록하기 ✓'}
                      </Text>
                    </Pressable>
                  </View>
                </View>
              </FadeIn>
            )}

            {/* 팀원 공유 기록 */}
            <View className="gap-3 pt-2">
              <View className="flex-row items-center justify-between px-1">
                <Text className="text-xs font-black tracking-tight text-pixel-navy">팀원 공유 기록 ({placeRecords.length})</Text>
                <Text className="text-[10px] font-bold text-teal-800">본인 글만 [수정/삭제] 권한 부여</Text>
              </View>

              {placeRecords.length === 0 ? (
                <View className="rounded-2xl border-2 border-dashed border-teal-300 bg-white/80 p-6">
                  <Text className="text-center text-xs text-slate-500">
                    아직 등록된 팀원 기록이 없습니다. 첫 번째 핀 기록을 남겨보세요!
                  </Text>
                </View>
              ) : (
                placeRecords.map((record) => {
                  const isMine = record.userId === currentUserId;
                  return (
                    <View
                      key={record.id}
                      className={`gap-3 rounded-3xl border-2 bg-white p-4 shadow-sm ${
                        isMine ? 'border-pixel-navy' : 'border-teal-700/30'
                      }`}
                    >
                      <View className="flex-row items-center justify-between">
                        <View className="flex-row items-center gap-2">
                          <View className="h-8 w-8 items-center justify-center overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
                            {isMine ? (
                              <Image source={toImageSource(currentUserAvatarUrl)} resizeMode="cover" className="h-full w-full" />
                            ) : isImageAvatar(record.authorAvatar) ? (
                              <Image source={toImageSource(record.authorAvatar)} resizeMode="cover" className="h-full w-full" />
                            ) : (
                              <Text className="text-lg">{record.authorAvatar}</Text>
                            )}
                          </View>
                          <View>
                            <View className="flex-row items-center gap-1.5">
                              <Text className="text-xs font-black text-slate-800">{record.authorName}</Text>
                              {isMine && (
                                <View className="rounded bg-pixel-navy px-1.5 py-0.5">
                                  <Text className="font-pixel text-[8px] text-white">ME (본인)</Text>
                                </View>
                              )}
                            </View>
                            <View className="flex-row items-center gap-1">
                              <Text className="text-[10px] text-slate-400">{record.createdAt}</Text>
                              <Text className="text-[10px] text-slate-400">·</Text>
                              <Text className="text-[10px] font-bold text-amber-500">★ {record.rating}</Text>
                            </View>
                          </View>
                        </View>

                        {/* 본인 기록에만 수정/삭제 */}
                        {isMine && (
                          <View className="flex-row items-center gap-1">
                            <Pressable
                              onPress={() => setIsEditing(true)}
                              className="rounded-lg border border-pixel-navy bg-slate-100 px-2 py-1 active:translate-y-0.5 active:bg-slate-200"
                            >
                              <Text className="text-[10px] font-bold text-pixel-navy">✏️ 수정</Text>
                            </Pressable>
                            <Pressable
                              onPress={() =>
                                Alert.alert('기록 삭제', '이 장소 기록을 정말 삭제하시겠습니까?', [
                                  { text: '취소', style: 'cancel' },
                                  { text: '삭제', style: 'destructive', onPress: () => onDeleteRecord(record.id) },
                                ])
                              }
                              className="rounded-lg border border-rose-300 bg-rose-50 px-2 py-1 active:translate-y-0.5 active:bg-rose-100"
                            >
                              <Text className="text-[10px] font-bold text-rose-600">🗑️ 삭제</Text>
                            </Pressable>
                          </View>
                        )}
                      </View>

                      <View className="rounded-xl border border-slate-100 bg-slate-50 p-2.5">
                        <Text className="text-xs font-medium leading-relaxed text-slate-700">💬 {record.memo}</Text>
                      </View>

                      {record.photos && record.photos.length > 0 && (
                        <View className="gap-1">
                          <Text className="text-[10px] font-bold text-slate-400">사진 ({record.photos.length}장)</Text>
                          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
                            {record.photos.map((img, i) => (
                              <Image
                                key={`${img}-${i}`}
                                source={{ uri: img }}
                                resizeMode="cover"
                                className="h-20 w-20 rounded-xl border border-slate-200"
                              />
                            ))}
                          </ScrollView>
                        </View>
                      )}

                      {record.videoClip && (
                        <Pressable
                          onPress={() => {
                            const url = record.videoClip?.videoUrl;
                            if (!url) return;
                            setPreviewClip({
                              videoUrl: url,
                              title: place.name,
                              authorName: record.authorName,
                              duration: record.videoClip?.duration || undefined,
                            });
                          }}
                          disabled={!record.videoClip.videoUrl}
                          accessibilityLabel={`${record.authorName} 영상 재생`}
                          className="flex-row items-center justify-between rounded-2xl border border-white/20 bg-slate-900 p-2.5 active:opacity-80"
                        >
                          <View className="flex-row items-center gap-2.5">
                            <View className="relative h-12 w-12 overflow-hidden rounded-lg border border-white/20 bg-black">
                              {record.videoClip.thumbnail ? (
                                <Image source={{ uri: record.videoClip.thumbnail }} resizeMode="cover" className="h-full w-full" />
                              ) : null}
                              <View className="absolute bottom-0 left-0 right-0 top-0 items-center justify-center bg-black/40">
                                <Text className="text-xs text-white">▶</Text>
                              </View>
                            </View>
                            <View>
                              <View className="flex-row items-center gap-1.5">
                                {record.videoClip.duration > 0 && (
                                  <View className="rounded bg-rose-500 px-1">
                                    <Text className="font-pixel text-[8px] text-white">{record.videoClip.duration}s</Text>
                                  </View>
                                )}
                                <Text className="text-xs font-bold text-white">PinLog 클립</Text>
                              </View>
                              {/* 합성 전에도 눌러서 확인할 수 있다. */}
                              <Text className="text-[10px] text-slate-400">
                                {record.videoClip.videoUrl ? '눌러서 재생' : 'PinLog 브이로그 후보'}
                              </Text>
                            </View>
                          </View>
                          <Text className="font-pixel text-[9px] text-[#56EADD]">▶ PLAY</Text>
                        </Pressable>
                      )}
                    </View>
                  );
                })
              )}
            </View>
          </ScrollView>

          {/* 하단 CTA */}
          <View className="flex-row gap-2 border-t-2 border-pixel-navy bg-white p-3">
            <Pressable
              onPress={onClose}
              className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5 active:bg-slate-200"
            >
              <Text className="text-xs font-black text-pixel-navy">닫기</Text>
            </Pressable>
            <Pressable
              onPress={onOpenPinLogMaker}
              className="h-11 flex-2 flex-row items-center justify-center gap-1.5 rounded-xl border-2 border-pixel-navy bg-mint-primary shadow-md active:translate-y-0.5 active:bg-mint-dark"
            >
              <Text className="text-xs font-black text-pixel-navy">🎬 팀원 영상으로 PinLog 만들기</Text>
            </Pressable>
          </View>
        </View>
      </View>

      {previewClip && <ClipPreviewModal clip={previewClip} onClose={() => setPreviewClip(null)} />}
    </Modal>
  );
}
