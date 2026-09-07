import React from 'react';
import { Image, StyleSheet } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';

import { HomeScreen } from '../features/home';
import { ExtractScreen } from '../features/extract';
import { TripScreen } from '../features/trip';
import { PinLogScreen } from '../features/pinlog';
import { MyPageScreen } from '../features/mypage';
import { BORDER_WIDTH, COLORS, FONT_FAMILY, TYPOGRAPHY } from '../theme';

const Tab = createBottomTabNavigator();

// pincok-icons 폴더에서 받은 픽셀 아트 아이콘 (이미 여러 색이 들어간 아이콘이라
// 탭 색상으로 틴트하지 않고, 원본 색 그대로 쓰고 선택 여부는 투명도로 구분해요)
const TAB_ICON_SOURCES: Record<string, ReturnType<typeof require>> = {
  Home: require('../assets/icons/tab-home.png'),
  Extract: require('../assets/icons/tab-extract.png'),
  Trips: require('../assets/icons/tab-trips.png'),
  PinLog: require('../assets/icons/tab-pinlog.png'),
  Profile: require('../assets/icons/tab-profile.png'),
};

// react-navigation의 tabBarIcon은 항상 렌더 시점에 함수를 새로 전달해야 하는 구조라
// react/no-unstable-nested-components 경고가 발생해요. 실제 동작에는 문제가 없어서
// 이 한 줄만 예외 처리합니다 (react-navigation 공식 예제와 동일한 패턴).
const TabIcon = ({ routeName, focused }: { routeName: string; focused: boolean }) => (
  <Image
    source={TAB_ICON_SOURCES[routeName] ?? TAB_ICON_SOURCES.Home}
    style={[styles.icon, { opacity: focused ? 1 : 0.45 }]}
    resizeMode="contain"
  />
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
        tabBarIcon: ({ focused }) => <TabIcon routeName={route.name} focused={focused} />,
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ title: '홈' }} />

      <Tab.Screen name="Extract" component={ExtractScreen} options={{ title: '추출' }} />

      <Tab.Screen name="Trips" component={TripScreen} options={{ title: 'Trips' }} />

      <Tab.Screen name="PinLog" component={PinLogScreen} options={{ title: 'PinLog' }} />

      <Tab.Screen name="Profile" component={MyPageScreen} options={{ title: '프로필' }} />
    </Tab.Navigator>
  );
};

const styles = StyleSheet.create({
  icon: {
    width: 26,
    height: 26,
  },
});

export default BottomTabNavigator;
