/**
 * 기록에 붙은 사진을 크게 보는 창.
 *
 * 목록에서는 80×80 으로 잘려 나와서 뭘 찍었는지 알아보기 어렵다. 눌렀을 때
 * 화면 가득 띄우고, 한 기록에 사진이 여러 장이면 옆으로 넘겨 볼 수 있게 한다.
 */
import React, { useState } from 'react';
import {
  Image,
  Modal,
  Pressable,
  ScrollView,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

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
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  const [index, setIndex] = useState(initialIndex);

  if (photos.length === 0) return null;

  return (
    <Modal visible animationType="fade" onRequestClose={onClose} statusBarTranslucent>
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
          horizontal
          pagingEnabled
          showsHorizontalScrollIndicator={false}
          contentOffset={{ x: initialIndex * width, y: 0 }}
          // 가로로 넘길 때마다 몇 번째인지 다시 센다. 반올림이라 중간에서 놓아도 맞는다.
          onMomentumScrollEnd={(e) =>
            setIndex(Math.round(e.nativeEvent.contentOffset.x / width))
          }
          className="flex-1"
        >
          {photos.map((uri, i) => (
            <Pressable
              key={`${uri}-${i}`}
              onPress={onClose}
              accessibilityLabel="사진 닫기"
              style={{ width }}
              className="flex-1 items-center justify-center"
            >
              {/* 잘라내지 않고 전체가 보이게 한다. */}
              <Image source={{ uri }} resizeMode="contain" style={{ width, height: '100%' }} />
            </Pressable>
          ))}
        </ScrollView>
      </View>
    </Modal>
  );
}
