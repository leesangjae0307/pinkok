import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import {
  PixelExtractIcon,
  PixelHomeIcon,
  PixelPinLogIcon,
  PixelProfileIcon,
  PixelTripMapIcon,
  PixelTripsIcon,
} from '../../../components/pixel/PixelIcons';
import { TabType } from '../../../data/mockData';
import { useApp } from '../AppContext';

function NavButton({
  active,
  label,
  onPress,
  children,
}: {
  active: boolean;
  label: string;
  onPress: () => void;
  children: React.ReactNode;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="tab"
      accessibilityState={{ selected: active }}
      accessibilityLabel={label}
      className={`flex-1 items-center justify-center rounded-2xl py-1.5 ${active ? 'bg-[#B4FAF0]' : 'active:bg-slate-50'}`}
      style={active ? { transform: [{ scale: 1.05 }] } : undefined}
    >
      <View className="h-10 items-center justify-center">{children}</View>
      <Text className={`mt-0.5 text-[10.5px] font-black ${active ? 'text-[#075953]' : 'text-slate-500'}`}>{label}</Text>
    </Pressable>
  );
}

export default function BottomNav() {
  const insets = useSafeAreaInsets();
  const { activeTab, setActiveTab, isSettingsOpen, setIsSettingsOpen, isTripMapOpen, setIsTripMapOpen, setFocusedInvitationId } =
    useApp();

  const go = (tab: TabType) => {
    setActiveTab(tab);
    setIsSettingsOpen(false);
  };
  const isActive = (tab: TabType) => activeTab === tab && !isSettingsOpen;

  return (
    <View
      className="flex-row items-center justify-around border-t border-[#82F0DC] bg-white px-2 pt-2"
      style={{ paddingBottom: Math.max(insets.bottom, 8) }}
    >
      <NavButton active={isActive('home')} label="홈" onPress={() => go('home')}>
        <PixelHomeIcon size={32} active={isActive('home')} />
      </NavButton>

      <NavButton active={isActive('extract')} label="추출" onPress={() => go('extract')}>
        <PixelExtractIcon size={38} active={isActive('extract')} />
      </NavButton>

      <NavButton
        active={isActive('trips')}
        label="여행"
        onPress={() => {
          go('trips');
          setIsTripMapOpen(false);
          setFocusedInvitationId(null);
        }}
      >
        {isTripMapOpen && activeTab === 'trips' ? (
          <PixelTripMapIcon size={36} active />
        ) : (
          <PixelTripsIcon size={32} active={isActive('trips')} />
        )}
      </NavButton>

      <NavButton active={isActive('pinlog')} label="핀로그" onPress={() => go('pinlog')}>
        <PixelPinLogIcon size={32} active={isActive('pinlog')} />
      </NavButton>

      <NavButton active={activeTab === 'profile' || isSettingsOpen} label="프로필" onPress={() => go('profile')}>
        <PixelProfileIcon size={32} active={activeTab === 'profile' || isSettingsOpen} />
      </NavButton>
    </View>
  );
}
