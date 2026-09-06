import React from 'react';
import {
  Modal as RNModal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../theme';
import PixelIcon from '../ui/PixelIcon/PixelIcon';

interface ModalProps {
  visible: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
}

const Modal = ({ visible, onClose, title, children }: ModalProps) => {
  return (
    <RNModal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          {title || onClose ? (
            <View style={styles.header}>
              <Text style={styles.title}>{title}</Text>
              <Pressable onPress={onClose} hitSlop={8}>
                <PixelIcon name="close" size={16} color={COLORS.SECONDARY} />
              </Pressable>
            </View>
          ) : null}
          <View>{children}</View>
        </View>
      </View>
    </RNModal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(26, 34, 56, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: SPACING.lg,
  },
  sheet: {
    width: '100%',
    backgroundColor: COLORS.SURFACE,
    borderWidth: BORDER_WIDTH.thick,
    borderColor: COLORS.INK,
    borderRadius: RADIUS.lg,
    padding: SPACING.lg,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.md,
  },
  title: {
    fontFamily: FONT_FAMILY.PIXEL_BOLD,
    fontSize: TYPOGRAPHY.h3,
    color: COLORS.INK,
  },
});

export default Modal;
