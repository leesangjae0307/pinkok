/**
 * 장소 검색 → 지도에 핀 추가
 *
 * 카카오맵 키워드 검색(GET /places/search)으로 찾은 결과를 그대로
 * POST /itinerary-items 에 넘긴다. 두 DTO 의 필드가 같아서 변환이 필요 없다.
 */
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ModalBackdrop } from '../../../components/pixel/Primitives';
import { errorMessage } from '../../../api/axios';
import {
  fetchPlaceCategories,
  placeAddress,
  searchPlaces,
  type PlaceCategory,
  type PlaceSearchResult,
} from '../../../api/placeApi';

/** 종류 목록을 못 받았을 때의 대비. 기본 종류는 늘 기타다. */
const DEFAULT_CATEGORY: PlaceCategory = { code: 'ETC', label: '기타', order: 8 };

export default function AddPlaceModal({
  tripTitle,
  onClose,
  onAdd,
}: {
  tripTitle: string;
  onClose: () => void;
  /** 고른 장소를 핀으로 추가한다. 성공하면 모달을 닫는다. */
  onAdd: (place: PlaceSearchResult) => Promise<boolean>;
}) {
  const insets = useSafeAreaInsets();
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<PlaceSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [addingId, setAddingId] = useState<string | null>(null);
  /** 주소로 찾은 위치를 골랐을 때: 이름과 종류를 입력받는 단계 */
  const [namingPlace, setNamingPlace] = useState<PlaceSearchResult | null>(null);
  const [placeName, setPlaceName] = useState('');
  const [categories, setCategories] = useState<PlaceCategory[]>([DEFAULT_CATEGORY]);
  const [categoryCode, setCategoryCode] = useState(DEFAULT_CATEGORY.code);

  useEffect(() => {
    let cancelled = false;
    fetchPlaceCategories()
      .then((list) => {
        if (!cancelled && list.length > 0) {setCategories(list);}
      })
      .catch(() => {
        // 못 받아도 기타로는 추가할 수 있다.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const isAddress = (place: PlaceSearchResult) => place.type === 'ADDRESS';
  /**
   * 목록의 키이자 '추가 중' 잠금의 키.
   *
   * 주소로 찾은 위치는 kakaoPlaceId 가 비어 있어서 이름만으로는 겹칠 수 있다.
   * 좌표까지 붙여 한 줄씩 구분한다.
   */
  const resultKey = (place: PlaceSearchResult) =>
    place.kakaoPlaceId ?? `${place.name}:${place.latitude},${place.longitude}`;
  const startNaming = (place: PlaceSearchResult) => {
    setNamingPlace(place);
    setPlaceName(place.name);
    setCategoryCode(DEFAULT_CATEGORY.code);
  };

  /** 타자마다 요청하면 금방 쌓이므로 300ms 쉬고 보낸다. */
  useEffect(() => {
    const q = keyword.trim();
    if (q.length < 2) {
      setResults([]);
      setSearchError(null);
      return;
    }
    let cancelled = false;
    setIsSearching(true);
    const timer = setTimeout(() => {
      searchPlaces(q)
        .then((page) => {
          if (cancelled) {return;}
          setResults(page.places);
          setSearchError(null);
        })
        .catch((e) => {
          if (cancelled) {return;}
          setResults([]);
          setSearchError(errorMessage(e, '장소를 찾지 못했어요.'));
        })
        .finally(() => {
          if (!cancelled) {setIsSearching(false);}
        });
    }, 300);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [keyword]);

  /** 같은 장소를 두 번 누르지 않도록 잠근다. */
  const handleAdd = async (place: PlaceSearchResult) => {
    const key = resultKey(place);
    if (addingId) {return;}
    setAddingId(key);
    try {
      const ok = await onAdd(place);
      if (ok) {onClose();}
    } finally {
      setAddingId(null);
    }
  };

  return (
    <ModalBackdrop onRequestClose={onClose}>
      <View
        className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]"
        style={{ marginBottom: insets.bottom, maxHeight: '85%' }}
      >
        <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
          <View className="flex-1 pr-2">
            <Text className="font-pixel text-[11px] text-[#075953]">장소 추가</Text>
            <Text numberOfLines={1} className="mt-2 text-[11px] font-bold text-slate-500">
              '{tripTitle}'에 넣을 장소를 검색해요.
            </Text>
          </View>
          <Pressable
            onPress={onClose}
            accessibilityLabel="장소 추가 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5"
          >
            <Text className="text-sm font-black text-slate-700">✕</Text>
          </Pressable>
        </View>

        {namingPlace ? (
          <View className="gap-3 p-4">
            <Text className="text-sm font-black text-slate-900">장소 이름을 입력하세요</Text>
            <View className="rounded-xl border border-[#82F0DC] bg-white px-3 py-2">
              <Text numberOfLines={2} className="text-[10px] font-semibold text-slate-500">
                📍 {placeAddress(namingPlace)}
              </Text>
            </View>
            <TextInput
              value={placeName}
              onChangeText={setPlaceName}
              placeholder="장소 이름"
              placeholderTextColor="#94A3B8"
              maxLength={100}
              autoFocus
              className="h-12 rounded-2xl border-2 border-pixel-navy bg-white px-3 py-0 text-sm font-bold text-slate-900"
            />
            <Text className="text-[11px] font-black text-slate-700">장소 종류</Text>
            <View className="flex-row flex-wrap gap-1.5">
              {categories.map((c) => {
                const on = c.code === categoryCode;
                return (
                  <Pressable
                    key={c.code}
                    onPress={() => setCategoryCode(c.code)}
                    accessibilityRole="radio"
                    accessibilityState={{ selected: on }}
                    style={{ backgroundColor: on ? '#56EADD' : '#fff' }}
                    className={`rounded-full border-2 px-3 py-1.5 ${on ? 'border-pixel-navy' : 'border-slate-200'}`}
                  >
                    <Text className={`text-[11px] font-black ${on ? 'text-pixel-navy' : 'text-slate-600'}`}>{c.label}</Text>
                  </Pressable>
                );
              })}
            </View>
            <View className="flex-row gap-2">
              <Pressable
                onPress={() => setNamingPlace(null)}
                disabled={addingId !== null}
                className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-slate-300 bg-white"
              >
                <Text className="text-xs font-black text-slate-600">뒤로</Text>
              </Pressable>
              <Pressable
                onPress={() =>
                  handleAdd({
                    ...namingPlace,
                    name: placeName.trim(),
                    categoryGroupCode: categoryCode,
                    categoryName: categories.find((c) => c.code === categoryCode)?.label ?? null,
                  })
                }
                disabled={!placeName.trim() || addingId !== null}
                style={{ opacity: !placeName.trim() || addingId !== null ? 0.5 : 1 }}
                className="h-11 flex-[2] items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
              >
                {addingId !== null ? (
                  <ActivityIndicator size="small" color="#075953" />
                ) : (
                  <Text className="text-xs font-black text-pixel-navy">추가</Text>
                )}
              </Pressable>
            </View>
          </View>
        ) : (
        <>
        <View className="p-4">
          <TextInput
            value={keyword}
            onChangeText={setKeyword}
            placeholder="🔍 장소 이름 또는 주소로 검색 (예: 협재해수욕장)"
            placeholderTextColor="#94A3B8"
            autoFocus
            className="h-12 rounded-2xl border-2 border-pixel-navy bg-white px-3 py-0 text-sm font-bold text-slate-900"
          />
        </View>

        {/*
          서버는 키워드 결과 뒤에 주소 결과를 붙여 준다. 앞에서 여덟 개만 잘라
          보여 주면 뒤에 붙은 주소 결과가 통째로 묻히므로, 전부 싣고 넘겨 본다.
        */}
        <ScrollView
          style={{ maxHeight: 420 }}
          keyboardShouldPersistTaps="handled"
          contentContainerClassName="gap-2 px-4 pb-4"
        >
          {isSearching ? (
            <View className="items-center rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <ActivityIndicator color="#075953" />
            </View>
          ) : searchError ? (
            <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <Text className="text-center text-xs font-bold text-slate-500">{searchError}</Text>
            </View>
          ) : results.length > 0 ? (
            results.map((place) => {
              const key = resultKey(place);
              const busy = addingId === key;
              return (
                <View
                  key={key}
                  className="flex-row items-center gap-3 rounded-2xl border border-[#82F0DC] bg-white p-3 shadow-sm"
                >
                  <View className="min-w-0 flex-1">
                    <Text numberOfLines={1} className="text-sm font-black text-slate-900">
                      {place.name}
                    </Text>
                    <Text numberOfLines={1} className="mt-1 text-[10px] font-semibold text-slate-500">
                      📍 {placeAddress(place)}
                    </Text>
                    {isAddress(place) ? (
                      <Text numberOfLines={1} className="mt-0.5 text-[9px] font-black text-amber-700">
                        🏠 주소로 찾은 위치
                      </Text>
                    ) : null}
                    {place.categoryName ? (
                      <Text numberOfLines={1} className="mt-0.5 text-[9px] font-bold text-[#075953]">
                        {place.categoryName}
                      </Text>
                    ) : null}
                  </View>
                  <Pressable
                    onPress={() => (isAddress(place) ? startNaming(place) : handleAdd(place))}
                    disabled={addingId !== null}
                    className="h-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD] px-3 active:translate-y-0.5"
                  >
                    {busy ? (
                      <ActivityIndicator size="small" color="#075953" />
                    ) : (
                      <Text className="text-[10px] font-black text-pixel-navy">추가</Text>
                    )}
                  </Pressable>
                </View>
              );
            })
          ) : (
            <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <Text className="text-center text-xs font-bold text-slate-400">
                {keyword.trim().length < 2 ? '두 글자 이상 입력해 주세요.' : '검색 결과가 없어요.'}
              </Text>
            </View>
          )}
        </ScrollView>
        </>
        )}
      </View>
    </ModalBackdrop>
  );
}
