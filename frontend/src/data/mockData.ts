/**
 * PINKOK 목업 데이터 (Figma Make 웹 디자인에서 그대로 이관)
 * 백엔드 API 연결 전까지 화면/기능 동작용으로 사용해요.
 */

/** 이모지(string) 또는 로컬 이미지(require 결과 number) */
export type AvatarSource = string | number;

/** 아바타가 이미지인지(이모지가 아닌지) 판별 */
export const isImageAvatar = (avatar: AvatarSource): boolean =>
  typeof avatar === 'number' || avatar.includes('/');

/** Image 컴포넌트 source 로 변환 (로컬 이미지 / 원격 URL 모두 지원) */
export const toImageSource = (src: AvatarSource) =>
  typeof src === 'number' ? src : { uri: src };

export type TabType = 'home' | 'extract' | 'trips' | 'pinlog' | 'profile';

/**
 * 여행에 함께하는 사람 한 명. 서버가 여행 응답에 실어 주는 값 그대로다.
 *
 * 아바타와 사진 중 하나만 채워져 오므로, 그릴 때 otherUserAvatar() 로 고른다.
 * 이메일 같은 개인정보는 서버도 주지 않고 화면에도 쓰지 않는다.
 */
export interface TripMemberInfo {
  userId: number;
  nickname: string;
  avatarImageUrl: string | null;
  profileImageUrl: string | null;
  /** OWNER / MEMBER */
  role: string;
  /** 나 자신인지 */
  me: boolean;
}

export interface TripFolder {
  id: number;
  title: string;
  date: string;
  dDay: string;
  /** 원격 URL 또는 로컬 이미지(기본 픽셀 표지). Image 에는 toImageSource() 로 넘긴다. */
  image: AvatarSource;
  placesCount: number;
  /** 동행 유형 꼬리표(커플여행 등). 서버의 companionType 으로 만든다. */
  tag: string;
  /** 사용자가 붙인 해시태그. 서버는 # 없이 주고 받는다. */
  tags: string[];
  /** 내가 이 여행의 주인인지. 삭제처럼 OWNER 만 되는 동작을 가릴 때 쓴다. */
  isOwner?: boolean;
  /** 함께 가는 사람들. 서버가 주지 않던 옛 응답에서는 빈 배열이다. */
  members: TripMemberInfo[];
  /** 나를 포함한 전체 팀원 수. */
  memberCount: number;
}

export interface PlaceItem {
  id: number;
  name: string;
  category: string;
  rating: number;
  reviewsCount: number;
  address: string;
  thumbnail: string;
  /** 실제 위도(WGS84). 카카오맵 마커에 그대로 쓴다. */
  lat: number;
  /** 실제 경도(WGS84) */
  lng: number;
  order: number;
  /** 1일차, 2일차 ... 날짜를 안 정한 핀은 0 */
  day: number;
  time?: string;
  /** 서버가 준 장소 종류. 여행 "전체" 탭에서 묶는 기준 */
  categoryCode?: string;
  categoryLabel?: string;
  categoryOrder?: number;
  /** 사용자가 이 핀의 분류를 직접 바꿨는지. '원래대로' 노출에 쓴다. */
  categoryOverridden?: boolean;
  note?: string;
}

export interface VlogClip {
  id: number;
  time: string;
  location: string;
  thumbnail: string;
  /** 완성된 영상 주소. 앱 안에서 재생한다. */
  videoUrl?: string;
  selected: boolean;
}


export interface TeamMember {
  id: number;
  name: string;
  avatar: string;
  role: string;
  isMe?: boolean;
}

export const CURRENT_USER: TeamMember = {
  id: 1,
  name: '상재',
  avatar: '🧢',
  role: '여행 리더',
  isMe: true,
};


export interface PlaceRecord {
  id: number;
  pinId: number;
  userId: number;
  authorName: string;
  /** 이모지 문자열 또는 require()/import 로 불러온 이미지 */
  authorAvatar: AvatarSource;
  createdAt: string;
  rating: number;
  memo: string;
  photos: string[];
  videoClip?: {
    id: number;
    duration: number; // e.g. 2.5
    thumbnail: string;
    videoUrl?: string;
    createdAt: string;
  };
}



