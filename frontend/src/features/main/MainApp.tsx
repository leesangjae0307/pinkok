/**
 * 로그인 이후 메인 앱 셸
 * 상단 토스트 · 화면 영역(알림/설정/5개 탭) · 하단 5탭 네비 · 각종 모달
 */
import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Me } from '../../api/authApi';
import { MAP_PLACES } from '../../data/mockData';
import { AnimatedLoop } from '../../components/pixel/Primitives';
import { AppProvider, useApp } from './AppContext';
import BottomNav from './components/BottomNav';
import NotificationScreen from './screens/NotificationScreen';
import SettingsScreen from './screens/SettingsScreen';
import HomeTab from './tabs/HomeTab';
import ExtractTab from './tabs/ExtractTab';
import TripsListView from './tabs/TripsListView';
import TripMapView from './tabs/TripMapView';
import PinLogTab from './tabs/PinLogTab';
import ProfileTab from './tabs/ProfileTab';
import PlaceDetailModal from './modals/PlaceDetailModal';
import PinLogSynthesisModal from './modals/PinLogSynthesisModal';
import TeamInviteModal from './modals/TeamInviteModal';
import QuickPinLogCameraModal from './modals/QuickPinLogCameraModal';
import { AvatarPickerModal, ClipPlaybackModal, ProfileDetailModal } from './modals/ProfileModals';

function Toast() {
  const insets = useSafeAreaInsets();
  const { toastMessage } = useApp();
  if (!toastMessage) return null;
  return (
    <AnimatedLoop
      kind="bounceSmall"
      pointerEvents="none"
      style={{ position: 'absolute', top: insets.top + 8, left: 16, right: 16, zIndex: 50 }}
    >
      <View className="flex-row items-center justify-between gap-2 rounded-2xl border border-[#56EADD] bg-slate-900 px-3.5 py-2.5 shadow-xl">
        <Text className="flex-1 text-xs font-medium text-[#B4FAF0]">{toastMessage}</Text>
        <Text className="font-pixel text-[10px] text-[#FFC837]">PINKOK</Text>
      </View>
    </AnimatedLoop>
  );
}

function ScreenBody() {
  const { isNotificationOpen, isSettingsOpen, activeTab, isTripMapOpen } = useApp();

  // 여행 지도는 자체 바텀시트가 스크롤을 담당하므로 ScrollView 밖에서 렌더링
  if (!isNotificationOpen && !isSettingsOpen && activeTab === 'trips' && isTripMapOpen) {
    return <TripMapView />;
  }

  const key = isNotificationOpen ? 'notification' : isSettingsOpen ? 'settings' : activeTab;

  return (
    <ScrollView key={key} className="flex-1" keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
      {isNotificationOpen ? (
        <NotificationScreen />
      ) : isSettingsOpen ? (
        <SettingsScreen />
      ) : activeTab === 'home' ? (
        <HomeTab />
      ) : activeTab === 'extract' ? (
        <ExtractTab />
      ) : activeTab === 'trips' ? (
        <TripsListView />
      ) : activeTab === 'pinlog' ? (
        <PinLogTab />
      ) : (
        <ProfileTab />
      )}
    </ScrollView>
  );
}

function GlobalModals() {
  const {
    isPlaceDetailModalOpen,
    selectedMapPin,
    placeRecords,
    setIsPlaceDetailModalOpen,
    handleSavePlaceRecord,
    handleDeletePlaceRecord,
    selectedProfileAvatar,
    setIsPinLogMakerOpen,
    isPinLogMakerOpen,
    handleSynthesizeComplete,
    inviteTrip,
    setInviteTrip,
    showToast,
    isQuickPinLogCameraOpen,
    setIsQuickPinLogCameraOpen,
    pinLogTrip,
    mapPlaces,
    handleQuickPinLogUpload,
  } = useApp();

  return (
    <>
      {isPlaceDetailModalOpen && selectedMapPin && (
        <PlaceDetailModal
          place={selectedMapPin}
          records={placeRecords}
          onClose={() => setIsPlaceDetailModalOpen(false)}
          onSaveRecord={handleSavePlaceRecord}
          onDeleteRecord={handleDeletePlaceRecord}
          currentUserAvatarUrl={selectedProfileAvatar.image}
          onOpenPinLogMaker={() => {
            setIsPlaceDetailModalOpen(false);
            setIsPinLogMakerOpen(true);
          }}
        />
      )}

      {isPinLogMakerOpen && (
        <PinLogSynthesisModal
          places={MAP_PLACES}
          records={placeRecords}
          currentUserAvatarUrl={selectedProfileAvatar.image}
          onClose={() => setIsPinLogMakerOpen(false)}
          onSynthesizeComplete={handleSynthesizeComplete}
        />
      )}

      {isQuickPinLogCameraOpen && (
        <QuickPinLogCameraModal
          trip={pinLogTrip}
          places={mapPlaces}
          onClose={() => setIsQuickPinLogCameraOpen(false)}
          onUpload={handleQuickPinLogUpload}
        />
      )}

      {inviteTrip && (
        <TeamInviteModal
          tripTitle={inviteTrip.title}
          currentAvatarUrl={selectedProfileAvatar.image}
          onClose={() => setInviteTrip(null)}
          onInvite={(nickname, loginId) => showToast(`${nickname}(@${loginId})님에게 여행 초대를 보냈어요.`)}
        />
      )}

      <AvatarPickerModal />
      <ProfileDetailModal />
      <ClipPlaybackModal />
    </>
  );
}

function Shell() {
  const insets = useSafeAreaInsets();
  const { isNotificationOpen } = useApp();

  return (
    <View className="flex-1 bg-[#B4FAF0]" style={{ paddingTop: insets.top }}>
      <View className="relative flex-1 bg-[#B4FAF0]">
        <ScreenBody />
      </View>
      {!isNotificationOpen && <BottomNav />}
      <Toast />
      <GlobalModals />
    </View>
  );
}

export default function MainApp({ me, onLogout }: { me: Me; onLogout: () => void }) {
  return (
    <AppProvider me={me} onLogout={onLogout}>
      <Shell />
    </AppProvider>
  );
}
