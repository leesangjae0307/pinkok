import { COLORS } from './colors';
import { FONT_FAMILY, TYPOGRAPHY, LINE_HEIGHT } from './typography';
import { SPACING } from './spacing';
import { RADIUS, BORDER_WIDTH } from './radius';
import { SHADOW_OFFSET, SOFT_SHADOW } from './shadows';

export const theme = {
  COLORS,
  FONT_FAMILY,
  TYPOGRAPHY,
  LINE_HEIGHT,
  SPACING,
  RADIUS,
  BORDER_WIDTH,
  SHADOW_OFFSET,
  SOFT_SHADOW,
};

export type Theme = typeof theme;
