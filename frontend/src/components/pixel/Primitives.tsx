/**
 * 웹(CSS)에만 있던 효과를 RN에서 똑같이 보이게 해주는 공통 부품
 * - 픽셀 하드 섀도우: shadow-[3px_3px_0_#075953]
 * - 그라데이션: bg-gradient-to-*
 * - 반복 애니메이션: animate-ping / pulse / spin / bounce / float-balloon
 * - 셀렉트 박스: <select>
 */
import React, { useEffect, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  Modal,
  Pressable,
  ScrollView,
  StyleProp,
  Text,
  View,
  ViewStyle,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { cssInterop } from 'nativewind';

// LinearGradient 에서도 className 을 쓸 수 있도록 연결
cssInterop(LinearGradient, { className: 'style' });

export { LinearGradient };

/* -------------------------------------------------------------------------- */
/*                               하드 섀도우 박스                               */
/* -------------------------------------------------------------------------- */

type ShadowProps = {
  /** 그림자 색 (예: '#075953') */
  shadowColor: string;
  /** 그림자 오프셋(px) — shadow-[3px_3px_0_...] 이면 3 */
  offset?: number;
  /** 카드의 border-radius 값(px) — rounded-2xl=16, rounded-3xl=24 ... */
  radius?: number;
  /** 바깥 레이아웃용 클래스 (w-full, flex-1, mt-3 등) */
  wrapperClassName?: string;
  /** 카드 자체 클래스 (배경, 테두리, 패딩 등) */
  className?: string;
  style?: StyleProp<ViewStyle>;
  children?: React.ReactNode;
};

/** 눌리지 않는 카드형 하드 섀도우 */
export function ShadowBox({
  shadowColor,
  offset = 3,
  radius = 16,
  wrapperClassName,
  className,
  style,
  children,
}: ShadowProps) {
  return (
    <View className={wrapperClassName}>
      <View
        pointerEvents="none"
        style={{
          position: 'absolute',
          left: offset,
          top: offset,
          right: -offset,
          bottom: -offset,
          borderRadius: radius,
          backgroundColor: shadowColor,
        }}
      />
      <View className={className} style={style}>
        {children}
      </View>
    </View>
  );
}

type PixelPressableProps = Omit<ShadowProps, 'shadowColor'> & {
  shadowColor?: string;
  onPress?: () => void;
  disabled?: boolean;
  /** 비활성일 때 카드 클래스 */
  disabledClassName?: string;
  /** 눌렀을 때 효과: 'shift' = 그림자 쪽으로 이동, 'down' = 아래로 살짝, 'scale' = 작아짐 */
  pressEffect?: 'shift' | 'down' | 'scale' | 'none';
  accessibilityLabel?: string;
};

/** 웹의 active:translate-x-0.5 active:translate-y-0.5 active:shadow-none 버튼 */
export function PixelPressable({
  shadowColor,
  offset = 3,
  radius = 16,
  wrapperClassName,
  className,
  disabledClassName,
  style,
  onPress,
  disabled,
  pressEffect = 'shift',
  accessibilityLabel,
  children,
}: PixelPressableProps) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      className={wrapperClassName}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
    >
      {({ pressed }) => {
        const showShadow = !!shadowColor && !disabled && !(pressed && pressEffect === 'shift');
        let transform: ViewStyle['transform'];
        if (pressed && !disabled) {
          if (pressEffect === 'shift') transform = [{ translateX: offset - 1 }, { translateY: offset - 1 }];
          if (pressEffect === 'down') transform = [{ translateY: 2 }];
          if (pressEffect === 'scale') transform = [{ scale: 0.95 }];
        }
        return (
          <>
            {showShadow && (
              <View
                pointerEvents="none"
                style={{
                  position: 'absolute',
                  left: offset,
                  top: offset,
                  right: -offset,
                  bottom: -offset,
                  borderRadius: radius,
                  backgroundColor: shadowColor,
                }}
              />
            )}
            <View
              className={disabled && disabledClassName ? disabledClassName : className}
              style={[style, transform ? { transform } : null]}
            >
              {children}
            </View>
          </>
        );
      }}
    </Pressable>
  );
}

/* -------------------------------------------------------------------------- */
/*                                반복 애니메이션                               */
/* -------------------------------------------------------------------------- */

export type LoopKind = 'ping' | 'pulse' | 'spin' | 'bounce' | 'float' | 'bounceSmall';

