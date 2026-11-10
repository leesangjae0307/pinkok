/**
 * 장소 기록용 짧은 영상 촬영.
 *
 * PinLog 는 장소별 클립을 동선 순서대로 이어붙이는 것이라 클립이 길면 안 된다.
 * 2초가 되면 자동으로 멈추고, 1초가 안 되면 쓰지 않고 다시 찍게 한다.
 */
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Linking, Modal, PermissionsAndroid, Platform, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  Camera,
  useCameraDevice,
  useCameraFormat,
  type Orientation,
  type VideoFile,
} from 'react-native-vision-camera';

/**
 * 너무 짧으면 이어붙여도 보이지 않는다.
 * 최대가 2초라 최소는 그보다 낮아야 한다(같으면 자동 정지 시점의 오차 때문에
 * 멀쩡히 찍은 클립도 "짧다"며 버려진다).
 */
const MIN_MS = 1000;
/** 이 길이가 되면 알아서 멈춘다. */
const MAX_MS = 2000;

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
  /**
   * PinLog 는 1280×720 가로 영상으로 합성되므로, 센서도 같은 비율로 담는다.
   * 미리보기만 16:9 로 보이게 하는 게 아니라 저장되는 파일 자체가 16:9 가 된다.
   */
  const preferredFormat = useCameraFormat(device, [
    { videoAspectRatio: 16 / 9 },
    { videoResolution: { width: 1280, height: 720 } },
  ]);
  /**
   * 기기에 따라 16:9 포맷과 미리보기를 한 세션에 같이 올리지 못하는 경우가 있다
   * (session/invalid-output-configuration). 그때는 기본 포맷으로 물러나
   * 적어도 촬영은 되게 한다.
   */
  const [useWideFormat, setUseWideFormat] = useState(true);
  const format = useWideFormat ? preferredFormat : undefined;
  /** 휴대폰을 어느 방향으로 들고 있는지. 가로 촬영을 안내하는 데 쓴다. */
  const [deviceOrientation, setDeviceOrientation] = useState<Orientation>('portrait');
  const isLandscape = deviceOrientation === 'landscape-left' || deviceOrientation === 'landscape-right';

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

  /** 2초가 되면 알아서 멈춘다. */
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
          setNotice('1초보다 짧아요. 조금만 더 길게 찍어주세요.');
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
                format={format}
                isActive
                video
                audio
                /*
                 * 'device' 는 화면이 세로로 고정돼 있어도 휴대폰을 돌린 방향 그대로
                 * 저장한다. 그래서 가로로 들고 찍으면 진짜 가로 영상이 나온다.
                 * 'preview' 로 두면 화면을 따라가 세로로만 저장된다.
                 */
                outputOrientation="device"
                onOutputOrientationChanged={setDeviceOrientation}
                onError={(e) => {
                  // 16:9 를 못 올리는 기기면 기본 포맷으로 한 번 더 시도한다.
                  if (useWideFormat && e.code === 'session/invalid-output-configuration') {
                    setUseWideFormat(false);
                    return;
                  }
                  setNotice('카메라를 열지 못했어요. 창을 닫았다가 다시 열어 주세요.');
                }}
                /*
                 * 미리보기는 기본값(cover)으로 화면을 채운다. contain 으로 바꾸면
                 * 담기는 16:9 가 그대로 보이는 대신 미리보기가 네모로 떠서
                 * 화면 가운데에 상자가 생긴 것처럼 보인다. 저장되는 파일은
                 * format 이 정하므로 미리보기 방식과 상관없이 16:9 다.
                 */
                style={{ flex: 1 }}
              />
              {/* 진행 막대 — 2초가 가득 */}
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
          {/*
            PinLog 는 가로형이라 세로로 찍으면 양옆이 비어 보인다. 화면은 세로로
            고정돼 있으니(버튼이 움직이지 않게) 들고 있는 방향만 보고 안내한다.
          */}
          <Text className="text-center text-[11px] font-bold text-[#56EADD]">
            {isLandscape ? '↔ 가로로 촬영 중이에요' : '📱 휴대폰을 옆으로 돌려 가로로 찍어주세요'}
          </Text>
          <Text className="font-pixel text-[10px] text-white/70">
            {isRecording ? `${(elapsed / 1000).toFixed(1)}초 / 2초` : '최대 2초로 짧게 찍어주세요'}
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
