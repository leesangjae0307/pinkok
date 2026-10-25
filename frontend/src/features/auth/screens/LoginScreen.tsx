import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { useNavigation } from '@react-navigation/native';

const LoginScreen = () => {
  const navigation = useNavigation<any>();

  return (
    <View
      style={{
        flex:1,
        justifyContent:'center',
        alignItems:'center'
      }}>
      <Text>Login Screen</Text>
      <TouchableOpacity
        style={{marginTop:16}}
        onPress={() => navigation.navigate('Signup')}>
        <Text style={{color:'#FF6B8B'}}>회원가입 하러 가기</Text>
      </TouchableOpacity>
    </View>
  );
};

export default LoginScreen;
