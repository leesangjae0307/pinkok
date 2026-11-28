/**
 * 서버가 준 시각 문자열을 해석한다.
 *
 * 서버는 이제 "2026-11-20T05:30:00Z" 처럼 UTC 표시를 붙여 준다. 그런 값은
 * 손대지 않고 그대로 읽는다.
 *
 * 전에는 표시 없이 "2026-11-20T05:30:00" 만 왔다. 자바스크립트는 이런 문자열을
 * "기기의 시간대" 로 읽어버려서 한국에서는 9시간이 어긋났고, 방금 온 알림이
 * "9시간 전" 으로 보였다. 그래서 시간대 표시가 없으면 UTC 로 보고 Z 를 붙인다.
 *
 * 이 보정은 그대로 둔다. 서버가 Z 를 붙이기 전에 깔린 앱도 아직 돌고 있고,
 * 오류 응답의 timestamp 처럼 아직 표시가 없는 자리도 남아 있다.
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
