import React from 'react';
import BottomTabNavigator from './BottomTabNavigator';
import AuthNavigator from './AuthNavigator';

// 나중에는 JWT 토큰으로 변경
const isLoggedIn = true;

const RootNavigator = () => {
  if (isLoggedIn) {
    return <BottomTabNavigator />;
  }

  return <AuthNavigator />;
};

export default RootNavigator;