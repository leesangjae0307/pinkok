import api from './axios';

/** 초대를 주고받는 사람. 목록에 얼굴과 이름을 띄우는 데 필요한 것만 온다. */
export interface InvitationPerson {
  userId: number;
  username: string;
  nickname: string;
  profileImageUrl: string | null;
  avatarImageUrl: string | null;
}

export interface TripInvitationResponse {
  id: number;
  tripId: number;
  tripTitle: string;
  tripRegion: string | null;
  startDate: string | null;
  endDate: string | null;
  /** PENDING / ACCEPTED / REJECTED */
  status: string;
  inviter: InvitationPerson;
  invitee: InvitationPerson;
  createdAt: string;
  respondedAt: string | null;
}

export interface TripMember {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  avatarImageUrl: string | null;
  /** OWNER / MEMBER */
  role: string;
  joinedAt: string;
  /** 요청한 사람 본인인지. "나" 표시와 나가기 버튼에 쓴다. */
  me: boolean;
}

/**
 * 초대할 사람 검색 결과.
 *
 * 닉네임은 중복될 수 있어 화면에 아이디(@username)를 같이 보여줘야 누구인지 구분된다.
 * 초대할 때 서버로 보내는 값도 닉네임이 아니라 아이디다.
 */
export interface UserSearchResult {
  username: string;
  nickname: string;
  profileImageUrl: string | null;
  avatarImageUrl: string | null;
  /** 이미 이 여행의 팀원인지 (tripId 를 같이 보냈을 때만 의미 있다) */
  alreadyMember: boolean;
  /** 이미 초대를 보내 답을 기다리는 중인지 */
  alreadyInvited: boolean;
}

/** 아이디와 닉네임 둘 다로 찾는다. 2글자 이상이어야 한다. */
export async function searchUsers(keyword: string, tripId?: number): Promise<UserSearchResult[]> {
  const { data } = await api.get<UserSearchResult[]>('/users/search', {
    params: tripId ? { keyword, tripId } : { keyword },
  });
  return data;
}

/** 내가 받은 초대장. 아직 답하지 않은 것만 쓰려면 status 로 걸러 쓴다. */
export async function fetchMyInvitations(): Promise<TripInvitationResponse[]> {
  const { data } = await api.get<TripInvitationResponse[]>('/trip-invitations');
  return data;
}

/** 여행을 만든 뒤에 팀원을 더 초대한다. 닉네임이 아니라 @아이디를 보낸다. */
export async function inviteToTrip(tripId: number, username: string): Promise<TripInvitationResponse> {
  const { data } = await api.post<TripInvitationResponse>('/trip-invitations', { tripId, username });
  return data;
}

export async function acceptInvitation(invitationId: number): Promise<TripInvitationResponse> {
  const { data } = await api.post<TripInvitationResponse>(`/trip-invitations/${invitationId}/accept`);
  return data;
}

export async function rejectInvitation(invitationId: number): Promise<TripInvitationResponse> {
  const { data } = await api.post<TripInvitationResponse>(`/trip-invitations/${invitationId}/reject`);
  return data;
}

export async function fetchTripMembers(tripId: number): Promise<TripMember[]> {
  const { data } = await api.get<TripMember[]>('/trip-members', { params: { tripId } });
  return data;
}

/** 내가 그 여행에서 나간다. 주인은 먼저 주인을 넘겨야 한다. */
export async function leaveTrip(tripId: number): Promise<void> {
  await api.delete('/trip-members/me', { params: { tripId } });
}

/** 다른 팀원을 내보낸다. 주인만 할 수 있다. */
export async function removeTripMember(tripId: number, targetUserId: number): Promise<void> {
  await api.delete(`/trip-members/${targetUserId}`, { params: { tripId } });
}

/** 목록 카드에 쓰는 '2026.10.12 - 10.15'. 날짜가 없으면 안내 문구를 돌려준다. */
export function invitationDateLabel(startDate: string | null, endDate: string | null): string {
  if (!startDate) return '날짜 미정';
  const start = startDate.replace(/-/g, '.');
  if (!endDate) return start;
  return `${start} - ${endDate.slice(5).replace('-', '.')}`;
}
