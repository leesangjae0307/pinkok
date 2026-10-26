/**
 * 여행 수정 (제목 · 지역 · 날짜)
 *
 * 목록 카드가 들고 있는 값은 화면용으로 가공돼 있다 — 제목에는 이모지가 붙어 있고
 * 날짜는 '2026.11.02 - 11.05' 형식이라 되돌릴 수 없으며 지역은 아예 없다.
 * 그래서 열릴 때 GET /trips/{id} 로 원본을 받아 채운다.
 */
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import PixelCalendar, { nightsLabel, toYmd } from '../../../components/pixel/PixelCalendar';
import { ModalBackdrop, PixelPressable } from '../../../components/pixel/Primitives';
import { errorMessage } from '../../../api/axios';
import { fetchTrip, updateTrip, type TripUpdatePayload } from '../../../api/tripApi';
import { TripFolder } from '../../../data/mockData';

const TITLE_MAX = 20;

export default function EditTripModal({
  trip,
  onClose,
  onSaved,
}: {
  trip: TripFolder;
  onClose: () => void;
  /** 저장된 뒤 목록·지도에 반영하기 위해 바뀐 값을 넘긴다. */
  onSaved: (payload: TripUpdatePayload) => void;
}) {
  const insets = useSafeAreaInsets();
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const [title, setTitle] = useState('');
  const [region, setRegion] = useState('');
  const [startDate, setStartDate] = useState(toYmd(new Date()));
  const [endDate, setEndDate] = useState(toYmd(new Date()));
  const [calendarOpen, setCalendarOpen] = useState(false);
  const [focused, setFocused] = useState<'title' | 'region' | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchTrip(trip.id)
      .then((detail) => {
        if (cancelled) return;
        setTitle(detail.title);
        setRegion(detail.region ?? '');
        if (detail.startDate) setStartDate(detail.startDate);
        if (detail.endDate) setEndDate(detail.endDate);
      })
      .catch((e) => {
        if (!cancelled) setLoadError(errorMessage(e, '여행 정보를 불러오지 못했어요.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [trip.id]);

  const canSave = title.trim().length > 0 && !isSaving;

  const handleSave = async () => {
    if (!canSave) return;
    setIsSaving(true);
    setSaveError(null);
    // 비운 지역은 null 이 아니라 빈 문자열로 보낸다. 보내지 않으면 서버가 그대로 둔다.
    const payload: TripUpdatePayload = {
      title: title.trim(),
      region: region.trim(),
      startDate,
      endDate,
    };
    try {
      await updateTrip(trip.id, payload);
      onSaved(payload);
      onClose();
    } catch (e) {
      setSaveError(errorMessage(e, '여행을 수정하지 못했어요.'));
    } finally {
      setIsSaving(false);
    }
  };

  const inputBorder = (key: typeof focused) => (focused === key ? 'border-[#56EADD]' : 'border-slate-200');

  if (calendarOpen) {
    return (
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
    );
  }

  return (
    <ModalBackdrop onRequestClose={onClose}>
      <View
        className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]"
        style={{ marginBottom: insets.bottom }}
      >
        <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
          <View className="flex-1 pr-2">
            <Text className="font-pixel text-[11px] text-[#075953]">여행 수정</Text>
            <Text className="mt-2 text-[11px] font-bold text-slate-500">제목 · 지역 · 날짜를 바꿀 수 있어요.</Text>
          </View>
          <Pressable
            onPress={onClose}
            accessibilityLabel="여행 수정 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5"
          >
            <Text className="text-sm font-black text-slate-700">✕</Text>
          </Pressable>
        </View>

        {isLoading ? (
          <View className="items-center p-10">
            <ActivityIndicator color="#075953" />
          </View>
        ) : loadError ? (
          <View className="items-center p-8">
            <Text className="text-center text-xs font-bold text-slate-500">{loadError}</Text>
          </View>
        ) : (
          <View className="gap-3 p-4">
            <View>
              <View className="flex-row items-center justify-between">
                <Text className="text-[11px] font-black text-slate-700">여행 제목</Text>
                <Text className="font-pixel text-[9px] text-slate-400">
                  {title.length}/{TITLE_MAX}
                </Text>
              </View>
              <TextInput
                value={title}
                onChangeText={setTitle}
                maxLength={TITLE_MAX}
                onFocus={() => setFocused('title')}
                onBlur={() => setFocused(null)}
                placeholder="여행 제목"
                placeholderTextColor="#94A3B8"
                className={`mt-1.5 h-11 rounded-xl border-2 bg-white px-3 py-0 text-sm font-black text-slate-900 ${inputBorder('title')}`}
              />
            </View>

            <View>
              <Text className="text-[11px] font-black text-slate-700">여행지 (지역)</Text>
              <TextInput
                value={region}
                onChangeText={setRegion}
                maxLength={100}
                onFocus={() => setFocused('region')}
                onBlur={() => setFocused(null)}
                placeholder="부산, 제주, 도쿄 등"
                placeholderTextColor="#94A3B8"
                className={`mt-1.5 h-11 rounded-xl border-2 bg-white px-3 py-0 text-sm font-bold text-slate-900 ${inputBorder('region')}`}
              />
            </View>

            <View>
              <Text className="text-[11px] font-black text-slate-700">여행 날짜</Text>
              <Pressable
                onPress={() => setCalendarOpen(true)}
                className="mt-1.5 h-11 flex-row items-center justify-between rounded-xl border-2 border-slate-200 bg-white px-3 active:translate-y-0.5"
              >
                <Text className="text-xs font-bold text-slate-900">
                  🗓️ {startDate.replace(/-/g, '.')} ~ {endDate.replace(/-/g, '.')}
                </Text>
                <Text className="font-pixel text-[9px] text-[#075953]">{nightsLabel(startDate, endDate)}</Text>
              </Pressable>
            </View>

            {saveError ? (
              <Text className="text-center text-[11px] font-bold text-rose-500">{saveError}</Text>
            ) : null}
          </View>
        )}

        <View className="border-t-2 border-pixel-navy bg-white p-3">
          <PixelPressable
            shadowColor="#075953"
            offset={3}
            radius={14}
            disabled={!canSave || isLoading || loadError !== null}
            accessibilityLabel="여행 수정 저장"
            className="h-12 items-center justify-center rounded-2xl border-2 border-pixel-navy bg-[#56EADD]"
            disabledClassName="h-12 items-center justify-center rounded-2xl border-2 border-slate-300 bg-slate-200"
            onPress={handleSave}
          >
            {isSaving ? (
              <ActivityIndicator size="small" color="#075953" />
            ) : (
              <Text className="text-sm font-black text-pixel-navy">저장하기</Text>
            )}
          </PixelPressable>
        </View>
      </View>
    </ModalBackdrop>
  );
}
