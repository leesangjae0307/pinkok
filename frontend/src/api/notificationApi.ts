import api from './axios';

export interface NotificationResponse {
  id: number;
  /** TRIP_INVITED / AI_EXTRACT_DONE / PINLOG_DONE */
  type: string;
  title: string;
  body: string | null;
  /** 눌렀을 때 어디로 갈지. TRIP / PINLOG / AI_REQUEST / INVITATION */
  targetType: string;
  targetId: number | null;
  read: boolean;
  createdAt: string;
}

export async function fetchNotifications(): Promise<NotificationResponse[]> {
  const { data } = await api.get<NotificationResponse[]>('/notifications');
  return data;
}

export async function fetchUnreadCount(): Promise<number> {
  const { data } = await api.get<{ count: number }>('/notifications/unread-count');
  return data.count;
}

export async function markNotificationRead(notificationId: number): Promise<void> {
  await api.patch(`/notifications/${notificationId}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await api.patch('/notifications/read-all');
}

/** '방금 전' / '2시간 전' / '어제' / '3일 전'. 그보다 오래되면 날짜로 보여준다. */
export function relativeTime(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const minutes = Math.floor((Date.now() - then) / 60000);
  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days === 1) return '어제';
  if (days < 7) return `${days}일 전`;
  return iso.slice(0, 10).replace(/-/g, '.');
}
