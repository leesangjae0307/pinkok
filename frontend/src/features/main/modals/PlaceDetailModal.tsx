/**
 * 장소별 팀 다이어리 모달 (1인 1기록 규칙 · 앱 내 최대 2초 촬영 · 팀원 피드)
 */
import React, { useEffect, useState } from 'react';
import {
  Alert,
  Image,
  Modal,
  Pressable,
  RefreshControl,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { launchImageLibrary, type PhotoQuality } from 'react-native-image-picker';

import { AvatarSource, PlaceItem, PlaceRecord, isImageAvatar, toImageSource } from '../../../data/mockData';
import RecordClipModal from './RecordClipModal';
import PhotoViewerModal from './PhotoViewerModal';
import VideoPlayerModal from './VideoPlayerModal';
import VideoThumb from '../../../components/VideoThumb';
import { PixelBackArrow } from '../../../components/pixel/PixelIcons';
import { FadeIn } from '../../../components/pixel/Primitives';

interface PlaceDetailModalProps {
  place: PlaceItem;
  records: PlaceRecord[];
  onClose: () => void;
  onSaveRecord: (record: PlaceRecord) => void;
  onDeleteRecord: (recordId: number) => void;
  onOpenPinLogMaker: () => void;
  /** 기록만 서버에서 다시 받는다. */
  onReload: () => void | Promise<void>;
  currentUserAvatarUrl: AvatarSource;
  /** 로그인한 사용자. 내 기록인지 가릴 때 쓴다. */
  currentUserId: number;
  currentUserName: string;
  /** true 면 기록 편집 + 카메라 화면으로 바로 연다 (지도 바텀시트의 "바로 촬영") */
  startInCamera?: boolean;
  /**
   * 저장에 실패해 임시저장된 내 기록. 있으면 편집 화면을 서버의 기록 대신 이 내용으로 채운다
   * (사용자가 쓴 메모·별점·고른 사진을 다시 쓰지 않아도 되게).
   */
  draft?: PlaceRecord;
  /** true 면 편집 화면으로 바로 연다 (장소의 "저장 실패 · 다시 시도") */
  startInEdit?: boolean;
}

/** 썸네일이 가질 수 있는 가로세로 비. 파노라마나 아주 긴 세로 사진에서 멈춘다. */
const THUMB_MIN_RATIO = 0.6;
const THUMB_MAX_RATIO = 2.2;

/**
 * 사진 비율을 지키는 썸네일.
 *
 * 정사각형 칸에 넣고 가운데를 잘라내면 가로로 찍은 사진은 양옆이 날아가
 * 무엇을 찍었는지 알아볼 수 없다. 그래서 높이만 고정하고 너비는 사진 비율을
 * 따라가게 둔다. 가로 스크롤 목록이라 사진마다 너비가 달라도 괜찮다.
 *
 * 비율은 사진을 읽어야 알 수 있으므로, 알기 전까지는 정사각형으로 둔다.
 */
function RatioThumb({ uri, height }: { uri: string; height: number }) {
  const [ratio, setRatio] = useState(1);

  return (
    <Image
      source={{ uri }}
      resizeMode="cover"
      onLoad={(e) => {
        const { width, height: h } = e.nativeEvent.source;
        if (width > 0 && h > 0) {
          setRatio(Math.min(Math.max(width / h, THUMB_MIN_RATIO), THUMB_MAX_RATIO));
        }
      }}
      style={{ height, width: Math.round(height * ratio) }}
    />
  );
}

export default function PlaceDetailModal({
  place,
  records,
  onClose,
  onSaveRecord,
  onDeleteRecord,
  onOpenPinLogMaker,
  onReload,
  currentUserAvatarUrl,
  currentUserId,
  currentUserName,
  startInCamera = false,
  draft,
  startInEdit = false,
}: PlaceDetailModalProps) {
  const myExistingRecord = records.find((r) => r.pinId === place.id && r.userId === currentUserId);
  // 편집 화면을 채울 내용. 저장하지 못한 임시저장본이 있으면 그것이 우선이다.
  const editSource = draft ?? myExistingRecord;

  const [isEditing, setIsEditing] = useState(startInCamera || startInEdit);
  const [rating, setRating] = useState(editSource?.rating || 5);
  const [memoText, setMemoText] = useState(editSource?.memo || '');
  const [photos, setPhotos] = useState<string[]>(editSource?.photos || []);
  const [videoClip, setVideoClip] = useState<PlaceRecord['videoClip'] | undefined>(editSource?.videoClip);
  /** 앱 안 전체화면 플레이어로 재생 중인 영상 주소. 핀로그를 만들기 전에도 확인할 수 있어야 한다. */
  const [playingVideo, setPlayingVideo] = useState<string | null>(null);
  /** 크게 보고 있는 사진. 어느 기록의 몇 번째인지 함께 들고 있는다. */
  const [viewerPhotos, setViewerPhotos] = useState<{ photos: string[]; index: number } | null>(null);
  const [memoFocused, setMemoFocused] = useState(false);
  const [isReloading, setIsReloading] = useState(false);
  /** 제약 규칙 안내를 펼쳐 둘지. 처음에는 지금까지처럼 펼쳐 둔다. */
  const [isRuleOpen, setIsRuleOpen] = useState(true);

  // 촬영
  const [isCameraActive, setIsCameraActive] = useState(startInCamera);

  useEffect(() => {
    const source = draft ?? myExistingRecord;
    if (source) {
      setRating(source.rating);
      setMemoText(source.memo);
      setPhotos(source.photos);
      setVideoClip(source.videoClip);
    } else {
      setRating(5);
      setMemoText('');
      setPhotos([]);
      setVideoClip(undefined);
    }
  }, [draft, myExistingRecord, place.id]);


  /**
   * 기기 사진첩에서 실제 사진을 고른다. 고른 file:// 주소를 그대로 들고 있다가
   * 기록을 저장할 때 uploadRecordMedia 가 서버로 올린다.
   *
   * 안드로이드 13+ 는 사진첩 접근에 권한이 따로 필요 없다. 시스템 사진 선택기가
   * 골라준 사진만 앱에 건네주기 때문이다(READ_MEDIA_IMAGES 를 받지 않아도 된다).
   */
  const handleAddPhoto = async () => {
    try {
      // 고를 때 줄여 둔다 (긴 변 2048px, JPEG 85%). 폰 원본은 장당 5~15MB 라 여러 장이면
      // 업로드가 오래 걸려 실패하기 쉽다. 비율은 유지되고, 회전 정보(EXIF)는 선택기가
      // 줄인 파일에 그대로 옮겨 준다. 영상은 이 선택기를 쓰지 않으므로 영향이 없다.
      const res = await launchImageLibrary({
        mediaType: 'photo',
        selectionLimit: 0,
        maxWidth: 2048,
        maxHeight: 2048,
        // 타입은 0.1 단위만 나열하지만 안드로이드는 값을 그대로 ×100 해서 쓴다 (Options.java).
        quality: 0.85 as PhotoQuality,
        // iOS 의 HEIC 는 호환되는 형식(JPEG)으로 바꿔 건넨다. 안드로이드는 영향이 없다.
        assetRepresentationMode: 'compatible',
      });
      if (res.didCancel) {return;}
      if (res.errorCode) {
        Alert.alert('사진을 열지 못했어요', res.errorMessage ?? '잠시 후 다시 시도해 주세요.');
        return;
      }
      const picked = (res.assets ?? []).map((a) => a.uri).filter((uri): uri is string => !!uri);
      if (picked.length > 0) {setPhotos((prev) => [...prev, ...picked]);}
    } catch {
      Alert.alert('사진을 열지 못했어요', '잠시 후 다시 시도해 주세요.');
    }
  };

  /** PinLog 용 영상을 사진첩에서 고른다. 길이는 선택기가 주는 duration(초) 을 쓴다. */
  const handlePickVideo = async () => {
    try {
      const res = await launchImageLibrary({ mediaType: 'video', selectionLimit: 1 });
      if (res.didCancel) {return;}
      if (res.errorCode) {
        Alert.alert('영상을 열지 못했어요', res.errorMessage ?? '잠시 후 다시 시도해 주세요.');
        return;
      }
      const asset = res.assets?.[0];
      if (!asset?.uri) {return;}
      setVideoClip({
        id: Date.now(),
        duration: asset.duration ?? 0,
        thumbnail: '',
        videoUrl: asset.uri,
        createdAt: '방금 전',
      });
    } catch {
      Alert.alert('영상을 열지 못했어요', '잠시 후 다시 시도해 주세요.');
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
            // 서버가 올린 뒤 실제 첫 프레임으로 썸네일을 만들어 준다.
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
          className="relative w-full max-w-md overflow-hidden rounded-3xl border-4 border-pixel-navy bg-[#B4FAF0]"
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
          {/* 당겨서 새로고침. 팀원이 올린 기록이 늦게 보일 때 이것만 다시 받는다. */}
          <ScrollView
            className="flex-1"
            contentContainerStyle={{ padding: 16, gap: 16 }}
            keyboardShouldPersistTaps="handled"
            refreshControl={
              <RefreshControl
                refreshing={isReloading}
                onRefresh={() => {
                  if (isReloading) {return;}
                  setIsReloading(true);
                  Promise.resolve(onReload()).finally(() => setIsReloading(false));
                }}
                colors={['#075953']}
                tintColor="#075953"
              />
            }
          >
            {/* 장소 요약 */}
            {/* 썸네일은 빼고 장소 정보만 남긴다. */}
            <View className="rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-md">
              <View className="min-w-0 flex-1">
                <View className="self-start rounded-md bg-[#B4FAF0] px-2 py-0.5">
                  <Text className="text-[10px] font-bold text-teal-800">{place.category}</Text>
                </View>
                {/* 어디 기록인지가 가장 중요하므로 이름은 줄여 쓰지 않고 다 보여 준다. */}
                <Text className="mt-0.5 text-sm font-black text-slate-800">{place.name}</Text>
                <Text numberOfLines={2} className="text-[10px] text-slate-500">
                  {place.address}
                </Text>
                <View className="mt-1 flex-row items-center gap-2">
                  <Text className="font-pixel text-[9px] text-amber-500">★ {place.rating}</Text>
                  <View className="rounded bg-teal-50 px-1.5">
                    <Text className="text-[10px] font-bold text-teal-900">팀 기록 {placeRecords.length}개</Text>
                  </View>
                </View>
              </View>
            </View>

            {/* 1인 1기록 규칙 안내. 한 번 읽으면 되는 내용이라 접어 둘 수 있다. */}
            <View className="rounded-2xl border-2 border-pixel-navy bg-white p-3 shadow-md">
              <Pressable
                onPress={() => setIsRuleOpen((v) => !v)}
                accessibilityLabel={`제약 규칙 ${isRuleOpen ? '접기' : '펼치기'}`}
                className="flex-row items-start gap-2 active:opacity-60"
              >
                <View className="rounded-lg bg-amber-100 p-1">
                  <Text className="text-base">⚖️</Text>
                </View>
                <Text className="flex-1 text-xs font-black text-teal-900">
                  1인 1기록 제약 규칙 (1 User = 1 Record)
                </Text>
                <Text className="font-pixel text-[10px] text-teal-900">{isRuleOpen ? '▾' : '▸'}</Text>
              </Pressable>

              {isRuleOpen ? (
                <View className="mt-1 gap-0.5 pl-9">
                  <Text className="text-[11px] leading-4 text-slate-600">
                    [같은 핀 + 같은 유저] 당 <Text className="font-bold">오직 1개의 기록</Text>만 허용됩니다. 작성 후에는
                    자동으로 <Text className="font-bold">[수정]</Text> 모드로 전환됩니다.
                  </Text>
                  <View className="flex-row flex-wrap items-center gap-x-2 gap-y-1 pt-1.5">
                    <Text className="text-[10px] font-bold text-teal-800">📝 메모 1개 (별점 포함)</Text>
                    <Text className="text-[10px] font-bold text-teal-800">·</Text>
                    <Text className="text-[10px] font-bold text-teal-800">🖼️ 사진 복수</Text>
                    <Text className="text-[10px] font-bold text-teal-800">·</Text>
                    <Text className="text-[10px] font-bold text-teal-800">🎥 최대 2s 영상 1개</Text>
                  </View>
                </View>
              ) : null}
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
                        onPress={handleAddPhoto}
                        className="rounded-lg border border-teal-300 bg-[#B4FAF0] px-2 py-0.5 active:bg-teal-200"
                      >
                        <Text className="text-[10px] font-extrabold text-teal-900">+ 사진 추가</Text>
                      </Pressable>
                    </View>
                    {photos.length === 0 ? (
                      <Pressable
                        onPress={handleAddPhoto}
                        className="w-full items-center justify-center rounded-xl border-2 border-dashed border-slate-300 py-4"
                      >
                        <Text className="text-xs text-slate-400">📷 사진을 추가해주세요 (클릭)</Text>
                      </Pressable>
                    ) : (
                      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
                        {photos.map((url, idx) => (
                          <View key={`${url}-${idx}`} className="relative overflow-hidden rounded-xl border border-slate-200">
                            {/* 붙이기 전에도 눌러서 크게 볼 수 있다. ✕ 는 위에 떠 있어 따로 눌린다. */}
                            <Pressable
                              onPress={() => setViewerPhotos({ photos, index: idx })}
                              accessibilityLabel={`붙인 사진 ${idx + 1} 크게 보기`}
                            >
                              <RatioThumb uri={url} height={64} />
                            </Pressable>
                            <Pressable
                              onPress={() => setPhotos((prev) => prev.filter((_, i) => i !== idx))}
                              accessibilityLabel={`붙인 사진 ${idx + 1} 빼기`}
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
                        <Text className="text-xs font-black text-slate-700">PinLog용 2초 영상</Text>
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
                          className="rounded-lg border border-slate-300 bg-slate-100 px-2 py-1 active:bg-slate-200"
                        >
                          <Text className="text-[10px] font-bold text-slate-700">갤러리</Text>
                        </Pressable>
                      </View>
                    </View>

                    {videoClip && (
                      <View className="relative flex-row items-center gap-3 overflow-hidden rounded-2xl border-2 border-pixel-navy bg-slate-900 p-2.5">
                        {/* 붙인 영상은 저장 전에도 눌러서 앱 안에서 바로 확인할 수 있다. */}
                        <Pressable
                          onPress={() => videoClip.videoUrl && setPlayingVideo(videoClip.videoUrl)}
                          accessibilityLabel="영상 재생"
                          className="relative h-16 w-16 overflow-hidden rounded-xl border border-white/20 bg-black"
                        >
                          <VideoThumb thumbnail={videoClip.thumbnail} videoUrl={videoClip.videoUrl} />
                          <View className="absolute inset-0 items-center justify-center bg-black/30">
                            <Text className="text-base text-white">▶</Text>
                          </View>
                        </Pressable>
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
                              <Pressable
                                key={`${img}-${i}`}
                                onPress={() => setViewerPhotos({ photos: record.photos ?? [], index: i })}
                                accessibilityLabel={`${record.authorName} 사진 ${i + 1} 크게 보기`}
                                className="overflow-hidden rounded-xl border border-slate-200"
                              >
                                <RatioThumb uri={img} height={80} />
                              </Pressable>
                            ))}
                          </ScrollView>
                        </View>
                      )}

                      {record.videoClip && (
                        <Pressable
                          onPress={() =>
                            record.videoClip?.videoUrl && setPlayingVideo(record.videoClip.videoUrl)
                          }
                          accessibilityLabel={`${record.authorName} 영상 재생`}
                          className="overflow-hidden rounded-2xl border-2 border-pixel-navy bg-slate-900"
                        >
                          {/* 16:9 대표 이미지 + 가운데 재생 버튼 + 길이 */}
                          <View className="relative aspect-video w-full">
                            <VideoThumb
                              thumbnail={record.videoClip.thumbnail}
                              videoUrl={record.videoClip.videoUrl}
                            />
                            <View className="absolute inset-0 items-center justify-center bg-black/25">
                              <View className="h-12 w-12 items-center justify-center rounded-full border-2 border-white bg-black/55">
                                <Text className="text-lg text-white">▶</Text>
                              </View>
                            </View>
                            <View className="absolute bottom-2 right-2 rounded bg-black/75 px-1.5 py-0.5">
                              <Text className="font-pixel text-[9px] text-white">{record.videoClip.duration}s</Text>
                            </View>
                          </View>
                          <View className="flex-row items-center justify-between px-3 py-2">
                            <View>
                              <Text className="text-xs font-bold text-white">최대 2초 동영상</Text>
                              <Text className="text-[10px] text-slate-400">PinLog 브이로그 후보</Text>
                            </View>
                            <Text className="font-pixel text-[9px] text-[#56EADD]">READY ✓</Text>
                          </View>
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

      {playingVideo && <VideoPlayerModal uri={playingVideo} onClose={() => setPlayingVideo(null)} />}

      {viewerPhotos && (
        <PhotoViewerModal
          photos={viewerPhotos.photos}
          initialIndex={viewerPhotos.index}
          onClose={() => setViewerPhotos(null)}
        />
      )}
    </Modal>
  );
}
