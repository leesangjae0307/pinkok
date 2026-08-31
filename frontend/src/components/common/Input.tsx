import React, { useState } from 'react';
import {
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
  ViewStyle,
} from 'react-native';
import {
  BORDER_WIDTH,
  COLORS,
  FONT_FAMILY,
  RADIUS,
  SPACING,
  TYPOGRAPHY,
} from '../../theme';

interface InputProps extends TextInputProps {
  label?: string;
  error?: string;
  icon?: React.ReactNode;
  containerStyle?: StyleProp<ViewStyle>;
}

const Input = ({ label, error, icon, containerStyle, style, ...rest }: InputProps) => {
  const [focused, setFocused] = useState(false);

  const borderColor = error ? COLORS.ERROR : focused ? COLORS.PRIMARY : COLORS.INK;

  return (
    <View style={[styles.container, containerStyle]}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <View style={[styles.inputRow, { borderColor }]}>
        {icon}
        <TextInput
          placeholderTextColor={COLORS.GRAY500}
          onFocus={(e) => {
            setFocused(true);
            rest.onFocus?.(e);
          }}
          onBlur={(e) => {
            setFocused(false);
            rest.onBlur?.(e);
          }}
          style={[styles.input, icon ? { marginLeft: SPACING.sm } : null, style]}
          {...rest}
        />
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },
  label: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.caption,
    color: COLORS.SECONDARY,
    marginBottom: SPACING.xs,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.SURFACE,
    borderWidth: BORDER_WIDTH.regular,
    borderRadius: RADIUS.md,
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm + 2,
  },
  input: {
    flex: 1,
    fontSize: TYPOGRAPHY.body,
    color: COLORS.INK,
    padding: 0,
  },
  error: {
    fontFamily: FONT_FAMILY.PIXEL,
    fontSize: TYPOGRAPHY.tiny,
    color: COLORS.ERROR,
    marginTop: SPACING.xs,
  },
});

export default Input;
