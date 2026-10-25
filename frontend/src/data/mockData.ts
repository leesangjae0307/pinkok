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

export const INITIAL_TRIPS: TripFolder[] = [
  {
    id: 1,
    title: '제주 감성 카페 & 오름 투어 🍊',
    date: '2026.10.12 - 10.15',
    dDay: 'D-28',
    image: 'https://images.unsplash.com/photo-1548115184-bc6544d06a58?auto=format&fit=crop&w=600&q=80',
    placesCount: 8,
    tag: '힐링'
  },
  {
    id: 2,
    title: '도쿄 레트로 미식 & 레고 여행 🗼',
    date: '2026.11.02 - 11.06',
    dDay: 'D-49',
    image: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=600&q=80',
    placesCount: 12,
    tag: '식도락'
  },
  {
    id: 3,
    title: '부산 광안리 바다 브런치 🌊',
    date: '2026.12.24 - 12.25',
    dDay: 'D-101',
    image: 'https://images.unsplash.com/photo-1570191830528-98e9b626e2e5?auto=format&fit=crop&w=600&q=80',
    placesCount: 5,
    tag: '우정여행'
  }
];

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

export const TEAM_MEMBERS: TeamMember[] = [
  CURRENT_USER,
  { id: 2, name: '소은', avatar: '🎀', role: '포토그래퍼' },
  { id: 3, name: '정훈', avatar: '🏄‍♂️', role: '운전/내비' },
];

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

export const INITIAL_PLACE_RECORDS: PlaceRecord[] = [
  // 1. 협재해수욕장 (p1)
  {
    id: 1,
    pinId: 1,
    userId: 1,
    authorName: '상재',
    authorAvatar: '🧢',
    createdAt: '14:25',
    rating: 5,
    memo: '물빛이 에메랄드색이라 너무 예쁨! 2.5초 영상으로 파도 부서지는 순간 담아둠.',
    photos: [
      'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=400&q=80',
      'https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=400&q=80',
    ],
    videoClip: {
      id: 1,
      duration: 2.5,
      thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=300&q=80',
      createdAt: '14:26',
    },
  },
  {
    id: 2,
    pinId: 1,
    userId: 2,
    authorName: '소은',
    authorAvatar: '🎀',
    createdAt: '14:30',
    rating: 4.5,
    memo: '바람이 좀 불지만 모래가 곱고 윤슬이 반짝반짝 빛나서 인생샷 건졌어요.',
    photos: [
      'https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?auto=format&fit=crop&w=400&q=80',
    ],
    videoClip: {
      id: 2,
      duration: 3.0,
      thumbnail: 'https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?auto=format&fit=crop&w=300&q=80',
      createdAt: '14:31',
    },
  },

  // 2. 카멜리아힐 (p2)
  {
    id: 3,
    pinId: 2,
    userId: 2,
    authorName: '소은',
    authorAvatar: '🎀',
    createdAt: '16:10',
    rating: 5,
    memo: '동백꽃 길 따라 걷는 코스 최고! 상재가 딴청 피우는 사이 2초 숏클립 찍음.',
    photos: [
      'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=400&q=80',
      'https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?auto=format&fit=crop&w=400&q=80',
    ],
    videoClip: {
      id: 3,
      duration: 2.0,
      thumbnail: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=300&q=80',
      createdAt: '16:12',
    },
  },

  // 3. 성산일출봉 (p3)
  {
    id: 4,
    pinId: 3,
    userId: 1,
    authorName: '상재',
    authorAvatar: '🧢',
    createdAt: '18:40',
    rating: 4.8,
    memo: '계단 오르느라 땀 뺐지만 정상에서 보는 분화구랑 바다 뷰는 무조건 필수 코스!',
    photos: [
      'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=400&q=80',
    ],
    videoClip: {
      id: 4,
      duration: 3.0,
      thumbnail: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=300&q=80',
      createdAt: '18:42',
    },
  },
  {
    id: 5,
    pinId: 3,
    userId: 3,
    authorName: '정훈',
    authorAvatar: '🏄‍♂️',
    createdAt: '18:45',
    rating: 5,
    memo: '일몰 직전 노을빛 바람 소리 예술입니다. 광각 2.8초 클립 등록 완료.',
    photos: [
      'https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?auto=format&fit=crop&w=400&q=80',
    ],
    videoClip: {
      id: 5,
      duration: 2.8,
      thumbnail: 'https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?auto=format&fit=crop&w=300&q=80',
      createdAt: '18:46',
    },
  },
];

