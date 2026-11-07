/**
 * 클립 한 개를 바로 재생해 보는 창.
 *
 * PinLog 를 만들기 전에는 각자 찍은 클립을 확인할 길이 없었다(썸네일에 ▶ 표시만
 * 있고 눌러도 아무 일도 없었다). 이 창은 기록에 붙은 영상이든 방금 찍은 로컬
 * 파일이든 같은 방식으로 재생한다 — videoUrl 은 http(서버에 올라간 것)일 수도
 * file://(아직 안 올린 것)일 수도 있고, react-native-video 는 둘 다 받는다.
 */
import React, { useState } from 'react';
import { ActivityIndicator, Modal, Pressable, Text, View } from 'react-native';
import Video from 'react-native-video';

export interface ClipPreview {
  /** 재생할 주소. http(s) 또는 file:// */
  videoUrl: string;
  /** 창 위에 적을 이름 (장소 이름 등) */
  title?: string;
  /** 누가 찍었는지 */
  authorName?: string;
  /** 초 단위 길이. 모르면 비운다. */
  duration?: number;
  /** 아직 서버에 올리지 않은 영상인지. 안내 문구에 쓴다. */
  local?: boolean;
}

export default function ClipPreviewModal({
  clip,
  onClose,
}: {
  clip: ClipPreview;
  onClose: () => void;
}) {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  return (
    <Modal transparent visible animationType="fade" onRequestClose={onClose} statusBarTranslucent>
      <View className="flex-1 items-center justify-center bg-slate-950/85 p-4">
        <View className="w-full max-w-md gap-3 rounded-3xl border-4 border-pixel-navy bg-white p-3">
          <View className="flex-row items-center justify-between">
            <View className="min-w-0 flex-1 pr-2">
              <Text className="font-pixel text-[8px] text-[#075953]">CLIP PREVIEW</Text>
              <Text numberOfLines={1} className="mt-1 text-sm font-black text-slate-900">
                {clip.title ?? '클립 미리보기'}
              </Text>
              {clip.authorName || clip.duration ? (
                <Text className="mt-0.5 text-[10px] font-semibold text-slate-500">
                  {[clip.authorName, clip.duration ? `${clip.duration.toFixed(1)}초` : null]
                    .filter(Boolean)
                    .join(' · ')}
                </Text>
              ) : null}
            </View>
            <Pressable
              onPress={onClose}
              accessibilityLabel="미리보기 닫기"
              className="h-9 w-9 items-center justify-center rounded-xl border-2 border-pixel-navy bg-slate-100 active:translate-y-0.5"
            >
              <Text className="text-xs font-black text-slate-800">✕</Text>
            </Pressable>
          </View>

          {/* 비율은 감싸는 View 의 style 로 잡는다. Video 에 준 aspect-* 클래스는 안 먹는다. */}
          <View className="w-full overflow-hidden rounded-xl border-2 border-pixel-navy bg-black" style={{ aspectRatio: 16 / 9 }}>
            {error ? (
              <View className="flex-1 items-center justify-center px-6">
                <Text className="text-center text-[11px] font-bold text-slate-300">{error}</Text>
              </View>
            ) : (
              <>
                <Video
                  source={{ uri: clip.videoUrl }}
                  controls
                  repeat
                  resizeMode="contain"
                  onLoad={() => setIsLoading(false)}
                  onError={() => {
                    setIsLoading(false);
                    setError('영상을 재생할 수 없어요. 파일이 지워졌거나 아직 올라가지 않았을 수 있어요.');
                  }}
                  style={{ width: '100%', height: '100%' }}
                />
                {isLoading && (
                  <View pointerEvents="none" className="absolute bottom-0 left-0 right-0 top-0 items-center justify-center">
                    <ActivityIndicator color="#56EADD" />
                  </View>
                )}
              </>
            )}
          </View>

          {clip.local ? (
            <Text className="text-center text-[10px] font-semibold text-slate-400">
              아직 저장하지 않은 영상이에요. 기록을 저장하면 서버에 올라가요.
            </Text>
          ) : null}

          <Pressable
            onPress={onClose}
            className="h-11 items-center justify-center rounded-xl border-2 border-pixel-navy bg-[#56EADD] active:translate-y-0.5"
          >
            <Text className="text-xs font-black text-[#075953]">닫기</Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}
