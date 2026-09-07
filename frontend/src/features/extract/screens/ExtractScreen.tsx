import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Card from '../../../components/common/Card';
import Button from '../../../components/common/Button';
import Badge from '../../../components/ui/Badge/Badge';
import Modal from '../../../components/common/Modal';
import PixelIcon from '../../../components/ui/PixelIcon/PixelIcon';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../../theme';

type ScanStatus = 'idle' | 'scanning' | 'done';

interface ExtractedPlace {
  id: string;
  name: string;
  address: string;
  category: string;
}

interface TripOption {
  id: string;
  title: string;
  date: string;
}

// TODO: 실제 AI 추출 연동 전까지 쓰는 목업 데이터
const MOCK_PLACES: ExtractedPlace[] = [
  { id: '1', name: '카멜리아힐', address: '제주 서귀포시 안덕면 병악로 166', category: '#명소' },
  { id: '2', name: '협재 해수욕장', address: '제주 제주시 한림읍 협재리', category: '#해변' },
  { id: '3', name: '오설록 티뮤지엄', address: '제주 서귀포시 안덕면 신화역사로 15', category: '#카페' },
];

const MOCK_TRIPS: TripOption[] = [
  { id: '1', title: '제주도 여행', date: '2025.08.01 ~ 08.05' },
  { id: '2', title: '도쿄 여행', date: '2025.07.10 ~ 07.14' },
  { id: '3', title: '유럽 배낭여행', date: '2024.12.20 ~ 2025.01.05' },
];

const SCAN_DURATION_MS = 1400;

/**
 * 추출 탭 (스포이드 · 돋보기 기능)
 * 실제 이미지 업로드/AI 추출 연동 전까지는, 업로드 박스를 탭하면
 * 스캔 애니메이션 → 목업 결과가 뜨는 흐름만 프런트에서 시뮬레이션해요.
 */
