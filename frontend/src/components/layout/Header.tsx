import React from 'react';
import {
  GestureResponderEvent,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { COLORS, FONT_FAMILY, SPACING, TYPOGRAPHY } from '../../theme';
import PixelIcon, { PixelIconName } from '../ui/PixelIcon/PixelIcon';

interface HeaderProps {
  title: string;
  onBack?: (e: GestureResponderEvent) => void;
  rightIcon?: PixelIconName;
  onRightPress?: (e: GestureResponderEvent) => void;
}

const Header = ({ title, onBack, rightIcon, onRightPress }: HeaderProps) => {
  return (
    <View style={styles.container}>
      <View style={styles.side}>
        {onBack ? (
          <Pressable onPress={onBack} hitSlop={8}>
            <PixelIcon name="back" size={20} color={COLORS.SECONDARY} />
          </Pressable>
        ) : null}
      </View>

      <Text style={styles.title} numberOfLines={1}>
        {title}
      </Text>

      <View style={[styles.side, styles.rightSide]}>
        {rightIcon ? (
          <Pressable onPress={onRightPress} hitSlop={8}>
            <PixelIcon name={rightIcon} size={20} color={COLORS.SECONDARY} />
          </Pressable>
        ) : null}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: SPACING.sm,
    marginBottom: SPACING.sm,
  },
  side: {
    width: 32,
  },
  rightSide: {
    alignItems: 'flex-end',
  },
  title: {
    flex: 1,
    textAlign: 'center',
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h2,
    color: COLORS.INK,
  },
});

export default Header;
