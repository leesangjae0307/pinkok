import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { COLORS, FONT_FAMILY, SPACING, TYPOGRAPHY } from '../../theme';
import PixelIcon, { PixelIconName } from '../ui/PixelIcon/PixelIcon';
import Button from '../common/Button';

interface EmptyStateProps {
  icon?: PixelIconName;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}

const EmptyState = ({
  icon = 'bag',
  title,
  description,
  actionLabel,
  onAction,
}: EmptyStateProps) => {
  return (
    <View style={styles.container}>
      <View style={styles.iconWrap}>
        <PixelIcon name={icon} size={40} color={COLORS.PRIMARY_DARK} />
      </View>
      <Text style={styles.title}>{title}</Text>
      {description ? <Text style={styles.description}>{description}</Text> : null}
      {actionLabel ? (
        <Button label={actionLabel} onPress={onAction} size="sm" style={styles.action} />
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: SPACING.xxl,
    paddingHorizontal: SPACING.lg,
  },
  iconWrap: {
    width: 88,
    height: 88,
    borderRadius: 24,
    backgroundColor: COLORS.PRIMARY_LIGHT,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: SPACING.md,
  },
  title: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
    textAlign: 'center',
  },
  description: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.GRAY500,
    textAlign: 'center',
  },
  action: {
    marginTop: SPACING.lg,
  },
});

export default EmptyState;
