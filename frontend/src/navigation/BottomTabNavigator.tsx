import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';

import {HomeScreen} from '../features/home';
import {MapScreen} from '../features/map';
import {AIScreen} from '../features/ai';
import {TripScreen} from '../features/trip';
import {MyPageScreen} from '../features/mypage';

const Tab = createBottomTabNavigator();

const BottomTabNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#22C55E',
        tabBarInactiveTintColor: '#9CA3AF',
        tabBarStyle: {
          height: 70,
          paddingBottom: 8,
          paddingTop: 8,
        },
        tabBarLabelStyle: {
          fontSize: 12,
          fontWeight: '600',
        },
      }}>
      <Tab.Screen
        name="Home"
        component={HomeScreen}
        options={{title: '홈'}}
      />

      <Tab.Screen
        name="Map"
        component={MapScreen}
        options={{title: '지도'}}
      />

      <Tab.Screen
        name="AI"
        component={AIScreen}
        options={{title: 'AI'}}
      />

      <Tab.Screen
        name="Trip"
        component={TripScreen}
        options={{title: '일정'}}
      />

      <Tab.Screen
        name="MyPage"
        component={MyPageScreen}
        options={{title: '마이'}}
      />
    </Tab.Navigator>
  );
};

export default BottomTabNavigator;