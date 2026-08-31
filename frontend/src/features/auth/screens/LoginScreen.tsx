import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Input from '../../../components/common/Input';
import Button from '../../../components/common/Button';
import PixelIcon from '../../../components/ui/PixelIcon/PixelIcon';
import { COLORS, FONT_FAMILY, RADIUS, SPACING, TYPOGRAPHY } from '../../../theme';

const LoginScreen = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  return (
    <ScreenLayout scroll>
      <View style={styles.logoArea}>
        <View style={styles.logoBadge}>
          <PixelIcon name="pin" size={40} color={COLORS.SECONDARY} />
        </View>
        <Text style={styles.logo}>PINKOK</Text>
        <Text style={styles.tagline}>Plan your trip,{'\n'}Keep your memories.</Text>
      </View>

      <View style={styles.form}>
        <Input
          label="이메일 또는 아이디"
          value={email}
          onChangeText={setEmail}
          placeholder="이메일 또는 아이디"
          autoCapitalize="none"
          containerStyle={styles.field}
        />
        <Input
          label="비밀번호"
          value={password}
          onChangeText={setPassword}
          placeholder="비밀번호"
          secureTextEntry
          containerStyle={styles.field}
        />

        <Button label="로그인" onPress={() => {}} fullWidth style={styles.loginButton} />

        <Text style={styles.signupHint}>
          계정이 없으신가요? <Text style={styles.signupLink}>회원가입</Text>
        </Text>
      </View>

      <View style={styles.divider}>
        <View style={styles.dividerLine} />
        <Text style={styles.dividerText}>간편 로그인</Text>
        <View style={styles.dividerLine} />
      </View>

      <View style={styles.socialRow}>
        {[
          { label: 'N', bg: '#03C75A' },
          { label: 'G', bg: COLORS.WHITE },
          { label: 'K', bg: '#FEE500' },
        ].map((social) => (
          <View key={social.label} style={[styles.socialButton, { backgroundColor: social.bg }]}>
            <Text style={styles.socialLabel}>{social.label}</Text>
          </View>
        ))}
      </View>
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  logoArea: {
    alignItems: 'center',
    marginTop: SPACING.xl,
    marginBottom: SPACING.xl,
  },
  logoBadge: {
    width: 88,
    height: 88,
    borderRadius: RADIUS.lg,
    backgroundColor: COLORS.PRIMARY_LIGHT,
    borderWidth: 3,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: SPACING.md,
  },
  logo: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.logo,
    color: COLORS.SECONDARY,
    letterSpacing: 2,
  },
  tagline: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
    textAlign: 'center',
    lineHeight: TYPOGRAPHY.caption + 6,
  },
  form: {
    marginBottom: SPACING.lg,
  },
  field: {
    marginBottom: SPACING.md,
  },
  loginButton: {
    marginTop: SPACING.sm,
  },
  signupHint: {
    marginTop: SPACING.md,
    textAlign: 'center',
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
  signupLink: {
    color: COLORS.PRIMARY_DARK,
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: SPACING.lg,
  },
  dividerLine: {
    flex: 1,
    height: 1.5,
    backgroundColor: COLORS.GRAY300,
  },
  dividerText: {
    marginHorizontal: SPACING.sm,
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  socialRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: SPACING.md,
  },
  socialButton: {
    width: 48,
    height: 48,
    borderRadius: RADIUS.md,
    borderWidth: 2,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
  },
  socialLabel: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
  },
});

export default LoginScreen;
