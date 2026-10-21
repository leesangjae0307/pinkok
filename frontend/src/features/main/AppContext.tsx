/**
 * 메인 앱 전역 상태 (웹 App.tsx 의 useState / 핸들러를 그대로 옮긴 것)
 * 화면(탭)들이 props 대신 useApp() 으로 꺼내 써요.
 */
import React, { createContext, useContext, useRef, useState } from 'react';

import avatarBookWinter from '../../assets/images/avatar-book-winter.png';
import avatarSilverBucket from '../../assets/images/avatar-silver-bucket.png';
import avatarPinkCamera from '../../assets/images/avatar-pink-camera.png';
import avatarMapPhone from '../../assets/images/avatar-map-phone.png';
import avatarHeadphoneWink from '../../assets/images/avatar-headphone-wink.png';
import avatarSunglasses from '../../assets/images/avatar-sunglasses.png';
import { Me, logout } from '../../api/authApi';
import { errorMessage } from '../../api/axios';
import { TRAVEL_STYLES, travelStyleLabel } from '../../constants';
import {
  CURRENT_USER,
  INITIAL_PLACE_RECORDS,
  INITIAL_TRIPS,
  MAP_PLACES,
  PlaceItem,
  PlaceRecord,
  SAMPLE_VLOG_CLIPS,
  TabType,
  TripFolder,
  VlogClip,
} from '../../data/mockData';
import type { SynthesizedVlog } from './modals/PinLogSynthesisModal';
import type { QuickClip } from './modals/QuickPinLogCameraModal';

export type NotificationType = 'TRIP_INVITED' | 'TRIP_REMINDER' | 'TRIP_PLACE_ADDED';

export interface NotificationItem {
  id: number;
  type: NotificationType;
  targetType: 'INVITATION' | 'TRIP';
  targetId: number;
  message: string;
  createdAt: string;
  read: boolean;
}

export interface TripInvitation {
  id: number;
  inviterName: string;
  inviterAvatar: string;
  tripTitle: string;
  date: string;
}

export interface ExtractedPlace extends PlaceItem {
  extractionStatus: 'success' | 'review';
  confidence: number;
}

export interface ProfileAvatar {
  id: string;
  image: number;
  label: string;
}

const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 1,
    type: 'TRIP_INVITED',
    targetType: 'INVITATION',
    targetId: 1,
    message: "'감지은'님이 '제주도 여행'에 초대했어요 ✉️",
    createdAt: '방금 전',
    read: false,
  },
  {
    id: 2,
    type: 'TRIP_REMINDER',
    targetType: 'TRIP',
    targetId: 1,
    message: "'제주도 여행' D-1입니다. 핀로그를 작성해 보세요! 🎬",
    createdAt: '2시간 전',
    read: false,
  },
  {
    id: 3,
    type: 'TRIP_PLACE_ADDED',
    targetType: 'TRIP',
    targetId: 2,
    message: '도쿄 여행에 새 장소 3개가 추가되었어요.',
    createdAt: '어제',
    read: true,
  },
];

const INITIAL_TRIP_INVITATIONS: TripInvitation[] = [
  {
    id: 1,
    inviterName: '감지은',
    inviterAvatar: '🎀',
    tripTitle: '제주도 여행',
    date: '2026.10.12 - 10.15',
  },
];

const INVITED_JEJU_TRIP: TripFolder = {
  id: 4,
  title: '제주도 여행 ✈️',
  date: '2026.10.12 - 10.15',
  dDay: 'D-28',
  image: 'https://images.unsplash.com/photo-1548115184-bc6544d06a58?auto=format&fit=crop&w=600&q=80',
  placesCount: 6,
  tag: '팀여행',
};

