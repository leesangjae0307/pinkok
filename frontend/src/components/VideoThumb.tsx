/**
 * 영상 대표 이미지. 서버가 따로 썸네일을 주지 않으면(thumbnail 이 영상 주소이거나 비어 있음)
 * 일시정지한 영상의 첫 프레임을 대신 보여준다. 현재 서버 API 로는 대표 프레임을 고르거나
 * 저장할 수 없어서 이 대체 방식을 쓴다.
 */
import React from 'react';
import { Image, StyleSheet, View } from 'react-native';
import Video from 'react-native-video';

const VIDEO_EXT = /\.(mp4|mov|m4v|webm|3gp|mkv)(\?|#|$)/i;

export default function VideoThumb({
  thumbnail,
  videoUrl,
}: {
  thumbnail?: string;
  videoUrl?: string;
}) {
  const hasImage = !!thumbnail && !VIDEO_EXT.test(thumbnail);
  const frameSource = videoUrl ?? thumbnail;
  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill} className="bg-black">
      {hasImage ? (
        <Image source={{ uri: thumbnail }} resizeMode="cover" style={StyleSheet.absoluteFill} />
      ) : frameSource ? (
        <Video source={{ uri: frameSource }} paused muted resizeMode="cover" style={StyleSheet.absoluteFill} />
      ) : null}
    </View>
  );
}
