/**
 * 회원 탈퇴 확인.
 *
 * 서버가 비밀번호를 한 번 더 확인하므로 확인 버튼만으로는 끝나지 않는다.
 * 안내 문구는 서버가 실제로 하는 일에 맞춘다 — 내가 팀장이던 여행은 남은 팀원에게
 * 넘어가고, 혼자 쓰던 여행은 함께 지워진다.
 */
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Keyboard,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ModalBackdrop } from '../../../components/pixel/Primitives';

export default function DeleteAccountModal({
  onClose,
  onConfirm,
}: {
  onClose: () => void;
  /** 실패하면 보여줄 메시지를 돌려준다. 성공하면 null. */
  onConfirm: (password: string) => Promise<string | null>;
}) {
  const insets = useSafeAreaInsets();
  const { height: screenHeight } = useWindowDimensions();
  const [password, setPassword] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  /**
   * 안드로이드에서는 statusBarTranslucent 모달 안까지 adjustResize 가 닿지 않아,
   * 키보드가 올라오면 탈퇴 버튼이 그대로 가려진다. 키보드 높이만큼 직접 띄운다.
   */
  const [keyboardHeight, setKeyboardHeight] = useState(0);

  useEffect(() => {
    const show = Keyboard.addListener('keyboardDidShow', (e) =>
      setKeyboardHeight(e.endCoordinates.height),
    );
    const hide = Keyboard.addListener('keyboardDidHide', () => setKeyboardHeight(0));
    return () => {
      show.remove();
      hide.remove();
    };
  }, []);

  const canSubmit = password.length > 0 && !isDeleting;

  const submit = async () => {
    if (!canSubmit) return;
    setIsDeleting(true);
    setError(null);
    const message = await onConfirm(password);
    if (message) {
      setError(message);
      setIsDeleting(false);
    }
    // 성공하면 로그인 화면으로 넘어가면서 이 창도 같이 사라진다.
  };

  return (
    <ModalBackdrop onRequestClose={onClose}>
      <View
        className="w-full overflow-hidden rounded-[28px] border-4 border-pixel-navy bg-[#B4FAF0]"
        style={{
          marginBottom: keyboardHeight > 0 ? keyboardHeight : insets.bottom,
          // 키보드만큼 띄우면 창이 화면 위로 넘칠 수 있다. 남는 높이를 넘지 않게
          // 묶어두고, 모자라면 본문이 스크롤되게 한다(머리글과 버튼은 항상 보인다).
          maxHeight: Math.max(240, screenHeight - keyboardHeight - insets.top - 24),
        }}
      >
        <View className="flex-row items-center justify-between border-b-2 border-pixel-navy bg-white px-4 py-3">
          <View className="flex-1 pr-2">
            <Text className="font-pixel text-[11px] text-rose-600">회원 탈퇴</Text>
            <Text className="mt-2 text-[11px] font-bold text-slate-500">되돌릴 수 없어요.</Text>
          </View>
          <Pressable
            onPress={onClose}
            accessibilityLabel="회원 탈퇴 닫기"
            className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5"
          >
            <Text className="text-sm font-black text-slate-700">✕</Text>
          </Pressable>
        </View>

        <ScrollView
          className="shrink"
          contentContainerStyle={{ gap: 12, padding: 16 }}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <View className="gap-2 rounded-2xl border-2 border-rose-200 bg-rose-50 p-3">
            <Text className="text-[11px] font-black text-rose-700">탈퇴하면</Text>
            <Text className="text-[11px] font-semibold leading-4 text-slate-700">
              · 계정과 프로필 정보가 지워지고 다시 로그인할 수 없어요.{'\n'}
              · 혼자 만든 여행은 함께 삭제돼요.{'\n'}
              · 팀원이 있는 여행은 다른 팀원에게 팀장 권한이 넘어가요.
            </Text>
          </View>

          <View>
            <Text className="text-[11px] font-black text-slate-700">비밀번호 확인</Text>
            <TextInput
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              autoCapitalize="none"
              placeholder="비밀번호"
              placeholderTextColor="#94A3B8"
              className="mt-1.5 h-11 rounded-xl border-2 border-slate-200 bg-white px-3 py-0 text-sm font-black text-slate-900"
            />
          </View>

          {error ? <Text className="text-[11px] font-bold text-rose-600">{error}</Text> : null}
        </ScrollView>

        {/* 버튼은 스크롤 밖에 둬서 키보드가 올라와도 항상 보이게 한다. */}
        <View className="flex-row gap-2 px-4 pb-4 pt-1">
            <Pressable
              onPress={onClose}
              disabled={isDeleting}
              className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-slate-300 bg-white active:translate-y-0.5"
            >
              <Text className="text-xs font-black text-slate-600">취소</Text>
            </Pressable>
            <Pressable
              onPress={submit}
              disabled={!canSubmit}
              // 못 누르는 상태를 className 으로 바꾸면 NativeWind 가 스타일 구조가
              // 바뀐 것으로 보고 그 렌더를 통째로 날려버린다(버튼이 계속 회색으로
              // 남아 눌리지 않는다). 색은 style 로 준다.
              style={{ backgroundColor: canSubmit ? '#F43F5E' : '#E2E8F0' }}
              className="h-11 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy active:translate-y-0.5"
            >
              {isDeleting ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text className="text-xs font-black" style={{ color: canSubmit ? '#fff' : '#94A3B8' }}>
                  탈퇴하기
                </Text>
              )}
            </Pressable>
        </View>
      </View>
    </ModalBackdrop>
  );
}
