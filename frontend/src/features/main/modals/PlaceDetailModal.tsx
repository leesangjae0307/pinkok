/**
 * 장소별 팀 다이어리 모달 (1인 1기록 규칙 · 앱 내 2~3초 촬영 · 팀원 피드)
 */
import React, { useEffect, useState } from 'react';
import { Alert, Image, Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { CURRENT_USER, PlaceItem, PlaceRecord, isImageAvatar, toImageSource } from '../../../data/mockData';
import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { FadeIn } from '../../../components/pixel/Primitives';

interface PlaceDetailModalProps {
  place: PlaceItem;
  records: PlaceRecord[];
  onClose: () => void;
  onSaveRecord: (record: PlaceRecord) => void;
  onDeleteRecord: (recordId: number) => void;
  onOpenPinLogMaker: () => void;
  currentUserAvatarUrl: number;
}

const SAMPLE_PHOTOS = [
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=400&q=80',
];

export default function PlaceDetailModal({
  place,
  records,
  onClose,
  onSaveRecord,
  onDeleteRecord,
  onOpenPinLogMaker,
  currentUserAvatarUrl,
}: PlaceDetailModalProps) {
  const insets = useSafeAreaInsets();
  const myExistingRecord = records.find((r) => r.pinId === place.id && r.userId === CURRENT_USER.id);

  const [isEditing, setIsEditing] = useState(false);
  const [rating, setRating] = useState(myExistingRecord?.rating || 5);
  const [memoText, setMemoText] = useState(myExistingRecord?.memo || '');
  const [photos, setPhotos] = useState<string[]>(myExistingRecord?.photos || []);
  const [videoClip, setVideoClip] = useState<PlaceRecord['videoClip'] | undefined>(myExistingRecord?.videoClip);
  const [memoFocused, setMemoFocused] = useState(false);

  // 촬영 시뮬레이션
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingProgress, setRecordingProgress] = useState(0);
  const [isUploadingVideo, setIsUploadingVideo] = useState(false);

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

  // 2.8초 자동 종료 촬영
  useEffect(() => {
    let timer: ReturnType<typeof setInterval> | undefined;
    if (isRecording) {
      const step = 50;
      const totalDuration = 2800;
      timer = setInterval(() => {
        setRecordingProgress((prev) => {
          const next = prev + (step / totalDuration) * 100;
          if (next >= 100) {
            if (timer) clearInterval(timer);
            setIsRecording(false);
            setIsUploadingVideo(true);
            // POST /files 응답 매핑: thumbnailUrl + durationMs
            const uploadResponse = {
              id: Date.now(),
              thumbnailUrl: place.thumbnail,
              durationMs: 2800,
            };
            setTimeout(() => {
              setVideoClip({
                id: uploadResponse.id,
                duration: uploadResponse.durationMs / 1000,
                thumbnail: uploadResponse.thumbnailUrl,
                createdAt: '방금 전',
              });
              setIsUploadingVideo(false);
              setIsCameraActive(false);
            }, 650);
            return 100;
          }
          return next;
        });
      }, step);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [isRecording, place.id, place.thumbnail]);

  const handleStartRecording = () => {
    setRecordingProgress(0);
    setIsRecording(true);
  };

  const handleAddSamplePhoto = () => {
    const randomPhoto = SAMPLE_PHOTOS[Math.floor(Math.random() * SAMPLE_PHOTOS.length)];
    setPhotos((prev) => [...prev, randomPhoto]);
  };

  const handleSave = () => {
    if (!memoText.trim()) {
      Alert.alert('PINKOK', '메모 내용을 입력해주세요.');
      return;
    }
    const record: PlaceRecord = {
      id: myExistingRecord ? myExistingRecord.id : Date.now(),
      pinId: place.id,
      userId: CURRENT_USER.id,
      authorName: CURRENT_USER.name,
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
    const busy = isRecording || isUploadingVideo;
    return (
      <Modal visible animationType="slide" onRequestClose={() => setIsCameraActive(false)} statusBarTranslucent>
        <View className="flex-1 bg-slate-950" style={{ paddingTop: insets.top, paddingBottom: insets.bottom }}>
          <View className="flex-row items-center justify-between border-b border-white/15 px-4 py-3">
            <Pressable
              onPress={() => {
                setIsCameraActive(false);
                setIsRecording(false);
              }}
              accessibilityLabel="카메라 닫기"
              className="h-10 w-10 items-center justify-center rounded-xl border border-white/20 bg-white/10"
            >
              <PixelBackArrow size={16} color="#FFFFFF" />
            </Pressable>
            <View className="items-center">
              <Text className="font-pixel text-[9px] text-[#56EADD]">PINKOK CAMERA</Text>
              <Text className="mt-1 text-sm font-black text-white">2~3초 앱 내 촬영</Text>
            </View>
            <Text className="w-10 text-right font-pixel text-[8px] text-rose-400">{isRecording ? 'REC' : 'READY'}</Text>
          </View>

          <View className="relative flex-1 overflow-hidden bg-black">
            <Image source={{ uri: place.thumbnail }} resizeMode="cover" className="h-full w-full" />
            {!isRecording && <View className="absolute inset-0 bg-black/25" />}
            <View pointerEvents="none" className="absolute inset-5 items-center justify-center border border-white/35">
              <View className="absolute h-px w-12 bg-white/70" />
              <View className="absolute h-12 w-px bg-white/70" />
            </View>
            <View className="absolute left-4 top-4 rounded-xl bg-black/60 px-3 py-2">
              <Text className="text-xs font-black text-white">{place.name}</Text>
              <Text className="mt-1 text-[10px] text-white/70">POST /files · 최대 3초</Text>
            </View>
            {busy && (
              <View className="absolute bottom-0 left-0 right-0 h-2 bg-black/60">
                <View className="h-full bg-rose-500" style={{ width: `${isUploadingVideo ? 100 : recordingProgress}%` }} />
              </View>
            )}
          </View>

          <View className="flex-row items-center justify-between bg-slate-950 px-5 py-5">
            <Text className="w-20 text-[10px] font-bold text-slate-400">
              {isUploadingVideo ? '업로드 중...' : isRecording ? '2.8초 자동 종료' : '준비 완료'}
            </Text>
            <Pressable
              disabled={busy}
              onPress={handleStartRecording}
              accessibilityLabel="3초 영상 촬영 시작"
              className={`h-20 w-20 items-center justify-center rounded-full border-4 border-white ${busy ? 'bg-slate-600' : 'bg-rose-500'}`}
            >
              <View className="h-10 w-10 rounded-full border-2 border-white bg-rose-600" />
            </Pressable>
            <Text className="w-20 text-right font-pixel text-[9px] text-[#56EADD]">{Math.round(recordingProgress)}%</Text>
          </View>
        </View>
      </Modal>
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
            <View className="flex-row items-center gap-3 rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-md">
              <Image source={{ uri: place.thumbnail }} resizeMode="cover" className="h-16 w-16 rounded-xl border border-slate-200" />
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
                      <Text className="text-xs font-black text-slate-700">사진 첨부 (복수 등록 가능)</Text>
                      <Pressable
                        onPress={handleAddSamplePhoto}
                        className="rounded-lg border border-teal-300 bg-[#B4FAF0] px-2 py-0.5 active:bg-teal-200"
                      >
                        <Text className="text-[10px] font-extrabold text-teal-900">+ 사진 추가</Text>
                      </Pressable>
                    </View>
                    {photos.length === 0 ? (
                      <Pressable
                        onPress={handleAddSamplePhoto}
                        className="w-full items-center justify-center rounded-xl border-2 border-dashed border-slate-300 py-4"
                      >
                        <Text className="text-xs text-slate-400">📷 사진을 추가해주세요 (클릭)</Text>
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
                          onPress={() =>
                            setVideoClip({
                              id: Date.now(),
                              duration: 2.5,
                              thumbnail: place.thumbnail,
                              createdAt: '방금 전',
                            })
                          }
                          className="rounded-lg border border-slate-300 bg-slate-100 px-2 py-1 active:bg-slate-200"
                        >
                          <Text className="text-[10px] font-bold text-slate-700">갤러리</Text>
                        </Pressable>
                      </View>
                    </View>

                    {videoClip && (
                      <View className="relative flex-row items-center gap-3 overflow-hidden rounded-2xl border-2 border-pixel-navy bg-slate-900 p-2.5">
                        <View className="relative h-16 w-16 overflow-hidden rounded-xl border border-white/20">
                          <Image source={{ uri: videoClip.thumbnail }} resizeMode="cover" className="h-full w-full" />
                          <View className="absolute inset-0 items-center justify-center bg-black/30">
                            <Text className="text-base text-white">▶</Text>
                          </View>
                        </View>
                        <View className="min-w-0 flex-1">
                          <View className="flex-row items-center gap-1.5">
                            <View className="rounded bg-rose-500 px-1.5 py-0.5">
                              <Text className="font-pixel text-[9px] text-white">{videoClip.duration}s</Text>
                            </View>
                            <Text numberOfLines={1} className="text-xs font-extrabold text-white">
                              PinLog 전용 영상
                            </Text>
                          </View>
                          <Text className="mt-0.5 text-[10px] text-slate-300">합성 시 1장소 1영상으로 쓰여요</Text>
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
                  const isMine = record.userId === CURRENT_USER.id;
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
                              <Image source={currentUserAvatarUrl} resizeMode="cover" className="h-full w-full" />
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
                        <View className="flex-row items-center justify-between rounded-2xl border border-white/20 bg-slate-900 p-2.5">
                          <View className="flex-row items-center gap-2.5">
                            <View className="relative h-12 w-12 overflow-hidden rounded-lg border border-white/20">
                              <Image source={{ uri: record.videoClip.thumbnail }} resizeMode="cover" className="h-full w-full" />
                              <View className="absolute inset-0 items-center justify-center bg-black/40">
                                <Text className="text-xs text-white">▶</Text>
                              </View>
                            </View>
                            <View>
                              <View className="flex-row items-center gap-1.5">
                                <View className="rounded bg-rose-500 px-1">
                                  <Text className="font-pixel text-[8px] text-white">{record.videoClip.duration}s</Text>
                                </View>
                                <Text className="text-xs font-bold text-white">2~3초 동영상</Text>
                              </View>
                              <Text className="text-[10px] text-slate-400">PinLog 브이로그 후보</Text>
                            </View>
                          </View>
                          <Text className="font-pixel text-[9px] text-[#56EADD]">READY ✓</Text>
                        </View>
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
    </Modal>
  );
}
