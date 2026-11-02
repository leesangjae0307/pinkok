/**
 * 장소 기록용 짧은 영상 촬영.
 *
 * PinLog 는 장소별 클립을 동선 순서대로 이어붙이는 것이라 클립이 길면 안 된다.
 * 5초가 되면 자동으로 멈추고, 2초가 안 되면 쓰지 않고 다시 찍게 한다.
 */
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Linking, Modal, PermissionsAndroid, Platform, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Camera, useCameraDevice, type VideoFile } from 'react-native-vision-camera';

/** 너무 짧으면 이어붙여도 보이지 않는다. */
const MIN_MS = 2000;
/** 이 길이가 되면 알아서 멈춘다. */
const MAX_MS = 5000;

export interface RecordedClip {
  /** file:// 로 시작하는 로컬 경로. 업로드는 저장할 때 한다. */
  uri: string;
  durationMs: number;
}

export default function RecordClipModal({
  title,
  onClose,
  onRecorded,
}: {
  /** 어디서 찍는 중인지 (장소 이름) */
  title: string;
  onClose: () => void;
  onRecorded: (clip: RecordedClip) => void;
}) {
  const insets = useSafeAreaInsets();
  const camera = useRef<Camera | null>(null);
  const device = useCameraDevice('back');

  const [isRecording, setIsRecording] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const [isBusy, setIsBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const startedAt = useRef(0);

  /**
   * 카메라·마이크 권한을 OS 에 직접 묻는다.
   *
   * vision-camera 의 useCameraPermission / useMicrophonePermission 은 쓰지 않는다.
   * 앱 안에서 막 허용한 직후에도 hasPermission 이 계속 false 로 남아 화면이 풀리지
   * 않았다(OS 는 granted=true 인데도). requestMultiple 은 다이얼로그를 하나씩
   * 차례로 띄워 주기도 해서, 두 권한을 받기에 이쪽이 맞다.
   */
  const [permsOk, setPermsOk] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        if (Platform.OS !== 'android') {
          setPermsOk(true);
          return;
        }
        const need = [
          PermissionsAndroid.PERMISSIONS.CAMERA,
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        ];
        const have = await Promise.all(need.map((p) => PermissionsAndroid.check(p)));
        if (cancelled) return;
        if (have.every(Boolean)) {
          setPermsOk(true);
          return;
        }
        const res = await PermissionsAndroid.requestMultiple(need);
        if (cancelled) return;
        setPermsOk(need.every((p) => res[p] === PermissionsAndroid.RESULTS.GRANTED));
      } catch {
        // 권한 조회가 실패하면 막힌 상태로 둔다. 화면이 설정으로 안내한다.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const stop = useCallback(async () => {
    if (!isRecording) return;
    setIsBusy(true);
    try {
      await camera.current?.stopRecording();
    } catch {
      // 이미 멈췄으면 그대로 둔다.
      setIsBusy(false);
    }
  }, [isRecording]);

  /** 5초가 되면 알아서 멈춘다. */
  useEffect(() => {
    if (!isRecording) return;
    const timer = setInterval(() => {
      const ms = Date.now() - startedAt.current;
      setElapsed(ms);
      if (ms >= MAX_MS) stop();
    }, 50);
    return () => clearInterval(timer);
  }, [isRecording, stop]);

  const start = async () => {
    if (isRecording || isBusy || !camera.current) return;
    setNotice(null);
    setElapsed(0);
    startedAt.current = Date.now();
    setIsRecording(true);
    camera.current.startRecording({
      onRecordingFinished: (video: VideoFile) => {
        setIsRecording(false);
        setIsBusy(false);
        const durationMs = Math.round((video.duration ?? 0) * 1000) || Date.now() - startedAt.current;
        if (durationMs < MIN_MS) {
          setNotice('2초보다 짧아요. 조금만 더 길게 찍어주세요.');
          setElapsed(0);
          return;
        }
        const uri = video.path.startsWith('file://') ? video.path : `file://${video.path}`;
        onRecorded({ uri, durationMs });
      },
      onRecordingError: () => {
        setIsRecording(false);
        setIsBusy(false);
        setNotice('촬영에 문제가 있었어요. 다시 시도해 주세요.');
      },
    });
  };

  const blocked = !permsOk;

  return (
    <Modal visible animationType="slide" onRequestClose={onClose} statusBarTranslucent>
      <View className="flex-1 bg-slate-950" style={{ paddingTop: insets.top }}>
        <View className="flex-row items-center justify-between border-b border-white/15 px-4 py-3">
          <Pressable onPress={onClose} accessibilityLabel="촬영 닫기" className="px-2 py-1">
            <Text className="text-sm font-black text-white">✕</Text>
          </Pressable>
          <Text numberOfLines={1} className="mx-3 flex-1 text-center text-xs font-black text-white">
            {title}
          </Text>
          <View className="w-8" />
        </View>

        <View className="flex-1">
          {blocked ? (
            <View className="flex-1 items-center justify-center gap-3 px-8">
              <Text className="text-center text-xs font-bold text-white">
                촬영하려면 카메라와 마이크 권한이 필요해요.
              </Text>
              <Pressable
                onPress={() => Linking.openSettings()}
                className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95"
              >
                <Text className="text-xs font-black text-[#075953]">설정 열기</Text>
              </Pressable>
            </View>
          ) : device == null ? (
            <View className="flex-1 items-center justify-center">
              <Text className="text-xs font-bold text-white">쓸 수 있는 카메라를 찾지 못했어요.</Text>
            </View>
          ) : (
            <>
              <Camera
                ref={camera}
                device={device}
                isActive
                video
                audio
                style={{ flex: 1 }}
              />
              {/* 진행 막대 — 5초가 가득 */}
              <View className="absolute left-0 right-0 top-0 h-1 bg-white/20">
                <View
                  className="h-full bg-[#FF5A87]"
                  style={{ width: `${Math.min(100, (elapsed / MAX_MS) * 100)}%` }}
                />
              </View>
            </>
          )}
        </View>

        <View className="items-center gap-3 px-6 pb-8 pt-4" style={{ paddingBottom: insets.bottom + 24 }}>
          {notice ? <Text className="text-center text-[11px] font-bold text-[#FFC837]">{notice}</Text> : null}
          <Text className="font-pixel text-[10px] text-white/70">
            {isRecording ? `${(elapsed / 1000).toFixed(1)}초 / 5초` : '2~5초로 짧게 찍어주세요'}
          </Text>
          <Pressable
            onPress={isRecording ? stop : start}
            disabled={blocked || device == null || isBusy}
            accessibilityLabel={isRecording ? '촬영 멈추기' : '촬영 시작'}
            // 못 누르는 상태를 className 의 opacity-* 로 주면 안 된다. NativeWind 가
            // 스타일 구조가 바뀐 것으로 보고 경고를 찍는데, 그 경고가 props 직렬화에서
            // 무한 재귀에 빠져 그 렌더를 통째로 날려버린다(바텀시트 그림자와 같은 문제).
            style={{ opacity: blocked || device == null ? 0.4 : 1 }}
            className={`h-16 w-16 items-center justify-center rounded-full border-4 active:scale-95 ${
              isRecording ? 'border-white bg-rose-500' : 'border-white/70 bg-white/15'
            }`}
          >
            {isBusy ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <View className={isRecording ? 'h-5 w-5 rounded bg-white' : 'h-10 w-10 rounded-full bg-rose-500'} />
            )}
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}