export const MAP_PLACES: PlaceItem[] = [
  // DAY 1 PLACES
  {
    id: 1,
    name: '1. 협재해수욕장',
    category: '해변 / 자연 명소',
    rating: 4.8,
    reviewsCount: 142,
    address: '제주특별자치도 제주시 한림읍 협재리 2497-1',
    thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=400&q=80',
    lat: 30,
    lng: 26,
    order: 1,
    day: 1,
    time: '11:30',
    transitToNext: {
      type: 'walk',
      label: '도보 이동',
      duration: '8분',
      distance: '450m',
    },
    note: '에메랄드빛 바다와 비양도 뷰가 최고인 곳'
  },
  {
    id: 2,
    name: '2. 한림 앤트러사이트 카페',
    category: '베이커리 / 감성 카페',
    rating: 4.9,
    reviewsCount: 389,
    address: '제주 제주시 한림읍 한림로 564',
    thumbnail: 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=400&q=80',
    lat: 38,
    lng: 32,
    order: 2,
    day: 1,
    time: '13:00',
    transitToNext: {
      type: 'car',
      label: '차로 이동',
      duration: '22분',
      distance: '14.2km',
    },
    note: '공장을 개조한 레트로 감성과 향긋한 원두'
  },
  {
    id: 3,
    name: '3. 카멜리아힐',
    category: '수목원 / 테마가든',
    rating: 4.9,
    reviewsCount: 512,
    address: '제주 서귀포시 안덕면 병악로 166',
    thumbnail: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=400&q=80',
    lat: 48,
    lng: 44,
    order: 3,
    day: 1,
    time: '15:30',
    note: '아기자기한 동백나무 숲과 픽셀 감성 포토존'
  },

  // DAY 2 PLACES
  {
    id: 4,
    name: '4. 새별오름 나홀로나무',
    category: '명소 / 자연 힐링',
    rating: 4.7,
    reviewsCount: 910,
    address: '제주 제주시 애월읍 봉성리 4554',
    thumbnail: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=400&q=80',
    lat: 56,
    lng: 52,
    order: 4,
    day: 2,
    time: '11:00',
    transitToNext: {
      type: 'car',
      label: '차로 이동',
      duration: '35분',
      distance: '24.0km',
    },
    note: '바람에 흩날리는 은빛 억새와 노을 명소'
  },
  {
    id: 5,
    name: '5. 중문 주상절리대',
    category: '지질 명소 / 해안',
    rating: 4.8,
    reviewsCount: 640,
    address: '제주 서귀포시 이어도로 272',
    thumbnail: 'https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?auto=format&fit=crop&w=400&q=80',
    lat: 68,
    lng: 48,
    order: 5,
    day: 2,
    time: '14:20',
    transitToNext: {
      type: 'walk',
      label: '도보 이동',
      duration: '12분',
      distance: '800m',
    },
    note: '육각 기둥 바위와 웅장한 부서지는 파도'
  },
  {
    id: 6,
    name: '6. 서귀포 올레시장',
    category: '전통 야시장 / 먹거리',
    rating: 4.6,
    reviewsCount: 1250,
    address: '제주 서귀포시 서귀동 340',
    thumbnail: 'https://images.unsplash.com/photo-1519501025264-65ba15a82390?auto=format&fit=crop&w=400&q=80',
    lat: 72,
    lng: 56,
    order: 6,
    day: 2,
    time: '18:00',
    note: '흑돼지 강정과 마농통닭 필수 야식 투어'
  },

  // DAY 3 PLACES
  {
    id: 7,
    name: '7. 성산일출봉',
    category: '유네스코 세계자연유산',
    rating: 4.9,
    reviewsCount: 1840,
    address: '제주 서귀포시 성산읍 일출로 284-12',
    thumbnail: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=400&q=80',
    lat: 52,
    lng: 78,
    order: 7,
    day: 3,
    time: '09:30',
    transitToNext: {
      type: 'car',
      label: '차로 이동',
      duration: '18분',
      distance: '11.5km',
    },
    note: '바다 위로 솟아오른 웅장한 분화구와 시원한 해풍'
  },
  {
    id: 8,
    name: '8. 비자림 피톤치드 숲',
    category: '천연기념물 / 숲길',
    rating: 4.8,
    reviewsCount: 770,
    address: '제주 제주시 구좌읍 비자숲길 55',
    thumbnail: 'https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?auto=format&fit=crop&w=400&q=80',
    lat: 42,
    lng: 72,
    order: 8,
    day: 3,
    time: '13:00',
    transitToNext: {
      type: 'walk',
      label: '도보 이동',
      duration: '15분',
      distance: '950m',
    },
    note: '수령 800년 비자나무 숲길 걷기'
  },
  {
    id: 9,
    name: '9. 월정리 카페거리',
    category: '에메랄드 해변 / 카페',
    rating: 4.7,
    reviewsCount: 930,
    address: '제주 제주시 구좌읍 월정리 33-3',
    thumbnail: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=400&q=80',
    lat: 34,
    lng: 66,
    order: 9,
    day: 3,
    time: '15:30',
    note: '하얀 모래사장과 오색 사색의자 포토존'
  }
];

export const SAMPLE_VLOG_CLIPS: VlogClip[] = [
  { id: 1, time: '14:20', location: '제주 공항 도달', thumbnail: 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?auto=format&fit=crop&w=300&q=80', selected: true },
  { id: 2, time: '15:45', location: '해안도로 랜선드라이브', thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=300&q=80', selected: true },
  { id: 3, time: '17:10', location: '감성 카페 말차라떼', thumbnail: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=300&q=80', selected: true },
  { id: 4, time: '18:30', location: '새별오름 붉은 노을', thumbnail: 'https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?auto=format&fit=crop&w=300&q=80', selected: true },
  { id: 5, time: '19:50', location: '흑돼지 구이 불쇼', thumbnail: 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=300&q=80', selected: false },
  { id: 6, time: '21:15', location: '야시장 밤산책 픽', thumbnail: 'https://images.unsplash.com/photo-1519501025264-65ba15a82390?auto=format&fit=crop&w=300&q=80', selected: true }
];
