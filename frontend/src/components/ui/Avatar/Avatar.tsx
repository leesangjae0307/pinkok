import React from 'react';
import { Image, StyleProp, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { BORDER_WIDTH, COLORS, FONT_FAMILY } from '../../../theme';

interface AvatarProps {
  uri?: string;
  name?: string;
  size?: number;
  style?: StyleProp<ViewStyle>;
}

/**
 * 두꺼운 잉크색 테두리 링을 두른 픽셀풍 아바타.
 * 이미지가 없으면 이름 첫 글자를 보여줍니다.
 */
const Avatar = ({ uri, name, size = 56, style }: AvatarProps) => {
  const initial = name?.trim().charAt(0) ?? '?';

  return (
    <View
      style={[
        styles.wrapper,
        { width: size, height: size, borderRadius: size / 4 },
        style,
      ]}
    >
      {uri ? (
        <Image
          source={{ uri }}
          style={{ width: '100%', height: '100%', borderRadius: size / 4 - BORDER_WIDTH.thick }}
        />
      ) : (
        <Text style={[styles.initial, { fontSize: size * 0.4 }]}>{initial}</Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    backgroundColor: COLORS.PRIMARY_LIGHT,
    borderWidth: BORDER_WIDTH.thick,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  initial: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    color: COLORS.SECONDARY,
  },
});

export default Avatar;
