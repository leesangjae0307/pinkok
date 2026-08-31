import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Header from '../../../components/layout/Header';
import Input from '../../../components/common/Input';
import Button from '../../../components/common/Button';
import { COLORS, SPACING, TYPOGRAPHY } from '../../../theme';

const SignupScreen = () => {
  const [nickname, setNickname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');

  const passwordMismatch =
    passwordConfirm.length > 0 && password !== passwordConfirm;

  return (
    <ScreenLayout scroll>
      <Header title="회원가입" />

      <Text style={styles.subtitle}>
        핀콕과 함께 여행을 계획하고{'\n'}기록해보세요.
      </Text>

      <View style={styles.form}>
        <Input
          label="닉네임"
          value={nickname}
          onChangeText={setNickname}
          placeholder="사용할 닉네임"
          containerStyle={styles.field}
        />
        <Input
          label="이메일"
          value={email}
          onChangeText={setEmail}
          placeholder="이메일 주소"
          autoCapitalize="none"
          keyboardType="email-address"
          containerStyle={styles.field}
        />
        <Input
          label="비밀번호"
          value={password}
          onChangeText={setPassword}
          placeholder="8자 이상 입력해주세요"
          secureTextEntry
          containerStyle={styles.field}
        />
        <Input
          label="비밀번호 확인"
          value={passwordConfirm}
          onChangeText={setPasswordConfirm}
          placeholder="비밀번호를 다시 입력해주세요"
          secureTextEntry
          error={passwordMismatch ? '비밀번호가 일치하지 않아요' : undefined}
          containerStyle={styles.field}
        />

        <Button label="회원가입" onPress={() => {}} fullWidth style={styles.submitButton} />
      </View>
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  subtitle: {
    marginTop: SPACING.xs,
    marginBottom: SPACING.lg,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
    lineHeight: TYPOGRAPHY.caption + 6,
  },
  form: {},
  field: {
    marginBottom: SPACING.md,
  },
  submitButton: {
    marginTop: SPACING.sm,
  },
});

export default SignupScreen;
