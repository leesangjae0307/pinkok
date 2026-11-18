/**
 * 기록에 붙은 사진을 크게 보는 창.
 *
 * 목록에서는 80×80 으로 잘려 나와서 뭘 찍었는지 알아보기 어렵다. 눌렀을 때
 * 화면 가득 띄우고, 한 기록에 사진이 여러 장이면 옆으로 넘겨 볼 수 있게 한다.
 * 핀치로 확대·축소하고(두 번 탭하면 확대/원래 크기), 기기를 눕히면 가로로 돌아간다.
 * 창을 닫으면 앱은 다시 세로로 돌아온다.
 */
import React, { useEffect, useRef, useState } from 'react';
import { Modal, Pressable, ScrollView, Text, View, useWindowDimensions } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import ZoomableImage from '../../../components/ZoomableImage';
import { useViewerOrientation } from '../../../hooks/useViewerOrientation';

export default function PhotoViewerModal({
  photos,
  initialIndex,
  onClose,
}: {
  photos: string[];
  /** 목록에서 누른 사진. 그 장부터 보여준다. */
  initialIndex: number;
  onClose: () => void;
}) {
  useViewerOrientation();
  const insets = useSafeAreaInsets();
  const { width, height } = useWindowDimensions();
  const [index, setIndex] = useState(initialIndex);
  /** 확대 중에는 가로 넘기기를 막는다. 사진을 끄는 손짓과 겹치기 때문이다. */
  const [zoomed, setZoomed] = useState(false);
  const scrollRef = useRef<ScrollView>(null);

  // 기기를 돌리면 한 장의 폭이 바뀐다. 보던 사진이 그대로 보이게 위치를 다시 맞춘다.
  useEffect(() => {
    scrollRef.current?.scrollTo({ x: index * width, y: 0, animated: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [width]);

  if (photos.length === 0) {return null;}

  const headerHeight = insets.top + 56;
  const imageHeight = Math.max(100, height - headerHeight);

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
            className="flex-row items-center justify-between px-4 py-3"
            style={{ paddingTop: insets.top + 12 }}
          >
            <Text className="font-pixel text-[10px] text-white/70">
              {photos.length > 1 ? `${index + 1} / ${photos.length}` : '사진'}
            </Text>
            <Pressable
              onPress={onClose}
              accessibilityLabel="사진 닫기"
              hitSlop={10}
              className="h-9 w-9 items-center justify-center rounded-xl border border-white/25 bg-white/10 active:opacity-70"
            >
              <Text className="text-sm font-black text-white">✕</Text>
            </Pressable>
          </View>

          <ScrollView
            ref={scrollRef}
            horizontal
            pagingEnabled
            scrollEnabled={!zoomed}
            showsHorizontalScrollIndicator={false}
            contentOffset={{ x: initialIndex * width, y: 0 }}
            // 가로로 넘길 때마다 몇 번째인지 다시 센다. 반올림이라 중간에서 놓아도 맞는다.
            onMomentumScrollEnd={(e) => setIndex(Math.round(e.nativeEvent.contentOffset.x / width))}
            className="flex-1"
          >
            {photos.map((uri, i) => (
              <View key={`${uri}-${i}`} style={{ width, height: imageHeight }}>
                <ZoomableImage
                  uri={uri}
                  width={width}
                  height={imageHeight}
                  onTap={onClose}
                  onZoomChange={setZoomed}
                />
              </View>
            ))}
          </ScrollView>
        </View>
      </GestureHandlerRootView>
    </Modal>
  );
}
