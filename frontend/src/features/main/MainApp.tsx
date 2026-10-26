/**
 * 로그인 이후 메인 앱 셸
 * 상단 토스트 · 화면 영역(알림/설정/5개 탭) · 하단 5탭 네비 · 각종 모달
 */
import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Me } from '../../api/authApi';
import { AnimatedLoop, PixelPressable } from '../../components/pixel/Primitives';
import { AppProvider, profileAvatarSource, useApp } from './AppContext';
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
import EditTripModal from './modals/EditTripModal';
import QuickPinLogCameraModal from './modals/QuickPinLogCameraModal';
import CreateTripScreen from './screens/CreateTripScreen';
import PinLogTripPickerModal from './modals/PinLogTripPickerModal';
import { AvatarPickerModal, ClipPlaybackModal, PasswordModal, ProfileDetailModal } from './modals/ProfileModals';

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
  const {
    me,
    isNotificationOpen,
    isSettingsOpen,
    activeTab,
    isTripMapOpen,
    isCreateTripOpen,
    selectedProfileAvatar,
    newTripMembers,
    closeCreateTrip,
    setIsCreateTripInviteOpen,
    removeNewTripMember,
    handleCreateTrip,
    isCreatingTrip,
  } = useApp();

  // 새 여행 만들기는 탭 위를 덮는 전체 화면 (웹과 같이 하단 탭바도 숨김)
  if (isCreateTripOpen) {
    return (
      <CreateTripScreen
        hostName={me.nickname}
        hostAvatar={profileAvatarSource(selectedProfileAvatar)}
        invitedMembers={newTripMembers}
        onBack={closeCreateTrip}
        onOpenInvite={() => setIsCreateTripInviteOpen(true)}
        onRemoveMember={removeNewTripMember}
        onCreate={handleCreateTrip}
        isCreating={isCreatingTrip}
      />
    );
  }

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
    me,
    isPlaceDetailModalOpen,
    selectedMapPin,
    placeRecords,
    setIsPlaceDetailModalOpen,
    startPlaceCamera,
    setStartPlaceCamera,
    handleSavePlaceRecord,
    handleDeletePlaceRecord,
    selectedProfileAvatar,
    setIsPinLogMakerOpen,
    isPinLogMakerOpen,
    isPinLogTripPickerOpen,
    setIsPinLogTripPickerOpen,
    trips,
    handleSynthesizeComplete,
    inviteTrip,
    editingTrip,
    setEditingTrip,
    handleTripUpdated,
    setInviteTrip,
    showToast,
    isQuickPinLogCameraOpen,
    setIsQuickPinLogCameraOpen,
    pinLogTrip,
    setPinLogTrip,
    mapPlaces,
    handleQuickPinLogUpload,
    isCreateTripInviteOpen,
    setIsCreateTripInviteOpen,
    addNewTripMember,
  } = useApp();

  return (
    <>
      {isPlaceDetailModalOpen && selectedMapPin && (
        <PlaceDetailModal
          place={selectedMapPin}
          records={placeRecords}
          onClose={() => {
            setIsPlaceDetailModalOpen(false);
            setStartPlaceCamera(false);
          }}
          startInCamera={startPlaceCamera}
          onSaveRecord={handleSavePlaceRecord}
          onDeleteRecord={handleDeletePlaceRecord}
          currentUserAvatarUrl={profileAvatarSource(selectedProfileAvatar)}
          currentUserId={me.id}
          currentUserName={me.nickname}
          onOpenPinLogMaker={() => {
            setIsPlaceDetailModalOpen(false);
            setIsPinLogMakerOpen(true);
          }}
        />
      )}

      {isPinLogTripPickerOpen && (
        <PinLogTripPickerModal
          trips={trips}
          onClose={() => setIsPinLogTripPickerOpen(false)}
          onSelect={(trip) => {
            setPinLogTrip(trip);
            setIsPinLogTripPickerOpen(false);
            setIsPinLogMakerOpen(true);
          }}
        />
      )}

      {isPinLogMakerOpen && pinLogTrip && (
        <PinLogSynthesisModal
          places={mapPlaces}
          records={placeRecords}
          currentUserAvatarUrl={profileAvatarSource(selectedProfileAvatar)}
          currentUserId={me.id}
          tripTitle={pinLogTrip.title}
          tripId={pinLogTrip.id}
          onClose={() => setIsPinLogMakerOpen(false)}
          onSynthesizeComplete={handleSynthesizeComplete}
        />
      )}

      {isQuickPinLogCameraOpen && pinLogTrip && (
        <QuickPinLogCameraModal
          trip={pinLogTrip}
          places={mapPlaces}
          onClose={() => setIsQuickPinLogCameraOpen(false)}
          onUpload={handleQuickPinLogUpload}
        />
      )}

      {editingTrip && (
        <EditTripModal
          trip={editingTrip}
          onClose={() => setEditingTrip(null)}
          onSaved={(payload) => handleTripUpdated(editingTrip.id, payload)}
        />
      )}

      {inviteTrip && (
        <TeamInviteModal
          tripTitle={inviteTrip.title}
          currentAvatarUrl={profileAvatarSource(selectedProfileAvatar)}
          currentNickname={me.nickname}
          showTeamManagement
          onClose={() => setInviteTrip(null)}
          onInvite={(nickname, loginId) => showToast(`${nickname}(@${loginId})님에게 여행 초대를 보냈어요.`)}
        />
      )}

      {isCreateTripInviteOpen && (
        <TeamInviteModal
          tripTitle="새 여행"
          currentAvatarUrl={profileAvatarSource(selectedProfileAvatar)}
          onClose={() => setIsCreateTripInviteOpen(false)}
          onInvite={(nickname, loginId, avatar) => addNewTripMember({ nickname, loginId, avatar })}
        />
      )}

      <AvatarPickerModal />
      <PasswordModal />
      <ProfileDetailModal />
      <ClipPlaybackModal />
    </>
  );
}

/** 여행 목록 오른쪽 아래에 떠 있는 "새 여행 만들기" 버튼 (웹의 sticky + 버튼) */
function CreateTripFab() {
  const { activeTab, isTripMapOpen, isSettingsOpen, isNotificationOpen, isCreateTripOpen, setIsCreateTripOpen } =
    useApp();
  if (activeTab !== 'trips' || isTripMapOpen || isSettingsOpen || isNotificationOpen || isCreateTripOpen) {
    return null;
  }
  return (
    <View pointerEvents="box-none" className="absolute bottom-3 right-4">
      <PixelPressable
        shadowColor="#075953"
        offset={4}
        radius={16}
        accessibilityLabel="새 여행 만들기"
        className="h-14 w-14 items-center justify-center rounded-2xl border-3 border-pixel-navy bg-[#56EADD]"
        onPress={() => setIsCreateTripOpen(true)}
      >
        <Text className="font-pixel text-3xl text-pixel-navy">+</Text>
      </PixelPressable>
    </View>
  );
}

function Shell() {
  const insets = useSafeAreaInsets();
  const { isNotificationOpen, isCreateTripOpen } = useApp();

  return (
    <View className="flex-1 bg-[#B4FAF0]" style={{ paddingTop: insets.top }}>
      <View className="relative flex-1 bg-[#B4FAF0]">
        <ScreenBody />
        <CreateTripFab />
      </View>
      {!isNotificationOpen && !isCreateTripOpen && <BottomNav />}
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
