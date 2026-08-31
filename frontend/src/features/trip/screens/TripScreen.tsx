import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Card from '../../../components/common/Card';
import Badge from '../../../components/ui/Badge/Badge';
import { COLORS, FONT_FAMILY, SPACING, TYPOGRAPHY } from '../../../theme';

interface ItineraryItem {
  id: string;
  order: number;
  title: string;
  time: string;
}

const MOCK_ITINERARY: ItineraryItem[] = [
  { id: '1', order: 1, title: '협재 해수욕장', time: '10:00 ~ 11:30' },
  { id: '2', order: 2, title: '카멜리아힐', time: '13:00 ~ 14:30' },
  { id: '3', order: 3, title: '오설록 티뮤지엄', time: '15:00 ~ 16:30' },
  { id: '4', order: 4, title: '성산 일출봉', time: '18:00 ~ 19:30' },
];

const TripScreen = () => {
  return (
    <ScreenLayout>
      <View style={styles.header}>
        <Text style={styles.title}>일정</Text>
        <Badge label="제주도 여행" variant="outline" />
      </View>

      <View style={styles.timeline}>
        {MOCK_ITINERARY.map((item, index) => (
          <View key={item.id} style={styles.timelineRow}>
            <View style={styles.timelineMarkerColumn}>
              <View style={styles.timelineDot}>
                <Text style={styles.timelineDotText}>{item.order}</Text>
              </View>
              {index < MOCK_ITINERARY.length - 1 ? (
                <View style={styles.timelineLine} />
              ) : null}
            </View>

            <Card style={styles.itemCardFront}>
              <Text style={styles.itemTitle}>{item.title}</Text>
              <Text style={styles.itemTime}>{item.time}</Text>
            </Card>
          </View>
        ))}
      </View>
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: SPACING.lg,
  },
  title: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h1,
    color: COLORS.INK,
  },
  timeline: {
    paddingBottom: SPACING.xl,
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
    backgroundColor: COLORS.PRIMARY,
    borderWidth: 2,
    borderColor: COLORS.INK,
    alignItems: 'center',
    justifyContent: 'center',
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
  itemCardFront: {
    flex: 1,
    padding: SPACING.sm + 4,
    marginLeft: SPACING.sm,
    marginBottom: SPACING.lg,
  },
  itemTitle: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  itemTime: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
});

export default TripScreen;