export const INITIAL_EXTRACTED_PLACES: ExtractedPlace[] = [
  {
    id: 101,
    name: '앤트러사이트 한림점',
    category: '제주 / 감성 베이커리 카페',
    rating: 4.9,
    reviewsCount: 512,
    address: '제주특별자치도 제주시 한림읍 한림로 564',
    thumbnail: 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=500&q=80',
    lat: 55,
    lng: 40,
    order: 1,
    day: 1,
    note: '영상 속 간판과 위치 태그가 일치해 정확도가 높아요.',
    extractionStatus: 'success',
    confidence: 98,
  },
  {
    id: 102,
    name: '협재해수욕장',
    category: '제주 / 해변 · 자연 명소',
    rating: 4.8,
    reviewsCount: 1420,
    address: '제주특별자치도 제주시 한림읍 협재리 2497-1',
    thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=500&q=80',
    lat: 30,
    lng: 26,
    order: 2,
    day: 1,
    note: '바다 풍경과 자막의 장소명이 함께 확인됐어요.',
    extractionStatus: 'success',
    confidence: 95,
  },
  {
    id: 103,
    name: '비양도 석양 포인트',
    category: '제주 / 전망대 후보',
    rating: 4.6,
    reviewsCount: 87,
    address: '제주특별자치도 제주시 한림읍 협재리 일대',
    thumbnail: 'https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?auto=format&fit=crop&w=500&q=80',
    lat: 65,
    lng: 35,
    order: 3,
    day: 1,
    note: '정확한 촬영 지점이 여러 곳으로 분석됐어요. 주소를 확인해 주세요.',
    extractionStatus: 'review',
    confidence: 72,
  },
];

export const PROFILE_AVATARS: ProfileAvatar[] = [
  { id: 'book-winter', image: avatarBookWinter, label: '책 읽는 겨울 여행가' },
  { id: 'silver-bucket', image: avatarSilverBucket, label: '은발 버킷햇 여행가' },
  { id: 'pink-camera', image: avatarPinkCamera, label: '핑크 카메라 여행가' },
  { id: 'map-phone', image: avatarMapPhone, label: '지도 보는 도시 여행가' },
  { id: 'headphone-wink', image: avatarHeadphoneWink, label: '윙크 헤드폰 여행가' },
  { id: 'sunglasses', image: avatarSunglasses, label: '선글라스 로컬 여행가' },
];

export const TRAVEL_TASTES = [
  { label: '맛집', icon: '🍜', percent: 45, color: 'bg-[#56EADD]' },
  { label: '카페', icon: '☕', percent: 25, color: 'bg-[#82F0DC]' },
  { label: '자연', icon: '🌲', percent: 20, color: 'bg-emerald-300' },
  { label: '야경', icon: '🌙', percent: 10, color: 'bg-violet-300' },
];

export { avatarSilverBucket };

/**
 * 서버 아바타 코드를 앱에 들어 있는 픽셀 에셋으로 바꾼다. 서버가 주는
 * imageUrl 은 상대 경로라 그대로는 못 쓰고, 같은 그림이 앱에도 있어서
 * 네트워크 없이 바로 그릴 수 있다. 모르는 코드면 null.
 */
const AVATAR_IMAGE_BY_SERVER_CODE: Record<string, number | undefined> = {
  F1: avatarBookWinter,
  F2: avatarSilverBucket,
  F3: avatarPinkCamera,
  M1: avatarMapPhone,
  M2: avatarHeadphoneWink,
  M3: avatarSunglasses,
};

export function avatarImageForCode(code: string | undefined | null): number | null {
  if (!code) return null;
  return AVATAR_IMAGE_BY_SERVER_CODE[code] ?? null;
}

