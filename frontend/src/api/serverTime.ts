/**
 * 서버가 준 시각 문자열을 해석한다.
 *
 * 서버는 "2026-11-20T05:30:00" 처럼 시간대 표시가 없는 UTC 시각을 준다.
 * 자바스크립트는 이런 문자열을 "기기의 시간대" 로 읽어버려서, 한국에서는
 * 9시간이 어긋난다. 방금 온 알림이 "9시간 전" 으로 보이던 이유가 이것이다.
 *
 * 그래서 시간대 표시가 없으면 UTC 로 보고 Z 를 붙여 읽는다. Z 나 +09:00 이
 * 이미 붙어 있으면 그대로 둔다. 나중에 서버가 시간대를 붙여 주더라도 그대로
 * 맞는다.
 */

/** 끝에 Z 가 있거나 ±HH:MM 오프셋이 붙어 있으면 시간대가 적힌 것이다. */
const HAS_TIMEZONE = /(?:Z|[+-]\d{2}:?\d{2})$/i;

/** 밀리초로 바꾼다. 읽을 수 없으면 NaN. */
export function parseServerTime(iso: string): number {
  if (!iso) {return NaN;}
  return new Date(HAS_TIMEZONE.test(iso) ? iso : `${iso}Z`).getTime();
}

/** 기기 시간대 기준 'YYYY.MM.DD'. 날짜만 보여줄 때 쓴다. */
export function serverDateLabel(iso: string): string {
  const ms = parseServerTime(iso);
  if (Number.isNaN(ms)) {return '';}
  const d = new Date(ms);
  const month = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}.${month}.${day}`;
}
