/**
 * 장소를 몇 시에 갈지 고르는 시간 선택기.
 *
 * 안드로이드에서는 OS 기본 시계 다이얼로그가 그대로 뜬다(따로 감싸는 창이 없다).
 * 고르면 onSelect, 취소하면 onClose 가 불린다.
 */
import React from 'react';
import DateTimePicker, { type DateTimePickerEvent } from '@react-native-community/datetimepicker';

import { PlaceItem } from '../../../data/mockData';

/**
 * 'HH:mm' 을 오늘 날짜의 Date 로. 값이 없거나 이상하면 오전 10시로 시작한다.
 *
 * Number('') 은 NaN 이 아니라 0 이라, 빈 값을 숫자로 바꾸기 전에 걸러야 한다.
 * (안 그러면 시각이 없는 핀이 00:00 으로 열린다.)
 */
function toDate(time: string | undefined): Date {
  const d = new Date();
  const parse = (value: string | undefined, fallback: number) => {
    if (value == null || value.trim() === '') return fallback;
    const n = Number(value);
    return Number.isFinite(n) ? n : fallback;
  };
  const [h, m] = (time ?? '').split(':');
  d.setHours(parse(h, 10), parse(m, 0), 0, 0);
  return d;
}

function hhmm(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function PlaceTimePicker({
  place,
  onClose,
  onSelect,
}: {
  place: PlaceItem;
  onClose: () => void;
  onSelect: (time: string) => void;
}) {
  const handleChange = (event: DateTimePickerEvent, date?: Date) => {
    // 안드로이드는 취소해도 콜백이 오고, type 으로만 구분된다.
    if (event.type !== 'set' || !date) {
      onClose();
      return;
    }
    onSelect(hhmm(date));
  };

  return (
    <DateTimePicker
      value={toDate(place.time)}
      mode="time"
      is24Hour
      display="default"
      onChange={handleChange}
    />
  );
}
