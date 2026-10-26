/**
 * 메인 앱 전역 상태 (웹 App.tsx 의 useState / 핸들러를 그대로 옮긴 것)
 * 화면(탭)들이 props 대신 useApp() 으로 꺼내 써요.
 */
import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { Alert } from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';

import avatarBookWinter from '../../assets/images/avatar-book-winter.png';
import avatarSilverBucket from '../../assets/images/avatar-silver-bucket.png';
import avatarPinkCamera from '../../assets/images/avatar-pink-camera.png';
import avatarMapPhone from '../../assets/images/avatar-map-phone.png';
import avatarHeadphoneWink from '../../assets/images/avatar-headphone-wink.png';
import avatarSunglasses from '../../assets/images/avatar-sunglasses.png';
import { Me, fetchAvatars, logout } from '../../api/authApi';
import { selectAvatar, updateNickname, updateProfileImage } from '../../api/userApi';
import { fileUrl, uploadFile, uploadFiles } from '../../api/fileApi';
import {
  createDiary,
  deleteDiary,
  fetchTripDiaries,
  toPlaceRecord,
  updateDiary,
  type DiaryMediaPayload,
} from '../../api/diaryApi';
import {
  createTrip,
  deleteTrip,
  fetchTrip,
  fetchTrips,
  toTripFolder,
  tripDDayLabel,
  tripDateLabel,
  updateTrip,
  type TripUpdatePayload,
} from '../../api/tripApi';
import { fetchItineraryDays, fetchItineraryItems, toPlaceItems } from '../../api/itineraryApi';
import { fetchTripStyles, replaceTripStyles } from '../../api/travelStyleApi';
import {
  acceptInvitation,
  fetchMyInvitations,
  invitationDateLabel,
  rejectInvitation,
} from '../../api/invitationApi';
import {
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  relativeTime,
} from '../../api/notificationApi';
import { BASE_URL, errorMessage } from '../../api/axios';
import { TRAVEL_STYLES, travelStyleLabel } from '../../constants';
import {
  AvatarSource,
  CURRENT_USER,
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
import type { InvitedTripMember, NewTripPayload } from './screens/CreateTripScreen';

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
  /** 서버 아바타 코드. PATCH /users/me/avatar 에 보낼 id 를 GET /avatars 에서 찾을 때 쓴다. */
  code: string;
  /** 아바타 대신 올린 프로필 사진. 이게 있으면 image 보다 우선한다. */
  photoUrl?: string;
}

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
  { id: 'book-winter', image: avatarBookWinter, label: '책 읽는 겨울 여행가', code: 'F1' },
  { id: 'silver-bucket', image: avatarSilverBucket, label: '은발 버킷햇 여행가', code: 'F2' },
  { id: 'pink-camera', image: avatarPinkCamera, label: '핑크 카메라 여행가', code: 'F3' },
  { id: 'map-phone', image: avatarMapPhone, label: '지도 보는 도시 여행가', code: 'M1' },
  { id: 'headphone-wink', image: avatarHeadphoneWink, label: '윙크 헤드폰 여행가', code: 'M2' },
  { id: 'sunglasses', image: avatarSunglasses, label: '선글라스 로컬 여행가', code: 'M3' },
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

/**
 * 로그인한 사용자의 프로필 표시값. 서버는 아바타와 사진 중 하나만 들고 있다.
 * 둘 다 없으면(아직 안 골랐으면) 첫 번째 아바타로 보여준다.
 */
export function profileAvatarOf(me: Me): ProfileAvatar {
  if (me.profileImageUrl) {
    return { ...PROFILE_AVATARS[0], photoUrl: me.profileImageUrl };
  }
  return PROFILE_AVATARS.find((a) => a.code === me.avatar?.code) ?? PROFILE_AVATARS[0];
}

/**
 * 프로필을 그릴 때 쓸 값. 올린 사진이 있으면 그 주소를, 없으면 아바타 이미지를 준다.
 * Image 에는 toImageSource() 로 넘긴다.
 */
export function profileAvatarSource(avatar: ProfileAvatar): AvatarSource {
  return fileUrl(avatar.photoUrl) ?? avatar.image;
}

