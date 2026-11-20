/**
 * 여행 지도 하단 조절형 바텀시트 (드래그 핸들 · 일자 필터 · 장소 타임라인)
 */
import React, { useMemo, useRef, useState } from 'react';
import {
  AccessibilityInfo,
  ActivityIndicator,
  Animated,
  type GestureResponderEvent,
  LayoutAnimation,
  PanResponder,
  Platform,
  Pressable,
  ScrollView,
  Text,
  UIManager,
  View,
} from 'react-native';

import { PlaceItem } from '../../../data/mockData';
import { AnimatedLoop, PixelPressable } from '../../../components/pixel/Primitives';

/**
 * 선택 상태에서만 주는 그림자.
 *
 * className 으로 shadow-* 를 붙였다 뗐다 하면 NativeWind 가 이미 올라간 컴포넌트를
 * "업그레이드"해야 한다고 보고 경고를 찍는데, 그 경고가 props 를 직렬화하다 무한
 * 재귀에 빠져 JS 스레드를 통째로 멈춘다. 그래서 그림자는 클래스 대신 style 로 준다.
 */
const SELECTED_SHADOW = {
  shadowColor: '#000',
  shadowOpacity: 0.12,
  shadowRadius: 4,
  shadowOffset: { width: 0, height: 2 },
  // elevation 은 쓰지 않는다. 안드로이드는 elevation 이 붙은 뷰를 따로 합성하는데,
  // 배경이 반투명하면 둥근 모서리를 무시한 네모가 카드 위에 덧그려진다.
  // 선택 상태는 테두리·배경·"선택됨" 배지로 이미 충분히 보인다.
} as const;

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

/** 시스템의 "애니메이션 줄이기" 설정. 켜져 있으면 시트 높이를 부드럽게 바꾸지 않고 바로 바꾼다. */
let reduceMotion = false;
AccessibilityInfo.isReduceMotionEnabled().then((v) => {
  reduceMotion = v;
});
AccessibilityInfo.addEventListener('reduceMotionChanged', (v) => {
  reduceMotion = v;
});

/** 다음 높이 변경을 부드럽게 이어 붙인다. 손가락을 따라 끄는 동안에는 쓰지 않는다. */
function animateNextSheetChange() {
  if (!reduceMotion) {
    LayoutAnimation.configureNext(LayoutAnimation.create(220, 'easeInEaseOut', 'opacity'));
  }
}

/**
 * 시트를 가장 크게 올렸을 때의 높이(화면 대비 %).
 *
 * 지도 위쪽의 여행 제목과 장소+ · 팀원+ · PinLog 버튼은 가리지 않아야 해서,
 * 그 줄 바로 아래까지만 올라간다.
 */
const MAX_SHEET = 86;
/** 카드 사이 간격(contentContainerStyle 의 gap) */
const CARD_GAP = 8;
/** 끌기가 시작되기까지 누르고 있어야 하는 시간 */
const DRAG_HOLD_MS = 250;

interface TripsBottomSheetProps {
  /** 바텀시트가 놓인 영역의 실제 높이(px) */
  containerHeight: number;
  sheetHeightPercent: number;
  onHeightChange: (height: number) => void;
  selectedDay: number | 'all';
  onSelectDay: (day: number | 'all') => void;
  /** 지금 고른 일자의 장소. 일자를 고르면 걸러진 목록이 온다. */
  places: PlaceItem[];
  /** 일자와 상관없는 전체 장소 수. "전체" 칩의 숫자로 쓴다. */
  totalCount: number;
  selectedPlaceId: number;
  onSelectPlace: (place: PlaceItem) => void;
  onOpenTeamRecord: (place: PlaceItem) => void;
  /** 장소 상세를 카메라 화면으로 바로 연다 */
  onOpenCamera: (place: PlaceItem) => void;
  /** 뒤에서 저장 중이거나 저장에 실패한 내 기록 (핀 ID 기준). 없으면 아무것도 띄우지 않는다. */
  recordSaveStatus?: {
    [pinId: number]: { state: 'saving'; done: number; total: number } | { state: 'failed' };
  };
  /** "저장 실패 · 다시 시도" 를 눌렀을 때 */
  onRetryRecord?: (place: PlaceItem) => void;
  teamRecordCounts: { [pinId: number]: number };
  onOptimizeRoute: () => void;
  isOptimizingRoute: boolean;
  /** 이 여행에 실제로 있는 일자 번호. 비어 있으면 일자 칩을 숨긴다. */
  dayNumbers: number[];
  isLoading: boolean;
  loadError: string | null;
  onRetry: () => void;
  onAddDay: () => void;
  isAddingDay: boolean;
  /** 일자 칩을 길게 눌렀을 때. 그 일자의 핀은 지워지지 않고 미배정으로 돌아간다. */
  onDeleteDay: (day: number) => void;
  /** 장소의 일자를 고르는 창을 연다. */
  onAssignDay: (place: PlaceItem) => void;
  onChangeCategory: (place: PlaceItem) => void;
  /** 일자 안에서 한 칸 위(-1)/아래(1)로 옮긴다. */
  onMovePlace: (placeId: number, direction: -1 | 1) => void;
  /** 끌어서 바꾼 순서. 그 일자의 핀 ID 를 새 방문 순서대로 준다. */
  onReorderPlaces: (orderedIds: number[]) => void;
  /** 여행할 시각을 고르는 창을 연다. */
  onEditTime: (place: PlaceItem) => void;
  /** 넣어둔 시각을 지운다. 시각이 있을 때만 불린다. */
  onClearTime: (place: PlaceItem) => void;
  /** 지도에서 장소를 아주 지운다. 확인은 부모가 받는다. */
  onDeletePlace: (place: PlaceItem) => void;
}

