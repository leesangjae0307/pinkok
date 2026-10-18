import React from 'react';
import { Image, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';

import { PixelExtractIcon, PixelHotAirBalloon, PixelStar } from '../../../components/pixel/PixelIcons';
import { AnimatedLoop, FadeIn, PixelPressable } from '../../../components/pixel/Primitives';
import { useApp } from '../AppContext';

export default function ExtractTab() {
  const {
    inputUrl,
    setInputUrl,
    isExtracting,
    extractedPlaces,
    selectedExtractedPlaceIds,
    isTripPickerOpen,
    setIsTripPickerOpen,
    trips,
    selectedTripForExtract,
    showToast,
    handleExtractSubmit,
    handleToggleExtractedPlace,
    handleAddPlaceToTrip,
  } = useApp();

  const handlePickScreenshot = async () => {
    const res = await launchImageLibrary({ mediaType: 'photo', selectionLimit: 1 });
    const asset = res.assets?.[0];
    if (asset) {
      setInputUrl(asset.fileName ?? asset.uri ?? '스크린샷');
      showToast('스크린샷이 분석 대기열에 추가됐어요.');
    }
  };

  const noneSelected = selectedExtractedPlaceIds.length === 0;

  return (
    <FadeIn style={{ padding: 16, paddingBottom: 80, gap: 16 }}>
      {/* 헤더 */}
      <View className="flex-row items-center justify-between pt-1">
        <View className="flex-row items-center gap-2">
          <PixelExtractIcon size={34} active />
          <View>
            <Text className="font-pixel text-xl tracking-tight text-[#0f6861]">PINKOK</Text>
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
          onPress={handlePickScreenshot}
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

      {/* 로딩: 떠다니는 열기구 */}
      {isExtracting ? (
        <FadeIn>
          <View className="my-4 items-center gap-4 rounded-3xl border-2 border-[#56EADD] bg-white p-8 shadow-md">
            <AnimatedLoop kind="float">
              <View className="h-20 w-20 items-center justify-center rounded-full border-2 border-[#50E6DC] bg-[#B4FAF0]">
                <PixelHotAirBalloon size={44} active />
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
      ) : (
        extractedPlaces.length > 0 && (
          <FadeIn style={{ gap: 12 }}>
            <View className="flex-row items-center justify-between px-1">
              <View>
                <Text className="font-pixel text-[9px] text-[#075953]">AI FOUND PLACES</Text>
                <Text className="mt-1 text-sm font-extrabold text-slate-900">장소 후보 {extractedPlaces.length}곳</Text>
              </View>
              <View className="rounded-xl border border-[#82F0DC] bg-white px-2.5 py-1">
                <Text className="text-[10px] font-bold text-[#075953]">{selectedExtractedPlaceIds.length}곳 선택</Text>
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

                      <View className="flex-row gap-3">
                        <View className="relative h-20 w-20 overflow-hidden rounded-2xl border border-slate-100">
                          <Image source={{ uri: place.thumbnail }} resizeMode="cover" className="h-full w-full" />
                          <View className="absolute bottom-1 right-1 rounded-md bg-slate-900/80 px-1.5 py-0.5">
                            <Text className="font-pixel text-[7px] text-white">PIN {place.order}</Text>
                          </View>
                        </View>
                        <View className="min-w-0 flex-1">
                          <Text className="text-[9px] font-bold text-[#08736d]">{place.category}</Text>
                          <Text numberOfLines={1} className="mt-0.5 text-sm font-extrabold text-slate-800">
                            {place.name}
                          </Text>
                          <View className="mt-1 flex-row items-center gap-1.5">
                            <PixelStar size={12} />
                            <Text className="text-[10px] font-bold text-slate-700">{place.rating}</Text>
                            <Text className="text-[10px] text-slate-400">리뷰 {place.reviewsCount}</Text>
                          </View>
                          <Text numberOfLines={1} className="mt-1 text-[10px] text-slate-500">
                            {place.address}
                          </Text>
                        </View>
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
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
                  {trips.map((trip) => (
                    <Pressable
                      key={trip.id}
                      onPress={() => handleAddPlaceToTrip(trip.id)}
                      className={`w-36 rounded-2xl border-2 p-2.5 ${
                        selectedTripForExtract === trip.id ? 'border-[#50E6DC] bg-[#B4FAF0]/65' : 'border-slate-200 bg-slate-50'
                      }`}
                    >
                      <Text className="font-pixel text-[8px] text-[#075953]">{trip.dDay}</Text>
                      <Text numberOfLines={1} className="mt-1 text-xs font-bold text-slate-800">
                        {trip.title}
                      </Text>
                      <View className="mt-2 rounded-xl bg-slate-900 px-2 py-1">
                        <Text className="text-center text-[9px] font-bold text-[#56EADD]">
                          {selectedExtractedPlaceIds.length}곳 보내기
                        </Text>
                      </View>
                    </Pressable>
                  ))}
                </ScrollView>
              </View>
            )}
          </FadeIn>
        )
      )}
    </FadeIn>
  );
}