export function avatarImageForCode(code: string | undefined | null): number | null {
  if (!code) return null;
  return AVATAR_IMAGE_BY_SERVER_CODE[code] ?? null;
}

function useAppState(me: Me, onLogout: () => void) {
  const [activeTab, setActiveTab] = useState<TabType>('home');
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [trips, setTrips] = useState<TripFolder[]>([]);
  const [isTripsLoading, setIsTripsLoading] = useState(true);
  const [tripsError, setTripsError] = useState<string | null>(null);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [tripInvitations, setTripInvitations] = useState<TripInvitation[]>([]);
  const [focusedInvitationId, setFocusedInvitationId] = useState<number | null>(null);
  const [isTripMapOpen, setIsTripMapOpen] = useState(false);
  const [activeTrip, setActiveTrip] = useState<TripFolder>(INITIAL_TRIPS[0]);
  const [tripSearchQuery, setTripSearchQuery] = useState('');
  const [recommendationPeople, setRecommendationPeople] = useState('2');
  const [recommendationStyle, setRecommendationStyle] = useState<string>(TRAVEL_STYLES[0].code);
  const [recommendationRegion, setRecommendationRegion] = useState('제주');
  const [inviteTrip, setInviteTrip] = useState<TripFolder | null>(null);
  /** 수정 모달을 띄울 여행. null 이면 닫혀 있다. */
  const [editingTrip, setEditingTrip] = useState<TripFolder | null>(null);

  // 새 여행 만들기
  const [isCreateTripOpen, setIsCreateTripOpen] = useState(false);
  const [isCreateTripInviteOpen, setIsCreateTripInviteOpen] = useState(false);
  const [newTripMembers, setNewTripMembers] = useState<InvitedTripMember[]>([]);
  const [isCreatingTrip, setIsCreatingTrip] = useState(false);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isUploadingProfilePhoto, setIsUploadingProfilePhoto] = useState(false);
  /** 수락·거절 응답을 기다리는 초대. 두 번 눌리지 않게 한다. */
  const [respondingInvitationId, setRespondingInvitationId] = useState<number | null>(null);
  const [deletingTripId, setDeletingTripId] = useState<number | null>(null);

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
  // 첨부한 스크린샷 파일명 (최대 10장). TODO(서버 연동): POST /files 로 올린 뒤 받은 id 로 교체.
  const [extractImageNames, setExtractImageNames] = useState<string[]>([]);

  // 여행 지도 & 팀 기록
  const [selectedMapPin, setSelectedMapPin] = useState<PlaceItem>(MAP_PLACES[0]);
  const [isPlaceDetailModalOpen, setIsPlaceDetailModalOpen] = useState(false);
  const [startPlaceCamera, setStartPlaceCamera] = useState(false);
  const [placeRecords, setPlaceRecords] = useState<PlaceRecord[]>([]);
  const [isSavingRecord, setIsSavingRecord] = useState(false);
  const [isPinLogMakerOpen, setIsPinLogMakerOpen] = useState(false);
  // 홈 빠른 촬영: 촬영 대상 여행과 열림 여부
  const [pinLogTrip, setPinLogTrip] = useState<TripFolder>(INITIAL_TRIPS[0]);
  const [isQuickPinLogCameraOpen, setIsQuickPinLogCameraOpen] = useState(false);
  const [mapPlaces, setMapPlaces] = useState<PlaceItem[]>([]);
  const [isMapPlacesLoading, setIsMapPlacesLoading] = useState(false);
  const [mapPlacesError, setMapPlacesError] = useState<string | null>(null);
  /** 이 여행에 실제로 있는 일자 번호 (1day, 2day ... 칩). */
  const [tripDayNumbers, setTripDayNumbers] = useState<number[]>([]);
  const [isOptimizingRoute, setIsOptimizingRoute] = useState(false);

  // 바텀시트
  const [tripsSheetHeight, setTripsSheetHeight] = useState(48);
  const [selectedDayFilter, setSelectedDayFilter] = useState<number | 'all'>('all');

  // 핀로그 & 프로필
  const [vlogClips, setVlogClips] = useState<VlogClip[]>(SAMPLE_VLOG_CLIPS);
  const [activeClipModal, setActiveClipModal] = useState<VlogClip | null>(null);
  const [selectedProfileAvatar, setSelectedProfileAvatar] = useState<ProfileAvatar>(() => profileAvatarOf(me));
  const [pendingProfileAvatar, setPendingProfileAvatar] = useState<ProfileAvatar>(() => profileAvatarOf(me));
  const [isAvatarPickerOpen, setIsAvatarPickerOpen] = useState(false);
  const [profileNickname, setProfileNickname] = useState(me.nickname);
  const [pendingProfileNickname, setPendingProfileNickname] = useState(me.nickname);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [isPinLogTripPickerOpen, setIsPinLogTripPickerOpen] = useState(false);
  const [profileDetailModal, setProfileDetailModal] = useState<'visited' | 'saved' | 'regions' | null>(null);

  const openProfileEditor = () => {
    setPendingProfileAvatar(selectedProfileAvatar);
    setPendingProfileNickname(profileNickname);
    setIsAvatarPickerOpen(true);
  };

  /**
   * 닉네임·아바타·프로필 사진을 저장한다.
   *
   * 서버는 아바타와 프로필 사진을 둘 중 하나만 들고 있어서, 사진을 고르면 아바타가
   * 해제되고 아바타를 고르면 사진이 지워진다. 그래서 둘 중 바뀐 쪽만 보낸다.
   */
  const saveProfile = async () => {
    const nickname = pendingProfileNickname.trim();
    if (!nickname || isSavingProfile) {
      return;
    }
    setIsSavingProfile(true);
    try {
      if (nickname !== profileNickname) {
        await updateNickname(nickname);
      }
      if (pendingProfileAvatar.photoUrl) {
        if (pendingProfileAvatar.photoUrl !== selectedProfileAvatar.photoUrl) {
          await updateProfileImage(pendingProfileAvatar.photoUrl);
        }
      } else if (pendingProfileAvatar.code !== selectedProfileAvatar.code || selectedProfileAvatar.photoUrl) {
        const avatars = await fetchAvatars();
        const match = avatars.find((a) => a.code === pendingProfileAvatar.code);
        if (match) await selectAvatar(match.id);
      }
      setSelectedProfileAvatar(pendingProfileAvatar);
      setProfileNickname(nickname);
      setIsAvatarPickerOpen(false);
      showToast('프로필을 변경했어요.');
    } catch (e) {
      showToast(errorMessage(e, '프로필을 저장하지 못했어요.'));
    } finally {
      setIsSavingProfile(false);
    }
  };

  /** 갤러리에서 고른 사진을 올려 프로필 사진 후보로 잡아둔다. 저장은 saveProfile 이 한다. */
  const pickProfilePhoto = async () => {
    const res = await launchImageLibrary({ mediaType: 'photo', selectionLimit: 1 });
    const asset = res.assets?.[0];
    if (res.didCancel || !asset?.uri) return;
    setIsUploadingProfilePhoto(true);
    try {
      const uploaded = await uploadFile({ uri: asset.uri, fileName: asset.fileName, type: asset.type });
      setPendingProfileAvatar((cur) => ({ ...cur, photoUrl: uploaded.url }));
    } catch (e) {
      showToast(errorMessage(e, '사진을 올리지 못했어요.'));
    } finally {
      setIsUploadingProfilePhoto(false);
    }
  };

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

  /** GET /trips. 실패하면 목록 자리에 다시 시도할 수 있는 안내를 띄운다. */
  const reloadTrips = useCallback(async () => {
    setIsTripsLoading(true);
    setTripsError(null);
    try {
      const list = await fetchTrips();
      setTrips(list.map((trip) => toTripFolder(trip)));
    } catch (e) {
      setTripsError(errorMessage(e, '여행 목록을 불러오지 못했어요.'));
    } finally {
      setIsTripsLoading(false);
    }
  }, []);

  /**
   * GET /notifications. 서버의 title·body 를 한 줄 메시지로 합친다.
   * 알림은 실패해도 화면 한 구석일 뿐이라 비워 두고 넘어간다.
   */
  const reloadNotifications = useCallback(async () => {
    try {
      const list = await fetchNotifications();
      setNotifications(
        list.map((n) => ({
          id: n.id,
          type: n.type as NotificationType,
          targetType: n.targetType as NotificationItem['targetType'],
          targetId: n.targetId ?? 0,
          message: n.body ? `${n.title} ${n.body}` : n.title,
          createdAt: relativeTime(n.createdAt),
          read: n.read,
        })),
      );
    } catch {
      setNotifications([]);
    }
  }, []);

  /** GET /trip-invitations 중 아직 답하지 않은 것만 "받은 초대"에 띄운다. */
  const reloadInvitations = useCallback(async () => {
    try {
      const list = await fetchMyInvitations();
      setTripInvitations(
        list
          .filter((i) => i.status === 'PENDING')
          .map((i) => ({
            id: i.id,
            inviterName: i.inviter.nickname,
            inviterAvatar: i.inviter.avatarImageUrl ?? i.inviter.profileImageUrl ?? '🎀',
            tripTitle: i.tripTitle,
            date: invitationDateLabel(i.startDate, i.endDate),
          })),
      );
    } catch {
      setTripInvitations([]);
    }
  }, []);

  useEffect(() => {
    reloadTrips();
    reloadNotifications();
    reloadInvitations();
  }, [reloadTrips, reloadNotifications, reloadInvitations]);

  /** 여행의 일자와 핀을 받아 지도·바텀시트에 올린다. */
  const loadItinerary = useCallback(async (tripId: number) => {
    setIsMapPlacesLoading(true);
    setMapPlacesError(null);
    try {
      const [days, items] = await Promise.all([
        fetchItineraryDays(tripId),
        fetchItineraryItems(tripId),
      ]);
      const places = toPlaceItems(items);
      setMapPlaces(places);
      // 기록은 핀과 같이 보여주므로 함께 받아둔다. 실패해도 지도는 그대로 쓸 수 있다.
      fetchTripDiaries(tripId)
        .then((diaries) => setPlaceRecords(diaries.map(toPlaceRecord)))
        .catch(() => setPlaceRecords([]));
      setTripDayNumbers(days.map((d) => d.dayNumber).sort((a, b) => a - b));
      // 핀을 고르기 전에도 상세 카드가 하나는 떠 있어야 해서 첫 핀을 잡아둔다.
      if (places.length > 0) setSelectedMapPin(places[0]);
    } catch (e) {
      setMapPlaces([]);
      setTripDayNumbers([]);
      setMapPlacesError(errorMessage(e, '일정을 불러오지 못했어요.'));
    } finally {
      setIsMapPlacesLoading(false);
    }
  }, []);

  /**
   * 목록의 여행을 연다. 카드가 들고 있는 값으로 먼저 열어 두고,
   * GET /trips/{id} 로 받은 최신 값(장소 개수 등)으로 덮어쓴다.
   * 지도에 띄울 일정은 여행마다 다르므로 열 때마다 새로 받는다.
   */
  const openTrip = async (trip: TripFolder) => {
    setActiveTrip(trip);
    setIsTripMapOpen(true);
    setSelectedDayFilter('all');
    loadItinerary(trip.id);
    try {
      // 목록(GET /trips)에는 테마가 없어서 여행을 열 때 따로 받아 카드에 반영한다.
      const [detail, styles] = await Promise.all([
        fetchTrip(trip.id),
        fetchTripStyles(trip.id).catch(() => []),
      ]);
      const fresh = toTripFolder(detail, {
        image: trip.image,
        tag: styles[0] ? travelStyleLabel(styles[0].code) : trip.tag,
      });
      setActiveTrip(fresh);
      setTrips((cur) => cur.map((t) => (t.id === fresh.id ? fresh : t)));
    } catch {
      // 상세를 못 받아와도 목록에서 온 값으로 화면은 그대로 쓸 수 있다.
    }
  };

  /**
   * 수정이 끝난 뒤 목록·지도에 반영한다.
   *
   * 서버를 다시 부르지 않고 바꾼 값만 덮어쓴다. 제목·날짜만 고쳤는데 장소 개수나
   * 표지까지 서버 기준으로 되돌아가면 사용자에겐 엉뚱한 변화로 보인다.
   */
  const handleTripUpdated = (tripId: number, payload: TripUpdatePayload) => {
    const apply = (t: TripFolder): TripFolder =>
      t.id === tripId
        ? {
            ...t,
            title: payload.title ?? t.title,
            date: tripDateLabel(payload.startDate ?? null, payload.endDate ?? null),
            dDay: tripDDayLabel(payload.startDate ?? null),
          }
        : t;
    setTrips((cur) => cur.map(apply));
    setActiveTrip((cur) => apply(cur));
    showToast('여행 정보를 수정했어요.');
  };

  /**
   * DELETE /trips/{id}. 되돌릴 수 없으므로 먼저 확인을 받는다.
   * 주인만 지울 수 있어서 카드에서도 OWNER 에게만 버튼을 보여준다.
   */
  const handleDeleteTrip = (trip: TripFolder) => {
    Alert.alert('여행 삭제', `'${trip.title}' 여행을 삭제할까요?\n되돌릴 수 없어요.`, [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: async () => {
          setDeletingTripId(trip.id);
          try {
            await deleteTrip(trip.id);
            setTrips((cur) => cur.filter((t) => t.id !== trip.id));
            // 지운 여행의 지도를 보고 있었다면 목록으로 돌려보낸다.
            if (activeTrip?.id === trip.id) setIsTripMapOpen(false);
            showToast('여행을 삭제했어요.');
          } catch (e) {
            showToast(errorMessage(e, '여행을 삭제하지 못했어요.'));
          } finally {
            setDeletingTripId(null);
          }
        },
      },
    ]);
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
  /**
   * 장소 기록을 저장한다. 사진·영상은 먼저 POST /files 로 올리고 받은 주소를 함께 보낸다.
   *
   * 서버는 핀 하나에 사람당 기록 하나만 두므로, 이미 내 기록이 있으면 PATCH 로 고친다.
   * 화면이 들고 있던 기록은 로컬 파일 주소라, 저장 뒤에는 서버가 돌려준 값으로 바꿔
   * 끼운다. 그래야 다시 열었을 때와 같은 것을 보게 된다.
   */
  const handleSavePlaceRecord = async (record: PlaceRecord) => {
    if (isSavingRecord) return;
    setIsSavingRecord(true);
    try {
      const media = await uploadRecordMedia(record);
      const existing = placeRecords.find((r) => r.pinId === record.pinId && r.userId === me.id);
      const saved = existing
        ? await updateDiary(existing.id, { content: record.memo, rating: record.rating, media })
        : await createDiary({
            itineraryItemId: record.pinId,
            content: record.memo,
            rating: record.rating,
            media,
          });
      const mapped = toPlaceRecord(saved);
      setPlaceRecords((prev) => {
        const idx = prev.findIndex((r) => r.id === mapped.id);
        if (idx >= 0) {
          const updated = [...prev];
          updated[idx] = mapped;
          return updated;
        }
        return [mapped, ...prev];
      });
      showToast('장소 기록이 성공적으로 저장되었습니다! 📍');
    } catch (e) {
      showToast(errorMessage(e, '기록을 저장하지 못했어요.'));
    } finally {
      setIsSavingRecord(false);
    }
  };

  /**
   * 기록에 붙은 사진·영상 중 아직 안 올린 것만 올린다.
   *
   * 이미 서버에 있는 파일은 주소가 BASE_URL 로 시작하므로 그대로 다시 보내고,
   * 갤러리·카메라에서 막 고른 것(file://, content://)만 업로드한다. 고칠 때마다
   * 같은 사진을 다시 올리면 서버에 똑같은 파일이 쌓인다.
   */
  const uploadRecordMedia = async (record: PlaceRecord): Promise<DiaryMediaPayload[]> => {
    const isUploaded = (uri: string) => uri.startsWith(BASE_URL);
    const toServerPath = (uri: string) => uri.slice(BASE_URL.length);

    const newPhotos = record.photos.filter((uri) => !isUploaded(uri));
    const uploaded = newPhotos.length > 0 ? await uploadFiles(newPhotos.map((uri) => ({ uri }))) : [];
    let next = 0;
    const photos: DiaryMediaPayload[] = record.photos.map((uri) =>
      isUploaded(uri) ? { fileUrl: toServerPath(uri) } : { fileUrl: uploaded[next++].url },
    );

    const clip = record.videoClip;
    if (!clip?.videoUrl) return photos;
    if (isUploaded(clip.videoUrl)) {
      return [...photos, { fileUrl: toServerPath(clip.videoUrl), durationMs: Math.round(clip.duration * 1000) }];
    }
    const video = await uploadFile({ uri: clip.videoUrl, type: 'video/mp4' });
    return [...photos, { fileUrl: video.url, durationMs: video.durationMs ?? Math.round(clip.duration * 1000) }];
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

  const closeCreateTrip = () => {
    setIsCreateTripOpen(false);
    setNewTripMembers([]);
  };

  const addNewTripMember = (member: InvitedTripMember) =>
    setNewTripMembers((cur) => (cur.some((m) => m.loginId === member.loginId) ? cur : [...cur, member]));

  const removeNewTripMember = (loginId: string) =>
    setNewTripMembers((cur) => cur.filter((m) => m.loginId !== loginId));

  /**
   * POST /trips. 초대한 사람들에게는 초대장이 갈 뿐이라 만든 직후에는 아직 나 혼자다.
   *
   * 표지는 POST /trips 에 실을 자리가 없어서 만든 뒤 PATCH 로 붙인다. 붙이기에 실패해도
   * 여행 자체는 만들어졌으므로 기본 표지로 두고 넘어간다.
   *
   * 태그는 여전히 서버에 보낼 자리가 없어 화면에만 얹어 둔다.
   */
  const handleCreateTrip = async (payload: NewTripPayload) => {
    if (isCreatingTrip) return;
    setIsCreatingTrip(true);
    try {
      let created = await createTrip({
        title: `${payload.title} ${payload.emoji}`.trim(),
        region: payload.location || undefined,
        startDate: payload.startDate,
        endDate: payload.endDate,
        inviteeUsernames: newTripMembers.map((m) => m.loginId),
      });
      if (payload.coverImageUrl) {
        try {
          created = await updateTrip(created.id, { coverImageUrl: payload.coverImageUrl });
        } catch {
          showToast('표지는 저장하지 못했어요. 기본 표지로 만들었어요.');
        }
      }
      if (payload.styleCodes.length > 0) {
        try {
          await replaceTripStyles(created.id, payload.styleCodes);
        } catch {
          showToast('테마는 저장하지 못했어요. 나중에 다시 골라 주세요.');
        }
      }
      const createdTrip = toTripFolder(created, {
        image: payload.coverImage,
        tag: travelStyleLabel(payload.styleCodes[0] ?? ''),
      });
      setTrips((cur) => [createdTrip, ...cur]);
      setActiveTrip(createdTrip);
      closeCreateTrip();
      showToast(`'${payload.title}' 여행을 만들었어요.`);
    } catch (e) {
      showToast(errorMessage(e, '여행을 만들지 못했어요.'));
    } finally {
      setIsCreatingTrip(false);
    }
  };

  /** 붙어 있던 사진·영상 파일도 서버에서 같이 지워진다. */
  const handleDeletePlaceRecord = async (recordId: number) => {
    const previous = placeRecords;
    setPlaceRecords((prev) => prev.filter((r) => r.id !== recordId));
    try {
      await deleteDiary(recordId);
      showToast('장소 기록이 삭제되었습니다.');
    } catch (e) {
      setPlaceRecords(previous);
      showToast(errorMessage(e, '기록을 삭제하지 못했어요.'));
    }
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
    if (!inputUrl && extractImageNames.length === 0) {
      return;
    }
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

  // 목록은 이제 서버가 갖고 있으므로 샘플을 되돌리는 대신 다시 받아온다.
  const handleResetTrips = async () => {
    await reloadTrips();
    showToast('여행 목록을 다시 불러왔어요.');
  };

  const handleMarkAllNotificationsRead = async () => {
    const previous = notifications;
    setNotifications((cur) => cur.map((n) => ({ ...n, read: true })));
    try {
      await markAllNotificationsRead();
    } catch (e) {
      setNotifications(previous);
      showToast(errorMessage(e, '읽음 처리를 하지 못했어요.'));
    }
  };

  const handleNotificationSelect = (notification: NotificationItem) => {
    setNotifications((cur) => cur.map((n) => (n.id === notification.id ? { ...n, read: true } : n)));
    // 읽음 표시가 실패해도 화면 이동까지 막을 이유는 없다. 다음 조회 때 다시 안 읽음으로 보일 뿐이다.
    if (!notification.read) markNotificationRead(notification.id).catch(() => {});

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

  /** 수락하면 그 여행의 팀원이 되므로 목록을 다시 받아와야 새 여행이 보인다. */
  const handleAcceptInvitation = async (invitation: TripInvitation) => {
    if (respondingInvitationId !== null) return;
    setRespondingInvitationId(invitation.id);
    try {
      await acceptInvitation(invitation.id);
      setTripInvitations((cur) => cur.filter((i) => i.id !== invitation.id));
      setFocusedInvitationId(null);
      showToast(`'${invitation.tripTitle}' 초대를 수락했어요.`);
      await reloadTrips();
    } catch (e) {
      showToast(errorMessage(e, '초대를 수락하지 못했어요.'));
    } finally {
      setRespondingInvitationId(null);
    }
  };

  const handleRejectInvitation = async (invitation: TripInvitation) => {
    if (respondingInvitationId !== null) return;
    setRespondingInvitationId(invitation.id);
    try {
      await rejectInvitation(invitation.id);
      setTripInvitations((cur) => cur.filter((i) => i.id !== invitation.id));
      setFocusedInvitationId(null);
      showToast(`'${invitation.tripTitle}' 초대를 거절했어요.`);
    } catch (e) {
      showToast(errorMessage(e, '초대를 거절하지 못했어요.'));
    } finally {
      setRespondingInvitationId(null);
    }
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
    isTripsLoading,
    tripsError,
    reloadTrips,
    openTrip,
    isCreatingTrip,
    handleDeleteTrip,
    deletingTripId,
    editingTrip,
    setEditingTrip,
    handleTripUpdated,
    respondingInvitationId,
    reloadNotifications,
    reloadInvitations,
    handleMarkAllNotificationsRead,
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
    isCreateTripOpen, setIsCreateTripOpen,
    isCreateTripInviteOpen, setIsCreateTripInviteOpen,
    newTripMembers,
    pushNotification, setPushNotification,
    marketingNotification, setMarketingNotification,
    cacheSize,
    inputUrl, setInputUrl,
    isExtracting,
    extractedPlaces,
    selectedExtractedPlaceIds,
    isTripPickerOpen, setIsTripPickerOpen,
    toastMessage,
    selectedTripForExtract, setSelectedTripForExtract,
    extractImageNames, setExtractImageNames,
    selectedMapPin, setSelectedMapPin,
    isPlaceDetailModalOpen, setIsPlaceDetailModalOpen,
    startPlaceCamera, setStartPlaceCamera,
    placeRecords,
    isPinLogMakerOpen, setIsPinLogMakerOpen,
    pinLogTrip, setPinLogTrip,
    isQuickPinLogCameraOpen, setIsQuickPinLogCameraOpen,
    mapPlaces,
    isMapPlacesLoading,
    mapPlacesError,
    tripDayNumbers,
    reloadItinerary: () => activeTrip && loadItinerary(activeTrip.id),
    isOptimizingRoute,
    tripsSheetHeight, setTripsSheetHeight,
    selectedDayFilter, setSelectedDayFilter,
    vlogClips,
    activeClipModal, setActiveClipModal,
    selectedProfileAvatar, setSelectedProfileAvatar,
    pendingProfileAvatar, setPendingProfileAvatar,
    isAvatarPickerOpen, setIsAvatarPickerOpen,
    profileNickname,
    pendingProfileNickname, setPendingProfileNickname,
    isPasswordModalOpen, setIsPasswordModalOpen,
    isPinLogTripPickerOpen, setIsPinLogTripPickerOpen,
    openProfileEditor,
    saveProfile,
    isSavingProfile,
    pickProfilePhoto,
    isUploadingProfilePhoto,
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
    closeCreateTrip,
    addNewTripMember,
    removeNewTripMember,
    handleCreateTrip,
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