/** Tailwind animate-* 와 같은 반복 애니메이션 래퍼 */
export function AnimatedLoop({
  kind,
  style,
  children,
  pointerEvents,
}: {
  kind: LoopKind;
  style?: StyleProp<ViewStyle>;
  children?: React.ReactNode;
  pointerEvents?: 'none' | 'auto' | 'box-none' | 'box-only';
}) {
  const value = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    let anim: Animated.CompositeAnimation;
    if (kind === 'spin' || kind === 'ping') {
      anim = Animated.loop(
        Animated.timing(value, {
          toValue: 1,
          duration: kind === 'spin' ? 1000 : 1000,
          easing: kind === 'spin' ? Easing.linear : Easing.bezier(0, 0, 0.2, 1),
          useNativeDriver: true,
        }),
      );
    } else {
      const duration = kind === 'pulse' ? 1000 : kind === 'float' ? 1200 : 500;
      const easing = kind === 'pulse' ? Easing.bezier(0.4, 0, 0.6, 1) : Easing.inOut(Easing.ease);
      anim = Animated.loop(
        Animated.sequence([
          Animated.timing(value, { toValue: 1, duration, easing, useNativeDriver: true }),
          Animated.timing(value, { toValue: 0, duration, easing, useNativeDriver: true }),
        ]),
      );
    }
    anim.start();
    return () => anim.stop();
  }, [kind, value]);

  let animStyle: any;
  switch (kind) {
    case 'ping':
      animStyle = {
        transform: [{ scale: value.interpolate({ inputRange: [0, 1], outputRange: [1, 2] }) }],
        opacity: value.interpolate({ inputRange: [0, 0.75, 1], outputRange: [1, 0, 0] }),
      };
      break;
    case 'pulse':
      animStyle = { opacity: value.interpolate({ inputRange: [0, 1], outputRange: [1, 0.5] }) };
      break;
    case 'spin':
      animStyle = {
        transform: [{ rotate: value.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] }) }],
      };
      break;
    case 'bounce':
      animStyle = {
        transform: [{ translateY: value.interpolate({ inputRange: [0, 1], outputRange: [0, -8] }) }],
      };
      break;
    case 'bounceSmall':
      animStyle = {
        transform: [{ translateY: value.interpolate({ inputRange: [0, 1], outputRange: [0, -4] }) }],
      };
      break;
    case 'float':
      animStyle = {
        transform: [
          { translateY: value.interpolate({ inputRange: [0, 1], outputRange: [0, -12] }) },
          { rotate: value.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '2deg'] }) },
        ],
      };
      break;
  }

  return (
    <Animated.View pointerEvents={pointerEvents} style={[style, animStyle]}>
      {children}
    </Animated.View>
  );
}

/** 처음 나타날 때 페이드 인 (animate-fadeIn / scaleUp / slideUp) */
export function FadeIn({
  children,
  style,
  from = 'fade',
}: {
  children?: React.ReactNode;
  style?: StyleProp<ViewStyle>;
  from?: 'fade' | 'up' | 'scale';
}) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.timing(v, { toValue: 1, duration: 260, easing: Easing.out(Easing.quad), useNativeDriver: true }).start();
  }, [v]);
  const transform =
    from === 'up'
      ? [{ translateY: v.interpolate({ inputRange: [0, 1], outputRange: [16, 0] }) }]
      : from === 'scale'
        ? [{ scale: v.interpolate({ inputRange: [0, 1], outputRange: [0.92, 1] }) }]
        : [];
  return <Animated.View style={[style, { opacity: v, transform }]}>{children}</Animated.View>;
}

/* -------------------------------------------------------------------------- */
/*                                  셀렉트 박스                                 */
/* -------------------------------------------------------------------------- */

export function PixelSelect({
  value,
  options,
  onChange,
  className,
  accessibilityLabel,
}: {
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
  className?: string;
  accessibilityLabel?: string;
}) {
  const [open, setOpen] = useState(false);
  const current = options.find((o) => o.value === value);

  return (
    <>
      <Pressable
        onPress={() => setOpen(true)}
        accessibilityLabel={accessibilityLabel}
        className={className}
      >
        <View className="h-9 flex-row items-center justify-between rounded-xl border border-[#075953]/30 bg-white px-2">
          <Text className="text-[10px] font-black text-slate-700" numberOfLines={1}>
            {current?.label ?? value}
          </Text>
          <Text className="text-[8px] text-slate-500">▼</Text>
        </View>
      </Pressable>

      <Modal transparent visible={open} animationType="fade" onRequestClose={() => setOpen(false)} statusBarTranslucent>
        <Pressable className="flex-1 items-center justify-center bg-slate-950/50 p-8" onPress={() => setOpen(false)}>
          <View className="w-full max-w-xs overflow-hidden rounded-2xl border-2 border-pixel-navy bg-white">
            <Text className="border-b border-slate-100 px-4 py-3 text-xs font-black text-[#075953]">
              {accessibilityLabel ?? '선택'}
            </Text>
            <ScrollView style={{ maxHeight: 320 }}>
              {options.map((o) => {
                const selected = o.value === value;
                return (
                  <Pressable
                    key={o.value}
                    onPress={() => {
                      onChange(o.value);
                      setOpen(false);
                    }}
                    className={`flex-row items-center justify-between px-4 py-3 ${selected ? 'bg-[#B4FAF0]' : 'bg-white'}`}
                  >
                    <Text className={`text-sm ${selected ? 'font-black text-[#075953]' : 'font-bold text-slate-700'}`}>
                      {o.label}
                    </Text>
                    {selected && <Text className="text-sm font-black text-[#075953]">✓</Text>}
                  </Pressable>
                );
              })}
            </ScrollView>
          </View>
        </Pressable>
      </Modal>
    </>
  );
}

/** 바텀시트/센터 모달 공통 배경 (fixed inset-0 bg-slate-950/65) */
export function ModalBackdrop({
  visible = true,
  onRequestClose,
  position = 'bottom',
  dimClassName = 'bg-slate-950/65',
  children,
}: {
  visible?: boolean;
  onRequestClose: () => void;
  position?: 'bottom' | 'center';
  dimClassName?: string;
  children?: React.ReactNode;
}) {
  return (
    <Modal transparent visible={visible} animationType="fade" onRequestClose={onRequestClose} statusBarTranslucent>
      <View className={`flex-1 p-3 ${dimClassName} ${position === 'bottom' ? 'justify-end' : 'justify-center'}`}>
        {children}
      </View>
    </Modal>
  );
}
