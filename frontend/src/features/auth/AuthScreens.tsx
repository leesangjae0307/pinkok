/**
 * 인증 플로우: 스플래시 → 로그인 → 회원가입 / 비밀번호 재설정
 * (웹 AuthScreens.tsx 를 React Native 로 이관)
 */
import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Animated,
  Easing,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Circle, Path, Rect } from 'react-native-svg';

import splashOriginalBg from '../../assets/images/splash-original-bg.png';
import beachCleanBg from '../../assets/images/beach-clean-bg.png';
import loginLogoCutout from '../../assets/images/login-logo-cutout.png';
import authPinLogo from '../../assets/images/auth-pin-logo.png';
import avatarBookWinter from '../../assets/images/avatar-book-winter.png';
import avatarSilverBucket from '../../assets/images/avatar-silver-bucket.png';
import avatarPinkCamera from '../../assets/images/avatar-pink-camera.png';
import avatarMapPhone from '../../assets/images/avatar-map-phone.png';
import avatarHeadphoneWink from '../../assets/images/avatar-headphone-wink.png';
import avatarSunglasses from '../../assets/images/avatar-sunglasses.png';
import { fetchAvatars, login, signup } from '../../api/authApi';
import { errorMessage } from '../../api/axios';
import { PixelBackArrow, PixelGoogleIcon, PixelKakaoIcon } from '../../components/pixel/PixelIcons';
import { AnimatedLoop, LinearGradient } from '../../components/pixel/Primitives';

type AuthScreen = 'splash' | 'login' | 'signup' | 'resetPassword';

interface AuthScreensProps {
  onComplete: () => void;
}

const SKY_BORDER = '#0E7490';

/* --------------------------------- 아이콘 --------------------------------- */

function EyeIcon({ open }: { open: boolean }) {
  const c = '#94A3B8';
  return (
    <Svg width={20} height={20} viewBox="0 0 20 20" fill="none">
      <Path
        d="M2.2 10s2.8-4.5 7.8-4.5 7.8 4.5 7.8 4.5-2.8 4.5-7.8 4.5S2.2 10 2.2 10Z"
        stroke={c}
        strokeWidth={1.6}
        strokeLinejoin="round"
      />
      {open ? (
        <Circle cx="10" cy="10" r="2.2" stroke={c} strokeWidth={1.6} />
      ) : (
        <Path d="m4 4 12 12" stroke={c} strokeWidth={1.6} />
      )}
    </Svg>
  );
}

function FieldIcon({ kind }: { kind: 'user' | 'email' | 'lock' }) {
  const c = SKY_BORDER;
  if (kind === 'lock') {
    return (
      <Svg width={18} height={18} viewBox="0 0 18 18" fill="none" style={{ marginRight: 8 }}>
        <Rect x="4" y="8" width="10" height="7" stroke={c} strokeWidth={1.5} />
        <Path d="M6 8V6a3 3 0 0 1 6 0v2" stroke={c} strokeWidth={1.5} />
        <Rect x="8" y="11" width="2" height="2" fill={c} />
      </Svg>
    );
  }
  if (kind === 'email') {
    return (
      <Svg width={18} height={18} viewBox="0 0 18 18" fill="none" style={{ marginRight: 8 }}>
        <Rect x="2.5" y="4" width="13" height="10" stroke={c} strokeWidth={1.5} />
        <Path d="m3 5 6 5 6-5" stroke={c} strokeWidth={1.5} />
      </Svg>
    );
  }
  return (
    <Svg width={18} height={18} viewBox="0 0 18 18" fill="none" style={{ marginRight: 8 }}>
      <Circle cx="9" cy="6" r="3" stroke={c} strokeWidth={1.5} />
      <Path d="M3.5 15c.4-3 2.2-4.5 5.5-4.5s5.1 1.5 5.5 4.5" stroke={c} strokeWidth={1.5} />
    </Svg>
  );
}

function NaverIcon() {
  const c = '#16A34A';
  return (
    <Svg width={22} height={22} viewBox="0 0 18 18">
      <Rect x="2" y="2" width="4" height="14" fill={c} />
      <Rect x="12" y="2" width="4" height="14" fill={c} />
      <Path d="M5 2h4l4 7v7H9L5 9Z" fill={c} />
    </Svg>
  );
}

/* --------------------------------- 공통 입력 -------------------------------- */

