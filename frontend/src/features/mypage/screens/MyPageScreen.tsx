import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Card from '../../../components/common/Card';
import Avatar from '../../../components/ui/Avatar/Avatar';
import Divider from '../../../components/ui/Divider/Divider';
import PixelIcon from '../../../components/ui/PixelIcon/PixelIcon';
import { COLORS, FONT_FAMILY, SPACING, TYPOGRAPHY } from '../../../theme';

const STATS = [
  { label: '여행', value: 12 },
  { label: '장소', value: 87 },
  { label: '메모', value: 156 },
];

const RECENT_TRIPS = [
  { id: '1', title: '제주도 여행', date: '2025.08.01 ~ 08.05' },
  { id: '2', title: '도쿄 여행', date: '2025.07.10 ~ 07.14' },
  { id: '3', title: '유럽 배낭여행', date: '2024.12.20 ~ 2025.01.05' },
];

const MyPageScreen = () => {
  return (
    <ScreenLayout scroll>
      <View style={styles.headerRow}>
        <Text style={styles.title}>내 프로필</Text>
        <Pressable hitSlop={8}>
          <PixelIcon name="user" size={18} color={COLORS.SECONDARY} />
        </Pressable>
      </View>

      <View style={styles.profileRow}>
        <Avatar name="감지은" size={64} />
        <View style={styles.profileText}>
          <Text style={styles.name}>감지은</Text>
          <Text style={styles.handle}>@gamjieun</Text>
        </View>
      </View>

      <Card style={styles.statsCardFront}>
        <View style={styles.statsRow}>
          {STATS.map((stat, i) => (
            <React.Fragment key={stat.label}>
              <View style={styles.statItem}>
                <Text style={styles.statValue}>{stat.value}</Text>
                <Text style={styles.statLabel}>{stat.label}</Text>
              </View>
              {i < STATS.length - 1 ? <View style={styles.statDivider} /> : null}
            </React.Fragment>
          ))}
        </View>
      </Card>

      <Text style={styles.sectionTitle}>최근 여행</Text>
      {RECENT_TRIPS.map((trip) => (
        <Card key={trip.id} onPress={() => {}} style={styles.tripCardFront}>
          <View style={styles.tripRow}>
            <View style={styles.tripThumb}>
              <PixelIcon name="pin" size={20} color={COLORS.WHITE} />
            </View>
            <View style={styles.tripText}>
              <Text style={styles.tripTitle}>{trip.title}</Text>
              <Text style={styles.tripDate}>{trip.date}</Text>
            </View>
          </View>
        </Card>
      ))}

      <Divider dashed style={styles.divider} />

      <Pressable style={styles.logoutRow}>
        <Text style={styles.logoutText}>로그아웃</Text>
      </Pressable>
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  headerRow: {
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
  profileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: SPACING.lg,
  },
  profileText: {
    marginLeft: SPACING.md,
  },
  name: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
  },
  handle: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
  statsCardFront: {
    paddingVertical: SPACING.md,
    marginBottom: SPACING.xl,
  },
  statsRow: {
    flexDirection: 'row',
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statDivider: {
    width: 1.5,
    backgroundColor: COLORS.GRAY300,
  },
  statValue: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h2,
    color: COLORS.PRIMARY_DARK,
  },
  statLabel: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.GRAY500,
  },
  sectionTitle: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
    marginBottom: SPACING.md,
  },
  tripCardFront: {
    padding: SPACING.sm + 4,
    marginBottom: SPACING.md,
  },
  tripRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  tripThumb: {
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: COLORS.PRIMARY,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: SPACING.sm,
  },
  tripText: {
    flex: 1,
  },
  tripTitle: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
  },
  tripDate: {
    marginTop: 2,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.GRAY500,
  },
  divider: {
    marginTop: SPACING.lg,
  },
  logoutRow: {
    alignItems: 'center',
    paddingVertical: SPACING.md,
  },
  logoutText: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.ERROR,
  },
});

export default MyPageScreen;
