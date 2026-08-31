import React from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Card from '../../../components/common/Card';
import Badge from '../../../components/ui/Badge/Badge';
import PixelIcon from '../../../components/ui/PixelIcon/PixelIcon';
import EmptyState from '../../../components/feedback/EmptyState';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../../theme';

interface Trip {
  id: string;
  title: string;
  startDate: string;
  endDate: string;
  days: number;
  places: number;
  liked: boolean;
  thumbColor: string;
}

// TODO: 백엔드 연동 전까지 쓰는 임시 데이터
const MOCK_TRIPS: Trip[] = [
  {
    id: '1',
    title: '제주도 여행',
    startDate: '2025.08.01',
    endDate: '2025.08.05',
    days: 5,
    places: 12,
    liked: true,
    thumbColor: COLORS.PRIMARY,
  },
  {
    id: '2',
    title: '도쿄 여행',
    startDate: '2025.07.10',
    endDate: '2025.07.14',
    days: 4,
    places: 8,
    liked: false,
    thumbColor: COLORS.ACCENT_PINK,
  },
  {
    id: '3',
    title: '유럽 배낭여행',
    startDate: '2024.12.20',
    endDate: '2025.01.05',
    days: 17,
    places: 23,
    liked: false,
    thumbColor: COLORS.SECONDARY,
  },
];

const renderSeparator = () => <View style={{ height: SPACING.lg }} />;

const TripCard = ({ trip }: { trip: Trip }) => {
  return (
    <Card onPress={() => {}} style={styles.tripCardFront}>
      <View style={styles.tripCardRow}>
        <View style={[styles.thumb, { backgroundColor: trip.thumbColor }]}>
          <PixelIcon name="pin" size={26} color={COLORS.WHITE} />
        </View>

        <View style={styles.tripInfo}>
          <View style={styles.tripTitleRow}>
            <Text style={styles.tripTitle} numberOfLines={1}>
              {trip.title}
            </Text>
            <PixelIcon
              name="heart"
              size={16}
              color={trip.liked ? COLORS.ACCENT_PINK : COLORS.GRAY300}
            />
          </View>
          <Text style={styles.tripDate}>
            {trip.startDate} ~ {trip.endDate}
          </Text>
          <View style={styles.badgeRow}>
            <Badge label={`${trip.days} days`} />
            <Badge label={`${trip.places} places`} style={styles.badgeGap} />
          </View>
        </View>
      </View>
    </Card>
  );
};

const HomeScreen = () => {
  return (
    <ScreenLayout>
      <View style={styles.header}>
        <Text style={styles.logo}>PINKOK</Text>
        <View style={styles.headerIcons}>
          <Pressable hitSlop={8} style={styles.headerIconButton}>
            <PixelIcon name="bell" size={18} color={COLORS.SECONDARY} />
          </Pressable>
          <Pressable hitSlop={8} style={[styles.headerIconButton, styles.addButton]}>
            <PixelIcon name="plus" size={16} color={COLORS.WHITE} />
          </Pressable>
        </View>
      </View>

      <Text style={styles.sectionTitle}>내 여행</Text>

      <FlatList
        data={MOCK_TRIPS}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => <TripCard trip={item} />}
        ItemSeparatorComponent={renderSeparator}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <EmptyState
            icon="bag"
            title="아직 등록된 여행이 없어요"
            description="첫 여행을 계획하고 핀을 꽂아보세요"
            actionLabel="여행 만들기"
          />
        }
      />
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
  logo: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h1,
    color: COLORS.SECONDARY,
    letterSpacing: 1,
  },
  headerIcons: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: SPACING.sm,
  },
  headerIconButton: {
    width: 36,
    height: 36,
    borderRadius: RADIUS.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addButton: {
    backgroundColor: COLORS.PRIMARY,
    borderWidth: BORDER_WIDTH.regular,
    borderColor: COLORS.INK,
  },
  sectionTitle: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
    marginBottom: SPACING.md,
  },
  list: {
    paddingBottom: SPACING.xl,
  },
  tripCardFront: {
    padding: SPACING.md,
  },
  tripCardRow: {
    flexDirection: 'row',
  },
  thumb: {
    width: 64,
    height: 64,
    borderRadius: RADIUS.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: SPACING.md,
  },
  tripInfo: {
    flex: 1,
    justifyContent: 'center',
  },
  tripTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  tripTitle: {
    flex: 1,
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
    marginRight: SPACING.sm,
  },
  tripDate: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
  badgeRow: {
    flexDirection: 'row',
    marginTop: SPACING.xs,
  },
  badgeGap: {
    marginLeft: SPACING.xs,
  },
});

export default HomeScreen;
