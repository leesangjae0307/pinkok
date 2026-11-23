/**
 * 앱 안에서 영상을 재생하는 전체화면 창.
 *
 * 외부 앱으로 넘기지 않고 react-native-video 로 바로 튼다. 재생·일시정지·이동 막대는
 * 플레이어 기본 컨트롤을 쓴다. 불러오는 동안은 로딩을, 실패하면 다시 시도를 보여준다.
 * 기기를 눕히면 가로로 돌아가고, 영상은 비율을 지킨 채 잘리지 않는다(contain).
 * 제목·닫기 줄은 영상 위에 겹쳐 띄운다. 줄이 자리를 차지하면 특히 가로에서 영상이
 * 그만큼 작아져 화면이 비어 보이기 때문이다.
 */
import React, { useState } from 'react';
import { ActivityIndicator, Modal, Pressable, Text, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Video from 'react-native-video';

import { useViewerOrientation } from '../../../hooks/useViewerOrientation';

export default function VideoPlayerModal({
  uri,
  title,
  onClose,
}: {
  uri: string;
  title?: string;
  onClose: () => void;
}) {
  useViewerOrientation();
  const insets = useSafeAreaInsets();
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  /** 다시 시도할 때 플레이어를 새로 만들기 위한 키 */
  const [attempt, setAttempt] = useState(0);

  return (
    <Modal
      visible
      animationType="fade"
      onRequestClose={onClose}
      statusBarTranslucent
      supportedOrientations={['portrait', 'landscape', 'landscape-left', 'landscape-right']}
    >
      <GestureHandlerRootView style={{ flex: 1 }}>
        <View className="flex-1 bg-black">
          <View
            className="absolute left-0 right-0 top-0 z-10 flex-row items-center justify-between py-3"
            style={{
              paddingTop: insets.top + 12,
              paddingLeft: insets.left + 16,
              paddingRight: insets.right + 16,
            }}
          >
            <Text numberOfLines={1} className="mr-3 min-w-0 flex-1 font-pixel text-[10px] text-white/70">
              {title ?? '영상'}
            </Text>
            <Pressable
              onPress={onClose}
              accessibilityLabel="영상 닫기"
              hitSlop={10}
              className="h-9 w-9 items-center justify-center rounded-xl border border-white/25 bg-white/10 active:opacity-70"
            >
              <Text className="text-sm font-black text-white">✕</Text>
            </Pressable>
          </View>

          <View className="flex-1 items-center justify-center">
            {!hasError && (
              <Video
                key={attempt}
                source={{ uri }}
                controls
                resizeMode="contain"
                style={{ width: '100%', height: '100%' }}
                onLoadStart={() => setIsLoading(true)}
                onLoad={() => setIsLoading(false)}
                onError={() => {
                  setIsLoading(false);
                  setHasError(true);
                }}
              />
            )}

            {isLoading && !hasError && (
              <View pointerEvents="none" className="absolute items-center justify-center">
                <ActivityIndicator size="large" color="#56EADD" />
              </View>
            )}

            {hasError && (
              <View className="items-center gap-3 px-8">
                <Text className="text-3xl">😵</Text>
                <Text className="text-sm font-extrabold text-white">영상을 재생하지 못했어요</Text>
                <Text className="text-center text-[11px] font-semibold text-white/60">
                  네트워크를 확인하고 다시 시도해 주세요.
                </Text>
                <Pressable
                  onPress={() => {
                    setHasError(false);
                    setIsLoading(true);
                    setAttempt((n) => n + 1);
                  }}
                  className="h-11 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD] px-6"
                >
                  <Text className="text-xs font-black text-pixel-navy">다시 시도</Text>
                </Pressable>
              </View>
            )}
          </View>
        </View>
      </GestureHandlerRootView>
    </Modal>
  );
}
