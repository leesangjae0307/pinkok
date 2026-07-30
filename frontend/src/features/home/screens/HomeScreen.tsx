import React from 'react';
import {View} from 'react-native';
import Button from '@/components/common/Button';

const HomeScreen = () => {
  return (
    <View
      style={{
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
      }}>
      <Button
        title="테스트 버튼"
        onPress={() => console.log('버튼 클릭')}
      />
    </View>
  );
};

export default HomeScreen;