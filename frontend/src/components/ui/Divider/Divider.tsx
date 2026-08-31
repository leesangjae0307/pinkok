import React from 'react';
import { StyleProp, View, ViewStyle } from 'react-native';
import { COLORS, SPACING } from '../../../theme';

interface DividerProps {
  style?: StyleProp<ViewStyle>;
  color?: string;
  dashed?: boolean;
}

/**
 * 얇은 실선, 또는 픽셀 느낌의 점선 구분선.
 */
const Divider = ({ style, color = COLORS.GRAY300, dashed = false }: DividerProps) => {
  if (!dashed) {
    return (
      <View
        style={[{ height: 2, backgroundColor: color, marginVertical: SPACING.md }, style]}
      />
    );
  }

  const dashCount = 24;

  return (
    <View
      style={[
        { flexDirection: 'row', marginVertical: SPACING.md },
        style,
      ]}
    >
      {Array.from({ length: dashCount }).map((_, i) => (
        <View
          key={i}
          style={{
            flex: 1,
            height: 2,
            marginRight: i === dashCount - 1 ? 0 : 4,
            backgroundColor: color,
          }}
        />
      ))}
    </View>
  );
};

export default Divider;
