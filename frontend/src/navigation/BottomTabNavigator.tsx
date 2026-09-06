import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';

import { HomeScreen } from '../features/home';
import { MapScreen } from '../features/map';
import { AIScreen } from '../features/ai';
import { TripScreen } from '../features/trip';
import { MyPageScreen } from '../features/mypage';
import PixelIcon, { PixelIconName } from '../components/ui/PixelIcon/PixelIcon';
import { BORDER_WIDTH, COLORS, FONT_FAMILY, TYPOGRAPHY } from '../theme';

const Tab = createBottomTabNavigator();

const TAB_ICONS: Record<string, PixelIconName> = {
  Home: 'home',
  Map: 'pin',
  AI: 'sparkle',
  Trip: 'bag',
  MyPage: 'user',
};

// react-navigation의 tabBarIcon은 항상 렌더 시점에 함수를 새로 전달해야 하는 구조라
// react/no-unstable-nested-components 경고가 발생해요. 실제 동작에는 문제가 없어서
// 이 한 줄만 예외 처리합니다 (react-navigation 공식 예제와 동일한 패턴).
const TabIcon = ({ color, name }: { color: string; name: PixelIconName }) => (
  <PixelIcon name={name} size={20} color={color} />
);

const BottomTabNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: COLORS.PRIMARY_DARK,
        tabBarInactiveTintColor: COLORS.GRAY500,
        tabBarStyle: {
          height: 70,
          paddingBottom: 8,
          paddingTop: 8,
          backgroundColor: COLORS.SURFACE,
          borderTopWidth: BORDER_WIDTH.regular,
          borderTopColor: COLORS.INK,
        },
        tabBarLabelStyle: {
          fontFamily: FONT_FAMILY.PIXEL,
          fontSize: TYPOGRAPHY.tiny,
        },
        // eslint-disable-next-line react/no-unstable-nested-components
        tabBarIcon: ({ color }) => (
          <TabIcon color={color} name={TAB_ICONS[route.name] ?? 'home'} />
        ),
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ title: '홈' }} />

      <Tab.Screen name="Map" component={MapScreen} options={{ title: '지도' }} />

      <Tab.Screen name="AI" component={AIScreen} options={{ title: 'AI' }} />

      <Tab.Screen name="Trip" component={TripScreen} options={{ title: '일정' }} />

      <Tab.Screen name="MyPage" component={MyPageScreen} options={{ title: '마이' }} />
    </Tab.Navigator>
  );
};

export default BottomTabNavigator;
