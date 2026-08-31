import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Input from '../../../components/common/Input';
import Button from '../../../components/common/Button';
import PixelIcon from '../../../components/ui/PixelIcon/PixelIcon';
import { COLORS, FONT_FAMILY, RADIUS, SPACING, TYPOGRAPHY } from '../../../theme';

const AIScreen = () => {
  const [question, setQuestion] = useState('');

  return (
    <ScreenLayout>
      <View style={styles.header}>
        <View style={styles.badge}>
          <PixelIcon name="sparkle" size={24} color={COLORS.SECONDARY} />
        </View>
        <Text style={styles.title}>AI 여행 추천</Text>
        <Text style={styles.description}>
          가고 싶은 여행 스타일을 알려주면{'\n'}핀콕 AI가 일정을 추천해드려요
        </Text>
      </View>

      <View style={styles.inputRow}>
        <Input
          value={question}
          onChangeText={setQuestion}
          placeholder="예) 3박 4일 제주도 힐링 여행"
          containerStyle={styles.input}
        />
      </View>
      <Button label="추천받기" onPress={() => {}} fullWidth />
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  header: {
    alignItems: 'center',
    marginTop: SPACING.xxl,
    marginBottom: SPACING.xl,
  },
  badge: {
    width: 72,
    height: 72,
    borderRadius: RADIUS.lg,
    backgroundColor: COLORS.PRIMARY_LIGHT,
    borderWidth: 3,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: SPACING.md,
  },
  title: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h2,
    color: COLORS.INK,
  },
  description: {
    marginTop: SPACING.sm,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
    textAlign: 'center',
    lineHeight: TYPOGRAPHY.caption + 6,
  },
  inputRow: {
    marginBottom: SPACING.md,
  },
  input: {},
});

export default AIScreen;
