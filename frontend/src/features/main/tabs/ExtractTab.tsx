import React from 'react';
import { Image, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';

import { PixelExtractIcon, PixelStar } from '../../../components/pixel/PixelIcons';
import { AnimatedLoop, FadeIn, PixelPressable, PixelSelect } from '../../../components/pixel/Primitives';
import { useApp } from '../AppContext';

const MAX_IMAGES = 10;

export default function ExtractTab() {
  const {
    inputUrl,
    setInputUrl,
    isExtracting,
    extractError,
    handleRetryExtract,
    lastAiRequestId,
    extractedPlaces,
    selectedExtractedPlaceIds,
    isTripPickerOpen,
    setIsTripPickerOpen,
    trips,
    selectedTripForExtract,
    setSelectedTripForExtract,
    extractImageNames,
    setExtractImageNames,
    extractImages,
    setExtractImages,
    handleExtractSubmit,
    handleToggleExtractedPlace,
    handleToggleAllExtractedPlaces,
    handleAddPlaceToTrip,
    handleCreateTripAndAdd,
    isCreatingExtractTrip,
  } = useApp();
  const [isNewTripOpen, setIsNewTripOpen] = React.useState(false);
  const [newTripTitle, setNewTripTitle] = React.useState('');
  /** 불러오지 못한 첨부 이미지(주소 기준). 이것만 대체 아이콘으로 바꾼다. */
  const [brokenImages, setBrokenImages] = React.useState<Record<string, boolean>>({});

  /** 이름 목록과 원본 목록은 짝이라 같은 칸을 함께 지운다. */
  const removeExtractImage = (index: number) => {
    setExtractImageNames(extractImageNames.filter((_, i) => i !== index));
    setExtractImages(extractImages.filter((_, i) => i !== index));
  };

  const handlePickScreenshots = async () => {
    const res = await launchImageLibrary({
      mediaType: 'photo',
      selectionLimit: MAX_IMAGES,
      // iOS 의 HEIC 는 서버가 받는 형식으로 바꿔 건넨다.
      assetRepresentationMode: 'compatible',
    });
    const assets = (res.assets ?? []).slice(0, MAX_IMAGES);
    if (assets.length > 0) {
      setExtractImageNames(assets.map((asset, i) => asset.fileName ?? `스크린샷 ${i + 1}`));
      // 업로드에는 이름이 아니라 원본 주소가 필요하다.
      setExtractImages(
        assets
          .filter((asset) => asset.uri)
          .map((asset) => ({ uri: asset.uri as string, fileName: asset.fileName, type: asset.type })),
      );
    }
  };

  // 저장된 선택이 지워진 여행이면 맨 앞 여행으로 대신한다.
  const targetTripId = trips.some((t) => t.id === selectedTripForExtract) ? selectedTripForExtract : trips[0]?.id;

  const noneSelected = selectedExtractedPlaceIds.length === 0;
  const allSelected =
    extractedPlaces.length > 0 && extractedPlaces.every((p) => selectedExtractedPlaceIds.includes(p.id));

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {/* 헤더 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2">
          <PixelExtractIcon size={34} active />
          <View>
            <Text className="font-pixel text-xl tracking-tight text-[#0f6861]">EXTRACT</Text>
            <Text className="-mt-1 text-[11px] font-semibold text-slate-600">AI 장소 자동 추출기</Text>
          </View>
        </View>
        <View className="rounded-full border border-[#82F0DC] bg-white px-2.5 py-1">
          <Text className="text-xs font-bold text-[#075953]">✨ SMART AI</Text>
        </View>
      </View>

      <View className="items-center pt-1">
        <Text className="text-center text-base font-extrabold text-slate-800">
          링크나 이미지를 넣으면{'\n'}여러 장소 후보를 한 번에 찾아드려요
        </Text>
      </View>

      {/* 입력 박스 */}
      <View className="flex-row items-center gap-2 rounded-3xl border-2 border-[#50E6DC] bg-white p-2.5 shadow-md">
        <Text className="pl-2 text-slate-400">🔗</Text>
        <TextInput
          value={inputUrl}
          onChangeText={setInputUrl}
          onSubmitEditing={handleExtractSubmit}
          returnKeyType="search"
          autoCapitalize="none"
          placeholder="인스타그램/유튜브/네이버 장소 링크 입력..."
          placeholderTextColor="#94A3B8"
          className="flex-1 p-0 text-xs font-medium text-slate-800"
        />
        <Pressable
          onPress={handlePickScreenshots}
          accessibilityLabel="여행 스크린샷 업로드"
          className="h-10 w-10 items-center justify-center rounded-2xl border border-[#82F0DC] bg-[#B4FAF0]/50 active:bg-[#B4FAF0]"
        >
          <Text className="text-lg">📂</Text>
        </Pressable>
        <Pressable
          onPress={handleExtractSubmit}
          disabled={isExtracting}
          accessibilityLabel="AI 장소 검색"
          className="items-center justify-center rounded-2xl border border-[#82F0DC] bg-[#56EADD] p-2.5 active:scale-90 active:bg-[#50E6DC]"
        >
          <Text className="text-lg leading-none">🔍</Text>
        </Pressable>
      </View>

      {extractImageNames.length > 0 && (
        <View className="rounded-2xl border border-[#82F0DC] bg-white/80 p-3">
          <View className="flex-row items-center justify-between">
            <Text className="text-[10px] font-black text-[#075953]">
              첨부 이미지 {extractImageNames.length}/{MAX_IMAGES}
            </Text>
            <Pressable onPress={() => { setExtractImageNames([]); setExtractImages([]); setBrokenImages({}); }} hitSlop={8}>
              <Text className="text-[9px] font-black text-rose-500">전체 삭제</Text>
            </Pressable>
          </View>
          {/* 파일명 대신 작은 썸네일. 여러 장이면 옆으로 넘겨 본다. */}
          <ScrollView horizontal showsHorizontalScrollIndicator={false} className="mt-2" contentContainerClassName="gap-2 pt-1.5">
            {extractImages.map((img, index) => (
              <View key={`${img.uri}-${index}`} className="relative">
                <View className="h-20 w-20 items-center justify-center overflow-hidden rounded-xl border border-[#82F0DC] bg-[#B4FAF0]/50">
                  {brokenImages[img.uri] ? (
                    // 불러오지 못했을 때만 대체 아이콘을 보여준다.
                    <Text className="text-2xl">🖼️</Text>
                  ) : (
                    <Image
                      source={{ uri: img.uri }}
                      // 비율을 지킨 채 칸 안에 전부 보이게 한다.
                      resizeMode="contain"
                      className="h-full w-full"
                      accessibilityLabel={`첨부 이미지 ${index + 1}`}
                      onError={() => setBrokenImages((cur) => ({ ...cur, [img.uri]: true }))}
                    />
                  )}
                </View>
                <Pressable
                  onPress={() => removeExtractImage(index)}
                  accessibilityLabel={`첨부 이미지 ${index + 1} 삭제`}
                  hitSlop={6}
                  className="absolute -right-1.5 -top-1.5 h-5 w-5 items-center justify-center rounded-full border border-white bg-slate-800"
                >
                  <Text className="text-[9px] font-black text-white">✕</Text>
                </Pressable>
              </View>
            ))}
          </ScrollView>
        </View>
      )}

      {/* 로딩: 떠다니는 AI 로봇 */}
      {isExtracting ? (
        <FadeIn>
          <View className="my-4 items-center gap-4 rounded-3xl border-2 border-[#56EADD] bg-white p-8 shadow-md">
            <AnimatedLoop kind="float">
              <View className="h-20 w-20 items-center justify-center rounded-full border-2 border-[#50E6DC] bg-[#B4FAF0]">
                <PixelExtractIcon size={52} active />
              </View>
            </AnimatedLoop>
            <View className="items-center gap-1">
              <AnimatedLoop kind="pulse">
                <View className="rounded-full bg-[#B4FAF0] px-2.5 py-0.5">
                  <Text className="font-pixel text-[10px] text-[#075953]">AI ANALYZING...</Text>
                </View>
              </AnimatedLoop>
              <Text className="text-sm font-extrabold text-slate-800">소셜 미디어 장소를 탐색 중입니다 🎈</Text>
              <Text className="text-center text-[11px] text-slate-500">
                링크 속 위도/경도 및 감성 키워드를 분석하여{'\n'}8비트 핀으로 변환하고 있어요.
              </Text>
            </View>
          </View>
        </FadeIn>
      ) : extractError ? (
        /* 서버가 늦어도 5분 안에 끝내주고, 앱도 6분이면 포기한다. 어느 쪽이든 여기로 온다. */
        <FadeIn>
          <View className="my-4 items-center gap-3 rounded-3xl border-2 border-rose-200 bg-white p-8 shadow-md">
            <Text className="text-3xl">😵</Text>
            <Text className="text-sm font-extrabold text-slate-800">추출하지 못했어요</Text>
            <Text className="text-center text-[11px] font-semibold text-slate-500">{extractError}</Text>
            <PixelPressable
              shadowColor="#075953"
              offset={3}
              radius={14}
              onPress={handleRetryExtract}
              className="mt-1 h-11 items-center justify-center rounded-2xl border-2 border-pixel-navy bg-mint-primary px-6"
            >
              <Text className="text-xs font-black text-pixel-navy">다시 시도</Text>
            </PixelPressable>
          </View>
        </FadeIn>
      ) : extractedPlaces.length === 0 && lastAiRequestId !== null ? (
        /* 서버가 끝까지 돌았는데 0곳이면 AI 가 장소를 못 찾은 것이다. 실패와 구분해서 보여준다. */
        <FadeIn>
          <View className="my-4 items-center gap-2 rounded-3xl border-2 border-dashed border-[#82F0DC] bg-white/70 p-8">
            <Text className="text-3xl">🔍</Text>
            <Text className="text-sm font-extrabold text-slate-800">찾은 장소가 없어요</Text>
            <Text className="text-center text-[11px] font-semibold text-slate-500">
              장소 이름이 드러나는 링크나 스크린샷이면 더 잘 찾아요.
            </Text>
          </View>
        </FadeIn>
      ) : (
        extractedPlaces.length > 0 && (
          <FadeIn style={{ gap: 12 }}>
            <View className="flex-row items-center justify-between px-1">
              <View>
                <Text className="font-pixel text-[9px] text-[#075953]">AI FOUND PLACES</Text>
                <Text className="mt-1 text-sm font-extrabold text-slate-900">장소 후보 {extractedPlaces.length}곳</Text>
              </View>
              <View className="items-end gap-1.5">
                <View className="rounded-xl border border-[#82F0DC] bg-white px-2.5 py-1">
                  <Text className="text-[10px] font-bold text-[#075953]">{selectedExtractedPlaceIds.length}곳 선택</Text>
                </View>
                <Pressable
                  onPress={handleToggleAllExtractedPlaces}
                  accessibilityRole="button"
                  accessibilityLabel={allSelected ? '전체 선택 해제' : '전체 선택'}
                  className="rounded-xl border-2 border-[#075953] bg-[#B4FAF0] px-2.5 py-1 active:bg-[#56EADD]"
                >
                  <Text className="text-[10px] font-black text-[#075953]">
                    {allSelected ? '전체 선택 해제' : '전체 선택'}
                  </Text>
                </Pressable>
              </View>
            </View>

            <ScrollView style={{ maxHeight: 350 }} nestedScrollEnabled showsVerticalScrollIndicator={false}>
              <View className="gap-3 pr-1">
                {extractedPlaces.map((place) => {
                  const isSelected = selectedExtractedPlaceIds.includes(place.id);
                  const needsReview = place.extractionStatus === 'review';
                  return (
                    <Pressable
                      key={place.id}
                      onPress={() => handleToggleExtractedPlace(place.id)}
                      accessibilityRole="checkbox"
                      accessibilityState={{ checked: isSelected }}
                      className={`relative rounded-3xl border-2 bg-white p-3 ${
                        isSelected ? 'border-[#56EADD] shadow-md' : 'border-slate-200 opacity-75 shadow-sm'
                      }`}
                    >
                      <View
                        className={`absolute right-3 top-3 h-6 w-6 items-center justify-center rounded-md border-2 ${
                          isSelected ? 'border-[#075953] bg-[#56EADD]' : 'border-slate-300 bg-white'
                        }`}
                      >
                        <Text className={`font-black ${isSelected ? 'text-[#075953]' : 'text-transparent'}`}>✓</Text>
                      </View>

                      <View className="mb-2 flex-row items-center justify-between pr-9">
                        <View
                          className={`rounded-lg border px-2.5 py-1.5 ${
                            needsReview ? 'border-amber-300 bg-amber-100' : 'border-[#50E6DC] bg-[#56EADD]'
                          }`}
                        >
                          <Text className={`text-[11px] font-black leading-none ${needsReview ? 'text-amber-800' : 'text-[#075953]'}`}>
                            {needsReview ? '확인필요' : 'AI 추출 성공!'}
                          </Text>
                        </View>
                        <Text className="text-[10px] font-bold text-slate-400">정확도 {place.confidence}%</Text>
                      </View>

                      {/* 썸네일을 빼고 장소 정보만 남긴다. 순번은 이름 앞 배지로 보여준다. */}
                      <View className="min-w-0 flex-1 pr-9">
                        <Text className="text-[9px] font-bold text-[#08736d]">{place.category}</Text>
                        <View className="mt-0.5 flex-row items-center gap-1.5">
                          <View className="rounded-md bg-slate-900/80 px-1.5 py-0.5">
                            <Text className="font-pixel text-[7px] text-white">PIN {place.order}</Text>
                          </View>
                          <Text numberOfLines={1} className="min-w-0 flex-1 text-sm font-extrabold text-slate-800">
                            {place.name}
                          </Text>
                        </View>
                        <View className="mt-1 flex-row items-center gap-1.5">
                          <PixelStar size={12} />
                          <Text className="text-[10px] font-bold text-slate-700">{place.rating}</Text>
                          <Text className="text-[10px] text-slate-400">리뷰 {place.reviewsCount}</Text>
                        </View>
                        <Text numberOfLines={1} className="mt-1 text-[10px] text-slate-500">
                          {place.address}
                        </Text>
                      </View>

                      {place.note ? (
                        <View
                          className={`mt-2 rounded-xl border px-2.5 py-2 ${
                            needsReview ? 'border-amber-200 bg-amber-50' : 'border-[#82F0DC] bg-[#B4FAF0]/40'
                          }`}
                        >
                          <Text className={`text-[10px] leading-4 ${needsReview ? 'text-amber-800' : 'text-[#075953]'}`}>
                            {place.note}
                          </Text>
                        </View>
                      ) : null}
                    </Pressable>
                  );
                })}
              </View>
            </ScrollView>

            <PixelPressable
              shadowColor="#075953"
              offset={3}
              radius={16}
              disabled={noneSelected}
              onPress={() => setIsTripPickerOpen((open) => !open)}
              wrapperClassName="w-full"
              className="h-12 w-full items-center justify-center rounded-2xl border-2 border-[#075953] bg-[#56EADD]"
              disabledClassName="h-12 w-full items-center justify-center rounded-2xl border-2 border-slate-300 bg-slate-200"
            >
              <Text className={`text-sm font-extrabold ${noneSelected ? 'text-slate-400' : 'text-slate-900'}`}>
                어느 여행으로 보낼까요? · {selectedExtractedPlaceIds.length}곳
              </Text>
            </PixelPressable>

            {isTripPickerOpen && (
              <View className="rounded-3xl border-2 border-[#82F0DC] bg-white p-3 shadow-md">
                <View className="mb-2 flex-row items-center justify-between">
                  <Text className="text-xs font-extrabold text-slate-800">내 여행 선택</Text>
                  <Text className="font-pixel text-[8px] text-[#075953]">BATCH ADD</Text>
                </View>
                {trips.length > 0 && targetTripId !== undefined ? (
                  <View className="gap-2">
                    <PixelSelect
                      size="lg"
                      accessibilityLabel="추출 장소를 보낼 내 여행"
                      value={String(targetTripId)}
                      options={trips.map((trip) => ({ value: String(trip.id), label: `${trip.dDay} · ${trip.title}` }))}
                      onChange={(value) => setSelectedTripForExtract(Number(value))}
                    />
                    <Pressable
                      onPress={() => handleAddPlaceToTrip(targetTripId)}
                      className="h-11 w-full items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-900 active:opacity-80"
                    >
                      <Text className="text-xs font-black text-[#56EADD]">
                        선택한 여행에 {selectedExtractedPlaceIds.length}곳 보내기
                      </Text>
                    </Pressable>
                  </View>
                ) : (
                  <Text className="py-3 text-center text-xs font-bold text-slate-400">
                    보낼 여행이 없어요. 아래에서 새 여행을 만들어 보내세요.
                  </Text>
                )}

                {/* 기존 여행과 따로 새 여행을 만들어 바로 보낸다. */}
                <View className="mt-2 border-t border-slate-100 pt-2">
                  {isNewTripOpen ? (
                    <View className="gap-2">
                      <TextInput
                        value={newTripTitle}
                        onChangeText={setNewTripTitle}
                        placeholder="새 여행 이름"
                        placeholderTextColor="#94A3B8"
                        maxLength={100}
                        autoFocus
                        className="h-11 rounded-xl border-2 border-[#82F0DC] bg-white px-3 py-0 text-sm font-bold text-slate-800"
                      />
                      <View className="flex-row gap-2">
                        <Pressable
                          onPress={() => setIsNewTripOpen(false)}
                          disabled={isCreatingExtractTrip}
                          className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-slate-300 bg-white"
                        >
                          <Text className="text-xs font-black text-slate-600">취소</Text>
                        </Pressable>
                        <Pressable
                          onPress={async () => {
                            if (await handleCreateTripAndAdd(newTripTitle)) {
                              setNewTripTitle('');
                              setIsNewTripOpen(false);
                            }
                          }}
                          disabled={!newTripTitle.trim() || isCreatingExtractTrip}
                          style={{ opacity: !newTripTitle.trim() || isCreatingExtractTrip ? 0.5 : 1 }}
                          className="h-11 flex-[2] items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
                        >
                          <Text className="text-xs font-black text-pixel-navy">
                            {isCreatingExtractTrip ? '만드는 중…' : `만들고 ${selectedExtractedPlaceIds.length}곳 보내기`}
                          </Text>
                        </Pressable>
                      </View>
                    </View>
                  ) : (
                    <Pressable
                      onPress={() => setIsNewTripOpen(true)}
                      accessibilityLabel="새 여행 만들기"
                      className="h-11 w-full items-center justify-center rounded-xl border-2 border-dashed border-[#075953] bg-white active:bg-[#B4FAF0]/40"
                    >
                      <Text className="text-xs font-black text-[#075953]">＋ 새 여행 만들기</Text>
                    </Pressable>
                  )}
                </View>
              </View>
            )}
          </FadeIn>
        )
      )}
    </FadeIn>
  );
}