function AuthInput({
  label,
  secure = false,
  value,
  onChange,
  placeholder,
  message,
  error = false,
  icon,
}: {
  label: string;
  secure?: boolean;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  message?: string;
  error?: boolean;
  icon?: 'user' | 'email' | 'lock';
}) {
  const [focused, setFocused] = useState(false);
  return (
    <View>
      <Text className="mb-1.5 text-xs font-extrabold text-pixel-navy">{label}</Text>
      <View
        className={`h-12 flex-row items-center rounded-lg border-2 bg-white px-3 ${
          error ? 'border-danger' : focused ? 'border-mint-primary' : 'border-sky-border'
        }`}
      >
        {icon && <FieldIcon kind={icon} />}
        <TextInput
          value={value}
          onChangeText={onChange}
          placeholder={placeholder}
          placeholderTextColor="#64748B"
          secureTextEntry={secure}
          autoCapitalize="none"
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          className="h-full flex-1 p-0 text-sm font-semibold text-ink"
        />
      </View>
      {message ? (
        <Text className={`mt-1 min-h-4 text-xs ${error ? 'font-bold text-danger' : 'text-muted'}`}>{message}</Text>
      ) : null}
    </View>
  );
}

/* ---------------------------------- 스플래시 --------------------------------- */

function LoadingBar() {
  const [width, setWidth] = useState(0);
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const anim = Animated.loop(
      Animated.timing(v, {
        toValue: 1,
        duration: 1350,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: true,
      }),
    );
    anim.start();
    return () => anim.stop();
  }, [v]);
  const translateX = v.interpolate({ inputRange: [0, 1], outputRange: [-width * 1.05, width * 1.05] });
  return (
    <View
      className="h-3 w-full overflow-hidden rounded-full border-2 border-white bg-white/40 p-0.5"
      onLayout={(e) => setWidth(e.nativeEvent.layout.width - 8)}
    >
      <Animated.View style={{ height: '100%', width: '100%', transform: [{ translateX }] }}>
        <LinearGradient
          colors={['#56EADD', '#00F5D4']}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 0 }}
          style={{ height: '100%', width: '100%', borderRadius: 999 }}
        />
      </Animated.View>
    </View>
  );
}

