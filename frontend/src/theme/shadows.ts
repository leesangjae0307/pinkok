/**
 * 픽셀 아트 UI의 "하드 섀도우"는 흐림(blur) 없이 진한 색 블록을 오른쪽 아래로
 * 오프셋해서 쌓는 방식이 특징입니다 (buttonShadow 등 컴포넌트 참고).
 * 아래 값은 그 오프셋 두께(px)를 정의합니다.
 */
export const SHADOW_OFFSET = {
  sm: 3,
  md: 4,
  lg: 6,
};

// 모달 오버레이처럼 일반적인 그림자가 필요한 곳을 위한 보조 값
export const SOFT_SHADOW = {
  shadowColor: '#000000',
  shadowOffset: { width: 0, height: 4 },
  shadowOpacity: 0.15,
  shadowRadius: 8,
  elevation: 6,
};
