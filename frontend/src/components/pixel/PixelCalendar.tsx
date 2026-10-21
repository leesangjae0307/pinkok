/**
 * 픽셀 달력 (여행 기간 선택)
 *
 * 웹 디자인은 <input type="date"> 두 개를 "PIXEL CALENDAR" 모달에 넣었는데,
 * RN 에는 기본 날짜 입력이 없다. 네이티브 모듈을 추가하지 않도록 달력을 직접 그린다.
 * 첫 탭 = 시작일, 두 번째 탭 = 종료일. 시작일보다 앞을 누르면 그 날이 새 시작일이 된다.
 * 날짜는 시간대 문제를 피하려고 'YYYY-MM-DD' 문자열로만 주고받는다.
 */
import React, { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { ModalBackdrop, PixelPressable } from './Primitives';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];
const CELL_WIDTH = `${100 / 7}%` as const;

/** 'YYYY-MM-DD' → 로컬 Date (UTC 파싱으로 하루 밀리는 문제 방지) */
export function parseYmd(value: string): Date {
  const [y, m, d] = value.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function toYmd(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

export function addDays(value: string, days: number): string {
  const date = parseYmd(value);
  date.setDate(date.getDate() + days);
  return toYmd(date);
}

/** 두 날짜 사이 일수 (같은 날이면 0) */
export function daysBetween(start: string, end: string): number {
  return Math.round((parseYmd(end).getTime() - parseYmd(start).getTime()) / 86400000);
}

/** '3박 4일' / '당일치기' */
export function nightsLabel(start: string, end: string): string {
  const nights = daysBetween(start, end);
  return nights <= 0 ? '당일치기' : `${nights}박 ${nights + 1}일`;
}

function monthCells(year: number, month: number): (string | null)[] {
  const firstWeekday = new Date(year, month, 1).getDay();
  const lastDate = new Date(year, month + 1, 0).getDate();
  const cells: (string | null)[] = Array(firstWeekday).fill(null);
  for (let d = 1; d <= lastDate; d += 1) {
    cells.push(toYmd(new Date(year, month, d)));
  }
  while (cells.length % 7 !== 0) {
    cells.push(null);
  }
  return cells;
}

export default function PixelCalendar({
  initialStart,
  initialEnd,
  onCancel,
  onApply,
}: {
  initialStart: string;
  initialEnd: string;
  onCancel: () => void;
  onApply: (start: string, end: string) => void;
}) {
  const [draftStart, setDraftStart] = useState(initialStart);
  const [draftEnd, setDraftEnd] = useState<string | null>(initialEnd);
  const startDate = parseYmd(initialStart);
  const [view, setView] = useState({ year: startDate.getFullYear(), month: startDate.getMonth() });

  const moveMonth = (delta: number) =>
    setView(({ year, month }) => {
      const next = new Date(year, month + delta, 1);
      return { year: next.getFullYear(), month: next.getMonth() };
    });

  const handlePick = (day: string) => {
    if (draftEnd !== null || day < draftStart) {
      // 이미 기간이 정해졌거나 시작일보다 앞이면 새로 시작
      setDraftStart(day);
      setDraftEnd(null);
      return;
    }
    setDraftEnd(day);
  };

  const today = toYmd(new Date());

  return (
    <ModalBackdrop onRequestClose={onCancel}>
      <View className="w-full rounded-[28px] border-4 border-pixel-navy bg-white p-4">
        <Text className="font-pixel text-[9px] text-[#075953]">PIXEL CALENDAR</Text>
        <Text className="mt-1 text-sm font-black text-slate-900">여행 날짜 선택</Text>

        {/* 선택 요약 */}
        <View className="mt-3 flex-row gap-2">
          {[
            { label: '시작일', value: draftStart },
            { label: '종료일', value: draftEnd },
          ].map((item) => (
            <View key={item.label} className="flex-1 rounded-xl border-2 border-[#82F0DC] px-2 py-1.5">
              <Text className="text-[9px] font-black text-slate-500">{item.label}</Text>
              <Text className="mt-0.5 font-pixel text-[9px] text-slate-900">
                {item.value ? item.value.replace(/-/g, '.') : '선택'}
              </Text>
            </View>
          ))}
        </View>

        {/* 월 이동 */}
        <View className="mt-3 flex-row items-center justify-between">
          <Pressable
            onPress={() => moveMonth(-1)}
            accessibilityLabel="이전 달"
            className="h-8 w-8 items-center justify-center rounded-lg border-2 border-pixel-navy bg-slate-100 active:opacity-70"
          >
            <Text className="font-pixel text-[10px] text-pixel-navy">‹</Text>
          </Pressable>
          <Text className="font-pixel text-[10px] text-[#075953]">
            {view.year}.{String(view.month + 1).padStart(2, '0')}
          </Text>
          <Pressable
            onPress={() => moveMonth(1)}
            accessibilityLabel="다음 달"
            className="h-8 w-8 items-center justify-center rounded-lg border-2 border-pixel-navy bg-slate-100 active:opacity-70"
          >
            <Text className="font-pixel text-[10px] text-pixel-navy">›</Text>
          </Pressable>
        </View>

        {/* 요일 */}
        <View className="mt-2 flex-row">
          {WEEKDAYS.map((w, i) => (
            <View key={w} style={{ width: CELL_WIDTH }} className="items-center py-1">
              <Text className={`text-[10px] font-black ${i === 0 ? 'text-rose-500' : 'text-slate-500'}`}>{w}</Text>
            </View>
          ))}
        </View>

        {/* 날짜 */}
        <View className="flex-row flex-wrap">
          {monthCells(view.year, view.month).map((day, i) => {
            if (!day) {
              return <View key={`empty-${i}`} style={{ width: CELL_WIDTH }} className="h-9" />;
            }
            const isEdge = day === draftStart || day === draftEnd;
            const inRange = draftEnd !== null && day > draftStart && day < draftEnd;
            const isToday = day === today;
            return (
              <Pressable
                key={day}
                onPress={() => handlePick(day)}
                accessibilityLabel={day}
                style={{ width: CELL_WIDTH }}
                className={`h-9 items-center justify-center ${inRange ? 'bg-[#B4FAF0]' : ''}`}
              >
                <View
                  className={`h-8 w-8 items-center justify-center rounded-lg ${
                    isEdge ? 'border-2 border-pixel-navy bg-[#56EADD]' : isToday ? 'border border-dashed border-[#075953]' : ''
                  }`}
                >
                  <Text className={`text-xs ${isEdge ? 'font-black text-pixel-navy' : 'font-bold text-slate-700'}`}>
                    {Number(day.slice(8))}
                  </Text>
                </View>
              </Pressable>
            );
          })}
        </View>

        <View className="mt-4 flex-row gap-2">
          <Pressable
            onPress={onCancel}
            className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:opacity-70"
          >
            <Text className="text-xs font-black text-slate-900">취소</Text>
          </Pressable>
          <PixelPressable
            shadowColor="#075953"
            offset={2}
            radius={12}
            wrapperClassName="flex-1"
            disabled={draftEnd === null}
            className="h-11 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD]"
            disabledClassName="h-11 items-center justify-center rounded-xl border-2 border-slate-300 bg-slate-200"
            onPress={() => draftEnd && onApply(draftStart, draftEnd)}
          >
            <Text className="text-xs font-black text-slate-900">{draftEnd ? '날짜 적용' : '종료일을 골라주세요'}</Text>
          </PixelPressable>
        </View>
      </View>
    </ModalBackdrop>
  );
}