const ExtractScreen = () => {
  const [status, setStatus] = useState<ScanStatus>('idle');
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [selectedTripId, setSelectedTripId] = useState(MOCK_TRIPS[0].id);
  const [pickerVisible, setPickerVisible] = useState(false);

  const pulse = useRef(new Animated.Value(0.4)).current;
  const scanTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (status !== 'scanning') {
      return;
    }
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 500, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0.4, duration: 500, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [status, pulse]);

  useEffect(() => {
    return () => {
      if (scanTimer.current) {
        clearTimeout(scanTimer.current);
      }
    };
  }, []);

  const startScan = () => {
    setStatus('scanning');
    scanTimer.current = setTimeout(() => {
      setStatus('done');
      const initialChecked: Record<string, boolean> = {};
      MOCK_PLACES.forEach((place) => {
        initialChecked[place.id] = true;
      });
      setChecked(initialChecked);
    }, SCAN_DURATION_MS);
  };

  const resetScan = () => {
    if (scanTimer.current) {
      clearTimeout(scanTimer.current);
    }
    setStatus('idle');
    setChecked({});
  };

  const toggleChecked = (id: string) => {
    setChecked((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const selectedTrip = useMemo(
    () => MOCK_TRIPS.find((trip) => trip.id === selectedTripId) ?? MOCK_TRIPS[0],
    [selectedTripId],
  );

  const selectedCount = Object.values(checked).filter(Boolean).length;
  const canSend = status === 'done' && selectedCount > 0;

  return (
    <ScreenLayout style={styles.screenPadding}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>장소 추출하기</Text>
        <Text style={styles.subtitle}>
          스크린샷을 올려주시면 AI가 장소를 자동으로 찾아서{'\n'}지도에 찍어드려요!
        </Text>

        <Pressable
          onPress={status === 'idle' ? startScan : undefined}
          style={styles.uploadBox}
        >
          {status === 'idle' ? (
            <>
              <PixelIcon name="camera" size={36} color={COLORS.PRIMARY} />
              <Text style={styles.uploadTitle}>탭해서 스크린샷 올리기</Text>
              <Text style={styles.uploadHint}>인스타그램, 블로그 캡처 이미지를 올려주세요</Text>
            </>
          ) : (
            <View style={styles.previewRow}>
              <View style={styles.previewThumb}>
                <PixelIcon name="camera" size={22} color={COLORS.WHITE} />
              </View>
              <View style={styles.previewInfo}>
                <Text style={styles.previewName} numberOfLines={1}>
                  sns_screenshot.png
                </Text>
                {status === 'scanning' ? (
                  <View style={styles.scanningRow}>
                    <Animated.View style={{ opacity: pulse }}>
                      <PixelIcon name="search" size={16} color={COLORS.SECONDARY} />
                    </Animated.View>
                    <Text style={styles.scanningText}>AI가 장소를 찾고 있어요...</Text>
                  </View>
                ) : (
                  <View style={styles.scanningRow}>
                    <PixelIcon name="check" size={14} color={COLORS.PRIMARY} />
                    <Text style={styles.doneText}>{MOCK_PLACES.length}개 장소를 찾았어요</Text>
                  </View>
                )}
              </View>
            </View>
          )}
        </Pressable>

        {status === 'done' ? (
          <>
            <Text style={styles.sectionTitle}>추출된 장소 ({MOCK_PLACES.length})</Text>

            <Card style={styles.listCardFront}>
              {MOCK_PLACES.map((place, index) => (
                <View
                  key={place.id}
                  style={[
                    styles.placeRow,
                    index < MOCK_PLACES.length - 1 && styles.placeRowDivider,
                  ]}
                >
                  <Pressable
                    onPress={() => toggleChecked(place.id)}
                    hitSlop={8}
                    style={[styles.checkbox, checked[place.id] && styles.checkboxChecked]}
                  >
                    {checked[place.id] ? (
                      <PixelIcon name="check" size={14} color={COLORS.WHITE} />
                    ) : null}
                  </Pressable>

                  <View style={styles.placeInfo}>
                    <View style={styles.placeNameRow}>
                      <Text style={styles.placeName}>{place.name}</Text>
                      <Badge label={place.category} />
                    </View>
                    <Text style={styles.placeAddress} numberOfLines={1}>
                      {place.address}
                    </Text>
                  </View>
                </View>
              ))}
            </Card>

            <Text style={styles.sectionTitle}>보낼 여행 카드</Text>
            <Pressable style={styles.tripPicker} onPress={() => setPickerVisible(true)}>
              <View style={styles.tripPickerText}>
                <Text style={styles.tripPickerTitle}>{selectedTrip.title}</Text>
                <Text style={styles.tripPickerDate}>{selectedTrip.date}</Text>
              </View>
              <PixelIcon name="chevronDown" size={16} color={COLORS.SECONDARY} />
            </Pressable>

            <Pressable onPress={resetScan} hitSlop={8}>
              <Text style={styles.resetText}>다른 스크린샷으로 다시 추출하기</Text>
            </Pressable>
          </>
        ) : null}
      </ScrollView>

      {status === 'done' ? (
        <View style={styles.bottomBar}>
          <Button
            label={
              selectedCount > 0
                ? `선택한 장소 ${selectedCount}개 내 여행 카드로 보내기`
                : '선택한 장소 내 여행 카드로 보내기'
            }
            onPress={() => {}}
            disabled={!canSend}
            fullWidth
          />
        </View>
      ) : null}

      <Modal
        visible={pickerVisible}
        onClose={() => setPickerVisible(false)}
        title="여행 카드 선택"
      >
        {MOCK_TRIPS.map((trip) => (
          <Pressable
            key={trip.id}
            style={[
              styles.tripOption,
              trip.id === selectedTripId && styles.tripOptionSelected,
            ]}
            onPress={() => {
              setSelectedTripId(trip.id);
              setPickerVisible(false);
            }}
          >
            <Text style={styles.tripOptionTitle}>{trip.title}</Text>
            <Text style={styles.tripOptionDate}>{trip.date}</Text>
          </Pressable>
        ))}
      </Modal>
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  screenPadding: {
    paddingBottom: 0,
  },
  scrollContent: {
    paddingBottom: SPACING.lg,
  },
  title: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h1,
    color: COLORS.INK,
  },
  subtitle: {
    marginTop: SPACING.xs,
    marginBottom: SPACING.lg,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
    lineHeight: TYPOGRAPHY.caption + 6,
  },
  uploadBox: {
    minHeight: 120,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    borderStyle: 'dashed',
    borderRadius: RADIUS.lg,
    backgroundColor: COLORS.SURFACE,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: SPACING.lg,
    paddingHorizontal: SPACING.md,
    marginBottom: SPACING.lg,
  },
  uploadTitle: {
    marginTop: SPACING.sm,
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  uploadHint: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
    textAlign: 'center',
  },
  previewRow: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
  },
  previewThumb: {
    width: 56,
    height: 56,
    borderRadius: RADIUS.md,
    backgroundColor: COLORS.PRIMARY,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: SPACING.md,
  },
  previewInfo: {
    flex: 1,
  },
  previewName: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  scanningRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: SPACING.xs,
  },
  scanningText: {
    marginLeft: SPACING.xs,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.SECONDARY,
  },
  doneText: {
    marginLeft: SPACING.xs,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.PRIMARY_DARK,
  },
  sectionTitle: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
    marginBottom: SPACING.sm,
  },
  listCardFront: {
    padding: 0,
    marginBottom: SPACING.lg,
    overflow: 'hidden',
  },
  placeRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: SPACING.md,
  },
  placeRowDivider: {
    borderBottomWidth: BORDER_WIDTH.thin,
    borderBottomColor: COLORS.GRAY300,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: RADIUS.sm,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    backgroundColor: COLORS.SURFACE,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: SPACING.sm,
  },
  checkboxChecked: {
    backgroundColor: COLORS.PRIMARY,
  },
  placeInfo: {
    flex: 1,
  },
  placeNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  placeName: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
    marginRight: SPACING.sm,
  },
  placeAddress: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  tripPicker: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: COLORS.SURFACE,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    borderRadius: RADIUS.md,
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm + 2,
    marginBottom: SPACING.md,
  },
  tripPickerText: {},
  tripPickerTitle: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  tripPickerDate: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  resetText: {
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
    textDecorationLine: 'underline',
    marginBottom: SPACING.md,
  },
  bottomBar: {
    paddingTop: SPACING.sm,
    paddingBottom: SPACING.md,
    borderTopWidth: BORDER_WIDTH.regular,
    borderTopColor: COLORS.INK,
    backgroundColor: COLORS.BACKGROUND,
  },
  tripOption: {
    paddingVertical: SPACING.sm + 2,
    paddingHorizontal: SPACING.sm,
    borderRadius: RADIUS.sm,
    marginBottom: SPACING.xs,
  },
  tripOptionSelected: {
    backgroundColor: COLORS.PRIMARY_LIGHT,
  },
  tripOptionTitle: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  tripOptionDate: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
});

export default ExtractScreen;
