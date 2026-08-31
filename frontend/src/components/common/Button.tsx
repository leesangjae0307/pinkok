import React from 'react';
import {
  GestureResponderEvent,
  Pressable,
  StyleProp,
  StyleSheet,
  Text,
  View,
  ViewStyle,
} from 'react-native';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SHADOW_OFFSET,
  SPACING,
  TYPOGRAPHY,
} from '../../theme';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps {
  label: string;
  onPress?: (e: GestureResponderEvent) => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  disabled?: boolean;
  fullWidth?: boolean;
  icon?: React.ReactNode;
  style?: StyleProp<ViewStyle>;
}

const VARIANT_COLORS: Record<
  ButtonVariant,
  { bg: string; border: string; shadow: string; text: string }
> = {
  primary: {
    bg: COLORS.PRIMARY,
    border: COLORS.INK,
    shadow: COLORS.PRIMARY_DARK,
    text: COLORS.WHITE,
  },
  secondary: {
    bg: COLORS.SECONDARY,
    border: COLORS.INK,
    shadow: COLORS.SECONDARY_DARK,
    text: COLORS.WHITE,
  },
  outline: {
    bg: COLORS.WHITE,
    border: COLORS.INK,
    shadow: COLORS.GRAY300,
    text: COLORS.INK,
  },
  ghost: {
    bg: 'transparent',
    border: 'transparent',
    shadow: 'transparent',
    text: COLORS.SECONDARY,
  },
};

const SIZE_STYLES: Record<
  ButtonSize,
  { paddingV: number; paddingH: number; fontSize: number; radius: number }
> = {
  sm: { paddingV: SPACING.xs + 2, paddingH: SPACING.md, fontSize: TYPOGRAPHY.caption, radius: RADIUS.sm },
  md: { paddingV: SPACING.sm + 2, paddingH: SPACING.lg, fontSize: TYPOGRAPHY.body, radius: RADIUS.md },
  lg: { paddingV: SPACING.md, paddingH: SPACING.xl, fontSize: TYPOGRAPHY.h3, radius: RADIUS.md },
};

/**
 * 픽셀 게임풍 "눌리는" 버튼.
 * 두꺼운 잉크색 테두리 + 우하단으로 오프셋된 진한 색 그림자 블록을 쌓고,
 * 눌렀을 때 버튼 본체를 그림자 쪽으로 이동시켜 눌림 효과를 냅니다.
 */
const Button = ({
  label,
  onPress,
  variant = 'primary',
  size = 'md',
  disabled = false,
  fullWidth = false,
  icon,
  style,
}: ButtonProps) => {
  const colors = VARIANT_COLORS[variant];
  const sizing = SIZE_STYLES[size];
  const offset = SHADOW_OFFSET.sm;
  const hasShadow = variant !== 'ghost';

  return (
    <View style={[fullWidth && styles.fullWidth, style]}>
      <View style={styles.stage}>
        {hasShadow && (
          <View
            style={[
              StyleSheet.absoluteFillObject,
              {
                backgroundColor: colors.shadow,
                borderRadius: sizing.radius,
                transform: [{ translateX: offset }, { translateY: offset }],
              },
            ]}
          />
        )}
        <Pressable
          onPress={onPress}
          disabled={disabled}
          hitSlop={8}
          style={({ pressed }) => [
            {
              backgroundColor: colors.bg,
              borderColor: colors.border,
              borderWidth: hasShadow ? BORDER_WIDTH.regular : 0,
              borderRadius: sizing.radius,
              paddingVertical: sizing.paddingV,
              paddingHorizontal: sizing.paddingH,
              opacity: disabled ? 0.5 : 1,
              transform:
                hasShadow && pressed
                  ? [{ translateX: offset }, { translateY: offset }]
                  : [{ translateX: 0 }, { translateY: 0 }],
            },
          ]}
        >
          <View style={styles.content}>
            {icon}
            <Text
              style={{
                fontFamily: FONT_FAMILY.PIXEL_BOLD,
                fontSize: sizing.fontSize,
                color: colors.text,
                marginLeft: icon ? SPACING.xs : 0,
              }}
            >
              {label}
            </Text>
          </View>
        </Pressable>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  fullWidth: { width: '100%' },
  stage: { position: 'relative' },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default Button;
