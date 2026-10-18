/**
 * PINKOK 픽셀 아이콘 세트 (웹 PixelIcons.tsx → react-native-svg / Image)
 */
import React from 'react';
import { Image, ImageStyle, Pressable, StyleProp, Text, View } from 'react-native';
import Svg, { Path, Rect } from 'react-native-svg';

import logoMainPinkok from '../../assets/images/logo-main-pinkok.png';
import navHomeImg from '../../assets/images/nav-home.png';
import navExtractAiRobotImg from '../../assets/images/nav-extract-ai-robot.png';
import navTripImg from '../../assets/images/nav-trip.png';
import navTripMapPinImg from '../../assets/images/nav-trip-map-pin.png';
import navPinlogImg from '../../assets/images/nav-pinlog.png';
import navProfileImg from '../../assets/images/nav-profile.png';

interface PixelIconProps {
  size?: number;
  active?: boolean;
  style?: StyleProp<ImageStyle>;
}

/** 이미지 기반 네비/로고 아이콘 공통 */
function ImageIcon({
  source,
  size,
  active,
  activeScale = 1.1,
  inactiveOpacity = 0.85,
  style,
}: {
  source: number;
  size: number;
  active?: boolean;
  activeScale?: number;
  inactiveOpacity?: number;
  style?: StyleProp<ImageStyle>;
}) {
  return (
    <Image
      source={source}
      resizeMode="contain"
      fadeDuration={0}
      style={[
        { width: size, height: size },
        active ? { transform: [{ scale: activeScale }] } : { opacity: inactiveOpacity },
        style,
      ]}
    />
  );
}

// 1번 메인 로고: PINKOK 열기구 핀 로고
export const PixelHotAirBalloon = ({ size = 28, active, style }: PixelIconProps) => (
  <ImageIcon source={logoMainPinkok} size={size} active={active} activeScale={1.05} inactiveOpacity={1} style={style} />
);

export const PixelBellIcon = ({ size = 22 }: PixelIconProps) => (
  <Svg width={size} height={size} viewBox="0 0 20 20" fill="none">
    <Path d="M8 2h4v2h2v2h1v6h2v3H3v-3h2V6h1V4h2V2Z" fill="#0F6861" />
    <Path d="M7 6h6v6H7V6Z" fill="#56EADD" />
    <Path d="M8 16h4v2H8v-2Z" fill="#0F6861" />
    <Rect x="8" y="5" width="2" height="2" fill="#B4FAF0" />
  </Svg>
);

// 2번 네비게이션 로고: HOME
export const PixelHomeIcon = ({ size = 32, active, style }: PixelIconProps) => (
  <ImageIcon source={navHomeImg} size={size} active={active} style={style} />
);

// 3번 네비게이션 로고: 추출 (AI 로봇)
export const PixelExtractIcon = ({ size = 32, active, style }: PixelIconProps) => (
  <ImageIcon source={navExtractAiRobotImg} size={size} active={active} style={style} />
);

// 4번 네비게이션 로고: TRIP
export const PixelTripsIcon = ({ size = 32, active, style }: PixelIconProps) => (
  <ImageIcon source={navTripImg} size={size} active={active} style={style} />
);

export const PixelTripMapIcon = ({ size = 32, active, style }: PixelIconProps) => (
  <ImageIcon source={navTripMapPinImg} size={size} active={active} inactiveOpacity={0.8} style={style} />
);

// 5번 네비게이션 로고: PINLOG
export const PixelPinLogIcon = ({ size = 32, active, style }: PixelIconProps) => (
  <ImageIcon source={navPinlogImg} size={size} active={active} style={style} />
);

// 6번 네비게이션 로고: PROFILE
export const PixelProfileIcon = ({ size = 32, active, style }: PixelIconProps) => (
  <ImageIcon source={navProfileImg} size={size} active={active} style={style} />
);

// Pixel Heart (찜/좋아요)
export const PixelHeart = ({ size = 18, filled = true }: { size?: number; filled?: boolean }) => (
  <Svg width={size} height={size} viewBox="0 0 12 12" fill="none">
    <Path
      d="M2 3H4V2H6V3H8V2H10V3H11V6H10V8H8V10H6V11H5V10H3V8H1V6H0V3H1V2H2V3Z"
      fill={filled ? '#FF477E' : '#CBD5E1'}
    />
    {filled && <Rect x="2" y="3" width="2" height="2" fill="#FFA5C0" />}
  </Svg>
);

// Pixel Map Pin (라벨 포함)
export const PixelPin = ({
  size = 28,
  color = '#FF5A87',
  label,
}: {
  size?: number;
  color?: string;
  label?: string;
  active?: boolean;
}) => (
  <View className="items-center">
    {label ? (
      <View className="mb-0.5 rounded border border-[#56EADD] bg-slate-900 px-1.5 py-0.5">
        <Text className="font-pixel text-[9px] text-white" numberOfLines={1}>
          {label}
        </Text>
      </View>
    ) : null}
    <Svg width={size} height={size * 1.2} viewBox="0 0 16 20" fill="none">
      <Rect x="4" y="2" width="8" height="2" fill={color} />
      <Rect x="2" y="4" width="12" height="6" fill={color} />
      <Rect x="4" y="10" width="8" height="2" fill={color} />
      <Rect x="6" y="5" width="4" height="4" fill="#FFFFFF" />
      <Rect x="7" y="6" width="2" height="2" fill="#50E6DC" />
      <Rect x="6" y="12" width="4" height="2" fill={color} />
      <Rect x="7" y="14" width="2" height="3" fill="#1E293B" />
      <Rect x="7.5" y="17" width="1" height="2" fill="#1E293B" />
    </Svg>
  </View>
);

