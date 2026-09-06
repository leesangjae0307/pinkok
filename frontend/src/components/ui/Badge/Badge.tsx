import React from 'react';
import { StyleProp, StyleSheet, Text, View, ViewStyle } from 'react-native';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../../theme';

type BadgeVariant = 'solid' | 'outline' | 'soft';

interface BadgeProps {
  label: string;
  variant?: BadgeVariant;
  color?: string;
  icon?: React.ReactNode;
  style?: StyleProp<ViewStyle>;
}

/**
 * "5 days", "#바다" 같은 작은 태그/배지.
 */
const Badge = ({ label, variant = 'soft', color = COLORS.PRIMARY, icon, style }: BadgeProps) => {
  const bg =
    variant === 'solid' ? color : variant === 'soft' ? COLORS.PRIMARY_LIGHT : 'transparent';
  const border = variant === 'outline' ? color : 'transparent';
  const text = variant === 'solid' ? COLORS.WHITE : COLORS.SECONDARY;

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: bg,
          borderColor: border,
          borderWidth: variant === 'outline' ? BORDER_WIDTH.thin : 0,
        },
        style,
      ]}
    >
      {icon}
      <Text style={[styles.text, { color: text, marginLeft: icon ? SPACING.xs : 0 }]}>
        {label}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    paddingHorizontal: SPACING.sm,
    paddingVertical: 4,
    borderRadius: RADIUS.pill,
  },
  text: {
    fontFamily: FONT_FAMILY.PIXEL_SMALL,
    fontSize: TYPOGRAPHY.tiny,
  },
});

export default Badge;
