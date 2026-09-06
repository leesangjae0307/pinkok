# 픽셀 폰트(Galmuri) 적용하기

`src/assets/fonts/`에 무료 한글 도트 폰트 **Galmuri**(SIL OFL 1.1 라이선스, 상업적 이용 무료 — https://github.com/quiple/galmuri)를 3개 굵기로 넣어뒀어요.

- `Galmuri11-Regular.ttf` — 본문/버튼용 (테마의 `FONT_FAMILY.PIXEL`)
- `Galmuri11-Bold.ttf` — 로고/제목용 (`FONT_FAMILY.PIXEL_BOLD`)
- `Galmuri7-Regular.ttf` — 아주 작은 배지/캡션용 (`FONT_FAMILY.PIXEL_SMALL`)

RN은 새 폰트 파일을 넣는 것만으로는 적용되지 않고, **네이티브 프로젝트에 링크 + 재빌드**가 한 번 필요해요. 아래 순서대로 터미널(VS Code 등)에서 실행해주세요.

## 1. 패키지 설치 확인

이미 설치돼 있다면 건너뛰어도 돼요.

```bash
cd frontend
npm install
```

## 2. 폰트 링크

`react-native.config.js`에 폰트 폴더(`src/assets/fonts`)를 이미 등록해뒀어요. 아래 명령으로 iOS/Android에 실제로 복사·등록합니다.

```bash
npx react-native-asset
```

- Android: `android/app/src/main/assets/fonts/`에 폰트 파일이 복사돼요.
- iOS: `Info.plist`의 `UIAppFonts`에 폰트가 자동으로 추가돼요.

(`react-native-asset`이 없다는 오류가 나면 `npm install react-native-asset --save-dev`로 한 번 설치한 뒤 다시 실행해주세요.)

## 3. 앱 재빌드

폰트 링크는 JS 핫 리로드로는 반영되지 않고, **네이티브 앱을 다시 빌드**해야 보여요.

```bash
# Android
npx react-native run-android

# iOS (Mac 전용, 최초 1회는 pod install도 필요해요)
cd ios && pod install && cd ..
npx react-native run-ios
```

## 확인

앱을 켰을 때 "PINKOK" 로고나 버튼 글씨가 도트(픽셀) 폰트로 보이면 성공이에요. 만약 여전히 기본 시스템 폰트로 보인다면:

1. `npx react-native-asset` 실행 여부 확인
2. (Android) `android/app/src/main/assets/fonts/` 폴더에 `.ttf` 3개가 실제로 들어있는지 확인
3. 앱을 완전히 삭제 후 다시 빌드(캐시 문제일 수 있어요)
