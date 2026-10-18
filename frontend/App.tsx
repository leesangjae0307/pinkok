import './global.css';

import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StatusBar, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { setSessionExpiredHandler } from './src/api/axios';
import AuthScreens from './src/features/auth/AuthScreens';
import MainApp from './src/features/main/MainApp';
import { tokenStore } from './src/services/tokenStore';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isTokenLoaded, setIsTokenLoaded] = useState(false);

  useEffect(() => {
    setSessionExpiredHandler(() => setIsAuthenticated(false));
    tokenStore.load().then((hasToken) => {
      setIsAuthenticated(hasToken);
      setIsTokenLoaded(true);
    });
  }, []);

  return (
    <SafeAreaProvider>
      <StatusBar translucent backgroundColor="transparent" barStyle="dark-content" />
      {!isTokenLoaded ? (
        <View className="flex-1 items-center justify-center bg-[#B4FAF0]">
          <ActivityIndicator color="#075953" />
        </View>
      ) : isAuthenticated ? (
        <MainApp onLogout={() => setIsAuthenticated(false)} />
      ) : (
        <AuthScreens onComplete={() => setIsAuthenticated(true)} />
      )}
    </SafeAreaProvider>
  );
}

export default App;
