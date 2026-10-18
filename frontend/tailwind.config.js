/**
 * PINKOK 디자인 토큰 (Figma Make 웹 디자인의 @theme 값을 그대로 옮김)
 * NativeWind v4 + Tailwind v3
 * @type {import('tailwindcss').Config}
 */
module.exports = {
  content: ['./App.{js,jsx,ts,tsx}', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        'mint-primary': '#56EADD',
        'mint-dark': '#50E6DC',
        'mint-deep': '#115E59',
        'mint-wash': '#B4FAF0',
        'mint-soft': '#82F0DC',
        'mint-accent': '#FF5A87',
        'mint-yellow': '#FFC837',
        surface: '#FFFFFF',
        ink: '#1E293B',
        muted: '#64748B',
        line: '#E2E8F0',
        danger: '#EF4444',
        disabled: '#CBD5E1',
        kakao: '#FDE047',
        'kakao-ink': '#0F172A',
        'app-backdrop': '#020617',
        device: '#1E293B',
        'pixel-navy': '#172554',
        'sky-primary': '#38BDF8',
        'sky-soft': '#A5F3FC',
        'sky-border': '#0E7490',
        'auth-canvas': '#BFF2E1',
        cloud: '#FFFFFF',
        'cloud-shadow': '#CFFAFE',
        sun: '#FDE047',
        ocean: '#06B6D4',
        'ocean-light': '#A5F3FC',
        'hill-far': '#14B8A6',
        hill: '#047857',
        sand: '#FDE68A',
        'tree-dark': '#065F46',
        trunk: '#92400E',
      },
      fontFamily: {
        // Press Start 2P (웹과 동일한 픽셀 폰트). 한글은 시스템 폰트로 자동 대체돼요.
        pixel: ['PressStart2P-Regular'],
      },
      borderWidth: {
        3: '3px',
      },
      borderRadius: {
        xs: '2px',
      },
      spacing: {
        6.5: '1.625rem',
        13: '3.25rem',
      },
      flex: {
        2: '2 2 0%',
      },
    },
  },
  plugins: [],
};