function useAppState(me: Me, onLogout: () => void) {
  const [activeTab, setActiveTab] = useState<TabType>('home');
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [trips, setTrips] = useState<TripFolder[]>(INITIAL_TRIPS);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);
  const [tripInvitations, setTripInvitations] = useState<TripInvitation[]>(INITIAL_TRIP_INVITATIONS);
  const [focusedInvitationId, setFocusedInvitationId] = useState<number | null>(null);
  const [isTripMapOpen, setIsTripMapOpen] = useState(false);
  const [activeTrip, setActiveTrip] = useState<TripFolder>(INITIAL_TRIPS[0]);
  const [tripSearchQuery, setTripSearchQuery] = useState('');
  const [recommendationPeople, setRecommendationPeople] = useState('2');
  const [recommendationStyle, setRecommendationStyle] = useState<string>(TRAVEL_STYLES[0].code);
  const [recommendationRegion, setRecommendationRegion] = useState('제주');
  const [inviteTrip, setInviteTrip] = useState<TripFolder | null>(null);

  // 환경설정
  const [pushNotification, setPushNotification] = useState(true);
  const [marketingNotification, setMarketingNotification] = useState(false);
  const [cacheSize, setCacheSize] = useState('12.4 MB');

  // 추출 탭
  const [inputUrl, setInputUrl] = useState('https://instagram.com/p/C_jeju_cafe_art');
  const [isExtracting, setIsExtracting] = useState(false);
  const [extractedPlaces, setExtractedPlaces] = useState<ExtractedPlace[]>(INITIAL_EXTRACTED_PLACES);
  const [selectedExtractedPlaceIds, setSelectedExtractedPlaceIds] = useState<number[]>(
    INITIAL_EXTRACTED_PLACES.map((p) => p.id),
  );
  const [isTripPickerOpen, setIsTripPickerOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [selectedTripForExtract, setSelectedTripForExtract] = useState(1);

  // 여행 지도 & 팀 기록
  const [selectedMapPin, setSelectedMapPin] = useState<PlaceItem>(MAP_PLACES[0]);
  const [isPlaceDetailModalOpen, setIsPlaceDetailModalOpen] = useState(false);
  const [placeRecords, setPlaceRecords] = useState<PlaceRecord[]>(INITIAL_PLACE_RECORDS);
  const [isPinLogMakerOpen, setIsPinLogMakerOpen] = useState(false);
  // 홈 빠른 촬영: 촬영 대상 여행과 열림 여부
  const [pinLogTrip, setPinLogTrip] = useState<TripFolder>(INITIAL_TRIPS[0]);
  const [isQuickPinLogCameraOpen, setIsQuickPinLogCameraOpen] = useState(false);
  const [mapPlaces, setMapPlaces] = useState<PlaceItem[]>(MAP_PLACES);
  const [isOptimizingRoute, setIsOptimizingRoute] = useState(false);

  // 바텀시트
  const [tripsSheetHeight, setTripsSheetHeight] = useState(48);
  const [selectedDayFilter, setSelectedDayFilter] = useState<number | 'all'>('all');

  // 핀로그 & 프로필
  const [vlogClips, setVlogClips] = useState<VlogClip[]>(SAMPLE_VLOG_CLIPS);
  const [activeClipModal, setActiveClipModal] = useState<VlogClip | null>(null);
  const [selectedProfileAvatar, setSelectedProfileAvatar] = useState<ProfileAvatar>(PROFILE_AVATARS[0]);
  const [pendingProfileAvatar, setPendingProfileAvatar] = useState<ProfileAvatar>(PROFILE_AVATARS[0]);
  const [isAvatarPickerOpen, setIsAvatarPickerOpen] = useState(false);
  const [profileDetailModal, setProfileDetailModal] = useState<'visited' | 'saved' | 'regions' | null>(null);

  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const displayedMapPlaces =
    selectedDayFilter === 'all' ? mapPlaces : mapPlaces.filter((p) => p.day === selectedDayFilter);
  const filteredTrips = trips.filter((trip) =>
    trip.title.toLowerCase().includes(tripSearchQuery.trim().toLowerCase()),
  );
  const unreadNotificationCount = notifications.filter((n) => !n.read).length;

  const showToast = (msg: string) => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToastMessage(msg);
    toastTimer.current = setTimeout(() => setToastMessage(null), 2800);
  };

  // 서버 호출이 실패해도 토큰은 지워져 있으므로 로그인 화면으로 보낸다.
  const handleLogout = async () => {
    try {
      await logout();
    } catch (e) {
      showToast(errorMessage(e, '로그아웃 처리 중 문제가 있었지만 기기에서는 로그아웃했어요.'));
    }
    onLogout();
  };

  // 장소 기록 저장 (1인 1기록 규칙)
  const handleSavePlaceRecord = (record: PlaceRecord) => {
    setPlaceRecords((prev) => {
      const idx = prev.findIndex((r) => r.pinId === record.pinId && r.userId === record.userId);
      if (idx >= 0) {
        const updated = [...prev];
        updated[idx] = record;
        return updated;
      }
      return [record, ...prev];
    });
    showToast('장소 기록이 성공적으로 저장되었습니다! 📍');
  };

  /** 'D-28' → 28. 형식이 다르면 맨 뒤로 보낸다. */
  const dDayNumber = (dDay: string) => {
    const n = Number(dDay.replace(/\D/g, ''));
    return Number.isFinite(n) && dDay.trim() !== '' ? n : Number.MAX_SAFE_INTEGER;
  };

  // 홈 "핀로그 촬영": D-Day 가 가장 가까운 여행으로 빠른 촬영 화면을 연다.
  const openQuickPinLogCamera = () => {
    if (!trips.length) {
      showToast('먼저 여행을 만들어 주세요.');
      return;
    }
    const closestTrip = [...trips].sort((a, b) => dDayNumber(a.dDay) - dDayNumber(b.dDay))[0];
    setPinLogTrip(closestTrip);
    setIsQuickPinLogCameraOpen(true);
  };

  // 빠른 촬영 영상을 그 장소의 내 기록에 붙인다. 기록이 없으면 새로 만든다(1인 1기록).
  const handleQuickPinLogUpload = (place: PlaceItem, clip: QuickClip) => {
    const existing = placeRecords.find((r) => r.pinId === place.id && r.userId === CURRENT_USER.id);
    handleSavePlaceRecord({
      id: existing?.id ?? Date.now(),
      pinId: place.id,
      userId: CURRENT_USER.id,
      authorName: CURRENT_USER.name,
      authorAvatar: selectedProfileAvatar.image,
      createdAt: '방금 전',
      rating: existing?.rating ?? 5,
      memo: existing?.memo ?? '홈 빠른 촬영으로 추가한 PinLog 영상',
      photos: existing?.photos ?? [],
      videoClip: clip,
    });
    setIsQuickPinLogCameraOpen(false);
    setActiveTab('pinlog');
  };

  const handleDeletePlaceRecord = (recordId: number) => {
    setPlaceRecords((prev) => prev.filter((r) => r.id !== recordId));
    showToast('장소 기록이 삭제되었습니다.');
  };

  const handleSynthesizeComplete = (newVlog: SynthesizedVlog) => {
    const newClip: VlogClip = {
      id: newVlog.id,
      time: '18:50',
      location: '팀원 제주 완성 브이로그 🎬',
      thumbnail: newVlog.clips[0]?.thumbnail || MAP_PLACES[0].thumbnail,
      selected: true,
    };
    setVlogClips((prev) => [newClip, ...prev]);
    showToast('🎬 팀원 영상으로 만든 PinLog 브이로그가 등록되었습니다!');
  };

  // AI 추출 시뮬레이션
  const handleExtractSubmit = () => {
    if (!inputUrl) return;
    setIsExtracting(true);
    setTimeout(() => {
      setIsExtracting(false);
      const refreshed = INITIAL_EXTRACTED_PLACES.map((p) => ({ ...p, id: Date.now() + p.id }));
      setExtractedPlaces(refreshed);
      setSelectedExtractedPlaceIds(refreshed.map((p) => p.id));
      setIsTripPickerOpen(false);
      showToast(`AI가 장소 ${refreshed.length}곳을 추출했습니다!`);
    }, 2000);
  };

  const handleToggleExtractedPlace = (placeId: number) => {
    setSelectedExtractedPlaceIds((cur) =>
      cur.includes(placeId) ? cur.filter((id) => id !== placeId) : [...cur, placeId],
    );
  };

  const handleAddPlaceToTrip = (tripId: number) => {
    const targetTrip = trips.find((t) => t.id === tripId);
    const count = selectedExtractedPlaceIds.length;
    if (!count) return;
    setTrips((prev) => prev.map((t) => (t.id === tripId ? { ...t, placesCount: t.placesCount + count } : t)));
    setSelectedTripForExtract(tripId);
    setIsTripPickerOpen(false);
    showToast(`선택한 ${count}개 장소를 '${targetTrip?.title}'에 추가했어요.`);
  };

  const handleClearCache = () => {
    setCacheSize('0.0 MB');
    showToast('임시 저장 데이터 및 캐시가 깔끔하게 정리되었습니다 🧹');
  };

  const handleClearAllTrips = () => {
    setTrips([]);
    showToast('여행 목록이 모두 지워졌습니다. 첫 여행을 생성해보세요!');
  };

  const handleResetTrips = () => {
    setTrips(INITIAL_TRIPS);
    showToast('기본 샘플 여행 목록이 복원되었습니다 🎉');
  };

  const handleNotificationSelect = (notification: NotificationItem) => {
    setNotifications((cur) => cur.map((n) => (n.id === notification.id ? { ...n, read: true } : n)));

    if (notification.type === 'TRIP_INVITED' && notification.targetType === 'INVITATION') {
      setFocusedInvitationId(notification.targetId);
      setIsNotificationOpen(false);
      setIsSettingsOpen(false);
      setIsTripMapOpen(false);
      setActiveTab('trips');
      return;
    }
    if (notification.type === 'TRIP_REMINDER') {
      setIsNotificationOpen(false);
      setIsSettingsOpen(false);
      setActiveTab('pinlog');
      setIsPinLogMakerOpen(true);
      return;
    }
    if (notification.type === 'TRIP_PLACE_ADDED') {
      const target = trips.find((t) => t.id === notification.targetId);
      if (target) setActiveTrip(target);
      setIsNotificationOpen(false);
      setIsSettingsOpen(false);
      setIsTripMapOpen(true);
      setActiveTab('trips');
    }
  };

  const handleAcceptInvitation = (invitation: TripInvitation) => {
    setTripInvitations((cur) => cur.filter((i) => i.id !== invitation.id));
    setTrips((cur) => (cur.some((t) => t.id === INVITED_JEJU_TRIP.id) ? cur : [INVITED_JEJU_TRIP, ...cur]));
    setFocusedInvitationId(null);
    showToast(`'${invitation.tripTitle}' 초대를 수락했어요.`);
  };

  const handleRejectInvitation = (invitation: TripInvitation) => {
    setTripInvitations((cur) => cur.filter((i) => i.id !== invitation.id));
    setFocusedInvitationId(null);
    showToast(`'${invitation.tripTitle}' 초대를 거절했어요.`);
  };

  const handleOptimizeRoute = () => {
    if (isOptimizingRoute) return;
    setIsOptimizingRoute(true);
    showToast('AI가 이동 거리와 방문 시간을 계산하고 있어요.');

    setTimeout(() => {
      setMapPlaces((current) => {
        if (current.length < 2) return current;
        const remaining = [...current];
        const optimized = [remaining.shift()!];
        while (remaining.length > 0) {
          const last = optimized[optimized.length - 1];
          let nearestIndex = 0;
          let nearestDistance = Number.POSITIVE_INFINITY;
          remaining.forEach((candidate, index) => {
            const distance =
              Math.hypot(candidate.lat - last.lat, candidate.lng - last.lng) + Math.abs(candidate.day - last.day) * 30;
            if (distance < nearestDistance) {
              nearestDistance = distance;
              nearestIndex = index;
            }
          });
          optimized.push(remaining.splice(nearestIndex, 1)[0]);
        }
        return optimized.map((place, index) => ({ ...place, order: index + 1 }));
      });
      setIsOptimizingRoute(false);
      showToast('가장 효율적인 동선으로 장소 순서를 정리했어요.');
    }, 1400);
  };

  const handleAiTripRecommendation = () => {
    setInputUrl(
      `${recommendationPeople}명 · ${recommendationRegion} · ${travelStyleLabel(recommendationStyle)} 여행 추천`,
    );
    setActiveTab('extract');
    showToast('선택한 조건으로 AI 추천 장소를 준비했어요.');
  };

  return {
    /** 로그인한 실제 사용자. 목업이 아니다. */
    me,
    // 상태
    activeTab, setActiveTab,
    isSettingsOpen, setIsSettingsOpen,
    trips, setTrips,
    isNotificationOpen, setIsNotificationOpen,
    notifications,
    tripInvitations,
    focusedInvitationId, setFocusedInvitationId,
    isTripMapOpen, setIsTripMapOpen,
    activeTrip, setActiveTrip,
    tripSearchQuery, setTripSearchQuery,
    recommendationPeople, setRecommendationPeople,
    recommendationStyle, setRecommendationStyle,
    recommendationRegion, setRecommendationRegion,
    inviteTrip, setInviteTrip,
    pushNotification, setPushNotification,
    marketingNotification, setMarketingNotification,
    cacheSize,
    inputUrl, setInputUrl,
    isExtracting,
    extractedPlaces,
    selectedExtractedPlaceIds,
    isTripPickerOpen, setIsTripPickerOpen,
    toastMessage,
    selectedTripForExtract,
    selectedMapPin, setSelectedMapPin,
    isPlaceDetailModalOpen, setIsPlaceDetailModalOpen,
    placeRecords,
    isPinLogMakerOpen, setIsPinLogMakerOpen,
    pinLogTrip, setPinLogTrip,
    isQuickPinLogCameraOpen, setIsQuickPinLogCameraOpen,
    mapPlaces,
    isOptimizingRoute,
    tripsSheetHeight, setTripsSheetHeight,
    selectedDayFilter, setSelectedDayFilter,
    vlogClips,
    activeClipModal, setActiveClipModal,
    selectedProfileAvatar, setSelectedProfileAvatar,
    pendingProfileAvatar, setPendingProfileAvatar,
    isAvatarPickerOpen, setIsAvatarPickerOpen,
    profileDetailModal, setProfileDetailModal,
    // 파생값
    displayedMapPlaces,
    filteredTrips,
    unreadNotificationCount,
    // 핸들러
    showToast,
    handleSavePlaceRecord,
    handleDeletePlaceRecord,
    openQuickPinLogCamera,
    handleQuickPinLogUpload,
    handleSynthesizeComplete,
    handleExtractSubmit,
    handleToggleExtractedPlace,
    handleAddPlaceToTrip,
    handleClearCache,
    handleClearAllTrips,
    handleResetTrips,
    handleNotificationSelect,
    handleAcceptInvitation,
    handleRejectInvitation,
    handleOptimizeRoute,
    handleAiTripRecommendation,
    handleLogout,
  };
}

export type AppState = ReturnType<typeof useAppState>;

const AppContext = createContext<AppState | null>(null);

export function AppProvider({
  children,
  me,
  onLogout,
}: {
  children: React.ReactNode;
  me: Me;
  onLogout: () => void;
}) {
  // 웹 원본과 같은 단일 상태 구조 (나중에 Zustand 스토어로 옮기기 쉬운 형태)
  const state = useAppState(me, onLogout);
  return <AppContext.Provider value={state}>{children}</AppContext.Provider>;
}

export function useApp(): AppState {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used inside <AppProvider>');
  return ctx;
}