function SplashScreen({ onNext }: { onNext: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onNext, 2600);
    return () => clearTimeout(timer);
  }, [onNext]);

  return (
    <Pressable
      onPress={onNext}
      accessibilityLabel="로그인 화면으로 이동"
      className="relative flex-1 items-center overflow-hidden bg-[#5ad1ff]"
    >
      {/* 1번 원본 스플래시 이미지 그대로 사용 */}
      <Image source={splashOriginalBg} resizeMode="cover" className="absolute inset-0 h-full w-full" />

      {/* 8비트 반짝이 */}
      <View pointerEvents="none" className="absolute inset-0">
        <AnimatedLoop kind="pulse" style={{ position: 'absolute', top: 96, left: 40 }}>
          <Text className="text-sm text-yellow-200">✦</Text>
        </AnimatedLoop>
        <AnimatedLoop kind="ping" style={{ position: 'absolute', top: 144, right: 32 }}>
          <Text className="text-xs text-white">✧</Text>
        </AnimatedLoop>
        <AnimatedLoop kind="bounce" style={{ position: 'absolute', top: 208, left: 64 }}>
          <Text className="text-base text-yellow-300">★</Text>
        </AnimatedLoop>
        <AnimatedLoop kind="pulse" style={{ position: 'absolute', top: 192, right: 56 }}>
          <Text className="text-sm text-white">✦</Text>
        </AnimatedLoop>
        <AnimatedLoop kind="pulse" style={{ position: 'absolute', bottom: 192, left: '25%' }}>
          <Text className="text-[10px] text-white/80">✧</Text>
        </AnimatedLoop>
        <AnimatedLoop kind="ping" style={{ position: 'absolute', bottom: 160, right: '33%' }}>
          <Text className="text-xs text-white/90">✦</Text>
        </AnimatedLoop>
      </View>

      {/* 원본 로고 바로 아래 로딩 상태 */}
      <View className="absolute left-0 right-0 items-center" style={{ top: '55%' }}>
        <View className="w-56 items-center">
          <LoadingBar />
          <AnimatedLoop kind="pulse">
            <Text
              className="mt-2.5 font-pixel text-[11px] tracking-widest text-white"
              style={{ textShadowColor: 'rgba(0,0,0,0.7)', textShadowOffset: { width: 0, height: 2 }, textShadowRadius: 4 }}
            >
              LOADING...
            </Text>
          </AnimatedLoop>
        </View>
      </View>

      {/* 하단 터치 가이드 */}
      <View className="absolute bottom-6 left-0 right-0 items-center">
        <Text
          className="text-[10px] font-bold text-white/80"
          style={{ textShadowColor: 'rgba(0,0,0,0.4)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 2 }}
        >
          화면을 터치하면 바로 시작됩니다
        </Text>
      </View>
    </Pressable>
  );
}

/* ---------------------------------- 로그인 ---------------------------------- */

function LoginScreen({
  onSignUp,
  onResetPassword,
  onComplete,
}: {
  onSignUp: () => void;
  onResetPassword: () => void;
  onComplete: () => void;
}) {
  const insets = useSafeAreaInsets();
  const [email, setEmail] = useState('hello@pinkok.kr');
  const [password, setPassword] = useState('pinkok123!');
  const [showPassword, setShowPassword] = useState(false);
  const [focus, setFocus] = useState<'email' | 'pw' | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async () => {
    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
      onComplete();
    } catch (e) {
      Alert.alert('PINKOK', errorMessage(e, '이메일 또는 비밀번호를 확인해 주세요.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  // 소셜 로그인은 백엔드에 엔드포인트가 없음
  const notReady = () => Alert.alert('PINKOK', '간편 로그인은 준비 중입니다. 이메일로 로그인해 주세요.');

  return (
    <View className="relative flex-1 overflow-hidden bg-sky-soft">
      {/* 3번 배경 이미지 풀스크린 */}
      <Image source={beachCleanBg} resizeMode="cover" className="absolute inset-0 h-full w-full" />

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1">
        <ScrollView
          contentContainerStyle={{ flexGrow: 1, justifyContent: 'center', paddingTop: insets.top + 24, paddingBottom: insets.bottom + 20 }}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <View className="gap-3 px-5">
            {/* 투명 로고 */}
            <View className="items-center">
              <Image
                source={loginLogoCutout}
                resizeMode="contain"
                className="w-48"
                style={{ aspectRatio: 426 / 437 }}
              />
            </View>

            {/* 중앙 로그인 카드 */}
            <View
              className="w-full rounded-3xl border-2 border-[#1E293B] bg-white p-5"
              style={{ elevation: 8, shadowColor: '#000', shadowOpacity: 0.18, shadowRadius: 15, shadowOffset: { width: 0, height: 12 } }}
            >
              <View className="gap-3">
                <View
                  className={`h-12 flex-row items-center rounded-xl border-2 bg-white px-3.5 ${
                    focus === 'email' ? 'border-[#56EADD]' : 'border-[#82F0DC]'
                  }`}
                >
                  <View className="mr-0.5">
                    <FieldIcon kind="user" />
                  </View>
                  <TextInput
                    value={email}
                    onChangeText={setEmail}
                    placeholder="hello@pinkok.kr"
                    placeholderTextColor="#94A3B8"
                    keyboardType="email-address"
                    autoCapitalize="none"
                    onFocus={() => setFocus('email')}
                    onBlur={() => setFocus(null)}
                    className="h-full flex-1 p-0 text-sm font-semibold text-slate-800"
                  />
                </View>

                <View
                  className={`h-12 flex-row items-center rounded-xl border-2 bg-white px-3.5 ${
                    focus === 'pw' ? 'border-[#56EADD]' : 'border-[#82F0DC]'
                  }`}
                >
                  <View className="mr-0.5">
                    <FieldIcon kind="lock" />
                  </View>
                  <TextInput
                    value={password}
                    onChangeText={setPassword}
                    placeholder="••••••••"
                    placeholderTextColor="#94A3B8"
                    secureTextEntry={!showPassword}
                    autoCapitalize="none"
                    onFocus={() => setFocus('pw')}
                    onBlur={() => setFocus(null)}
                    className="h-full flex-1 p-0 text-sm font-semibold tracking-wider text-slate-800"
                  />
                  <Pressable
                    onPress={() => setShowPassword((v) => !v)}
                    className="p-1"
                    accessibilityLabel={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                  >
                    <EyeIcon open={showPassword} />
                  </Pressable>
                </View>
              </View>

              {/* 로그인 버튼 */}
              <Pressable
                onPress={handleLogin}
                disabled={isSubmitting}
                className={`mt-4 h-12 w-full items-center justify-center rounded-2xl border-2 border-[#1E293B] active:translate-y-0.5 ${
                  isSubmitting ? 'bg-slate-200' : 'bg-[#56EADD] active:bg-[#43d9cc]'
                }`}
              >
                <Text className="text-base font-black text-slate-900">{isSubmitting ? '로그인 중…' : '로그인'}</Text>
              </Pressable>

              {/* 비밀번호 찾기 | 회원가입 */}
              <View className="mt-4 flex-row items-center justify-center gap-3">
                <Pressable onPress={onResetPassword}>
                  <Text className="text-xs font-bold text-slate-600">비밀번호 찾기</Text>
                </Pressable>
                <View className="h-3 w-[1.5px] bg-slate-300" />
                <Pressable onPress={onSignUp}>
                  <Text className="text-xs font-bold text-slate-600">회원가입</Text>
                </Pressable>
              </View>
            </View>

            {/* 간편 로그인 */}
            <View className="w-full">
              <View className="mb-3.5 flex-row items-center gap-3">
                <View className="h-px flex-1 bg-[#1E293B]/20" />
                <Text className="font-pixel text-xs text-slate-700">간편 로그인</Text>
                <View className="h-px flex-1 bg-[#1E293B]/20" />
              </View>

              <View className="flex-row justify-center gap-4">
                <Pressable
                  onPress={notReady}
                  accessibilityLabel="네이버로 로그인 (준비 중)"
                  className="h-12 w-12 items-center justify-center rounded-2xl border-2 border-[#1E293B] bg-white shadow-md active:translate-y-0.5"
                >
                  <NaverIcon />
                </Pressable>
                <Pressable
                  onPress={notReady}
                  accessibilityLabel="Google로 로그인 (준비 중)"
                  className="h-12 w-12 items-center justify-center rounded-2xl border-2 border-[#1E293B] bg-white shadow-md active:translate-y-0.5"
                >
                  <PixelGoogleIcon size={22} />
                </Pressable>
                <Pressable
                  onPress={notReady}
                  accessibilityLabel="카카오로 로그인 (준비 중)"
                  className="h-12 w-12 items-center justify-center rounded-2xl border-2 border-[#1E293B] bg-[#FEE500] shadow-md active:translate-y-0.5"
                >
                  <PixelKakaoIcon size={22} />
                </Pressable>
              </View>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

/* -------------------------------- 약관 체크박스 ------------------------------- */

function AgreementRow({
  checked,
  onChange,
  required,
  children,
}: {
  checked: boolean;
  onChange: () => void;
  required: boolean;
  children: React.ReactNode;
}) {
  return (
    <Pressable onPress={onChange} className="flex-row items-start gap-2" accessibilityRole="checkbox" accessibilityState={{ checked }}>
      <View
        className={`mt-0.5 h-5 w-5 items-center justify-center border-2 ${
          checked ? 'border-pixel-navy bg-mint-deep' : 'border-sky-border bg-white'
        }`}
      >
        <Text className="text-xs font-black text-white">✓</Text>
      </View>
      <Text className="flex-1 text-xs leading-5 text-muted">
        <Text className={`font-bold ${required ? 'text-mint-deep' : 'text-muted'}`}>{required ? '[필수]' : '[선택]'}</Text>{' '}
        {children}
      </Text>
    </Pressable>
  );
}

/**
 * 로컬 아바타 에셋 ↔ 서버 `GET /avatars` 코드. 서버의 `static/images/avatars/{code}.png` 와
 * 바이트 단위로 같은 파일임을 확인해서 짝지었다.
 * 서버 목록에 없는 코드면 avatarId 없이 가입한다(서버에서 선택 항목).
 */
const PIXEL_AVATARS = [
  { id: 'book-winter', serverCode: 'F1', label: '책 읽는 겨울 여행가', gender: '여', image: avatarBookWinter },
  { id: 'silver-bucket', serverCode: 'F2', label: '은발 버킷햇 여행가', gender: '여', image: avatarSilverBucket },
  { id: 'pink-camera', serverCode: 'F3', label: '핑크 카메라 여행가', gender: '여', image: avatarPinkCamera },
  { id: 'map-phone', serverCode: 'M1', label: '지도 보는 도시 여행가', gender: '남', image: avatarMapPhone },
  { id: 'headphone-wink', serverCode: 'M2', label: '윙크 헤드폰 여행가', gender: '남', image: avatarHeadphoneWink },
  { id: 'sunglasses', serverCode: 'M3', label: '선글라스 로컬 여행가', gender: '남', image: avatarSunglasses },
];

/* ------------------------------ 서브 화면 공통 헤더 ----------------------------- */

function AuthHeader({ onBack, eyebrow, title }: { onBack: () => void; eyebrow: string; title: string }) {
  const insets = useSafeAreaInsets();
  return (
    <View
      className="relative flex-row items-end overflow-hidden bg-sky-soft px-5 pb-3"
      style={{ height: 144 + insets.top }}
    >
      <Image source={beachCleanBg} resizeMode="cover" className="absolute inset-0 h-full w-full opacity-80" />
      <LinearGradient
        colors={['rgba(255,255,255,0)', 'rgba(165,243,252,0.4)', 'rgba(255,255,255,0.9)']}
        start={{ x: 0, y: 0 }}
        end={{ x: 0, y: 1 }}
        style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}
      />
      <Pressable
        onPress={onBack}
        accessibilityLabel="로그인으로 돌아가기"
        className="z-10 h-9 w-9 items-center justify-center rounded-lg border-2 border-pixel-navy bg-white shadow-sm active:translate-y-1"
      >
        <PixelBackArrow size={16} />
      </Pressable>
      <View className="z-10 ml-3">
        <Text className="font-pixel text-[10px] text-teal-800">{eyebrow}</Text>
        <Text className="text-lg font-black text-pixel-navy">{title}</Text>
      </View>
      <View className="absolute bottom-2 right-5 z-10 h-11 w-11 items-center justify-center rounded-xl border border-white/60 bg-white/80 p-0.5">
        <Image source={authPinLogo} resizeMode="contain" className="h-full w-full" />
      </View>
    </View>
  );
}

function BottomSubmit({ label, disabled, onPress }: { label: string; disabled: boolean; onPress: () => void }) {
  const insets = useSafeAreaInsets();
  return (
    <View className="border-t-2 border-sky-border bg-cloud px-5 pt-3" style={{ paddingBottom: insets.bottom + 20 }}>
      <Pressable
        disabled={disabled}
        onPress={onPress}
        className={`h-12 w-full items-center justify-center rounded-lg border-2 ${
          disabled ? 'border-disabled bg-disabled' : 'border-pixel-navy bg-mint-primary shadow-md active:translate-y-1'
        }`}
      >
        <Text className={`text-sm font-black ${disabled ? 'text-white' : 'text-pixel-navy'}`}>{label}</Text>
      </Pressable>
    </View>
  );
}

/* ---------------------------------- 회원가입 --------------------------------- */

function SignUpScreen({ onBack, onComplete }: { onBack: () => void; onComplete: () => void }) {
  const [nickname, setNickname] = useState('여행하는 콕이');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('hello@pinkok.kr');
  const [isEmailChecked, setIsEmailChecked] = useState(true);
  const [emailCheckMessage, setEmailCheckMessage] = useState('사용 가능한 이메일 형식입니다.');
  const [password, setPassword] = useState('pinkok123!');
  const [confirmation, setConfirmation] = useState('pinkok123!');
  const [selectedAvatarId, setSelectedAvatarId] = useState('book-winter');
  const [requiredTerms, setRequiredTerms] = useState(true);
  const [optionalTerms, setOptionalTerms] = useState(false);
  const [emailFocused, setEmailFocused] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [avatarIdByCode, setAvatarIdByCode] = useState<Record<string, number>>({});

  useEffect(() => {
    // 로그인 없이 호출 가능. 실패해도 avatarId 없이 가입할 수 있으므로 조용히 넘긴다.
    fetchAvatars()
      .then((list) => setAvatarIdByCode(Object.fromEntries(list.map((a) => [a.code, a.id]))))
      .catch(() => {});
  }, []);

  const mismatch = confirmation.length > 0 && password !== confirmation;
  const canSubmit =
    nickname.length >= 2 &&
    username.trim().length >= 3 &&
    email.includes('@') &&
    isEmailChecked &&
    password.length >= 8 &&
    !mismatch &&
    requiredTerms &&
    !isSubmitting;

  // 서버에 중복 확인 전용 엔드포인트가 없어서 형식만 본다. 실제 중복은 가입 시 409 로 온다.
  const handleCheckEmailDuplicate = () => {
    if (!email.includes('@')) {
      setEmailCheckMessage('올바른 이메일 형식을 입력해주세요.');
      setIsEmailChecked(false);
      return;
    }
    setIsEmailChecked(true);
    setEmailCheckMessage('형식은 올바릅니다. 중복 여부는 가입할 때 확인돼요.');
  };

  const handleSignUp = async () => {
    const serverCode = PIXEL_AVATARS.find((a) => a.id === selectedAvatarId)?.serverCode;
    setIsSubmitting(true);
    try {
      await signup({
        email: email.trim(),
        password,
        username: username.trim(),
        nickname: nickname.trim(),
        avatarId: serverCode ? avatarIdByCode[serverCode] : undefined,
      });
      await login(email.trim(), password);
      onComplete();
    } catch (e) {
      Alert.alert('PINKOK', errorMessage(e, '가입에 실패했어요. 입력 내용을 확인해 주세요.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-auth-canvas">
      <AuthHeader onBack={onBack} eyebrow="JOIN PINKOK" title="회원가입" />

      <ScrollView className="flex-1" contentContainerClassName="px-5 pb-5 pt-3" keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
        <View className="gap-3 rounded-xl border-2 border-pixel-navy bg-white p-4 shadow-xl">
          {/* 이메일 + 중복확인 */}
          <View>
            <Text className="mb-1 text-xs font-extrabold text-pixel-navy">이메일 계정</Text>
            <View className="flex-row gap-2">
              <View
                className={`h-12 flex-1 flex-row items-center rounded-lg border-2 bg-white px-3 ${
                  emailFocused ? 'border-mint-primary' : 'border-sky-border'
                }`}
              >
                <FieldIcon kind="email" />
                <TextInput
                  value={email}
                  onChangeText={(t) => {
                    setEmail(t);
                    setIsEmailChecked(false);
                    setEmailCheckMessage('');
                  }}
                  placeholder="이메일을 입력해주세요"
                  placeholderTextColor="#64748B"
                  keyboardType="email-address"
                  autoCapitalize="none"
                  onFocus={() => setEmailFocused(true)}
                  onBlur={() => setEmailFocused(false)}
                  className="h-full flex-1 p-0 text-sm font-semibold text-ink"
                />
              </View>
              <Pressable
                onPress={handleCheckEmailDuplicate}
                className={`items-center justify-center rounded-lg border-2 border-pixel-navy px-3 active:translate-y-1 ${
                  isEmailChecked ? 'bg-teal-500' : 'bg-mint-primary'
                }`}
              >
                <Text className={`text-xs font-black ${isEmailChecked ? 'text-white' : 'text-pixel-navy'}`}>
                  {isEmailChecked ? '확인완료 ✓' : '중복확인'}
                </Text>
              </Pressable>
            </View>
            {emailCheckMessage ? (
              <Text className={`mt-1 text-xs font-bold ${isEmailChecked ? 'text-teal-700' : 'text-danger'}`}>
                {emailCheckMessage}
              </Text>
            ) : null}
          </View>

          <AuthInput
            label="비밀번호"
            secure
            value={password}
            onChange={setPassword}
            placeholder="비밀번호를 입력해주세요"
            message="영문, 숫자를 포함해 8자 이상 입력해주세요."
            icon="lock"
          />

          <AuthInput
            label="비밀번호 확인"
            secure
            value={confirmation}
            onChange={setConfirmation}
            placeholder="비밀번호를 다시 입력해주세요"
            message={mismatch ? '비밀번호가 일치하지 않아요.' : '비밀번호가 일치해요. ✓'}
            error={mismatch}
            icon="lock"
          />

          <AuthInput
            label="닉네임"
            value={nickname}
            onChange={setNickname}
            placeholder="닉네임을 입력해주세요"
            message="2~12자의 닉네임을 입력해주세요."
            icon="user"
          />

          <AuthInput
            label="아이디"
            value={username}
            onChange={setUsername}
            placeholder="팀원이 나를 찾을 때 쓰는 @아이디"
            message="영문·숫자 3~30자. 닉네임과 달리 팀원 검색에 쓰여요."
            icon="user"
          />

          {/* 프로필 아바타 선택 */}
          <View className="pt-2">
            <View className="mb-2">
              <View className="flex-row items-center justify-between">
                <Text className="text-xs font-black text-pixel-navy">프로필 아바타 선택</Text>
                <View className="rounded bg-[#B4FAF0] px-1.5 py-0.5">
                  <Text className="font-pixel text-[8px] text-[#075953]">6 AVATARS</Text>
                </View>
              </View>
              <Text className="mt-1 text-[10px] font-semibold text-slate-500">
                내 여행 스타일과 어울리는 픽셀 캐릭터를 선택해 보세요!
              </Text>
            </View>

            <View className="flex-row flex-wrap justify-between gap-y-2">
              {PIXEL_AVATARS.map((avatar) => {
                const isSelected = selectedAvatarId === avatar.id;
                return (
                  <Pressable
                    key={avatar.id}
                    onPress={() => setSelectedAvatarId(avatar.id)}
                    style={{ width: '31.5%' }}
                  >
                    {isSelected && (
                      <View
                        pointerEvents="none"
                        style={{ position: 'absolute', left: 3, top: 3, right: -3, bottom: -3, borderRadius: 12, backgroundColor: '#075953' }}
                      />
                    )}
                    <View
                      className={`relative overflow-hidden rounded-xl border-3 bg-white ${
                        isSelected ? 'border-[#56EADD]' : 'border-slate-200'
                      }`}
                    >
                      {isSelected && (
                        <View className="absolute right-1 top-1 z-10 h-5 w-5 items-center justify-center rounded-md border-2 border-white bg-[#56EADD]">
                          <Text className="text-[9px] font-black text-[#075953]">✓</Text>
                        </View>
                      )}
                      <Image source={avatar.image} resizeMode="cover" className="aspect-square w-full" />
                      <View className="items-center px-1 py-1.5">
                        <Text className="min-h-6 text-center text-[8px] font-black leading-3 text-slate-700">
                          {avatar.label}
                        </Text>
                        <View className={`mt-1 rounded px-1 ${avatar.gender === '남' ? 'bg-blue-50' : 'bg-rose-50'}`}>
                          <Text className={`font-pixel text-[7px] ${avatar.gender === '남' ? 'text-blue-700' : 'text-rose-700'}`}>
                            {avatar.gender}
                          </Text>
                        </View>
                      </View>
                    </View>
                  </Pressable>
                );
              })}
            </View>
          </View>

          {/* 약관 */}
          <View className="mt-3 gap-2 border-t border-line pt-3">
            <AgreementRow checked={requiredTerms} onChange={() => setRequiredTerms((v) => !v)} required>
              이용약관 및 개인정보 처리방침에 동의합니다.
            </AgreementRow>
            <AgreementRow checked={optionalTerms} onChange={() => setOptionalTerms((v) => !v)} required={false}>
              여행 추천 및 이벤트 소식 수신에 동의합니다.
            </AgreementRow>
          </View>
        </View>
      </ScrollView>

      <BottomSubmit label={isSubmitting ? '가입 중…' : '가입완료'} disabled={!canSubmit} onPress={handleSignUp} />
    </KeyboardAvoidingView>
  );
}

/* ------------------------------- 비밀번호 재설정 ------------------------------- */

function ResetPasswordScreen({ onBack, onComplete }: { onBack: () => void; onComplete: () => void }) {
  const [email, setEmail] = useState('hello@pinkok.kr');
  const [authCode, setAuthCode] = useState('');
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [notification, setNotification] = useState('');
  const [focus, setFocus] = useState<'email' | 'code' | null>(null);

  const handleSendCode = () => {
    if (!email.includes('@')) {
      setNotification('올바른 이메일 주소를 입력해주세요.');
      return;
    }
    setIsCodeSent(true);
    setNotification('인증번호 6자리가 이메일로 발송되었습니다. (테스트 코드: 778899)');
    setAuthCode('778899');
  };

  const canSubmit = isCodeSent && authCode.length === 6;

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-auth-canvas">
      <AuthHeader onBack={onBack} eyebrow="FIND PASSWORD" title="비밀번호 재설정" />

      <ScrollView className="flex-1" contentContainerClassName="px-5 pb-5 pt-4" keyboardShouldPersistTaps="handled">
        <View className="gap-4 rounded-xl border-2 border-pixel-navy bg-white p-5 shadow-xl">
          <View className="rounded-lg border border-teal-200 bg-teal-50 p-3">
            <Text className="text-xs font-semibold leading-relaxed text-teal-900">
              💡 가입하신 이메일 주소를 입력하시면 인증 번호를 발송해 드립니다.
            </Text>
          </View>

          <View>
            <Text className="mb-1.5 text-xs font-extrabold text-pixel-navy">가입된 이메일 주소</Text>
            <View className="flex-row gap-2">
              <View
                className={`h-12 flex-1 flex-row items-center rounded-lg border-2 bg-white px-3 ${
                  focus === 'email' ? 'border-mint-primary' : 'border-sky-border'
                }`}
              >
                <FieldIcon kind="email" />
                <TextInput
                  value={email}
                  onChangeText={setEmail}
                  placeholder="가입 이메일 입력"
                  placeholderTextColor="#64748B"
                  keyboardType="email-address"
                  autoCapitalize="none"
                  onFocus={() => setFocus('email')}
                  onBlur={() => setFocus(null)}
                  className="h-full flex-1 p-0 text-sm font-semibold text-ink"
                />
              </View>
              <Pressable
                onPress={handleSendCode}
                className="items-center justify-center rounded-lg border-2 border-pixel-navy bg-mint-primary px-3 active:translate-y-1"
              >
                <Text className="text-xs font-black text-pixel-navy">{isCodeSent ? '재발송' : '인증번호 발송'}</Text>
              </Pressable>
            </View>
          </View>

          <View>
            <View className="mb-1.5 flex-row items-center justify-between">
              <Text className="text-xs font-extrabold text-pixel-navy">인증번호 6자리</Text>
              {isCodeSent && <Text className="font-pixel text-[10px] text-rose-600">유효시간 02:59</Text>}
            </View>
            <View
              className={`h-12 flex-row items-center rounded-lg border-2 bg-white px-3 ${
                focus === 'code' ? 'border-mint-primary' : 'border-sky-border'
              }`}
            >
              <TextInput
                maxLength={6}
                keyboardType="number-pad"
                value={authCode}
                onChangeText={setAuthCode}
                placeholder="6자리 숫자 입력 (예: 778899)"
                placeholderTextColor="#64748B"
                onFocus={() => setFocus('code')}
                onBlur={() => setFocus(null)}
                className={`h-full flex-1 p-0 text-sm text-ink ${authCode ? 'font-pixel tracking-widest' : 'font-semibold'}`}
              />
            </View>
          </View>

          {notification ? (
            <View className="rounded-lg border border-[#50E6DC] bg-[#B4FAF0]/50 p-2.5">
              <Text className="text-xs font-bold text-teal-800">{notification}</Text>
            </View>
          ) : null}
        </View>
      </ScrollView>

      <BottomSubmit label="비밀번호 재설정 링크 받기" disabled={!canSubmit} onPress={onComplete} />
    </KeyboardAvoidingView>
  );
}

/* ---------------------------------- 진입점 ---------------------------------- */

export default function AuthScreens({ onComplete }: AuthScreensProps) {
  const [screen, setScreen] = useState<AuthScreen>('splash');

  return (
    <View className="flex-1 bg-auth-canvas">
      {screen === 'splash' && <SplashScreen onNext={() => setScreen('login')} />}
      {screen === 'login' && (
        <LoginScreen
          onSignUp={() => setScreen('signup')}
          onResetPassword={() => setScreen('resetPassword')}
          onComplete={onComplete}
        />
      )}
      {screen === 'signup' && <SignUpScreen onBack={() => setScreen('login')} onComplete={onComplete} />}
      {screen === 'resetPassword' && (
        <ResetPasswordScreen
          onBack={() => setScreen('login')}
          onComplete={() => {
            Alert.alert('PINKOK', '비밀번호 재설정 링크가 이메일로 전송되었습니다! 로그인 화면으로 이동합니다.', [
              { text: '확인', onPress: () => setScreen('login') },
            ]);
          }}
        />
      )}
    </View>
  );
}
