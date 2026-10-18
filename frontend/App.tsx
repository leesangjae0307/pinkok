import './global.css';

import React, { useState } from 'react';
import { StatusBar } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import AuthScreens from './src/features/auth/AuthScreens';
import MainApp from './src/features/main/MainApp';

function App() {
  // TODO: 백엔드 연결 시 JWT 토큰 유무로 초기값 결정
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  return (
    <SafeAreaProvider>
      <StatusBar translucent backgroundColor="transparent" barStyle="dark-content" />
      {isAuthenticated ? <MainApp /> : <AuthScreens onComplete={() => setIsAuthenticated(true)} />}
    </SafeAreaProvider>
  );
}

export default App;
