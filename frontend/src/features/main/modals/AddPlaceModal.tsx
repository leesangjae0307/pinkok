/**
 * 장소 검색 → 지도에 핀 추가
 *
 * 카카오맵 키워드 검색(GET /places/search)으로 찾은 결과를 그대로
 * POST /itinerary-items 에 넘긴다. 두 DTO 의 필드가 같아서 변환이 필요 없다.
 */
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ModalBackdrop } from '../../../components/pixel/Primitives';
import { errorMessage } from '../../../api/axios';
import { placeAddress, searchPlaces, type PlaceSearchResult } from '../../../api/placeApi';

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
          if (cancelled) return;
          setResults(page.places);
          setSearchError(null);
        })
        .catch((e) => {
          if (cancelled) return;
          setResults([]);
          setSearchError(errorMessage(e, '장소를 찾지 못했어요.'));
        })
        .finally(() => {
          if (!cancelled) setIsSearching(false);
        });
    }, 300);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [keyword]);

  /** 같은 장소를 두 번 누르지 않도록 카카오 id 로 잠근다. */
  const handleAdd = async (place: PlaceSearchResult) => {
    const key = place.kakaoPlaceId ?? place.name;
    if (addingId) return;
    setAddingId(key);
    try {
      const ok = await onAdd(place);
      if (ok) onClose();
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

        <View className="p-4">
          <TextInput
            value={keyword}
            onChangeText={setKeyword}
            placeholder="🔍 장소 이름으로 검색 (예: 협재해수욕장)"
            placeholderTextColor="#94A3B8"
            autoFocus
            className="h-12 rounded-2xl border-2 border-pixel-navy bg-white px-3 py-0 text-sm font-bold text-slate-900"
          />
        </View>

        <View className="gap-2 px-4 pb-4">
          {isSearching ? (
            <View className="items-center rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <ActivityIndicator color="#075953" />
            </View>
          ) : searchError ? (
            <View className="rounded-2xl border-2 border-dashed border-[#82F0DC] bg-white/60 p-8">
              <Text className="text-center text-xs font-bold text-slate-500">{searchError}</Text>
            </View>
          ) : results.length > 0 ? (
            results.slice(0, 8).map((place) => {
              const key = place.kakaoPlaceId ?? place.name;
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
                    {place.categoryName ? (
                      <Text numberOfLines={1} className="mt-0.5 text-[9px] font-bold text-[#075953]">
                        {place.categoryName}
                      </Text>
                    ) : null}
                  </View>
                  <Pressable
                    onPress={() => handleAdd(place)}
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
        </View>
      </View>
    </ModalBackdrop>
  );
}
