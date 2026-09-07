import React, { useEffect, useRef, useState } from 'react';
import { Animated, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Card from '../../../components/common/Card';
import Button from '../../../components/common/Button';
import Input from '../../../components/common/Input';
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

type VlogStatus = 'idle' | 'composing' | 'done';

interface PlaceItem {
  id: string;
  order: number;
  name: string;
}

interface PlaceRecord {
  photo: boolean;
  clip: boolean;
  memo: string;
}

// TODO: 실제 여행/장소 데이터 연동 전까지 쓰는 목업 데이터
const MOCK_PLACES: PlaceItem[] = [
  { id: '1', order: 1, name: '협재 해수욕장' },
  { id: '2', order: 2, name: '카멜리아힐' },
  { id: '3', order: 3, name: '오설록 티뮤지엄' },
  { id: '4', order: 4, name: '성산 일출봉' },
];

const INITIAL_RECORDS: Record<string, PlaceRecord> = {
  '1': { photo: true, clip: true, memo: '노을이 진짜 예뻤다' },
  '2': { photo: true, clip: true, memo: '핑크뮬리 인생샷 성공' },
  '3': { photo: false, clip: false, memo: '' },
  '4': { photo: false, clip: false, memo: '' },
};

const COMPOSE_DURATION_MS = 1600;

const isRecordComplete = (record: PlaceRecord) => record.photo && record.clip;

/**
 * PinLog 탭
 * 셋로그(Set-Log)에서 영감을 받아, 여행 중 각 장소에서 남긴
 * 사진 + 메모 + 2~5초 영상 클립을 동선 순서대로 자동 합성해
 * 하나의 미니 여행 브이로그를 만드는 화면.
 * 실제 카메라 촬영/영상 합성 연동 전까지는, 기록 여부를 토글하는
 * 목업 데이터로 "기록 → 합성 중 → 완성" 흐름만 프런트에서 시뮬레이션해요.
 */
const PinLogScreen = () => {
  const [records, setRecords] = useState<Record<string, PlaceRecord>>(INITIAL_RECORDS);
  const [vlogStatus, setVlogStatus] = useState<VlogStatus>('idle');

  const [activePlaceId, setActivePlaceId] = useState<string | null>(null);
  const [draftPhoto, setDraftPhoto] = useState(false);
  const [draftClip, setDraftClip] = useState(false);
  const [draftMemo, setDraftMemo] = useState('');

  const pulse = useRef(new Animated.Value(0.4)).current;
  const composeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (vlogStatus !== 'composing') {
      return;
    }
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 450, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0.4, duration: 450, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [vlogStatus, pulse]);

  useEffect(() => {
    return () => {
      if (composeTimer.current) {
        clearTimeout(composeTimer.current);
      }
    };
  }, []);

  const openRecordModal = (place: PlaceItem) => {
    const record = records[place.id];
    setDraftPhoto(record.photo);
    setDraftClip(record.clip);
    setDraftMemo(record.memo);
    setActivePlaceId(place.id);
  };

  const saveRecord = () => {
    if (!activePlaceId) {
      return;
    }
    setRecords((prev) => ({
      ...prev,
      [activePlaceId]: { photo: draftPhoto, clip: draftClip, memo: draftMemo },
    }));
    setActivePlaceId(null);
  };

  const startCompose = () => {
    setVlogStatus('composing');
    composeTimer.current = setTimeout(() => {
      setVlogStatus('done');
    }, COMPOSE_DURATION_MS);
  };

  const backToTimeline = () => {
    if (composeTimer.current) {
      clearTimeout(composeTimer.current);
    }
    setVlogStatus('idle');
  };

  const completedCount = MOCK_PLACES.filter((place) => isRecordComplete(records[place.id])).length;
  const allComplete = completedCount === MOCK_PLACES.length;
  const activePlace = MOCK_PLACES.find((place) => place.id === activePlaceId) ?? null;
  const routeText = MOCK_PLACES.map((place) => place.name).join(' → ');

  return (
    <ScreenLayout style={styles.screenPadding}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>PinLog</Text>
        <Text style={styles.subtitle}>
          장소마다 남긴 사진·메모·짧은 영상을{'\n'}동선 순서대로 이어 붙여 미니 브이로그를 만들어요
        </Text>

        <View style={styles.tripRow}>
          <Badge label="제주도 여행" variant="outline" />
          <Text style={styles.progressText}>{completedCount}/{MOCK_PLACES.length}개 장소 기록 완료</Text>
        </View>

        {vlogStatus === 'done' ? (
          <>
            <Card style={styles.resultCardFront}>
              <View style={styles.thumb}>
                <View style={styles.playButton}>
                  <PixelIcon name="play" size={22} color={COLORS.WHITE} />
                </View>
                <Text style={styles.thumbDuration}>00:42</Text>
              </View>
              <Text style={styles.resultTitle}>제주도 여행 브이로그</Text>
              <Text style={styles.resultRoute}>{routeText}</Text>
            </Card>

            <View style={styles.resultActions}>
              <Button
                label="타임라인으로 돌아가기"
                variant="outline"
                onPress={backToTimeline}
                fullWidth
                style={styles.resultActionButton}
              />
              <Button label="공유하기" onPress={() => {}} fullWidth />
            </View>
          </>
        ) : vlogStatus === 'composing' ? (
          <Card style={styles.composingCardFront}>
            <Animated.View style={{ opacity: pulse }}>
              <PixelIcon name="sparkle" size={40} color={COLORS.SECONDARY} />
            </Animated.View>
            <Text style={styles.composingTitle}>영상을 순서대로 합치고 있어요...</Text>
            <Text style={styles.composingRoute}>{routeText}</Text>
          </Card>
        ) : (
          <>
            <View style={styles.timeline}>
              {MOCK_PLACES.map((place, index) => {
                const record = records[place.id];
                const complete = isRecordComplete(record);

                return (
                  <View key={place.id} style={styles.timelineRow}>
                    <View style={styles.timelineMarkerColumn}>
                      <View style={[styles.timelineDot, complete && styles.timelineDotComplete]}>
                        <Text style={styles.timelineDotText}>{place.order}</Text>
                      </View>
                      {index < MOCK_PLACES.length - 1 ? <View style={styles.timelineLine} /> : null}
                    </View>

                    <Pressable
                      onPress={() => openRecordModal(place)}
                      style={styles.placeCardWrapper}
                    >
                      <Card style={styles.placeCardFront}>
                        <View style={styles.placeCardHeader}>
                          <Text style={styles.placeName}>{place.name}</Text>
                          {complete ? (
                            <View style={styles.completeBadge}>
                              <PixelIcon name="check" size={12} color={COLORS.WHITE} />
                            </View>
                          ) : (
                            <View style={styles.incompleteBadge}>
                              <PixelIcon name="plus" size={12} color={COLORS.GRAY500} />
                            </View>
                          )}
                        </View>
                        {complete ? (
                          <>
                            <Text style={styles.placeMemo} numberOfLines={1}>
                              {record.memo || '메모 없음'}
                            </Text>
                            <View style={styles.placeTagsRow}>
                              <Badge label="사진" icon={<PixelIcon name="camera" size={10} color={COLORS.SECONDARY} />} />
                              <Badge
                                label="영상 클립"
                                icon={<PixelIcon name="play" size={10} color={COLORS.SECONDARY} />}
                                style={styles.placeTagSpacing}
                              />
                            </View>
                          </>
                        ) : (
                          <Text style={styles.placeHint}>탭해서 사진 · 메모 · 영상 기록하기</Text>
                        )}
                      </Card>
                    </Pressable>
                  </View>
                );
              })}
            </View>
          </>
        )}
      </ScrollView>

      {vlogStatus === 'idle' ? (
        <View style={styles.bottomBar}>
          <Button
            label={allComplete ? '브이로그 만들기' : `모든 장소를 기록하면 만들 수 있어요 (${completedCount}/${MOCK_PLACES.length})`}
            onPress={startCompose}
            disabled={!allComplete}
            fullWidth
          />
        </View>
      ) : null}

      <Modal
        visible={activePlace !== null}
        onClose={() => setActivePlaceId(null)}
        title={activePlace ? `${activePlace.name} 기록하기` : ''}
      >
        <Pressable
          style={[styles.toggleRow, draftPhoto && styles.toggleRowActive]}
          onPress={() => setDraftPhoto((prev) => !prev)}
        >
          <View style={[styles.toggleCheckbox, draftPhoto && styles.toggleCheckboxActive]}>
            {draftPhoto ? <PixelIcon name="check" size={12} color={COLORS.WHITE} /> : null}
          </View>
          <PixelIcon name="camera" size={18} color={COLORS.SECONDARY} />
          <Text style={styles.toggleLabel}>사진 추가하기</Text>
        </Pressable>

        <Pressable
          style={[styles.toggleRow, draftClip && styles.toggleRowActive]}
          onPress={() => setDraftClip((prev) => !prev)}
        >
          <View style={[styles.toggleCheckbox, draftClip && styles.toggleCheckboxActive]}>
            {draftClip ? <PixelIcon name="check" size={12} color={COLORS.WHITE} /> : null}
          </View>
          <PixelIcon name="play" size={18} color={COLORS.SECONDARY} />
          <Text style={styles.toggleLabel}>2~5초 영상 촬영하기</Text>
        </Pressable>

        <Input
          label="메모"
          placeholder="이 장소에서의 기억을 남겨보세요"
          value={draftMemo}
          onChangeText={setDraftMemo}
          multiline
          containerStyle={styles.memoInput}
        />

        <Button
          label="저장하기"
          onPress={saveRecord}
          disabled={!draftPhoto || !draftClip}
          fullWidth
        />
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
  tripRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: SPACING.lg,
  },
  progressText: {
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  timeline: {
    paddingBottom: SPACING.md,
  },
  timelineRow: {
    flexDirection: 'row',
  },
  timelineMarkerColumn: {
    alignItems: 'center',
    width: 32,
  },
  timelineDot: {
    width: 24,
    height: 24,
    borderRadius: 8,
    backgroundColor: COLORS.GRAY300,
    borderWidth: 2,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
  },
  timelineDotComplete: {
    backgroundColor: COLORS.PRIMARY,
  },
  timelineDotText: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.WHITE,
  },
  timelineLine: {
    flex: 1,
    width: 2,
    backgroundColor: COLORS.PRIMARY_LIGHT,
    marginVertical: 4,
  },
  placeCardWrapper: {
    flex: 1,
    marginLeft: SPACING.sm,
    marginBottom: SPACING.lg,
  },
  placeCardFront: {
    padding: SPACING.sm + 4,
  },
  placeCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  placeName: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  completeBadge: {
    width: 20,
    height: 20,
    borderRadius: RADIUS.sm,
    backgroundColor: COLORS.PRIMARY,
    alignItems: 'center',
    justifyContent: 'center',
  },
  incompleteBadge: {
    width: 20,
    height: 20,
    borderRadius: RADIUS.sm,
    borderWidth: BORDER_WIDTH.thin,
    borderColor: COLORS.GRAY300,
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeMemo: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
  placeTagsRow: {
    flexDirection: 'row',
    marginTop: SPACING.sm,
  },
  placeTagSpacing: {
    marginLeft: SPACING.xs,
  },
  placeHint: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  bottomBar: {
    paddingTop: SPACING.sm,
    paddingBottom: SPACING.md,
    borderTopWidth: BORDER_WIDTH.regular,
    borderTopColor: COLORS.INK,
    backgroundColor: COLORS.BACKGROUND,
  },
  composingCardFront: {
    alignItems: 'center',
    paddingVertical: SPACING.xxl,
    paddingHorizontal: SPACING.lg,
  },
  composingTitle: {
    marginTop: SPACING.md,
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  composingRoute: {
    marginTop: SPACING.sm,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
    textAlign: 'center',
  },
  resultCardFront: {
    padding: SPACING.md,
    marginBottom: SPACING.lg,
  },
  thumb: {
    height: 160,
    borderRadius: RADIUS.md,
    backgroundColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: SPACING.md,
  },
  playButton: {
    width: 52,
    height: 52,
    borderRadius: RADIUS.pill,
    backgroundColor: COLORS.PRIMARY,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.WHITE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  thumbDuration: {
    position: 'absolute',
    right: SPACING.sm,
    bottom: SPACING.sm,
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.WHITE,
  },
  resultTitle: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
  },
  resultRoute: {
    marginTop: SPACING.xs,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  resultActions: {
    marginBottom: SPACING.lg,
  },
  resultActionButton: {
    marginBottom: SPACING.sm,
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.SURFACE,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    borderRadius: RADIUS.md,
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm + 2,
    marginBottom: SPACING.sm,
  },
  toggleRowActive: {
    backgroundColor: COLORS.PRIMARY_LIGHT,
  },
  toggleCheckbox: {
    width: 22,
    height: 22,
    borderRadius: RADIUS.sm,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
    backgroundColor: COLORS.SURFACE,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: SPACING.sm,
  },
  toggleCheckboxActive: {
    backgroundColor: COLORS.PRIMARY,
  },
  toggleLabel: {
    marginLeft: SPACING.sm,
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  memoInput: {
    marginBottom: SPACING.md,
  },
});

export default PinLogScreen;