export const PixelSearchIcon = ({ size = 20 }: { size?: number }) => (
  <Svg width={size} height={size} viewBox="0 0 14 14" fill="none">
    <Rect x="3" y="1" width="5" height="2" fill="#1E293B" />
    <Rect x="1" y="3" width="2" height="5" fill="#1E293B" />
    <Rect x="8" y="3" width="2" height="5" fill="#1E293B" />
    <Rect x="3" y="8" width="5" height="2" fill="#1E293B" />
    <Rect x="8" y="8" width="3" height="3" fill="#FF5A87" />
    <Rect x="10" y="10" width="3" height="3" fill="#1E293B" />
  </Svg>
);

export const PixelAirplane = ({ size = 20 }: { size?: number }) => (
  <Svg width={size} height={size} viewBox="0 0 16 16" fill="none">
    <Rect x="7" y="1" width="2" height="3" fill="#38BDF8" />
    <Rect x="6" y="4" width="4" height="4" fill="#0284C7" />
    <Rect x="1" y="7" width="14" height="2" fill="#38BDF8" />
    <Rect x="4" y="9" width="8" height="2" fill="#0284C7" />
    <Rect x="6" y="11" width="4" height="2" fill="#0284C7" />
    <Rect x="5" y="13" width="6" height="2" fill="#FF5A87" />
  </Svg>
);

// 별점
export const PixelStar = ({ size = 16, color = '#FFC837' }: { size?: number; color?: string }) => (
  <Svg width={size} height={size} viewBox="0 0 12 12" fill="none">
    <Rect x="5" y="1" width="2" height="2" fill={color} />
    <Rect x="4" y="3" width="4" height="2" fill={color} />
    <Rect x="1" y="4" width="10" height="2" fill={color} />
    <Rect x="2" y="6" width="8" height="2" fill={color} />
    <Rect x="3" y="8" width="2" height="2" fill={color} />
    <Rect x="7" y="8" width="2" height="2" fill={color} />
    <Rect x="2" y="10" width="2" height="1" fill={color} />
    <Rect x="8" y="10" width="2" height="1" fill={color} />
  </Svg>
);

export const PixelSettingsIcon = ({ size = 20, color = '#1E293B' }: { size?: number; color?: string }) => (
  <Svg width={size} height={size} viewBox="0 0 16 16" fill="none">
    <Rect x="6" y="1" width="4" height="2" fill={color} />
    <Rect x="6" y="13" width="4" height="2" fill={color} />
    <Rect x="1" y="6" width="2" height="4" fill={color} />
    <Rect x="13" y="6" width="2" height="4" fill={color} />
    <Rect x="3" y="3" width="2" height="2" fill={color} />
    <Rect x="11" y="3" width="2" height="2" fill={color} />
    <Rect x="3" y="11" width="2" height="2" fill={color} />
    <Rect x="11" y="11" width="2" height="2" fill={color} />
    <Rect x="4" y="4" width="8" height="8" fill={color} />
    <Rect x="6" y="6" width="4" height="4" fill="#FFFFFF" />
  </Svg>
);

// 뒤로가기 (<)
export const PixelBackArrow = ({ size = 18, color = '#075953' }: { size?: number; color?: string }) => (
  <Svg width={size} height={size} viewBox="0 0 16 16" fill="none">
    <Rect x="8" y="2" width="2" height="2" fill={color} />
    <Rect x="6" y="4" width="2" height="2" fill={color} />
    <Rect x="4" y="6" width="2" height="2" fill={color} />
    <Rect x="2" y="8" width="2" height="2" fill={color} />
    <Rect x="4" y="10" width="2" height="2" fill={color} />
    <Rect x="6" y="12" width="2" height="2" fill={color} />
    <Rect x="8" y="14" width="2" height="2" fill={color} />
  </Svg>
);

export const PixelKakaoIcon = ({ size = 16 }: { size?: number }) => (
  <Svg width={size} height={size} viewBox="0 0 14 14" fill="none">
    <Rect x="3" y="2" width="8" height="8" fill="#FEE500" />
    <Rect x="2" y="4" width="10" height="4" fill="#FEE500" />
    <Rect x="4" y="5" width="2" height="2" fill="#000000" />
    <Rect x="8" y="5" width="2" height="2" fill="#000000" />
    <Rect x="3" y="10" width="3" height="2" fill="#FEE500" />
  </Svg>
);

export const PixelGoogleIcon = ({ size = 16 }: { size?: number }) => (
  <Svg width={size} height={size} viewBox="0 0 14 14" fill="none">
    <Rect x="3" y="2" width="8" height="2" fill="#EA4335" />
    <Rect x="2" y="4" width="2" height="6" fill="#4285F4" />
    <Rect x="10" y="4" width="2" height="6" fill="#34A853" />
    <Rect x="3" y="10" width="8" height="2" fill="#FBBC05" />
    <Rect x="6" y="6" width="5" height="2" fill="#4285F4" />
  </Svg>
);

// 레트로 콘솔 토글 스위치
export const PixelToggleSwitch = ({ checked, onChange }: { checked: boolean; onChange: () => void }) => (
  <Pressable
    onPress={onChange}
    accessibilityRole="switch"
    accessibilityState={{ checked }}
    className={`h-6 w-12 flex-row items-center rounded-md border-2 border-slate-800 p-0.5 ${
      checked ? 'bg-[#56EADD]' : 'bg-slate-300'
    }`}
  >
    <View
      className="h-4 w-4 rounded-xs border border-slate-800 bg-white"
      style={{ transform: [{ translateX: checked ? 24 : 0 }] }}
    >
      <View
        className={`mx-auto mt-1 h-1.5 w-1.5 rounded-full ${checked ? 'bg-[#075953]' : 'bg-slate-400'}`}
      />
    </View>
  </Pressable>
);
