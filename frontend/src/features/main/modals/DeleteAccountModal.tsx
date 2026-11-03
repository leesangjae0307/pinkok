/**
 * 회원 탈퇴 확인.
 *
 * 서버가 비밀번호를 한 번 더 확인하므로 확인 버튼만으로는 끝나지 않는다.
 * 안내 문구는 서버가 실제로 하는 일에 맞춘다 — 내가 팀장이던 여행은 남은 팀원에게
 * 넘어가고, 혼자 쓰던 여행은 함께 지워진다.
 */
import React, { useState } from 'react';
import { ActivityIndicator, Pressable, Text, TextInput, View } from 'react-native';
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
  const [password, setPassword] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
        style={{ marginBottom: insets.bottom }}
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

        <View className="gap-3 p-4">
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

          <View className="flex-row gap-2 pt-1">
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
              className={`h-11 flex-1 items-center justify-center rounded-xl border-2 border-pixel-navy ${
                canSubmit ? 'bg-rose-500 active:translate-y-0.5' : 'bg-slate-200'
              }`}
            >
              {isDeleting ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text className="text-xs font-black text-white">탈퇴하기</Text>
              )}
            </Pressable>
          </View>
        </View>
      </View>
    </ModalBackdrop>
  );
}
