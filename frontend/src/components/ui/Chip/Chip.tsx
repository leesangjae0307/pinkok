import React from 'react';
import {
  GestureResponderEvent,
  Pressable,
  StyleProp,
  StyleSheet,
  Text,
  ViewStyle,
} from 'react-native';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../../theme';

interface ChipProps {
  label: string;
  selected?: boolean;
  onPress?: (e: GestureResponderEvent) => void;
  style?: StyleProp<ViewStyle>;
}

/**
 * 선택 가능한 필터/옵션 칩 (예: 컴포넌트 목업의 "기본 / 선택 / 비활성" 버튼).
 */
const Chip = ({ label, selected = false, onPress, style }: ChipProps) => {
  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.chip,
        {
          backgroundColor: selected ? COLORS.PRIMARY : COLORS.SURFACE,
          borderColor: selected ? COLORS.INK : COLORS.GRAY300,
        },
        style,
      ]}
    >
      <Text style={[styles.text, { color: selected ? COLORS.WHITE : COLORS.SECONDARY }]}>
        {label}
      </Text>
    </Pressable>
  );
};

const styles = StyleSheet.create({
  chip: {
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.xs + 2,
    borderRadius: RADIUS.md,
    borderWidth: BORDER_WIDTH.regular,
  },
  text: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.caption,
  },
});

export default Chip;
