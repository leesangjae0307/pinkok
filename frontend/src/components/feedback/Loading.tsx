import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { COLORS, FONT_FAMILY, SPACING, TYPOGRAPHY } from '../../theme';

interface LoadingProps {
  label?: string;
}

const Loading = ({ label = '불러오는 중...' }: LoadingProps) => {
  return (
    <View style={styles.container}>
      <ActivityIndicator color={COLORS.PRIMARY} size="large" />
      <Text style={styles.label}>{label}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: SPACING.xxl,
  },
  label: {
    marginTop: SPACING.sm,
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
});

export default Loading;
