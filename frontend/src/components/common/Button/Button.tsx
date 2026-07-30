import React from 'react';
import {
  TouchableOpacity,
  Text,
  ActivityIndicator,
  GestureResponderEvent,
} from 'react-native';

interface ButtonProps {
  title: string;
  onPress?: (event: GestureResponderEvent) => void;
  disabled?: boolean;
  loading?: boolean;
}

const Button = ({
  title,
  onPress,
  disabled = false,
  loading = false,
}: ButtonProps) => {
  return (
    <TouchableOpacity
      style={{
        backgroundColor: 'red',
        padding: 20,
      }}
      onPress={onPress}
      disabled={disabled || loading}>
      {loading ? (
        <ActivityIndicator color="white" />
      ) : (
        <Text style={{color: 'white', fontSize: 20}}>
          {title}
        </Text>
      )}
    </TouchableOpacity>
  );
};

export default Button;