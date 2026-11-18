/**
 * 핀치로 확대·축소하고, 확대한 상태에서 끌어 이동하고, 두 번 탭하면 확대/원래 크기를 오가는 사진.
 * 원래 크기(1배)에서는 이동 제스처를 꺼서 바깥의 가로 넘기기(ScrollView)를 막지 않는다.
 */
import React, { useState } from 'react';
import { Image } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, { runOnJS, useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';

const MAX_SCALE = 5;
const DOUBLE_TAP_SCALE = 2.5;

export default function ZoomableImage({
  uri,
  width,
  height,
  onTap,
  onZoomChange,
}: {
  uri: string;
  width: number;
  height: number;
  /** 확대하지 않은 상태에서 한 번 탭했을 때 */
  onTap?: () => void;
  onZoomChange?: (zoomed: boolean) => void;
}) {
  const [zoomed, setZoomed] = useState(false);
  const scale = useSharedValue(1);
  const savedScale = useSharedValue(1);
  const tx = useSharedValue(0);
  const ty = useSharedValue(0);
  const savedTx = useSharedValue(0);
  const savedTy = useSharedValue(0);

  const report = (value: boolean) => {
    setZoomed(value);
    onZoomChange?.(value);
  };

  const reset = () => {
    'worklet';
    scale.value = withTiming(1);
    savedScale.value = 1;
    tx.value = withTiming(0);
    ty.value = withTiming(0);
    savedTx.value = 0;
    savedTy.value = 0;
    runOnJS(report)(false);
  };

  const pinch = Gesture.Pinch()
    .onUpdate((e) => {
      scale.value = Math.min(MAX_SCALE, Math.max(0.8, savedScale.value * e.scale));
    })
    .onEnd(() => {
      if (scale.value <= 1.02) {
        reset();
      } else {
        savedScale.value = scale.value;
        runOnJS(report)(true);
      }
    });

  const pan = Gesture.Pan()
    .enabled(zoomed)
    .onUpdate((e) => {
      // 확대한 만큼만 화면 밖으로 밀 수 있다.
      const maxX = ((scale.value - 1) * width) / 2;
      const maxY = ((scale.value - 1) * height) / 2;
      tx.value = Math.min(maxX, Math.max(-maxX, savedTx.value + e.translationX));
      ty.value = Math.min(maxY, Math.max(-maxY, savedTy.value + e.translationY));
    })
    .onEnd(() => {
      savedTx.value = tx.value;
      savedTy.value = ty.value;
    });

  const doubleTap = Gesture.Tap()
    .numberOfTaps(2)
    .onEnd(() => {
      if (scale.value > 1.02) {
        reset();
      } else {
        scale.value = withTiming(DOUBLE_TAP_SCALE);
        savedScale.value = DOUBLE_TAP_SCALE;
        runOnJS(report)(true);
      }
    });

  const singleTap = Gesture.Tap().onEnd(() => {
    if (scale.value <= 1.02 && onTap) {runOnJS(onTap)();}
  });

  const composed = Gesture.Race(
    Gesture.Exclusive(doubleTap, singleTap),
    Gesture.Simultaneous(pinch, pan),
  );

  const style = useAnimatedStyle(() => ({
    transform: [{ translateX: tx.value }, { translateY: ty.value }, { scale: scale.value }],
  }));

  return (
    <GestureDetector gesture={composed}>
      <Animated.View style={[{ width, height }, style]}>
        {/* 잘라내지 않고 비율을 지킨 채 전체가 보이게 한다. */}
        <Image source={{ uri }} resizeMode="contain" style={{ width, height }} />
      </Animated.View>
    </GestureDetector>
  );
}
