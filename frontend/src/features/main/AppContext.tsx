/**
 * 메인 앱 전역 상태 (웹 App.tsx 의 useState / 핸들러를 그대로 옮긴 것)
 * 화면(탭)들이 props 대신 useApp() 으로 꺼내 써요.
 */
import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Alert } from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';

import avatarBookWinter from '../../assets/images/avatar-book-winter.png';
import avatarSilverBucket from '../../assets/images/avatar-silver-bucket.png';
import avatarPinkCamera from '../../assets/images/avatar-pink-camera.png';
import avatarMapPhone from '../../assets/images/avatar-map-phone.png';
import avatarHeadphoneWink from '../../assets/images/avatar-headphone-wink.png';
import avatarSunglasses from '../../assets/images/avatar-sunglasses.png';
import { Me, fetchAvatars, logout } from '../../api/authApi';
import { deleteAccount, selectAvatar, updateNickname, updateProfileImage } from '../../api/userApi';
import { fileUrl, uploadFile, uploadFiles, type PickedFile } from '../../api/fileApi';
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
import {
  createItineraryDay,
  createItineraryItem,
  deleteItineraryDay,
  deleteItineraryItem,
  moveItineraryItemToDay,
  reorderItineraryItems,
  updateItineraryItemTime,
  fetchItineraryDays,
  fetchItineraryItems,
  toPlaceItems,
} from '../../api/itineraryApi';
import type { ItineraryDay } from '../../api/itineraryApi';
import type { PlaceSearchResult } from '../../api/placeApi';
import { fetchTripStyles, replaceTripStyles } from '../../api/travelStyleApi';
import { addDays } from '../../components/pixel/PixelCalendar';
import { fetchPinlogs, pinlogThumbnailUrl, pinlogVideoUrl } from '../../api/pinlogApi';
import {
  addCandidatesToTrip,
  createAiRequest,
  fetchAiRequest,
  loadPlaceCandidates,
  retryAiRequest,
  waitForAiRequest,
  type AiRequest,
  type PlaceCandidate,
} from '../../api/aiRequestApi';
import {
  acceptRecommendation,
  createRecommendation,
  fetchRecommendations,
  rejectRecommendation,
  type Recommendation,
} from '../../api/recommendationApi';
import {
  applyRouteOptimization,
  createRouteOptimization,
  savedDistanceM,
} from '../../api/routeOptimizationApi';
import {
  acceptInvitation,
  inviteToTrip,
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
import { onNotificationOpened, startPush, stopPush, type PushTarget } from '../../services/push';
import { TRAVEL_STYLES, travelStyleLabel } from '../../constants';
import {
  AvatarSource,
  PlaceItem,
  PlaceRecord,
  TabType,
  TripFolder,
  VlogClip,
} from '../../data/mockData';
import { EMPTY_PROFILE_STATS, loadProfileStats, type ProfileStats } from './profileStats';
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
  /** 이모지이거나, 앱 에셋(number) 이거나, 서버 사진 주소다. Image 로 그릴 때는 toImageSource() 를 쓴다. */
  inviterAvatar: AvatarSource;
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


export const PROFILE_AVATARS: ProfileAvatar[] = [
  { id: 'book-winter', image: avatarBookWinter, label: '책 읽는 겨울 여행가', code: 'F1' },
  { id: 'silver-bucket', image: avatarSilverBucket, label: '은발 버킷햇 여행가', code: 'F2' },
  { id: 'pink-camera', image: avatarPinkCamera, label: '핑크 카메라 여행가', code: 'F3' },
  { id: 'map-phone', image: avatarMapPhone, label: '지도 보는 도시 여행가', code: 'M1' },
  { id: 'headphone-wink', image: avatarHeadphoneWink, label: '윙크 헤드폰 여행가', code: 'M2' },
  { id: 'sunglasses', image: avatarSunglasses, label: '선글라스 로컬 여행가', code: 'M3' },
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
 * 나 아닌 사용자(초대한 사람, 팀원, 기록 작성자)의 아바타를 그릴 값으로 바꾼다.
 *
 * 서버는 '/images/avatars/f1.png' 같은 상대 경로를 주므로 그대로 Image 에 넘기면
 * 아무것도 뜨지 않는다. 앞에 BASE_URL 을 붙여야 한다.
 *
 * 파일 이름이 아바타 코드와 같으면(f1 → F1) 앱에 들어 있는 같은 그림을 쓴다.
 * 서버 파일과 바이트까지 같은 그림이라, 네트워크 없이도 보이고 내 화면과
 * 남의 화면에 같은 아바타가 뜬다.
 */
export function otherUserAvatar(
  avatarImageUrl: string | null | undefined,
  profileImageUrl: string | null | undefined,
): AvatarSource {
  // 직접 올린 사진이 있으면 그게 우선이다(아바타와 둘 중 하나만 쓰인다).
  const photo = fileUrl(profileImageUrl);
  if (photo) return photo;

  const code = avatarImageUrl?.match(/([fm][123])\.[a-z]+$/i)?.[1]?.toUpperCase();
  const local = code ? AVATAR_IMAGE_BY_SERVER_CODE[code] : undefined;
  if (local) return local;

  return fileUrl(avatarImageUrl) ?? '🙂';
}

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

/**
 * AI 후보를 추출 탭이 쓰는 ExtractedPlace 로 바꾼다.
 *
 * 여기 lat/lng 는 목록에만 쓰이고 지도에 찍히지 않아서 가운데 값으로 둔다. 지도에
 * 올라가는 건 여행에 추가한 뒤 GET /itinerary-items 로 다시 받은 핀이고, 그때
 * 좌표가 제대로 투영된다.
 */
/** 서버가 준 errorCode 를 사람이 읽을 문장으로 바꾼다. 모르는 코드는 그대로 덧붙인다. */
export function aiErrorMessage(code: string | null): string {
  switch (code) {
    case 'GEMINI_TIMEOUT':
      return 'AI 응답이 너무 오래 걸려 멈췄어요. 다시 시도해 주세요.';
    case 'SERVER_RESTARTED':
      return '처리 중에 서버가 다시 시작됐어요. 다시 시도해 주세요.';
    default:
      return code ? `추출에 실패했어요. (${code})` : '추출에 실패했어요. 다시 시도해 주세요.';
  }
}

export function toExtractedPlace(candidate: PlaceCandidate): ExtractedPlace {
  return {
    id: candidate.id,
    name: candidate.place?.name ?? candidate.rawName,
    category: candidate.categoryText ?? candidate.place?.categoryName ?? '장소',
    rating: 0,
    reviewsCount: 0,
    address: candidate.place?.roadAddress ?? candidate.rawAddress ?? '',
    thumbnail: '',
    lat: 50,
    lng: 50,
    order: candidate.displayOrder ?? 0,
    day: 0,
    note: candidate.description ?? undefined,
    // 좌표를 못 찾았거나 이름이 흐릿하면 사용자가 한 번 봐야 한다.
    extractionStatus: candidate.mapped && !candidate.needsReview ? 'success' : 'review',
    confidence: Math.round((candidate.confidence ?? 0) * 100),
  };
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
  /** 지도를 보고 있는 여행. 아직 아무것도 열지 않았으면 null 이다. */
  const [activeTrip, setActiveTrip] = useState<TripFolder | null>(null);
  const [tripSearchQuery, setTripSearchQuery] = useState('');
  const [recommendationPeople, setRecommendationPeople] = useState('2');
  const [recommendationStyle, setRecommendationStyle] = useState<string>(TRAVEL_STYLES[0].code);
  const [recommendationRegion, setRecommendationRegion] = useState('제주');
  const [inviteTrip, setInviteTrip] = useState<TripFolder | null>(null);
  /** 수정 모달을 띄울 여행. null 이면 닫혀 있다. */
  const [editingTrip, setEditingTrip] = useState<TripFolder | null>(null);
  const [isAddPlaceOpen, setIsAddPlaceOpen] = useState(false);
  const [isAddingDay, setIsAddingDay] = useState(false);

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
  const [inputUrl, setInputUrl] = useState('');
  const [isExtracting, setIsExtracting] = useState(false);
  const [extractedPlaces, setExtractedPlaces] = useState<ExtractedPlace[]>([]);
  /** 추출이 실패했을 때 보여줄 문구. 성공하면 null 로 돌아간다. */
  const [extractError, setExtractError] = useState<string | null>(null);
  /** 마지막으로 보낸 추출 요청. 실패했을 때 이것만 있으면 다시 돌릴 수 있다. */
  const [lastAiRequestId, setLastAiRequestId] = useState<number | null>(null);
  /** 맞춤 추천 결과. 아직 수락·거절하지 않은 것만 담는다. */
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [selectedExtractedPlaceIds, setSelectedExtractedPlaceIds] = useState<number[]>(
    [],
  );
  const [isTripPickerOpen, setIsTripPickerOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [selectedTripForExtract, setSelectedTripForExtract] = useState(1);
  // 첨부한 스크린샷 파일 이름. 업로드에 쓰는 원본은 extractImages 가 들고 있다.
  const [extractImageNames, setExtractImageNames] = useState<string[]>([]);
  /** 고른 스크린샷 원본. 이름만으로는 업로드할 수 없어 따로 들고 있는다. */
  const [extractImages, setExtractImages] = useState<PickedFile[]>([]);

  // 여행 지도 & 팀 기록
  const [selectedMapPin, setSelectedMapPin] = useState<PlaceItem | null>(null);
  const [isPlaceDetailModalOpen, setIsPlaceDetailModalOpen] = useState(false);
  const [startPlaceCamera, setStartPlaceCamera] = useState(false);
  const [placeRecords, setPlaceRecords] = useState<PlaceRecord[]>([]);
  const [isSavingRecord, setIsSavingRecord] = useState(false);
  const [isPinLogMakerOpen, setIsPinLogMakerOpen] = useState(false);
  // 홈 빠른 촬영: 촬영 대상 여행과 열림 여부
  const [pinLogTrip, setPinLogTrip] = useState<TripFolder | null>(null);
  const [isQuickPinLogCameraOpen, setIsQuickPinLogCameraOpen] = useState(false);
  const [mapPlaces, setMapPlaces] = useState<PlaceItem[]>([]);
  const [isMapPlacesLoading, setIsMapPlacesLoading] = useState(false);
  const [mapPlacesError, setMapPlacesError] = useState<string | null>(null);
  /** 이 여행에 실제로 있는 일자 번호 (1day, 2day ... 칩). */
  /** 일자 번호만으로는 삭제할 수 없어서(삭제는 id 로 한다) 받은 일자를 그대로 들고 있는다. */
  const [itineraryDays, setItineraryDays] = useState<ItineraryDay[]>([]);
  const tripDayNumbers = useMemo(
    () => itineraryDays.map((d) => d.dayNumber).sort((a, b) => a - b),
    [itineraryDays],
  );
  const [isOptimizingRoute, setIsOptimizingRoute] = useState(false);

  // 바텀시트
  const [tripsSheetHeight, setTripsSheetHeight] = useState(48);
  const [selectedDayFilter, setSelectedDayFilter] = useState<number | 'all'>('all');

  // 핀로그 & 프로필
  const [vlogClips, setVlogClips] = useState<VlogClip[]>([]);
  const [isVlogClipsLoading, setIsVlogClipsLoading] = useState(false);
  const [activeClipModal, setActiveClipModal] = useState<VlogClip | null>(null);
  const [selectedProfileAvatar, setSelectedProfileAvatar] = useState<ProfileAvatar>(() => profileAvatarOf(me));
  const [pendingProfileAvatar, setPendingProfileAvatar] = useState<ProfileAvatar>(() => profileAvatarOf(me));
  const [isAvatarPickerOpen, setIsAvatarPickerOpen] = useState(false);
  const [profileNickname, setProfileNickname] = useState(me.nickname);
  const [pendingProfileNickname, setPendingProfileNickname] = useState(me.nickname);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [isPinLogTripPickerOpen, setIsPinLogTripPickerOpen] = useState(false);
  const [profileDetailModal, setProfileDetailModal] = useState<'visited' | 'saved' | 'regions' | null>(null);
  /** 프로필 화면이 쓰는 집계. 여행을 하나씩 훑어 만들기 때문에 탭을 열 때만 받는다. */
  const [profileStats, setProfileStats] = useState<ProfileStats>(EMPTY_PROFILE_STATS);
  const [isProfileStatsLoading, setIsProfileStatsLoading] = useState(false);

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
            inviterAvatar: otherUserAvatar(i.inviter.avatarImageUrl, i.inviter.profileImageUrl),
            tripTitle: i.tripTitle,
            date: invitationDateLabel(i.startDate, i.endDate),
          })),
      );
    } catch {
      setTripInvitations([]);
    }
  }, []);

  /** 핀로그 탭에서 고른 여행의 완성본 목록. 합성 중인 것은 아직 재생할 수 없어 뺀다. */
  const reloadVlogClips = useCallback(async (tripId: number) => {
    setIsVlogClipsLoading(true);
    try {
      const list = await fetchPinlogs(tripId);
      setVlogClips(
        list
          .filter((p) => p.status === 'DONE')
          .map((p) => ({
            id: p.id,
            time: p.createdAt.slice(0, 10).replace(/-/g, '.'),
            location: p.title ?? 'PinLog',
            thumbnail: pinlogThumbnailUrl(p) ?? pinlogVideoUrl(p) ?? '',
            selected: false,
          })),
      );
    } catch {
      setVlogClips([]);
    } finally {
      setIsVlogClipsLoading(false);
    }
  }, []);

  useEffect(() => {
    reloadTrips();
    reloadNotifications();
    reloadInvitations();
  }, [reloadTrips, reloadNotifications, reloadInvitations]);

  /**
   * 로그인한 상태로 이 화면이 뜬 것이므로 여기서 이 기기를 푸시 대상으로 등록한다.
   * 토큰이 갱신될 때 다시 등록하는 구독은 화면을 벗어날 때 끊는다.
   */
  useEffect(() => {
    let unsubscribe: (() => void) | null = null;
    let cancelled = false;
    startPush().then((off) => {
      if (cancelled) off();
      else unsubscribe = off;
    });
    return () => {
      cancelled = true;
      unsubscribe?.();
    };
  }, []);

  /**
   * 폰 알림을 눌러 들어온 경우 그 화면으로 보낸다.
   * 알림함에서 누른 것과 가는 곳이 같아야 해서 같은 함수를 쓴다.
   */
  useEffect(
    () => onNotificationOpened((target) => openTargetRef.current(target)),
    // 구독은 한 번만 걸고, 안에서 쓰는 최신 함수는 ref 로 읽는다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  // 핀로그 탭은 고른 여행이 바뀔 때마다 그 여행의 완성본을 다시 받는다.
  useEffect(() => {
    if (pinLogTrip) reloadVlogClips(pinLogTrip.id);
    else setVlogClips([]);
  }, [pinLogTrip, reloadVlogClips]);

  /** 프로필 집계를 다시 받는다. 기록을 남기거나 여행이 늘면 숫자가 달라진다. */
  const reloadProfileStats = useCallback(async () => {
    if (trips.length === 0) {
      setProfileStats(EMPTY_PROFILE_STATS);
      return;
    }
    setIsProfileStatsLoading(true);
    try {
      setProfileStats(await loadProfileStats(trips, me.id));
    } catch {
      // 집계는 보조 정보라 실패해도 화면을 막지 않는다. 비어 있는 상태로 둔다.
      setProfileStats(EMPTY_PROFILE_STATS);
    } finally {
      setIsProfileStatsLoading(false);
    }
  }, [trips, me.id]);

  // 프로필 탭을 열 때만 모은다. 여행 수만큼 요청이 늘어서 항상 받아두지 않는다.
  useEffect(() => {
    if (activeTab === 'profile') reloadProfileStats();
  }, [activeTab, reloadProfileStats]);

  /**
   * 검색해서 고른 장소를 지금 보고 있는 여행에 핀으로 추가한다.
   *
   * 검색 결과와 서버의 PlaceInput 은 필드가 같아서 그대로 넘긴다.
   *
   * 일자 탭을 보고 있을 때 추가하면 그 일자로 바로 넣는다. 전체를 보고 있으면
   * 날짜를 정하지 않아 "미배정 핀"으로 들어간다. 추가 뒤에는 일정을 다시 받는다.
   */
  const handleAddPlace = async (place: PlaceSearchResult): Promise<boolean> => {
    if (!activeTrip) return false;
    const targetDay =
      selectedDayFilter === 'all'
        ? undefined
        : itineraryDays.find((d) => d.dayNumber === selectedDayFilter)?.id;
    try {
      await createItineraryItem({ tripId: activeTrip.id, place, dayId: targetDay });
      await loadItinerary(activeTrip.id);
      setTrips((cur) =>
        cur.map((t) => (t.id === activeTrip.id ? { ...t, placesCount: t.placesCount + 1 } : t)),
      );
      showToast(
        targetDay
          ? `'${place.name}'을(를) ${selectedDayFilter}일차에 추가했어요.`
          : `'${place.name}'을(를) 지도에 추가했어요.`,
      );
      return true;
    } catch (e) {
      showToast(errorMessage(e, '장소를 추가하지 못했어요.'));
      return false;
    }
  };

  /** 어떤 핀을 일자에 배정할지 고르는 중. null 이면 고르는 창이 닫혀 있다. */
  const [assigningPlace, setAssigningPlace] = useState<PlaceItem | null>(null);

  /** 어떤 핀의 시각을 고치는 중. null 이면 시간 선택기가 닫혀 있다. */
  const [editingTimePlace, setEditingTimePlace] = useState<PlaceItem | null>(null);

  /**
   * 핀의 여행할 시각을 저장한다. 'HH:mm' 으로 보낸다.
   *
   * 지우는 건 안 된다. 서버가 plannedArrivalTime 이 비어 있으면 "바꾸지 말라"로 읽어서,
   * 한 번 넣은 시각을 없애려면 서버 쪽 수정이 필요하다.
   */
  const handleSetPlaceTime = async (placeId: number, time: string) => {
    if (!activeTrip) return;
    setEditingTimePlace(null);
    try {
      await updateItineraryItemTime(placeId, time);
      await loadItinerary(activeTrip.id);
      showToast(`${time} 으로 정했어요.`);
    } catch (e) {
      showToast(errorMessage(e, '시각을 저장하지 못했어요.'));
    }
  };

  /** 핀을 고른 일자로 옮긴다. 옮기면 그 일자의 맨 뒤 순서가 된다. */
  const handleAssignDay = async (placeId: number, dayNumber: number) => {
    if (!activeTrip) return;
    const target = itineraryDays.find((d) => d.dayNumber === dayNumber);
    if (!target) return;
    setAssigningPlace(null);
    try {
      await moveItineraryItemToDay(placeId, target.id);
      await loadItinerary(activeTrip.id);
      showToast(`${dayNumber}일차로 옮겼어요.`);
    } catch (e) {
      showToast(errorMessage(e, '일자를 바꾸지 못했어요.'));
    }
  };

  /**
   * 일자 안에서 핀을 한 칸 위/아래로 옮긴다.
   *
   * 서버는 한 일자의 순서를 통째로 받으므로, 지금 보이는 순서에서 두 개를 바꿔
   * 전체 목록을 다시 보낸다. 미배정 핀은 순서를 매기는 일자가 없어 바꿀 수 없다.
   */
  const handleMovePlace = async (placeId: number, direction: -1 | 1) => {
    if (!activeTrip || selectedDayFilter === 'all') return;
    const day = itineraryDays.find((d) => d.dayNumber === selectedDayFilter);
    if (!day) return;

    const ordered = mapPlaces
      .filter((p) => p.day === selectedDayFilter)
      .sort((a, b) => a.order - b.order);
    const from = ordered.findIndex((p) => p.id === placeId);
    const to = from + direction;
    if (from < 0 || to < 0 || to >= ordered.length) return;

    const next = [...ordered];
    [next[from], next[to]] = [next[to], next[from]];
    try {
      await reorderItineraryItems(day.id, next.map((p) => p.id));
      await loadItinerary(activeTrip.id);
    } catch (e) {
      showToast(errorMessage(e, '순서를 바꾸지 못했어요.'));
    }
  };

  /**
   * 다음 번호의 일자를 만든다.
   *
   * 날짜는 여행 시작일 + (N-1)일로 채운다. 서버는 visitDate 없이도 받아주지만,
   * 날짜가 없으면 화면에서 "며칠인지" 알 수 없어 일자 칩이 번호뿐이 된다.
   * 여행 시작일을 모르면(상세 조회 실패) 번호만 보낸다.
   */
  const handleAddDay = async () => {
    if (!activeTrip || isAddingDay) return;
    setIsAddingDay(true);
    try {
      const nextNumber = (tripDayNumbers[tripDayNumbers.length - 1] ?? 0) + 1;
      let visitDate: string | undefined;
      try {
        const detail = await fetchTrip(activeTrip.id);
        if (detail.startDate) visitDate = addDays(detail.startDate, nextNumber - 1);
      } catch {
        // 날짜 없이도 일자는 만들 수 있다.
      }
      await createItineraryDay(activeTrip.id, nextNumber, visitDate);
      await loadItinerary(activeTrip.id);
      showToast(`${nextNumber}일차를 만들었어요.`);
    } catch (e) {
      showToast(errorMessage(e, '일자를 만들지 못했어요.'));
    } finally {
      setIsAddingDay(false);
    }
  };

  /**
   * 일자를 지운다. 그 일자에 있던 핀은 삭제되지 않고 "미배정"으로 돌아가므로
   * 전체 탭에서 계속 보인다.
   *
   * 뒤 일자의 번호는 그대로 둔다(3일차를 지워도 4일차는 4일차). 서버에 일자 번호를
   * 고치는 API 가 없어서 다시 매길 방법이 없다.
   */
  const handleDeleteDay = (dayNumber: number) => {
    if (!activeTrip) return;
    const target = itineraryDays.find((d) => d.dayNumber === dayNumber);
    if (!target) return;
    const count = mapPlaces.filter((p) => p.day === dayNumber).length;

    Alert.alert(
      `${dayNumber}일차를 지울까요?`,
      count > 0
        ? `이 일자에 있던 장소 ${count}곳은 지워지지 않고 날짜 미배정으로 돌아갑니다.`
        : '되돌릴 수 없습니다.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteItineraryDay(target.id);
              // 지운 일자를 보고 있었으면 더 볼 것이 없으므로 전체로 돌린다.
              setSelectedDayFilter((cur) => (cur === dayNumber ? 'all' : cur));
              await loadItinerary(activeTrip.id);
              showToast(`${dayNumber}일차를 지웠어요.`);
            } catch (e) {
              showToast(errorMessage(e, '일자를 지우지 못했어요.'));
            }
          },
        },
      ],
    );
  };

  /**
   * 지도에 꽂은 장소를 지운다. 서버에서 실제로 지우므로 다시 켜도 돌아오지 않는다.
   * 그 장소에 달린 팀 기록도 함께 사라지므로 개수를 세어 미리 알려준다.
   */
  const handleDeletePlace = (place: PlaceItem) => {
    if (!activeTrip) return;
    const recordCount = placeRecords.filter((r) => r.pinId === place.id).length;

    Alert.alert(
      `[${place.name}] 을(를) 지울까요?`,
      recordCount > 0
        ? `이 장소에 남긴 팀 기록 ${recordCount}개도 함께 사라집니다. 되돌릴 수 없습니다.`
        : '되돌릴 수 없습니다.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteItineraryItem(place.id);
              // 지운 핀을 보고 있었으면 상세 카드가 빈 장소를 가리키게 된다.
              setSelectedMapPin((cur) => (cur?.id === place.id ? null : cur));
              await loadItinerary(activeTrip.id);
              showToast(`[${place.name}] 을(를) 지웠어요.`);
            } catch (e) {
              showToast(errorMessage(e, '장소를 지우지 못했어요.'));
            }
          },
        },
      ],
    );
  };

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
      setItineraryDays(days);
      // 핀을 고르기 전에도 상세 카드가 하나는 떠 있어야 해서 첫 핀을 잡아둔다.
      if (places.length > 0) setSelectedMapPin(places[0]);
    } catch (e) {
      setMapPlaces([]);
      setItineraryDays([]);
      setMapPlacesError(errorMessage(e, '일정을 불러오지 못했어요.'));
    } finally {
      setIsMapPlacesLoading(false);
    }
  }, []);

  // 핀로그로 고른 여행은 지도에서 열어둔 여행과 다를 수 있다.
  // 합성 화면은 mapPlaces 를 그대로 쓰므로, 고른 여행의 일정을 여기서 받아둔다.
  useEffect(() => {
    if (pinLogTrip) loadItinerary(pinLogTrip.id);
  }, [pinLogTrip, loadItinerary]);

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
            // 표지를 바꿨으면 목록·홈 카드도 바로 새 사진으로 바뀌어야 한다.
            image: fileUrl(payload.coverImageUrl) ?? t.image,
          }
        : t;
    setTrips((cur) => cur.map(apply));
    setActiveTrip((cur) => (cur ? apply(cur) : cur));
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
    // 먼저 푸시를 끊는다. 토큰이 남아 있으면 다음에 이 폰을 쓰는 사람에게
    // 내 알림이 간다. 실패해도 로그아웃은 그대로 진행한다.
    await stopPush();
    try {
      await logout();
    } catch (e) {
      showToast(errorMessage(e, '로그아웃 처리 중 문제가 있었지만 기기에서는 로그아웃했어요.'));
    }
    onLogout();
  };

  /** 회원 탈퇴 창이 열려 있는지 */
  const [isDeleteAccountOpen, setIsDeleteAccountOpen] = useState(false);

  /**
   * 회원 탈퇴. 성공하면 null, 실패하면 창에 띄울 메시지를 돌려준다.
   *
   * 성공 뒤에는 로그아웃과 같은 길을 탄다 — onLogout 이 토큰을 비우고 로그인 화면으로
   * 돌려보낸다.
   */
  const handleDeleteAccount = async (password: string): Promise<string | null> => {
    await stopPush();
    try {
      await deleteAccount(password);
    } catch (e) {
      const status = (e as { response?: { status?: number } })?.response?.status;
      if (status === 401) return '비밀번호가 맞지 않아요.';
      return errorMessage(e, '탈퇴하지 못했어요.');
    }
    setIsDeleteAccountOpen(false);
    onLogout();
    return null;
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
    // 저장이 겹치면 그냥 버리지 말고 알려준다. 조용히 빠져나가면 사용자는 눌렀는데
    // 아무 일도 안 일어난 것으로 보인다.
    if (isSavingRecord) {
      showToast('아직 저장 중이에요. 잠시 후 다시 시도해 주세요.');
      return;
    }
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

  /**
   * 홈 "핀로그 촬영": D-Day 가 가장 가까운 여행으로 빠른 촬영 화면을 연다.
   *
   * 촬영한 영상은 장소에 붙이는 것이라 그 여행의 핀 목록이 있어야 한다. 지도를
   * 거치지 않고 홈에서 바로 들어오면 mapPlaces 가 비어 있어서 "어떤 장소의
   * 영상인가요?" 가 고를 것 없는 빈 목록이 됐다. 그래서 여기서 일정을 받아 둔다.
   */
  const openQuickPinLogCamera = () => {
    if (!trips.length) {
      showToast('먼저 여행을 만들어 주세요.');
      return;
    }
    /*
     * 찍은 영상은 "어느 장소의 기록인지" 를 정해야 올라간다. 그래서 장소가 없는
     * 여행을 열면 다 찍고 나서야 고를 것이 없어 막힌다. 날짜가 가깝더라도 장소가
     * 없는 여행은 건너뛰고, 담아둔 장소가 있는 여행 중 가장 가까운 것을 연다.
     */
    const byDDay = [...trips].sort((a, b) => dDayNumber(a.dDay) - dDayNumber(b.dDay));
    const target = byDDay.find((t) => t.placesCount > 0);
    if (!target) {
      showToast('여행에 장소를 먼저 추가해 주세요. 찍은 영상은 장소에 붙어요.');
      setActiveTab('trips');
      return;
    }
    setPinLogTrip(target);
    setIsQuickPinLogCameraOpen(true);
    loadItinerary(target.id);
  };

  // 빠른 촬영 영상을 그 장소의 내 기록에 붙인다. 기록이 없으면 새로 만든다(1인 1기록).
  const handleQuickPinLogUpload = (place: PlaceItem, clip: QuickClip) => {
    const existing = placeRecords.find((r) => r.pinId === place.id && r.userId === me.id);
    handleSavePlaceRecord({
      id: existing?.id ?? Date.now(),
      pinId: place.id,
      userId: me.id,
      authorName: me.nickname,
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

  /** 합성이 끝나면 목록 맨 앞에 끼워 넣는다. 서버에서 다시 받아도 같은 자리에 온다. */
  const handleSynthesizeComplete = (newVlog: SynthesizedVlog) => {
    const newClip: VlogClip = {
      id: newVlog.id,
      time: newVlog.createdAt,
      location: newVlog.title,
      thumbnail: newVlog.clips[0]?.thumbnail ?? '',
      selected: true,
    };
    setVlogClips((prev) => [newClip, ...prev]);
    showToast('🎬 PinLog 브이로그가 등록되었습니다!');
  };

  /**
   * AI 장소 추출. 스크린샷이 있으면 먼저 올리고 그 주소로, 없으면 링크로 요청한다.
   *
   * Gemini 호출이라 바로 끝나지 않는다. 요청은 202 로 끝나고 상태를 폴링해서
   * SUCCESS 가 되면 후보 목록을 읽는다. 좌표를 못 찾은 후보(mapped=false)는 핀으로
   * 추가할 수 없어서 기본 선택에서 빼둔다.
   */
  /** 실패한 추출 요청을 끝까지 따라가 결과를 화면에 올린다. 성공하면 true. */
  const consumeAiRequest = async (request: AiRequest): Promise<boolean> => {
    setLastAiRequestId(request.id);
    const finished = await waitForAiRequest(request.id);

    if (finished.status !== 'SUCCESS') {
      setExtractError(
        finished.status === 'FAILED'
          ? aiErrorMessage(finished.errorCode)
          : '시간이 너무 오래 걸려 멈췄어요. 다시 시도해 주세요.',
      );
      return false;
    }

    // candidatesReady 가 false 면 후보 저장이 실패한 것이라 다시 만들어야 한다.
    const candidates = await loadPlaceCandidates(finished);
    setExtractedPlaces(candidates.map(toExtractedPlace));
    setSelectedExtractedPlaceIds(candidates.filter((c) => c.mapped).map((c) => c.id));
    setIsTripPickerOpen(false);

    if (candidates.length === 0) {
      // 서버가 끝까지 돌았는데 0곳이면 AI 가 장소를 못 찾은 것이다. 실패가 아니다.
      showToast('링크에서 장소를 찾지 못했어요.');
    } else {
      showToast(`AI가 장소 ${candidates.length}곳을 추출했습니다!`);
    }
    return true;
  };

  const handleExtractSubmit = async () => {
    if (isExtracting) return;
    if (!inputUrl.trim() && extractImages.length === 0) return;
    setIsExtracting(true);
    setExtractError(null);
    setExtractedPlaces([]);
    try {
      const request =
        extractImages.length > 0
          ? await createAiRequest({
              inputType: 'IMAGE',
              imageUrls: (await uploadFiles(extractImages)).map((f) => f.url),
            })
          : await createAiRequest({ inputType: 'LINK', sourceUrl: inputUrl.trim() });

      await consumeAiRequest(request);
    } catch (e) {
      setExtractError(errorMessage(e, '추출 요청을 보내지 못했어요.'));
    } finally {
      setIsExtracting(false);
    }
  };

  /**
   * 실패한 추출을 다시 돌린다. 링크·스크린샷을 다시 넣을 필요가 없다.
   * 후보 저장만 실패한 경우는 추출을 처음부터 돌리지 않고 후보만 다시 만든다.
   */
  const handleRetryExtract = async () => {
    if (isExtracting || lastAiRequestId === null) return;
    setIsExtracting(true);
    setExtractError(null);
    try {
      const current = await fetchAiRequest(lastAiRequestId);
      if (current.status === 'SUCCESS') {
        await consumeAiRequest(current);
        return;
      }
      await consumeAiRequest(await retryAiRequest(lastAiRequestId));
    } catch (e) {
      setExtractError(errorMessage(e, '다시 시도하지 못했어요.'));
    } finally {
      setIsExtracting(false);
    }
  };

  const handleToggleExtractedPlace = (placeId: number) => {
    setSelectedExtractedPlaceIds((cur) =>
      cur.includes(placeId) ? cur.filter((id) => id !== placeId) : [...cur, placeId],
    );
  };

  /** 체크한 후보를 여행에 핀으로 추가한다. 좌표를 못 찾은 것은 서버가 건너뛴다. */
  const handleAddPlaceToTrip = async (tripId: number) => {
    const targetTrip = trips.find((t) => t.id === tripId);
    if (selectedExtractedPlaceIds.length === 0) return;
    try {
      const { addedCount, skippedCount } = await addCandidatesToTrip(tripId, selectedExtractedPlaceIds);
      setTrips((prev) => prev.map((t) => (t.id === tripId ? { ...t, placesCount: t.placesCount + addedCount } : t)));
      setSelectedTripForExtract(tripId);
      setIsTripPickerOpen(false);
      showToast(
        skippedCount > 0
          ? `${addedCount}개를 '${targetTrip?.title}'에 추가했어요. ${skippedCount}개는 지도에서 찾지 못했어요.`
          : `선택한 ${addedCount}개 장소를 '${targetTrip?.title}'에 추가했어요.`,
      );
      // 지금 보고 있는 여행이면 지도에도 바로 반영한다.
      if (activeTrip?.id === tripId) loadItinerary(tripId);
    } catch (e) {
      showToast(errorMessage(e, '장소를 추가하지 못했어요.'));
    }
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

  /**
   * 폰 알림을 눌렀을 때 그 화면으로 보낸다.
   * 서버가 알림함과 같은 targetType / targetId 를 함께 보내준다.
   */
  const openNotificationTarget = ({ targetType, targetId }: PushTarget) => {
    setIsNotificationOpen(false);
    setIsSettingsOpen(false);

    if (targetType === 'INVITATION') {
      setFocusedInvitationId(targetId);
      setIsTripMapOpen(false);
      setActiveTab('trips');
      return;
    }
    if (targetType === 'TRIP') {
      const target = trips.find((t) => t.id === targetId);
      if (target) {
        setActiveTrip(target);
        setIsTripMapOpen(true);
      }
      setActiveTab('trips');
      return;
    }
    if (targetType === 'PINLOG') {
      setIsTripMapOpen(false);
      setActiveTab('pinlog');
      return;
    }
    if (targetType === 'AI_REQUEST') {
      setIsTripMapOpen(false);
      setActiveTab('extract');
    }
  };

  // 구독은 한 번만 걸리므로, 최신 상태를 보는 함수를 ref 에 담아둔다.
  const openTargetRef = useRef(openNotificationTarget);
  openTargetRef.current = openNotificationTarget;

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

  /**
   * 팀원 초대. 닉네임이 아니라 @아이디를 보낸다 — 닉네임은 중복될 수 있어 누구인지
   * 특정할 수 없기 때문이다. 초대장이 갈 뿐이라 상대가 수락해야 팀원이 된다.
   */
  const handleInviteToTrip = async (tripId: number, nickname: string, loginId: string) => {
    try {
      await inviteToTrip(tripId, loginId);
      showToast(`${nickname}(@${loginId})님에게 여행 초대를 보냈어요.`);
    } catch (e) {
      showToast(errorMessage(e, '초대를 보내지 못했어요.'));
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

  /**
   * 동선 최적화. 서버가 제안을 만들고(일정은 아직 그대로) 반영은 따로 요청해야 한다.
   *
   * 화면에는 "최적화" 버튼 하나뿐이라 둘을 이어서 부른다. 다만 제안만 만들고 끝나면
   * 사용자 눈에는 아무 일도 안 일어난 것으로 보이기 때문이다. 반영까지 끝나면 일정을
   * 다시 받아 바뀐 순서를 보여준다.
   */
  const handleOptimizeRoute = async () => {
    if (isOptimizingRoute || !activeTrip) return;
    if (mapPlaces.length < 2) {
      showToast('최적화하려면 장소가 두 곳 이상 필요해요.');
      return;
    }
    // 최적화는 일자 배정과 순서를 다시 짠다. 직접 배정해 둔 게 있으면 먼저 묻는다.
    const assigned = mapPlaces.filter((p) => p.day > 0).length;
    if (assigned > 0) {
      const ok = await new Promise<boolean>((resolve) => {
        Alert.alert(
          '동선을 다시 짤까요?',
          `직접 일자를 정해 둔 장소 ${assigned}곳의 일자와 순서가 바뀔 수 있어요.`,
          [
            { text: '취소', style: 'cancel', onPress: () => resolve(false) },
            { text: '계속', onPress: () => resolve(true) },
          ],
          { cancelable: true, onDismiss: () => resolve(false) },
        );
      });
      if (!ok) return;
    }
    setIsOptimizingRoute(true);
    showToast('AI가 이동 거리와 방문 시간을 계산하고 있어요.');
    try {
      const proposal = await createRouteOptimization(activeTrip.id);
      await applyRouteOptimization(proposal.id);
      await loadItinerary(activeTrip.id);
      const saved = savedDistanceM(proposal);
      showToast(
        saved > 0
          ? `동선을 정리했어요. 약 ${(saved / 1000).toFixed(1)}km 짧아졌어요.`
          : '가장 효율적인 동선으로 장소 순서를 정리했어요.',
      );
    } catch (e) {
      showToast(errorMessage(e, '동선을 최적화하지 못했어요.'));
    } finally {
      setIsOptimizingRoute(false);
    }
  };

  /**
   * 맞춤 추천. 고른 조건으로 요청하고, 완성되면 추출 탭의 후보 목록으로 보여준다.
   *
   * 추천은 AI 요청과 같은 파이프라인이라 상태를 똑같이 폴링한다. 다만 결과는
   * place-candidates 가 아니라 /recommendations 에서 읽는다.
   */
  const handleAiTripRecommendation = async () => {
    if (isExtracting) return;
    const trip = activeTrip ?? trips[0];
    if (!trip) {
      showToast('먼저 여행을 만들어 주세요.');
      return;
    }
    setIsExtracting(true);
    setExtractError(null);
    setActiveTab('extract');
    setInputUrl(
      `${recommendationPeople}명 · ${recommendationRegion} · ${travelStyleLabel(recommendationStyle)} 여행 추천`,
    );
    try {
      const request = await createRecommendation({
        tripId: trip.id,
        styleCodes: [recommendationStyle],
      });
      const finished = await waitForAiRequest(request.id);
      if (finished.status !== 'SUCCESS') {
        // 추천에는 다시 시도 API 가 없다. 실패하면 처음부터 새로 요청해야 한다.
        setExtractError(
          finished.status === 'FAILED'
            ? `${aiErrorMessage(finished.errorCode)} (추천은 다시 요청해 주세요)`
            : '추천이 너무 오래 걸려 멈췄어요. 다시 요청해 주세요.',
        );
        return;
      }
      const list = await fetchRecommendations(trip.id);
      const suggested = list.filter((r) => r.status === 'SUGGESTED');
      setRecommendations(suggested);
      showToast(`조건에 맞는 장소 ${suggested.length}곳을 추천했어요.`);
    } catch (e) {
      showToast(errorMessage(e, '추천을 받지 못했어요.'));
    } finally {
      setIsExtracting(false);
    }
  };

  /** 추천 수락 — 서버가 좌표를 찾아 일정에 핀으로 넣는다. 못 찾으면 422 다. */
  const handleAcceptRecommendation = async (recommendationId: number) => {
    try {
      await acceptRecommendation(recommendationId);
      setRecommendations((cur) => cur.filter((r) => r.id !== recommendationId));
      showToast('추천 장소를 일정에 추가했어요.');
      if (activeTrip) loadItinerary(activeTrip.id);
    } catch (e) {
      showToast(errorMessage(e, '추천 장소를 추가하지 못했어요.'));
    }
  };

  const handleRejectRecommendation = async (recommendationId: number) => {
    const previous = recommendations;
    setRecommendations((cur) => cur.filter((r) => r.id !== recommendationId));
    try {
      await rejectRecommendation(recommendationId);
    } catch (e) {
      setRecommendations(previous);
      showToast(errorMessage(e, '추천을 거절하지 못했어요.'));
    }
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
    isAddPlaceOpen,
    setIsAddPlaceOpen,
    handleAddPlace,
    handleAddDay,
    handleDeleteDay,
    handleDeletePlace,
    handleAssignDay,
    handleMovePlace,
    assigningPlace, setAssigningPlace,
    editingTimePlace, setEditingTimePlace,
    handleSetPlaceTime,
    itineraryDays,
    isAddingDay,
    respondingInvitationId,
    reloadNotifications,
    reloadInvitations,
    handleMarkAllNotificationsRead,
    handleInviteToTrip,
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
    extractError,
    handleRetryExtract,
    lastAiRequestId,
    extractedPlaces,
    selectedExtractedPlaceIds,
    isTripPickerOpen, setIsTripPickerOpen,
    toastMessage,
    selectedTripForExtract, setSelectedTripForExtract,
    extractImageNames, setExtractImageNames,
    extractImages, setExtractImages,
    recommendations,
    handleAcceptRecommendation,
    handleRejectRecommendation,
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
    isVlogClipsLoading,
    reloadVlogClips,
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
    profileStats,
    isProfileStatsLoading,
    reloadProfileStats,
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
    isDeleteAccountOpen, setIsDeleteAccountOpen,
    handleDeleteAccount,
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
