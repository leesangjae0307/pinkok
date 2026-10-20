import './global.css';

import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StatusBar, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { Me, fetchMe } from './src/api/authApi';
import { setSessionExpiredHandler } from './src/api/axios';
import AuthScreens from './src/features/auth/AuthScreens';
import MainApp from './src/features/main/MainApp';
import { tokenStore } from './src/services/tokenStore';

function App() {
  /**
   * 로그인한 사용자. 토큰과 짝이 맞아야 하므로 로그아웃·세션만료 때 같이 비운다.
   * null 이면 로그인 화면을 보여주므로 이 값 하나가 인증 상태를 대신한다.
   */
  const [me, setMe] = useState<Me | null>(null);
  const [isBootstrapped, setIsBootstrapped] = useState(false);

  useEffect(() => {
    setSessionExpiredHandler(() => setMe(null));

    // 앱을 다시 켜면 토큰만 남아 있다. 토큰이 있으면 서버에서 사용자를 복원한다.
    tokenStore
      .load()
      .then((hasToken) => (hasToken ? fetchMe() : null))
      .then((restored) => setMe(restored))
      .catch(async () => {
        // 토큰이 만료·폐기됐으면 들고 있어 봐야 쓸 데가 없다.
        await tokenStore.clear();
        setMe(null);
      })
      .finally(() => setIsBootstrapped(true));
  }, []);

  return (
    <SafeAreaProvider>
      <StatusBar translucent backgroundColor="transparent" barStyle="dark-content" />
      {!isBootstrapped ? (
        <View className="flex-1 items-center justify-center bg-[#B4FAF0]">
          <ActivityIndicator color="#075953" />
        </View>
      ) : me ? (
        <MainApp me={me} onLogout={() => setMe(null)} />
      ) : (
        <AuthScreens onComplete={setMe} />
      )}
    </SafeAreaProvider>
  );
}

export default App;
