# Figma Make 웹 디자인 → React Native 이관 안내

`Travel_App_UI_Design` (Vite + React + Tailwind 웹 프로토타입)의 **디자인과 기능을 그대로** 이 RN 프로젝트로 옮겼어요.
웹 코드의 Tailwind 클래스는 **NativeWind v4** 덕분에 RN에서도 거의 같은 이름으로 쓰여요.

## 처음 한 번 실행 순서 (Windows)

```bash
cd frontend
npm install
npx react-native run-android   # 새 네이티브 라이브러리가 추가돼서 반드시 다시 빌드해야 해요
```

- 이미 Metro가 켜져 있었다면 끄고 `npx react-native start --reset-cache`로 다시 켜 주세요 (NativeWind CSS 캐시 초기화).
- 폰트(본문 Plus Jakarta Sans + 픽셀 Press Start 2P)는 `android/app/src/main/assets/fonts/`에 미리 넣어 뒀어요. iOS에서는 `npx react-native-asset`을 한 번 실행하면 돼요. 자세한 내용은 `FONT_SETUP.md` 참고.

## 새로 추가된 라이브러리

| 패키지 | 용도 (웹에서 쓰던 것) |
| --- | --- |
| `nativewind` + `tailwindcss@3` | `className="..."` Tailwind 스타일 |
| `react-native-svg` (15.12.1 고정) | `<svg>` 픽셀 아이콘, 지도 일러스트 |
| `react-native-linear-gradient` | `bg-gradient-to-*` |
| `react-native-image-picker` | 추출 탭의 스크린샷 업로드 `<input type="file">` |
| `react-native-video` | PinLog 완성 영상 `<video>` |

> `react-native-svg`는 15.13 이상이 RN 0.79와 codegen 충돌이 나서 15.12.1로 고정했어요.

## 폴더 구조

```
App.tsx                         로그인 여부에 따라 Auth / Main 전환
global.css, tailwind.config.js  NativeWind 설정 (웹 @theme 색상 토큰 그대로)
src/
  assets/images/                웹에서 쓰던 PNG (로고, 탭 아이콘, 아바타, 배경)
  assets/fonts/                 PlusJakartaSans(웹 본문 폰트, 굵기별) + PressStart2P(웹 font-pixel)
  data/mockData.ts              웹 목업 데이터 그대로
  components/pixel/
    PixelIcons.tsx              픽셀 아이콘 세트
    Primitives.tsx              하드 섀도우 버튼, 그라데이션, 반복 애니메이션, 셀렉트, 모달 배경
  features/
    auth/AuthScreens.tsx        스플래시 · 로그인 · 회원가입 · 비밀번호 재설정
    main/
      AppContext.tsx            웹 App.tsx의 모든 상태/핸들러 (useApp()으로 사용)
      MainApp.tsx               토스트 · 화면 영역 · 하단 5탭 · 모달
      tabs/                     홈 · 추출 · 여행(목록/지도) · 핀로그 · 프로필
      screens/                  알림 · 환경설정
      components/               하단 네비, 여행 바텀시트
      modals/                   장소 팀 다이어리, PinLog 합성, 팀원 초대, 아바타/상세/클립
src/api, services, hooks, theme, utils   기존 파일 유지 (백엔드 연동용)
```

이전 화면(`src/features/home|map|ai|trip|mypage|diary`, `src/navigation`, `src/components/common|ui|layout|feedback`)은 새 디자인으로 대체되어 삭제했어요.

## 웹과 다른 점 (RN 한계로 바꾼 부분)

- **블러(backdrop-blur)**: RN에 기본 지원이 없어 반투명 배경으로만 표현했어요.
- **부드러운 그림자(shadow-md 등)**: Android에서는 elevation 근사치라 웹보다 약하게 보여요. 픽셀 하드 섀도우(`shadow-[3px_3px_0_...]`)는 `PixelPressable`/`ShadowBox`로 똑같이 구현했어요.
- **`<select>`** → 누르면 뜨는 `PixelSelect` 선택 모달.
- **`alert()` / `confirm()`** → `Alert.alert`.
- 가짜 상태바(9:41)와 폰 테두리 목업은 실제 기기에서 필요 없어서 뺐어요.
- 카메라 촬영, AI 추출, PinLog 생성은 웹과 똑같이 **시뮬레이션**이에요. 코드에 `POST /files`, `GET /pinlogs/{id}` 같은 API 자리 주석을 그대로 남겨 뒀어요.
- 여행 지도는 웹과 같은 픽셀 일러스트예요. 카카오맵 WebView로 바꿀 때는 `tabs/TripMapView.tsx`의 지도 배경 부분만 교체하면 돼요.

## 백엔드 연결할 때

`features/main/AppContext.tsx`의 핸들러(`handleExtractSubmit`, `handleAcceptInvitation`, `handleSavePlaceRecord` …)가 웹 목업 로직 그대로예요.
이 함수 안의 `setTimeout` 시뮬레이션을 `src/api/*` 호출로 바꾸면 화면은 그대로 둔 채 실제 데이터로 연결돼요.
