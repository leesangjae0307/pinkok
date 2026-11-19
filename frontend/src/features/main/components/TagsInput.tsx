/**
 * 여행 해시태그를 넣고 지우는 입력칸.
 *
 * 서버도 같은 정리를 하지만(앞의 # 제거, 공백 제거, 중복 제거), 규칙을 어기면
 * 400 으로 거절한다. 그래서 보내기 전에 앱에서 먼저 정리해 보여 준다. 그래야
 * 사용자가 저장을 누른 뒤가 아니라 입력하는 중에 결과를 알 수 있다.
 *
 * 저장할 때는 # 없이 보내고, 화면에 보여줄 때만 # 를 붙인다.
 */
import React, { useState } from 'react';
import { Pressable, Text, TextInput, View } from 'react-native';

/** 서버 제한과 같게 둔다. 넘기면 400 이 온다. */
export const MAX_TAGS = 10;
export const MAX_TAG_LENGTH = 20;

/** 앞의 # 를 떼고 공백을 없앤다. '#부산 여행' → '부산여행' */
export function normalizeTag(raw: string): string {
  return raw.replace(/^#+/, '').replace(/\s+/g, '');
}

/** 대소문자를 무시하고 이미 있는 태그인지 본다. */
function hasTag(tags: string[], tag: string): boolean {
  const lower = tag.toLowerCase();
  return tags.some((t) => t.toLowerCase() === lower);
}

export default function TagsInput({
  tags,
  onChange,
}: {
  tags: string[];
  onChange: (tags: string[]) => void;
}) {
  const [draft, setDraft] = useState('');
  const [focused, setFocused] = useState(false);

  const isFull = tags.length >= MAX_TAGS;

  const add = () => {
    const tag = normalizeTag(draft);
    setDraft('');
    if (!tag || isFull || hasTag(tags, tag)) {return;}
    // 서버가 받지 않는 길이는 잘라서 넣는다. 거절보다 보여 주는 쪽이 낫다.
    onChange([...tags, tag.slice(0, MAX_TAG_LENGTH)]);
  };

  return (
    <View>
      <View className="flex-row items-center justify-between">
        <Text className="text-xs font-black text-slate-800">해시태그</Text>
        <Text className="font-pixel text-[9px] text-slate-400">
          {tags.length}/{MAX_TAGS}
        </Text>
      </View>

      {tags.length > 0 && (
        <View className="mt-2 flex-row flex-wrap gap-1.5">
          {tags.map((tag) => (
            <Pressable
              key={tag}
              onPress={() => onChange(tags.filter((t) => t !== tag))}
              accessibilityLabel={`${tag} 태그 빼기`}
              className="flex-row items-center gap-1 rounded-full border border-[#82F0DC] bg-[#E8FFFB] px-2.5 py-1 active:bg-[#B4FAF0]"
            >
              <Text className="text-[11px] font-bold text-teal-800">#{tag}</Text>
              <Text className="text-[10px] font-black text-slate-400">✕</Text>
            </Pressable>
          ))}
        </View>
      )}

      <View className="mt-2 flex-row items-center gap-2">
        <TextInput
          value={draft}
          onChangeText={setDraft}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          onSubmitEditing={add}
          editable={!isFull}
          placeholder={isFull ? `태그는 ${MAX_TAGS}개까지예요` : '예) 제주도, 맛집투어'}
          placeholderTextColor="#94A3B8"
          maxLength={MAX_TAG_LENGTH + 1}
          returnKeyType="done"
          className={`h-11 flex-1 rounded-xl border-2 bg-white px-3 text-sm text-slate-900 ${
            focused ? 'border-[#56EADD]' : 'border-slate-200'
          }`}
        />
        <Pressable
          onPress={add}
          disabled={isFull}
          accessibilityLabel="태그 추가"
          // 못 누르는 상태를 className 으로 바꾸면 NativeWind 가 그 렌더를 날려버린다.
          style={{ backgroundColor: isFull ? '#E2E8F0' : '#56EADD' }}
          className="h-11 items-center justify-center rounded-xl border-2 border-pixel-navy px-4 active:translate-y-0.5"
        >
          <Text className="text-xs font-black text-[#075953]">추가</Text>
        </Pressable>
      </View>
    </View>
  );
}