export default function TripsBottomSheet({
  containerHeight,
  sheetHeightPercent,
  onHeightChange,
  selectedDay,
  onSelectDay,
  places,
  totalCount,
  selectedPlaceId,
  onSelectPlace,
  onOpenTeamRecord,
  onOpenCamera,
  recordSaveStatus = {},
  onRetryRecord,
  teamRecordCounts,
  onOptimizeRoute,
  isOptimizingRoute,
  dayNumbers,
  isLoading,
  loadError,
  onRetry,
  onAddDay,
  isAddingDay,
  onDeleteDay,
  onAssignDay,
  onChangeCategory,
  onMovePlace,
  onReorderPlaces,
  onEditTime,
  onClearTime,
  onDeletePlace,
}: TripsBottomSheetProps) {
  // PanResponder 콜백 안에서 최신 값을 읽기 위한 ref
  const heightRef = useRef(sheetHeightPercent);
  heightRef.current = sheetHeightPercent;
  const startHeightRef = useRef(sheetHeightPercent);
  const containerRef = useRef(containerHeight);
  containerRef.current = containerHeight;
  const onChangeRef = useRef(onHeightChange);
  onChangeRef.current = onHeightChange;

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        onPanResponderGrant: () => {
          startHeightRef.current = heightRef.current;
        },
        onPanResponderMove: (_, g) => {
          const base = containerRef.current || 800;
          const deltaPercent = (-g.dy / base) * 100; // 위로 끌면 +
          const next = Math.min(MAX_SHEET, Math.max(22, startHeightRef.current + deltaPercent));
          onChangeRef.current(next);
        },
        onPanResponderRelease: () => {
          // 가까운 단계로 스냅: 접힘(25%) / 중간(50%) / 최대(MAX_SHEET)
          const h = heightRef.current;
          animateNextSheetChange();
          if (h < 36) {onChangeRef.current(25);}
          else if (h > 62) {onChangeRef.current(MAX_SHEET);}
          else {onChangeRef.current(50);}
        },
        onPanResponderTerminationRequest: () => false,
      }),
    [],
  );

  // ---- 종류별 묶음("전체" 탭) ----
  // 서버가 준 category/categoryLabel/categoryOrder 만 쓴다. 값이 없는 장소는 기타(ETC)로 모은다.
  // 비어 있는 종류는 만들어지지 않고, 기타는 항상 맨 뒤에 온다.
  const categoryGroups = useMemo(() => {
    const map = new Map<string, { code: string; label: string; order: number; places: PlaceItem[] }>();
    places.forEach((p) => {
      const code = p.categoryCode ?? 'ETC';
      const group = map.get(code) ?? {
        code,
        label: p.categoryLabel ?? '기타',
        order: code === 'ETC' ? Number.MAX_SAFE_INTEGER : (p.categoryOrder ?? Number.MAX_SAFE_INTEGER - 1),
        places: [],
      };
      group.places.push(p);
      map.set(code, group);
    });
    return [...map.values()].sort((a, b) => a.order - b.order);
  }, [places]);

  /**
   * 접어 둔 종류. 기본은 모두 펼쳐진 상태라 접은 것만 기억한다.
   * 새로 생긴 종류가 저절로 접혀 있지 않게 하려는 것이다.
   */
  const [collapsedCategories, setCollapsedCategories] = useState<string[]>([]);
  const toggleCategory = (code: string) =>
    setCollapsedCategories((cur) => (cur.includes(code) ? cur.filter((c) => c !== code) : [...cur, code]));

  // ---- 길게 눌러 끌어서 순서 바꾸기 (같은 일자 안) ----
  const placesRef = useRef(places);
  placesRef.current = places;
  const onReorderRef = useRef(onReorderPlaces);
  onReorderRef.current = onReorderPlaces;
  const cardHeights = useRef<Record<number, number>>({});
  const dragY = useRef(new Animated.Value(0)).current;
  /** 카드마다 아래로/위로 비켜 서는 거리 */
  const shifts = useRef<Record<number, Animated.Value>>({});
  const shiftOf = (id: number) => (shifts.current[id] ??= new Animated.Value(0));
  const [draggingId, setDraggingId] = useState<number | null>(null);
  const drag = useRef<{ id: number; from: number; over: number; active: boolean; timer?: ReturnType<typeof setTimeout> }>({
    id: -1,
    from: 0,
    over: 0,
    active: false,
  });
  const settleShifts = (list: PlaceItem[], from: number, over: number) => {
    const moved = cardHeights.current[list[from]?.id] ?? 0;
    list.forEach((p, i) => {
      if (i === from) {return;}
      let to = 0;
      if (from < over && i > from && i <= over) {to = -(moved + CARD_GAP);}
      if (from > over && i >= over && i < from) {to = moved + CARD_GAP;}
      Animated.timing(shiftOf(p.id), { toValue: to, duration: 140, useNativeDriver: true }).start();
    });
  };

  const endDrag = (commit: boolean) => {
    const d = drag.current;
    if (d.timer) {clearTimeout(d.timer);}
    const list = placesRef.current;
    if (d.active) {
      if (commit && d.over !== d.from) {
        const ids = list.map((p) => p.id);
        const [moved] = ids.splice(d.from, 1);
        ids.splice(d.over, 0, moved);
        onReorderRef.current(ids);
      }
      dragY.setValue(0);
      list.forEach((p) => shiftOf(p.id).setValue(0));
    }
    drag.current = { id: -1, from: 0, over: 0, active: false };
    setDraggingId(null);
  };

  /** 끌기가 시작될 때 카드가 떠오르는 정도(0~1) */
  const lift = useRef(new Animated.Value(0)).current;
  /** 길게 눌러 끌기를 시작한 직후에는 손을 뗄 때 카드 탭(상세 진입)이 일어나지 않게 한다. */
  const suppressPress = useRef(false);
  const touchStart = useRef({ x: 0, y: 0 });

  /**
   * 카드 전체에 거는 손짓. 카드를 누르고 있어도(DRAG_HOLD_MS) 거의 움직이지 않으면 끌기가 시작된다.
   * 그 전에 손가락이 움직이면(스크롤) 끌기는 시작하지 않고, 평소 탭·스크롤이 그대로 동작한다.
   * 시작되기 전에는 응답자를 가로채지 않아서 카드 안의 버튼(시각·일자·삭제·기록)도 그대로 눌린다.
   */
  const touchHandlersFor = (id: number) => ({
    onTouchStart: (e: GestureResponderEvent) => {
      touchStart.current = { x: e.nativeEvent.pageX, y: e.nativeEvent.pageY };
      const list = placesRef.current;
      const from = list.findIndex((p) => p.id === id);
      if (drag.current.timer) {clearTimeout(drag.current.timer);}
      drag.current = { id, from, over: from, active: false };
      drag.current.timer = setTimeout(() => {
        if (drag.current.id !== id) {return;}
        drag.current.active = true;
        // 이 순간의 스크롤을 기준으로 삼는다. 이후 흐른 만큼이 카드 이동에 더해진다.
        scrollAtDragStart.current = scrollY.current;
        lastDy.current = 0;
        suppressPress.current = true;
        lift.setValue(0);
        Animated.spring(lift, { toValue: 1, useNativeDriver: true, friction: 7 }).start();
        setDraggingId(id);
      }, DRAG_HOLD_MS);
    },
    onTouchMove: (e: GestureResponderEvent) => {
      const d = drag.current;
      if (d.id !== id || d.active) {return;}
      const dx = Math.abs(e.nativeEvent.pageX - touchStart.current.x);
      const dy = Math.abs(e.nativeEvent.pageY - touchStart.current.y);
      // 시작 전에 움직였다면 스크롤이다. 끌기를 포기한다.
      if (dx > 8 || dy > 8) {
        if (d.timer) {clearTimeout(d.timer);}
        drag.current = { id: -1, from: 0, over: 0, active: false };
      }
    },
    onTouchEnd: () => {
      // 끌기를 시작했지만 움직이지 않고 뗀 경우(응답자를 못 잡았을 때)도 정리한다.
      if (drag.current.id === id) {endDrag(drag.current.active);}
      setTimeout(() => {
        suppressPress.current = false;
      }, 50);
    },
    onTouchCancel: () => {
      if (drag.current.id === id) {endDrag(false);}
      suppressPress.current = false;
    },
  });

  /* ---- 끌고 가다 목록 끝에 닿으면 목록도 같이 흐르게 한다 ----
   *
   * 끄는 동안에는 목록 스크롤을 꺼 두기 때문에(손가락 하나로 둘 다 할 수는 없다)
   * 화면 밖으로는 옮길 수 없었다. 그래서 손가락이 위아래 가장자리에 닿아 있으면
   * 앱이 대신 조금씩 스크롤해 준다.
   *
   * 스크롤이 움직이면 카드도 그만큼 따라가야 손가락 아래에 그대로 머문다. 그래서
   * 자리 계산에는 손가락이 움직인 거리(g.dy)에 그동안 흐른 스크롤을 더해서 쓴다.
   */
  const listRef = useRef<ScrollView>(null);
  const listHeight = useRef(0);
  const scrollY = useRef(0);
  const maxScrollY = useRef(0);
  /** 끌기를 시작한 순간의 스크롤 위치 */
  const scrollAtDragStart = useRef(0);
  const autoScroll = useRef<ReturnType<typeof setInterval> | null>(null);
  /** 가장자리에서 자동 스크롤이 시작되는 거리와 한 번에 흐르는 양 */
  const EDGE = 72;
  const STEP = 14;

  const stopAutoScroll = () => {
    if (autoScroll.current) {
      clearInterval(autoScroll.current);
      autoScroll.current = null;
    }
  };

  /** 마지막으로 본 손가락 위치. 자동 스크롤 중에는 손이 멈춰 있어도 자리를 다시 계산해야 한다. */
  const lastMoveY = useRef(0);
  const lastDy = useRef(0);
  /** 목록의 화면상 위쪽 끝. 가장자리 판단에 쓴다. */
  const listTop = useRef(0);

  /** 손가락이 움직인 거리에 그동안 흐른 스크롤을 더해 카드 위치와 들어갈 자리를 정한다. */
  const applyDrag = () => {
    const d = drag.current;
    if (!d.active) {return;}
    const offset = lastDy.current + (scrollY.current - scrollAtDragStart.current);
    dragY.setValue(offset);
    const list = placesRef.current;
    let over = d.from;
    let acc = 0;
    if (offset > 0) {
      for (let i = d.from + 1; i < list.length; i++) {
        const h = (cardHeights.current[list[i].id] ?? 0) + CARD_GAP;
        if (offset > acc + h / 2) {over = i; acc += h;} else {break;}
      }
    } else {
      for (let i = d.from - 1; i >= 0; i--) {
        const h = (cardHeights.current[list[i].id] ?? 0) + CARD_GAP;
        if (-offset > acc + h / 2) {over = i; acc += h;} else {break;}
      }
    }
    if (over !== d.over) {
      d.over = over;
      settleShifts(list, d.from, over);
    }
  };

  /** 손가락이 위아래 가장자리에 있으면 목록을 조금씩 흘린다. 벗어나면 멈춘다. */
  const startOrStopAutoScroll = (moveY: number) => {
    const top = listTop.current;
    const bottom = top + listHeight.current;
    const up = moveY < top + EDGE;
    const down = moveY > bottom - EDGE;
    if (!up && !down) {
      stopAutoScroll();
      return;
    }
    if (autoScroll.current) {return;}
    autoScroll.current = setInterval(() => {
      if (!drag.current.active) {
        stopAutoScroll();
        return;
      }
      const goingUp = lastMoveY.current < listTop.current + EDGE;
      const next = goingUp ? scrollY.current - STEP : scrollY.current + STEP;
      const clamped = Math.max(0, Math.min(maxScrollY.current, next));
      if (clamped === scrollY.current) {
        stopAutoScroll();
        return;
      }
      scrollY.current = clamped;
      listRef.current?.scrollTo({ y: clamped, animated: false });
      applyDrag();
    }, 16);
  };

  const grips = useRef<Record<number, ReturnType<typeof PanResponder.create>>>({});
  const gripFor = (id: number) => {
    if (grips.current[id]) {return grips.current[id];}
    grips.current[id] = PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      // 끌기가 시작된 뒤의 움직임만 가로챈다(카드 안 버튼과 스크롤보다 먼저).
      onMoveShouldSetPanResponderCapture: () => drag.current.id === id && drag.current.active,
      onPanResponderTerminationRequest: () => false,
      onPanResponderMove: (_, g) => {
        const d = drag.current;
        if (!d.active) {return;}
        lastMoveY.current = g.moveY;
        lastDy.current = g.dy;
        applyDrag();
        startOrStopAutoScroll(g.moveY);
      },
      onPanResponderRelease: () => { stopAutoScroll(); endDrag(true); },
      onPanResponderTerminate: () => { stopAutoScroll(); endDrag(false); },
    });
    return grips.current[id];
  };

  /** 카드에 입히는 이동. 끄는 카드는 손가락을 따라가고, 나머지는 자리를 비켜 준다. */
  const cardStyle = (id: number) =>
    id === draggingId
      ? ({
          zIndex: 50,
          transform: [
            { translateY: dragY },
            { scale: lift.interpolate({ inputRange: [0, 1], outputRange: [1, 1.03] }) },
          ],
          ...SELECTED_SHADOW,
          shadowOpacity: 0.28,
          shadowRadius: 8,
        } as const)
      : ({ transform: [{ translateY: shiftOf(id) }] } as const);

  /** 장소 카드 한 장. 일자별 목록과 "전체"의 종류별 목록이 같이 쓴다. */
  const renderPlace = (place: PlaceItem) => {
            const isSelected = selectedPlaceId === place.id;
            const saveStatus = recordSaveStatus[place.id];
            const recordsCount = teamRecordCounts[place.id] || 0;

            return (
              <Animated.View
                key={place.id}
                onLayout={(e) => {
                  cardHeights.current[place.id] = e.nativeEvent.layout.height;
                }}
                style={cardStyle(place.id)}
                {...(selectedDay !== 'all' ? { ...gripFor(place.id).panHandlers, ...touchHandlersFor(place.id) } : {})}
              >
                <Pressable
                  onPress={() => {
                    if (suppressPress.current) {return;}
                    onSelectPlace(place);
                  }}
                  accessibilityActions={
                    selectedDay !== 'all'
                      ? [
                          { name: 'decrement', label: '위로 옮기기' },
                          { name: 'increment', label: '아래로 옮기기' },
                        ]
                      : undefined
                  }
                  onAccessibilityAction={(e) => {
                    if (e.nativeEvent.actionName === 'decrement') {onMovePlace(place.id, -1);}
                    if (e.nativeEvent.actionName === 'increment') {onMovePlace(place.id, 1);}
                  }}
                  style={[
                    isSelected ? SELECTED_SHADOW : undefined,
                    // 들어 올린 카드는 배경이 살짝 짙어진다. 클래스가 아니라 style 로 준다.
                    draggingId === place.id ? { backgroundColor: '#D3F6EE' } : undefined,
                  ]}
                  className={`flex-row items-center justify-between gap-3 rounded-2xl border-2 p-3 ${
                    isSelected ? 'border-pixel-navy bg-[#B4FAF0]' : 'border-slate-200 bg-white'
                  }`}
                >
                  <View className="min-w-0 flex-1 flex-row items-center gap-3">
                    {/* 순번 픽셀 뱃지 */}
                    {isSelected ? (
                      <AnimatedLoop kind="bounceSmall">
                        <View className="h-7 w-7 items-center justify-center rounded-full border-2 border-pixel-navy bg-[#FF5A87]">
                          <Text className="font-pixel text-xs text-white">{place.order}</Text>
                        </View>
                      </AnimatedLoop>
                    ) : (
                      <View className="h-7 w-7 items-center justify-center rounded-full border-2 border-pixel-navy bg-pixel-navy">
                        <Text className="font-pixel text-xs text-white">{place.order}</Text>
                      </View>
                    )}

                    {/* 장소명 · 주소 · 시각 · 배지 (썸네일을 뺀 만큼 글자 영역을 넓게 쓴다) */}
                    <View className="min-w-0 flex-1">
                      <Text numberOfLines={1} className="text-xs font-extrabold leading-4 text-slate-900">
                        {place.name}
                      </Text>
                      {place.address ? (
                        <Text numberOfLines={1} className="mt-0.5 text-[10px] font-semibold text-slate-500">
                          {place.address}
                        </Text>
                      ) : null}

                      {/* 여행할 시각. 누르면 고치고, 길게 누르면 지운다. */}
                      <Pressable
                        onPress={() => onEditTime(place)}
                        onLongPress={place.time ? () => onClearTime(place) : undefined}
                        accessibilityLabel={`${place.name} 시각 ${
                          place.time ? `${place.time}. 길게 누르면 지움` : '추가'
                        }`}
                        className="mt-1 flex-row items-center gap-1 self-start"
                      >
                        <Text className="text-[10px]">🕑</Text>
                        <Text
                          className={`font-pixel text-[10px] ${place.time ? 'text-[#075953]' : 'text-slate-400'}`}
                        >
                          {place.time ?? '시각 추가'}
                        </Text>
                      </Pressable>

                      <View className="mt-1.5 flex-row flex-wrap items-center gap-1.5">
                        <Text className="font-pixel text-[9px] text-amber-500">★ {place.rating}</Text>
                        {/* 날짜를 안 정한 핀은 day 가 0 이라 "0day" 로 보이면 안 된다. */}
                        <Pressable
                          onPress={() => onAssignDay(place)}
                          accessibilityLabel={`${place.name} 일자 지정`}
                          className={`rounded px-1.5 py-0.5 ${place.day > 0 ? 'bg-[#B4FAF0]' : 'border border-dashed border-slate-400 bg-white'}`}
                        >
                          <Text className="text-[9px] font-bold text-teal-800">
                            {place.day > 0 ? `${place.day}day` : '일자 지정'}
                          </Text>
                        </Pressable>
                        {/* 분류는 "전체" 탭에서 핀을 묶는 기준이라 여기서 바로 바꿀 수 있게 한다. */}
                        <Pressable
                          onPress={() => onChangeCategory(place)}
                          accessibilityLabel={`${place.name} 장소 종류 바꾸기`}
                          className="rounded border border-[#82F0DC] bg-white px-1.5 py-0.5"
                        >
                          <Text className="text-[9px] font-bold text-teal-800">
                            {place.categoryLabel ?? '분류'}
                            {place.categoryOverridden ? ' ✎' : ''}
                          </Text>
                        </Pressable>
                        {isSelected && (
                          <View className="rounded border border-rose-200 bg-rose-50 px-1.5 py-0.5">
                            <Text className="text-[8px] font-black text-rose-600">선택됨 ✓</Text>
                          </View>
                        )}
                      </View>
                    </View>
                  </View>

                  {/* 팀 기록 · 바로 촬영 */}
                  <View className="items-end gap-1">
                    <View className="flex-row items-center gap-1">
                      {/* 장소 삭제. 되돌릴 수 없어서 누르면 확인을 한 번 받는다. */}
                      <Pressable
                        onPress={() => onDeletePlace(place)}
                        accessibilityLabel={`${place.name} 삭제`}
                        hitSlop={6}
                        className="h-7 w-7 items-center justify-center rounded-xl border border-rose-300 bg-white active:bg-rose-50"
                      >
                        <Text className="text-[11px] font-black text-rose-500">✕</Text>
                      </Pressable>
                      {/* 기록이 있든 없든 같은 크기. 개수는 같은 자리 안에서만 바뀐다. */}
                      <Pressable
                        onPress={() => onOpenTeamRecord(place)}
                        className="h-8 w-[72px] flex-row items-center justify-center gap-1 rounded-xl border border-teal-600 bg-white active:translate-y-0.5 active:bg-teal-50"
                      >
                        <Text className="text-[10px] font-black text-teal-900">👥 기록</Text>
                        {recordsCount > 0 && (
                          <View className="rounded-full bg-teal-700 px-1">
                            <Text className="font-pixel text-[8px] text-white">{recordsCount}</Text>
                          </View>
                        )}
                      </Pressable>
                    </View>
                    {/* 내 기록 저장 상태: 실패하면 다시 시도, 저장 중이면 진행 상황 */}
                    {saveStatus?.state === 'failed' && (
                      <Pressable
                        onPress={() => onRetryRecord?.(place)}
                        accessibilityLabel={`${place.name} 기록 저장 실패, 다시 시도`}
                        hitSlop={4}
                        className="rounded-xl border border-rose-400 bg-rose-50 px-2 py-1 active:bg-rose-100"
                      >
                        <Text className="text-[9px] font-black text-rose-600">저장 실패 · 다시 시도</Text>
                      </Pressable>
                    )}
                    {saveStatus?.state === 'saving' && (
                      <View className="rounded-xl border border-teal-300 bg-teal-50 px-2 py-1">
                        <Text className="text-[9px] font-black text-teal-800">
                          {saveStatus.total > 0 ? `업로드 중… ${saveStatus.done}/${saveStatus.total}` : '저장 중…'}
                        </Text>
                      </View>
                    )}
                    <PixelPressable
                      shadowColor="#075953"
                      offset={2}
                      radius={12}
                      accessibilityLabel={`${place.name}에서 바로 촬영`}
                      wrapperClassName="w-[104px]"
                      className="h-8 w-[104px] items-center justify-center rounded-xl border-2 border-pixel-navy bg-rose-500"
                      onPress={() => onOpenCamera(place)}
                    >
                      <Text className="text-[9px] font-black text-white">● 바로 촬영</Text>
                    </PixelPressable>
                  </View>
                </Pressable>
              </Animated.View>
            );
  };

  return (
    <View
      className="absolute bottom-0 left-0 right-0 z-30 overflow-hidden rounded-t-[32px] border-x-3 border-t-3 border-pixel-navy bg-white"
      style={{
        height: `${sheetHeightPercent}%`,
        elevation: 16,
        shadowColor: '#000',
        shadowOpacity: 0.18,
        shadowRadius: 15,
        shadowOffset: { width: 0, height: -8 },
      }}
    >
      {/* 1. 드래그 핸들 */}
      <View {...panResponder.panHandlers} className="w-full items-center justify-center pb-2 pt-3">
        <View className="mb-1.5 h-1.5 w-14 rounded-full bg-slate-300" />
        <View className="flex-row items-center gap-1.5">
          <Text className="font-pixel text-[8px] text-teal-700">DRAG SHEET</Text>
          <Text className="text-[10px] font-bold text-slate-400">·</Text>
          <Text className="text-[10px] font-bold text-slate-400">
            {sheetHeightPercent <= 30
              ? '접힘 (올려서 일정 보기)'
              : sheetHeightPercent >= 70
                ? '최대 확장 (전체 일정 모드)'
                : '중간 (지도 & 일정 동시 보기)'}
          </Text>
        </View>
      </View>

      {/* 2. 일자 탭 */}
      <View className="flex-row items-center justify-between border-b border-slate-100 bg-white px-4 py-2">
        {/*
          칩은 하나하나 shrink-0 이어야 한다. 가로 ScrollView 안이라도 기본값(shrink:1)이면
          공간이 모자랄 때 눌려서 글자가 잘린다("1day" 가 잘려 보이던 원인).
        */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} className="flex-1" contentContainerStyle={{ gap: 8, paddingVertical: 2 }}>
          <Pressable
            onPress={() => onSelectDay('all')}
            style={selectedDay === 'all' ? SELECTED_SHADOW : undefined}
            className={`shrink-0 flex-row items-center gap-1 rounded-full border-2 px-3 py-1.5 ${
              selectedDay === 'all' ? 'border-pixel-navy bg-mint-primary' : 'border-slate-200 bg-slate-100'
            }`}
          >
            <Text className={`text-xs font-black ${selectedDay === 'all' ? 'text-pixel-navy' : 'text-slate-600'}`}>전체</Text>
            <View className="rounded-full bg-white/70 px-1">
              <Text className="font-pixel text-[9px] text-slate-700">{totalCount}</Text>
            </View>
          </Pressable>

          {dayNumbers.map((d) => (
            <Pressable
              key={d}
              onPress={() => onSelectDay(d)}
              onLongPress={() => onDeleteDay(d)}
              accessibilityLabel={`${d}일차. 길게 누르면 삭제`}
              style={selectedDay === d ? SELECTED_SHADOW : undefined}
              className={`shrink-0 flex-row items-center gap-1 rounded-full border-2 px-3 py-1.5 ${
                selectedDay === d ? 'border-pixel-navy bg-mint-primary' : 'border-slate-200 bg-slate-100'
              }`}
            >
              <Text className={`font-pixel text-[10px] ${selectedDay === d ? 'text-pixel-navy' : 'text-slate-600'}`}>{d}day</Text>
              {/* 눌러서 지우는 버튼. 전에는 그림일 뿐이라 눌러도 일자가 골라지기만 했다. */}
              {selectedDay === d && (
                <Pressable
                  onPress={() => onDeleteDay(d)}
                  accessibilityLabel={`${d}일차 삭제`}
                  hitSlop={8}
                  className="active:opacity-50"
                >
                  <Text className="text-[10px] font-black text-pixel-navy/50">✕</Text>
                </Pressable>
              )}
            </Pressable>
          ))}

          {/* 일자가 하나도 없으면 날짜별 보기도, 동선 최적화도 할 수 없다. 항상 맨 뒤에 둔다. */}
          <Pressable
            onPress={onAddDay}
            disabled={isAddingDay}
            accessibilityLabel="일자 추가"
            className="shrink-0 flex-row items-center gap-1 rounded-full border-2 border-dashed border-[#075953] bg-white px-3 py-1.5 active:scale-95"
          >
            {isAddingDay ? (
              <ActivityIndicator size="small" color="#075953" />
            ) : (
              <Text className="text-xs font-black text-[#075953]">+ 일자</Text>
            )}
          </Pressable>
        </ScrollView>

        {/* 왼쪽 일자 칩 목록에 밀려 글자가 잘리지 않도록 줄어들지 않게 둔다. */}
        <View className="shrink-0 flex-row items-center gap-1 pl-2">
          <Pressable
            onPress={onOptimizeRoute}
            disabled={isOptimizingRoute}
            // 조건부 className 으로 스타일 구조(opacity 유무)를 바꾸면 NativeWind 가
            // 그 렌더를 날려버린다. 흐리게 하는 건 style 로 준다.
            style={{ opacity: isOptimizingRoute ? 0.6 : 1 }}
            className="h-8 shrink-0 items-center justify-center rounded-lg border-2 border-pixel-navy bg-[#56EADD] px-2.5"
          >
            <Text numberOfLines={1} className="text-[9px] font-black text-pixel-navy">
              {isOptimizingRoute ? '최적화 중...' : '🪄 AI 동선 최적화'}
            </Text>
          </Pressable>
          <Pressable
            onPress={() => {
              animateNextSheetChange();
              onHeightChange(sheetHeightPercent > 50 ? 25 : MAX_SHEET);
            }}
            accessibilityLabel="높이 전환"
            className="h-7 w-7 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 active:bg-slate-100"
          >
            <Text className="text-xs font-black text-slate-600">{sheetHeightPercent > 50 ? '▼' : '▲'}</Text>
          </Pressable>
        </View>
      </View>

      {/* 3. 장소 타임라인 */}
      <ScrollView
        ref={listRef}
        className="flex-1"
        scrollEnabled={draggingId === null}
        onLayout={(e) => {
          listHeight.current = e.nativeEvent.layout.height;
          // 가장자리 판단은 화면 기준 좌표(g.moveY)와 견주므로 화면에서의 위치를 재둔다.
          e.currentTarget?.measureInWindow?.((_x, y) => {
            listTop.current = y;
          });
        }}
        onScroll={(e) => {
          scrollY.current = e.nativeEvent.contentOffset.y;
          maxScrollY.current = Math.max(
            0,
            e.nativeEvent.contentSize.height - e.nativeEvent.layoutMeasurement.height,
          );
        }}
        scrollEventThrottle={16}
        contentContainerStyle={{ paddingHorizontal: 16, paddingVertical: 12, gap: CARD_GAP }}
        showsVerticalScrollIndicator={false}
      >
        {isLoading ? (
          <View className="items-center rounded-2xl border-2 border-dashed border-slate-200 p-8">
            <ActivityIndicator color="#075953" />
          </View>
        ) : loadError ? (
          <View className="items-center gap-3 rounded-2xl border-2 border-dashed border-slate-200 p-8">
            <Text className="text-center text-xs text-slate-500">{loadError}</Text>
            <Pressable onPress={onRetry} className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95">
              <Text className="text-xs font-black text-[#075953]">다시 시도</Text>
            </Pressable>
          </View>
        ) : places.length === 0 ? (
          <View className="rounded-2xl border-2 border-dashed border-slate-200 p-8">
            <Text className="text-center text-xs text-slate-400">해당 일자에 등록된 장소가 없습니다.</Text>
          </View>
        ) : (
          selectedDay === 'all'
            ? categoryGroups.map((group) => {
                const isOpen = !collapsedCategories.includes(group.code);
                return (
                  <View key={group.code} className="gap-2">
                    {/* 제목을 누르면 그 종류의 장소가 접혔다 펴진다. */}
                    <Pressable
                      onPress={() => toggleCategory(group.code)}
                      accessibilityLabel={`${group.label} ${isOpen ? '접기' : '펼치기'}`}
                      className="flex-row items-center gap-1.5 px-1 pt-1 active:opacity-60"
                    >
                      <Text className="text-xs font-black text-[#075953]">{group.label}</Text>
                      <View className="rounded-full bg-[#B4FAF0] px-1.5 py-0.5">
                        <Text className="font-pixel text-[9px] text-[#075953]">{group.places.length}곳</Text>
                      </View>
                      <Text className="font-pixel text-[9px] text-[#075953]">{isOpen ? '▾' : '▸'}</Text>
                    </Pressable>
                    {isOpen ? group.places.map(renderPlace) : null}
                  </View>
                );
              })
            : places.map(renderPlace)
        )}
      </ScrollView>
    </View>
  );
}
