import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { COLORS, FONT_FAMILY, SPACING, TYPOGRAPHY } from '../../theme';
import Button from '../common/Button';

interface ErrorViewProps {
  message?: string;
  onRetry?: () => void;
}

const ErrorView = ({
  message = '문제가 발생했어요. 잠시 후 다시 시도해주세요.',
  onRetry,
}: ErrorViewProps) => {
  return (
    <View style={styles.container}>
      <Text style={styles.emoji}>⚠️</Text>
      <Text style={styles.message}>{message}</Text>
      {onRetry ? (
        <Button label="다시 시도" onPress={onRetry} size="sm" variant="outline" style={styles.action} />
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
  emoji: {
    fontSize: 32,
    marginBottom: SPACING.sm,
  },
  message: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.SECONDARY,
    textAlign: 'center',
  },
  action: {
    marginTop: SPACING.lg,
  },
});

export default ErrorView;
