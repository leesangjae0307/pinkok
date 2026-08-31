import React from 'react';
import {
  GestureResponderEvent,
  Pressable,
  StyleProp,
  StyleSheet,
  View,
  ViewStyle,
} from 'react-native';
import { BORDER_WIDTH, COLORS, RADIUS, SHADOW_OFFSET } from '../../theme';

interface CardProps {
  children: React.ReactNode;
  onPress?: (e: GestureResponderEvent) => void;
  style?: StyleProp<ViewStyle>;
  shadowColor?: string;
  borderColor?: string;
  backgroundColor?: string;
}

/**
 * 홈 화면의 여행 카드, 마이페이지의 통계 카드 등에 공통으로 쓰는
 * 픽셀 스타일 카드(두꺼운 테두리 + 하드 섀도우) 컨테이너.
 */
const Card = ({
  children,
  onPress,
  style,
  shadowColor = COLORS.GRAY300,
  borderColor = COLORS.INK,
  backgroundColor = COLORS.SURFACE,
}: CardProps) => {
  const offset = SHADOW_OFFSET.md;

  const content = (
    <View style={styles.stage}>
      <View
        style={[
          StyleSheet.absoluteFillObject,
          {
            backgroundColor: shadowColor,
            borderRadius: RADIUS.lg,
            transform: [{ translateX: offset }, { translateY: offset }],
          },
        ]}
      />
      <View
        style={[
          styles.front,
          {
            backgroundColor,
            borderColor,
            borderRadius: RADIUS.lg,
          },
          style,
        ]}
      >
        {children}
      </View>
    </View>
  );

  if (onPress) {
    return (
      <Pressable onPress={onPress} style={({ pressed }) => [pressed && styles.pressed]}>
        {content}
      </Pressable>
    );
  }

  return content;
};

const styles = StyleSheet.create({
  stage: { position: 'relative' },
  front: {
    borderWidth: BORDER_WIDTH.regular,
  },
  pressed: { opacity: 0.85 },
});

export default Card;
