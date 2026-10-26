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

export interface TripFolder {
  id: number;
  title: string;
  date: string;
  dDay: string;
  /** 원격 URL 또는 로컬 이미지(기본 픽셀 표지). Image 에는 toImageSource() 로 넘긴다. */
  image: AvatarSource;
  placesCount: number;
  tag: string;
  /** 내가 이 여행의 주인인지. 삭제처럼 OWNER 만 되는 동작을 가릴 때 쓴다. */
  isOwner?: boolean;
}

export interface TransitInfo {
  type: 'car' | 'walk' | 'bus';
  label: string;
  duration: string;
  distance?: string;
}

export interface PlaceItem {
  id: number;
  name: string;
  category: string;
  rating: number;
  reviewsCount: number;
  address: string;
  thumbnail: string;
  lat: number;
  lng: number;
  order: number;
  day: number; // 1, 2, or 3
  time?: string;
  transitToNext?: TransitInfo;
  note?: string;
}

export interface VlogClip {
  id: number;
  time: string;
  location: string;
  thumbnail: string;
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



