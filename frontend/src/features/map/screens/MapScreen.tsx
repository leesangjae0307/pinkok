import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Button from '../../../components/common/Button';
import PixelIcon from '../../../components/ui/PixelIcon/PixelIcon';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../../theme';

/**
 * 실제 지도 SDK(카카오맵/네이버맵 등) 연동은 다음 단계에서 진행하고,
 * 지금은 디자인 시스템에 맞춘 화면 구조만 먼저 잡아둔 상태입니다.
 * 지도가 들어갈 자리에는 안내 placeholder를 표시해요.
 */
const MapScreen = () => {
  return (
    <ScreenLayout>
      <View style={styles.header}>
        <Text style={styles.title}>제주도 여행</Text>
        <Text style={styles.subtitle}>2025.08.01 ~ 08.05</Text>
      </View>

      <View style={styles.mapPlaceholder}>
        <PixelIcon name="pin" size={44} color={COLORS.PRIMARY_DARK} />
        <Text style={styles.placeholderTitle}>지도 화면 준비 중</Text>
        <Text style={styles.placeholderDescription}>
          실제 지도 연동 전까지 보여줄{'\n'}디자인 레이아웃이에요
        </Text>
      </View>

      <Button label="여정 보기" onPress={() => {}} fullWidth />
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  header: {
    marginBottom: SPACING.lg,
  },
  title: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h2,
    color: COLORS.INK,
  },
  subtitle: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
  mapPlaceholder: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: COLORS.PRIMARY_LIGHT,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    borderStyle: 'dashed',
    borderRadius: RADIUS.lg,
    marginBottom: SPACING.lg,
  },
  placeholderTitle: {
    marginTop: SPACING.md,
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.SECONDARY,
  },
  placeholderDescription: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
    textAlign: 'center',
    lineHeight: TYPOGRAPHY.caption + 6,
  },
});

export default MapScreen;
